package com.memoria.meaningoflife.ui.timeline.free

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.data.database.timeline.CourseEntity
import com.memoria.meaningoflife.data.database.timeline.TimelineEventEntity
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

class LiTimelineDayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class CourseBlock(val rect: RectF, val course: CourseEntity)
    private data class EventBlock(val rect: RectF, val event: TimelineEventEntity)

    private val courseBlocks = mutableListOf<CourseBlock>()
    private val eventBlocks = mutableListOf<EventBlock>()

    private var courses: List<CourseEntity> = emptyList()
    private var events: List<TimelineEventEntity> = emptyList()

    private val density = resources.displayMetrics.density
    private val hourHeight = 76f * density
    private val axisTopPadding = 24f * density
    private val axisBottomPadding = 24f * density
    private val laneGap = 14f * density
    private val blockRadius = 10f * density
    private val textPad = 8f * density

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.nd_border_visible)
        strokeWidth = 2f
    }
    private val lxgwTypeface = ResourcesCompat.getFont(context, R.font.lxgw) ?: android.graphics.Typeface.DEFAULT
    private val coursePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.nd_surface_raised)
    }
    private val eventPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resolvePrimaryColor().let { Color.argb(42, Color.red(it), Color.green(it), Color.blue(it)) }
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = ContextCompat.getColor(context, R.color.nd_border_visible)
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.nd_text_primary)
        textSize = 13f * density
        typeface = lxgwTypeface
    }
    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.nd_text_secondary)
        textSize = 11f * density
        typeface = lxgwTypeface
    }
    private val hourLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.nd_text_secondary)
        textSize = 10f * density
        typeface = lxgwTypeface
    }
    private val dragHintBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(210, 20, 20, 20)
    }
    private val dragHintTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 12f * density
        typeface = lxgwTypeface
    }

    private var onCourseClick: ((CourseEntity) -> Unit)? = null
    private var onEventClick: ((TimelineEventEntity) -> Unit)? = null
    private var onEventDragChanged: ((TimelineEventEntity, Long, Long) -> Unit)? = null

    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private var pendingLongPress = false
    private var dragging = false
    private var downX = 0f
    private var downY = 0f
    private var draggedEventBlock: EventBlock? = null
    private var draggedRect: RectF? = null
    private var draggedDurationMinutes = 60
    private var dragTouchOffsetY = 0f
    private var downInEvent = false
    private var dragHintText: String? = null

    private val longPressRunnable = Runnable {
        if (pendingLongPress && draggedEventBlock != null) {
            dragging = true
            parent?.requestDisallowInterceptTouchEvent(true)
            draggedRect = RectF(draggedEventBlock!!.rect)
            dragTouchOffsetY = (downY - draggedRect!!.top).coerceAtLeast(0f)
            updateDragHintText()
            invalidate()
        }
    }

    fun setData(courses: List<CourseEntity>, events: List<TimelineEventEntity>) {
        this.courses = courses
        this.events = events
        requestLayout()
        invalidate()
    }

    fun setOnCourseClickListener(listener: (CourseEntity) -> Unit) {
        onCourseClick = listener
    }

    fun setOnEventClickListener(listener: (TimelineEventEntity) -> Unit) {
        onEventClick = listener
    }

    fun setOnEventDragChangedListener(listener: (TimelineEventEntity, Long, Long) -> Unit) {
        onEventDragChanged = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val desiredHeight = (24f * hourHeight + axisTopPadding + axisBottomPadding).toInt()
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(width, max(height, desiredHeight))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        courseBlocks.clear()
        eventBlocks.clear()

        val centerX = width / 2f
        val axisTop = axisTopPadding
        val axisBottom = height - axisBottomPadding
        canvas.drawLine(centerX, axisTop, centerX, axisBottom, axisPaint)

        drawHourTicks(canvas, centerX, axisTop)

        val leftLaneLeft = 12f * density
        val leftLaneRight = centerX - laneGap
        val rightLaneLeft = centerX + laneGap
        val rightLaneRight = width - 12f * density

        courses.forEach { course ->
            val startMinute = toMinuteOfDayOffset(course.startTime)
            val endMinuteRaw = toMinuteOfDayOffset(course.endTime)
            val endMinute = if (endMinuteRaw <= startMinute) startMinute + 30 else endMinuteRaw
            val rect = buildRectByMinutes(leftLaneLeft, leftLaneRight, startMinute, endMinute)
            drawBlock(canvas, rect, course.name)
            courseBlocks.add(CourseBlock(rect, course))
        }

        events.forEach { event ->
            val start = toMinuteOfAbsoluteTime(event.startTime)
            val end = toMinuteOfAbsoluteTime(event.endTime ?: (event.startTime + 60 * 60 * 1000L))
            val rect = buildRectByMinutes(rightLaneLeft, rightLaneRight, start, end)
            drawBlock(canvas, rect, event.title, isEvent = true)
            eventBlocks.add(EventBlock(rect, event))
        }

        if (dragging && draggedRect != null && draggedEventBlock != null) {
            drawBlock(canvas, draggedRect!!, draggedEventBlock!!.event.title, isEvent = true)
            drawDragHint(canvas, draggedRect!!, dragHintText.orEmpty())
        }

        // 将整点文字放在最上层，避免被时间块覆盖。
        drawHourLabels(canvas, centerX, axisTop)
    }

    private fun drawDragHint(canvas: Canvas, rect: RectF, text: String) {
        if (text.isBlank()) return
        val textWidth = dragHintTextPaint.measureText(text)
        val padH = 8f * density
        val padV = 5f * density
        val hintWidth = textWidth + padH * 2
        val hintHeight = dragHintTextPaint.textSize + padV * 2
        val left = (rect.centerX() - hintWidth / 2f).coerceIn(8f * density, width - hintWidth - 8f * density)
        val top = (rect.top - hintHeight - 6f * density).coerceAtLeast(axisTopPadding)
        val hintRect = RectF(left, top, left + hintWidth, top + hintHeight)
        canvas.drawRoundRect(hintRect, 8f * density, 8f * density, dragHintBgPaint)
        canvas.drawText(text, hintRect.left + padH, hintRect.bottom - padV, dragHintTextPaint)
    }

    private fun drawHourTicks(canvas: Canvas, centerX: Float, axisTop: Float) {
        for (hour in 0..24) {
            val y = axisTop + hour * hourHeight
            canvas.drawLine(centerX - 6f * density, y, centerX + 6f * density, y, axisPaint)
        }
    }

    private fun drawHourLabels(canvas: Canvas, centerX: Float, axisTop: Float) {
        for (hour in 0..24) {
            val y = axisTop + hour * hourHeight
            val label = if (hour == 24) "24:00" else String.format(Locale.getDefault(), "%02d:00", hour)
            hourLabelPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(label, centerX - 10f * density, y - 2f * density, hourLabelPaint)
        }
    }

    private fun buildRectByMinutes(left: Float, right: Float, startMinute: Int, endMinuteInput: Int): RectF {
        val endMinuteRaw = endMinuteInput
        val endMinute = if (endMinuteRaw <= startMinute) startMinute + 30 else endMinuteRaw

        val top = axisTopPadding + (startMinute / 60f) * hourHeight
        val bottom = axisTopPadding + (endMinute / 60f) * hourHeight
        val minBottom = top + 54f * density
        return RectF(left, top, right, max(bottom, minBottom))
    }

    private fun drawBlock(canvas: Canvas, rect: RectF, title: String, isEvent: Boolean = false) {
        val fillPaint = if (isEvent) eventPaint else coursePaint
        canvas.drawRoundRect(rect, blockRadius, blockRadius, fillPaint)
        canvas.drawRoundRect(rect, blockRadius, blockRadius, strokePaint)

        val startMinute = ((rect.top - axisTopPadding) / hourHeight * 60f).toInt().coerceIn(0, 23 * 60 + 59)
        val endMinute = ((rect.bottom - axisTopPadding) / hourHeight * 60f).toInt().coerceIn(1, 24 * 60)

        val startText = minuteToText(startMinute)
        val endText = minuteToText(endMinute)

        val textX = rect.left + textPad
        val titleY = rect.top + textPad + 16f * density
        val startY = rect.top + textPad + 11f * density
        val endY = rect.bottom - textPad

        canvas.drawText(startText, textX, startY, timePaint)
        canvas.drawText(title, textX, titleY + 15f * density, titlePaint)
        canvas.drawText(endText, textX, endY, timePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                downInEvent = false
                draggedEventBlock = eventBlocks.lastOrNull { it.rect.contains(event.x, event.y) }
                if (draggedEventBlock != null) {
                    downInEvent = true
                            val start = toMinuteOfAbsoluteTime(draggedEventBlock!!.event.startTime)
                            val end = toMinuteOfAbsoluteTime(draggedEventBlock!!.event.endTime ?: (draggedEventBlock!!.event.startTime + 60 * 60 * 1000L))
                    draggedDurationMinutes = (if (end <= start) start + 30 else end) - start
                    pendingLongPress = true
                    postDelayed(longPressRunnable, longPressTimeout)
                    return true
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                val dy = event.y - downY
                if (pendingLongPress && !dragging && (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop)) {
                    removeCallbacks(longPressRunnable)
                    pendingLongPress = false
                    draggedEventBlock = null
                }

                if (dragging && draggedRect != null) {
                    downY = event.y
                    updateDraggedRectByFingerY(event.y)
                    updateDragHintText()
                    invalidate()
                    return true
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                removeCallbacks(longPressRunnable)

                if (dragging && draggedRect != null && draggedEventBlock != null) {
                    val (newStart, newEnd) = rectToTimeRange(draggedEventBlock!!.event, draggedRect!!)
                    onEventDragChanged?.invoke(draggedEventBlock!!.event, newStart, newEnd)
                    resetDragState()
                    performClick()
                    return true
                }

                if (downInEvent) {
                    draggedEventBlock?.let {
                        onEventClick?.invoke(it.event)
                        resetDragState()
                        performClick()
                        return true
                    }
                }

                courseBlocks.lastOrNull { it.rect.contains(event.x, event.y) }?.let {
                    onCourseClick?.invoke(it.course)
                    resetDragState()
                    performClick()
                    return true
                }

                resetDragState()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(longPressRunnable)
                resetDragState()
                return true
            }
        }
        return true
    }

    private fun updateDraggedRectByFingerY(fingerY: Float) {
        val rect = draggedRect ?: return
        val top = (fingerY - dragTouchOffsetY).coerceAtLeast(axisTopPadding)
        val bottomLimit = axisTopPadding + 24f * hourHeight
        val height = rect.height()
        val clampedTop = top.coerceAtMost(bottomLimit - height)
        rect.offsetTo(rect.left, clampedTop)
    }

    private fun rectToTimeRange(event: TimelineEventEntity, rect: RectF): Pair<Long, Long> {
        val startMinute = (((rect.top - axisTopPadding) / hourHeight) * 60f).toInt().coerceIn(0, 24 * 60 - 1)
        val snappedStart = (startMinute / 5) * 5
        val snappedEnd = (snappedStart + draggedDurationMinutes).coerceAtMost(24 * 60)
        val dayStart = startOfLocalDay(event.startTime)
        val newStart = dayStart + snappedStart * 60_000L
        val newEnd = dayStart + snappedEnd * 60_000L
        return newStart to newEnd
    }

    private fun updateDragHintText() {
        val rect = draggedRect ?: run {
            dragHintText = null
            return
        }
        val startMinute = (((rect.top - axisTopPadding) / hourHeight) * 60f).toInt().coerceIn(0, 24 * 60 - 1)
        val snappedStart = (startMinute / 5) * 5
        val snappedEnd = (snappedStart + draggedDurationMinutes).coerceAtMost(24 * 60)
        dragHintText = "${minuteToText(snappedStart)}-${minuteToText(snappedEnd)}"
    }

    private fun resetDragState() {
        pendingLongPress = false
        dragging = false
        downInEvent = false
        draggedEventBlock = null
        draggedRect = null
        dragTouchOffsetY = 0f
        dragHintText = null
        parent?.requestDisallowInterceptTouchEvent(false)
        invalidate()
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun toMinuteOfAbsoluteTime(epochMillis: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    private fun toMinuteOfDayOffset(offsetMillis: Long): Int {
        val totalMinutes = (offsetMillis / 60_000L).toInt()
        val dayMinutes = 24 * 60
        var normalized = totalMinutes % dayMinutes
        if (normalized < 0) normalized += dayMinutes
        return normalized
    }

    private fun startOfLocalDay(epochMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = epochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun minuteToText(minute: Int): String {
        val clamped = minute.coerceIn(0, 24 * 60)
        val hour = (clamped / 60) % 24
        val min = clamped % 60
        return String.format(Locale.getDefault(), "%02d:%02d", hour, min)
    }


    private fun resolvePrimaryColor(): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        return if (typedValue.resourceId != 0) {
            ContextCompat.getColor(context, typedValue.resourceId)
        } else {
            typedValue.data
        }
    }
}

