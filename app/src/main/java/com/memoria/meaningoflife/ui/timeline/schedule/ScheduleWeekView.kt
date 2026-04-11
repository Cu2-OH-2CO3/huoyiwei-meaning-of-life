// 路径：ui/timeline/schedule/ScheduleWeekView.kt
package com.memoria.meaningoflife.ui.timeline.schedule

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.data.database.timeline.CourseEntity
import kotlin.math.max
import kotlin.math.min

class ScheduleWeekView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val colorBorderVisible = ContextCompat.getColor(context, R.color.nd_border_visible)
    private val colorTextSecondary = ContextCompat.getColor(context, R.color.nd_text_secondary)
    private val colorTextPrimary = ContextCompat.getColor(context, R.color.nd_text_primary)
    private val colorAccent = ContextCompat.getColor(context, R.color.nd_accent)
    private val colorSurface = ContextCompat.getColor(context, R.color.nd_surface)
    private val colorPrimary = resolvePrimaryColor(context)
    private val monoTypeface: Typeface = Typeface.MONOSPACE
    private val bodyTypeface: Typeface = ResourcesCompat.getFont(context, R.font.lxgw) ?: Typeface.DEFAULT

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = colorBorderVisible
    }

    private var courses: List<CourseEntity> = emptyList()
    private var currentWeek = 1
    private var currentWeekStartMillis: Long = -1L
    private var lastRenderBlocks: List<RenderBlock> = emptyList()

    private var cellWidth = 0f
    private var cellHeight = 0f
    private var timeColumnWidth = 44f * resources.displayMetrics.density
    private var headerHeight = 0f
    private val totalBlocks = 61f
    private val slotMinutes = 5

    private val timeBoundaries = listOf(
        8 * 60 + 30,
        9 * 60 + 20,
        10 * 60 + 25,
        11 * 60 + 15,
        12 * 60,
        14 * 60,
        14 * 60 + 50,
        15 * 60 + 55,
        16 * 60 + 45,
        17 * 60 + 30,
        19 * 60,
        19 * 60 + 55,
        20 * 60 + 35,
        21 * 60 + 25,
    )

    private val blockSpans = listOf(5, 5, 5, 5, 3, 5, 5, 5, 5, 3, 5, 5, 5)

    private data class AxisRow(
        val title: String,
        val startLabel: String,
        val endLabel: String,
        val blockSpan: Int,
        val isBreak: Boolean = false
    )

    private data class RenderBlock(
        val weekDay: Int,
        var startBlock: Float,
        var endBlock: Float,
        val courses: MutableList<CourseEntity>,
        var primary: CourseEntity
    )

    private val axisRows = listOf(
        AxisRow("1", "08:30", "09:20", 5),
        AxisRow("2", "09:20", "10:25", 5),
        AxisRow("3", "10:25", "11:15", 5),
        AxisRow("4", "11:15", "12:00", 5),
        AxisRow("午休", "", "", 3, true),
        AxisRow("5", "14:00", "14:50", 5),
        AxisRow("6", "14:50", "15:55", 5),
        AxisRow("7", "15:55", "16:45", 5),
        AxisRow("8", "16:45", "17:30", 5),
        AxisRow("晚休", "", "", 3, true),
        AxisRow("9", "19:00", "19:55", 5),
        AxisRow("10", "19:55", "20:35", 5),
        AxisRow("11", "20:35", "21:25", 5),
    )

    private var onCourseClickListener: ((CourseEntity) -> Unit)? = null
    private var onCourseLongClickListener: ((CourseEntity) -> Unit)? = null
    private var pressedCourse: CourseEntity? = null

    init {
        setBackgroundColor(colorSurface)
    }

    fun setCourses(courses: List<CourseEntity>, week: Int, axisSourceCourses: List<CourseEntity> = courses) {
        // 当前周空数据时保留轴样本，维持触控和展示区域一致性。
        this.courses = if (courses.isEmpty()) axisSourceCourses else courses
        this.currentWeek = week
        updateCellMetrics()
        invalidate()
    }

    fun setWeekStartDate(startMillis: Long) {
        currentWeekStartMillis = startMillis
        invalidate()
    }

    fun setOnCourseClickListener(listener: (CourseEntity) -> Unit) {
        onCourseClickListener = listener
    }

    fun setOnCourseLongClickListener(listener: (CourseEntity) -> Unit) {
        onCourseLongClickListener = listener
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateCellMetrics()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val desiredHeight = (totalBlocks * 15f * resources.displayMetrics.density).toInt()
        val mode = MeasureSpec.getMode(heightMeasureSpec)
        val size = MeasureSpec.getSize(heightMeasureSpec)

        val height = when (mode) {
            MeasureSpec.EXACTLY -> size
            MeasureSpec.AT_MOST -> min(size, desiredHeight)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }

    private fun updateCellMetrics() {
        cellWidth = (width - timeColumnWidth) / 7f
        cellHeight = (height - headerHeight) / totalBlocks
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawTimeColumn(canvas)
        drawGrid(canvas)
        drawCourses(canvas)
    }

    private fun drawTimeColumn(canvas: Canvas) {
        var top = headerHeight
        axisRows.forEach { row ->
            val rowHeight = row.blockSpan * cellHeight

            if (row.isBreak) {
                textPaint.apply {
                    color = colorTextSecondary
                    textSize = 30f
                    textAlign = Paint.Align.CENTER
                    typeface = monoTypeface
                }
                canvas.drawText(row.title, timeColumnWidth / 2, top + rowHeight / 2 + textPaint.textSize / 3, textPaint)
            } else {
                textPaint.apply {
                    color = colorTextSecondary
                    textSize = 30f
                    textAlign = Paint.Align.CENTER
                    typeface = monoTypeface
                }
                canvas.drawText(row.title, timeColumnWidth / 2, top + rowHeight * 0.35f, textPaint)

                textPaint.textSize = 30f
                textPaint.typeface = monoTypeface
                canvas.drawText(row.startLabel, timeColumnWidth / 2, top + rowHeight * 0.62f, textPaint)
                canvas.drawText(row.endLabel, timeColumnWidth / 2, top + rowHeight * 0.86f, textPaint)
            }

            top += rowHeight
        }
    }

    private fun drawGrid(canvas: Canvas) {
        // 竖线
        for (i in 0..7) {
            val x = timeColumnWidth + i * cellWidth
            canvas.drawLine(x, headerHeight, x, height.toFloat(), borderPaint)
        }

        // 横线
        var accumulatedBlocks = 0
        canvas.drawLine(0f, headerHeight, width.toFloat(), headerHeight, borderPaint)
        axisRows.forEach { row ->
            accumulatedBlocks += row.blockSpan
            val y = headerHeight + accumulatedBlocks * cellHeight
            canvas.drawLine(0f, y, width.toFloat(), y, borderPaint)
        }
    }

    private fun drawCourses(canvas: Canvas) {
        val renderBlocks = buildRenderBlocks()
        lastRenderBlocks = renderBlocks

        renderBlocks.forEach { block ->
            val col = block.weekDay - 1
            val left = timeColumnWidth + col * cellWidth + 2
            val right = left + cellWidth - 4
            val top = headerHeight + block.startBlock * cellHeight + 2
            val bottom = headerHeight + block.endBlock * cellHeight - 4
            if (bottom <= top) return@forEach

            val primary = block.primary
            val baseColor = primary.color ?: colorAccent

            // Nothing 风格：平面+边框，不使用阴影。
            bgPaint.style = Paint.Style.FILL
            bgPaint.color = colorSurface
            canvas.drawRoundRect(left, top, right, bottom, 8f, 8f, bgPaint)

            bgPaint.style = Paint.Style.STROKE
            bgPaint.strokeWidth = 1f
            bgPaint.color = colorBorderVisible
            canvas.drawRoundRect(left, top, right, bottom, 8f, 8f, bgPaint)

            // 左侧强调线用于区分课程。
            bgPaint.style = Paint.Style.FILL
            bgPaint.color = colorPrimary
            canvas.drawRoundRect(left, top, left + 3f, bottom, 4f, 4f, bgPaint)

            val inner = RectF(left + 5f, top + 3f, right - 4f, bottom - 3f)

            val padX = 6f
            textPaint.apply {
                color = colorTextPrimary
                textAlign = Paint.Align.LEFT
                typeface = bodyTypeface
                textSize = 40f
            }

            val titleFm = textPaint.fontMetrics
            val titleLineStep = (textPaint.fontSpacing * 0.78f).coerceAtLeast(18f)
            var textY = inner.top - titleFm.ascent + 2f

            val titleLines = splitToLines(
                text = primary.name,
                maxWidth = (inner.width() - padX * 2).coerceAtLeast(20f),
                maxLines = 5,
                paint = textPaint
            )
            val subLineStepEstimate = 25f * 0.82f
            val reservedBottom = if (block.courses.size > 1) subLineStepEstimate * 2f else subLineStepEstimate

            for (line in titleLines) {
                if (textY + textPaint.fontMetrics.descent > inner.bottom - reservedBottom) break
                canvas.drawText(line, inner.left + padX, textY, textPaint)
                textY += titleLineStep
            }

            textPaint.apply {
                typeface = bodyTypeface
                textSize = 25f
                color = colorTextSecondary
            }

            val subLineStep = (textPaint.fontSpacing * 0.82f).coerceAtLeast(14f)
            textY += subLineStep
            canvas.drawText("@${ellipsize(primary.location ?: "待定", 10)}", inner.left + padX, textY, textPaint)

            if (block.courses.size > 1) {
                textY += subLineStep
                canvas.drawText("另${block.courses.size - 1}门", inner.left + padX, textY, textPaint)

                val triangle = Path().apply {
                    moveTo(inner.right - 10f, inner.top)
                    lineTo(inner.right, inner.top)
                    lineTo(inner.right, inner.top + 10f)
                    close()
                }
                bgPaint.color = adjustAlpha(baseColor, 0.9f)
                canvas.drawPath(triangle, bgPaint)
            }
        }
    }

    private fun buildRenderBlocks(): List<RenderBlock> {
        val perDay = (1..7).associateWith { mutableListOf<RenderBlock>() }.toMutableMap()

        courses.forEach { course ->
            if (!shouldShowCourse(course)) return@forEach

            val day = course.weekDay.coerceIn(1, 7)
            val startMinute = toMinuteOfDay(course.startTime)
            val endMinuteRaw = toMinuteOfDay(course.endTime)
            val endMinute = if (endMinuteRaw <= startMinute) startMinute + slotMinutes else endMinuteRaw
            val startBlock = transferMinuteToBlock(startMinute)
            val endBlock = max(startBlock + 0.75f, transferMinuteToBlock(endMinute))
            if (endBlock <= 0f || startBlock >= totalBlocks) return@forEach

            val dayBlocks = perDay.getValue(day)
            val overlapIndex = dayBlocks.indexOfFirst { existing ->
                existing.startBlock < endBlock && existing.endBlock > startBlock
            }

            if (overlapIndex == -1) {
                dayBlocks.add(
                    RenderBlock(
                        weekDay = day,
                        startBlock = startBlock,
                        endBlock = endBlock,
                        courses = mutableListOf(course),
                        primary = course
                    )
                )
            } else {
                val existing = dayBlocks[overlapIndex]
                val mergedStart = min(existing.startBlock, startBlock)
                val mergedEnd = max(existing.endBlock, endBlock)
                existing.startBlock = mergedStart
                existing.endBlock = mergedEnd
                existing.courses.add(course)

                val newDuration = endBlock - startBlock
                val oldDuration = durationOf(existing.primary)
                if (newDuration >= oldDuration) {
                    existing.primary = course
                }
            }
        }

        return perDay.values.flatten().sortedWith(compareBy<RenderBlock> { it.weekDay }.thenBy { it.startBlock })
    }

    private fun durationOf(course: CourseEntity): Float {
        val startMinute = toMinuteOfDay(course.startTime)
        val endMinuteRaw = toMinuteOfDay(course.endTime)
        val endMinute = if (endMinuteRaw <= startMinute) startMinute + slotMinutes else endMinuteRaw
        return transferMinuteToBlock(endMinute) - transferMinuteToBlock(startMinute)
    }

    private fun shouldShowCourse(course: CourseEntity): Boolean {
        return CourseWeekPattern.shouldShowInWeek(course, currentWeek)
    }

    private fun toMinuteOfDay(timeInMillis: Long): Int {
        val total = (timeInMillis / 60000L).toInt()
        val dayMinutes = 24 * 60
        var normalized = total % dayMinutes
        if (normalized < 0) normalized += dayMinutes
        return normalized
    }


    private fun transferMinuteToBlock(minuteOfDay: Int): Float {
        if (minuteOfDay <= timeBoundaries.first()) return 0f
        if (minuteOfDay >= timeBoundaries.last()) return totalBlocks

        var previousBoundary = timeBoundaries.first()
        var accumulatedBlocks = 0f

        for (index in 1 until timeBoundaries.size) {
            val currentBoundary = timeBoundaries[index]
            if (minuteOfDay < currentBoundary) {
                val intervalMinutes = (currentBoundary - previousBoundary).toFloat().coerceAtLeast(1f)
                val ratio = ((minuteOfDay - previousBoundary).toFloat() / intervalMinutes).coerceIn(0f, 1f)
                return accumulatedBlocks + blockSpans[index - 1] * ratio
            }

            accumulatedBlocks += blockSpans[index - 1]
            previousBoundary = currentBoundary
        }

        return totalBlocks
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressedCourse = findCourseAtPoint(event.x, event.y)
                if (pressedCourse != null) {
                    // 课程命中后锁定触摸，避免 NestedScrollView 抢占点击事件。
                    parent?.requestDisallowInterceptTouchEvent(true)
                    true
                } else {
                    pressedCourse = null
                    super.onTouchEvent(event)
                }
            }

            MotionEvent.ACTION_UP -> {
                val downCourse = pressedCourse
                val upCourse = findCourseAtPoint(event.x, event.y)
                pressedCourse = null
                parent?.requestDisallowInterceptTouchEvent(false)

                if (downCourse != null && upCourse?.id == downCourse.id) {
                    if (event.eventTime - event.downTime > 500) {
                        onCourseLongClickListener?.invoke(downCourse)
                    } else {
                        onCourseClickListener?.invoke(downCourse)
                        performClick()
                    }
                    true
                } else {
                    super.onTouchEvent(event)
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                pressedCourse = null
                parent?.requestDisallowInterceptTouchEvent(false)
                super.onTouchEvent(event)
            }

            else -> {
                if (pressedCourse != null) true else super.onTouchEvent(event)
            }
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun findCourseAt(weekDay: Int, row: Int): CourseEntity? {
        val touchStartBlock = row.coerceAtLeast(0).toFloat()
        val touchEndBlock = touchStartBlock + 1f

        return lastRenderBlocks.firstOrNull { block ->
            block.weekDay == weekDay &&
                    block.startBlock < touchEndBlock &&
                    block.endBlock > touchStartBlock
        }?.primary
    }

    private fun findCourseAtPoint(x: Float, y: Float): CourseEntity? {
        if (x <= timeColumnWidth || y <= headerHeight || cellWidth <= 0f || cellHeight <= 0f) return null
        val col = ((x - timeColumnWidth) / cellWidth).toInt()
        val row = ((y - headerHeight) / cellHeight).toInt()
        if (col !in 0..6) return null
        return findCourseAt(col + 1, row)
    }


    private fun adjustAlpha(color: Int, alpha: Float): Int {
        return Color.argb((alpha.coerceIn(0f, 1f) * 255).toInt(), Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun ellipsize(text: String, maxLen: Int): String {
        if (text.length <= maxLen) return text
        return text.take(maxLen - 2) + ".."
    }

    private fun splitToLines(text: String, maxWidth: Float, maxLines: Int, paint: Paint): List<String> {
        if (text.isBlank() || maxLines <= 0) return emptyList()

        val lines = mutableListOf<String>()
        var rest = text.trim()

        while (rest.isNotEmpty() && lines.size < maxLines) {
            val count = paint.breakText(rest, true, maxWidth, null).coerceAtLeast(1)
            var line = rest.substring(0, count)
            rest = rest.substring(count)

            if (lines.size == maxLines - 1 && rest.isNotEmpty()) {
                line = if (line.length > 2) line.dropLast(2) + ".." else ".."
                rest = ""
            }
            lines.add(line)
        }

        return lines
    }

    private fun resolvePrimaryColor(context: Context): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        return typedValue.data
    }
}