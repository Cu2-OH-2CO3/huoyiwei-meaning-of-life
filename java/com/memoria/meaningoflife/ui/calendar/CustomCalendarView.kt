package com.memoria.meaningoflife.ui.calendar

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.model.Mood
import com.memoria.meaningoflife.utils.DateUtils
import java.util.*

class CustomCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ==================== 可调节字体参数 ====================
    private var DRAWER_TITLE_SIZE = 48f
    private var DRAWER_CONTENT_SIZE = 32f
    private var DRAWER_DATE_SIZE = 48f
    private var DRAWER_SMALL_SIZE = 24f
    private var DRAWER_LINE_HEIGHT = 52f

    // ==================== 原有变量 ====================
    private var currentYear = 0
    private var currentMonth = 0
    private var selectedDay = 0

    private var markedDates = mutableMapOf<String, MarkData>()
    private var diaryMoods = mutableMapOf<String, Mood>()

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var cellSize = 0f
    private var startX = 0f
    private var startY = 0f
    private var yearBarHeight = 0f
    private var monthBarHeight = 0f
    private var weekBarHeight = 0f
    private var calendarTop = 0f
    private var gridStartY = 0f

    // 抽屉相关变量
    private var drawerHeight = 0f
    private var drawerOpenProgress = 0f
    private var drawerAnimator: ValueAnimator? = null
    private var lastTouchY = 0f
    private var isDraggingDrawer = false
    private var isScrollingContent = false
    private var drawerScrollOffset = 0f
    private var drawerMaxScroll = 0f

    private var drawerTasks: List<DrawerTaskItem> = emptyList()
    private var drawerDiaries: List<DrawerDiaryItem> = emptyList()
    private var drawerPaintings: List<DrawerPaintingItem> = emptyList()
    private var onDrawerStateChangedListener: ((Boolean) -> Unit)? = null

    private var gestureDetector: GestureDetector
    private var onDateSelectedListener: ((year: Int, month: Int, day: Int) -> Unit)? = null
    private var onMonthChangedListener: ((year: Int, month: Int) -> Unit)? = null

    private var customTypeface: Typeface? = null

    private var animator: ValueAnimator? = null
    private var animationProgress = 0f
    private var isAnimating = false
    private var targetYear = 0
    private var targetMonth = 0

    // 把手区域
    private var handleRect = RectF()

    data class MarkData(
        val hasPainting: Boolean,
        val hasDiary: Boolean
    )

    data class DrawerTaskItem(
        val id: Long,
        val title: String,
        val priority: String,
        val priorityColor: Int,
        val deadline: String?
    )

    data class DrawerDiaryItem(
        val id: Long,
        val title: String?,
        val content: String,
        val moodIcon: String
    )

    data class DrawerPaintingItem(
        val id: Long,
        val title: String,
        val thumbnailPath: String?
    )

    init {
        val calendar = Calendar.getInstance()
        currentYear = calendar.get(Calendar.YEAR)
        currentMonth = calendar.get(Calendar.MONTH) + 1
        selectedDay = calendar.get(Calendar.DAY_OF_MONTH)

        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = 3f
        bgPaint.style = Paint.Style.FILL

        try {
            customTypeface = ResourcesCompat.getFont(context, R.font.lxgw)
        } catch (e: Exception) {
            customTypeface = Typeface.DEFAULT
        }

        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (isAnimating) return false

                val diffX = e2.x - (e1?.x ?: e2.x)
                if (Math.abs(diffX) > 100) {
                    if (diffX > 0) {
                        animateMonthChange(-1)
                    } else {
                        animateMonthChange(1)
                    }
                    return true
                }
                return false
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (isAnimating) return false
                val x = e.x
                val y = e.y

                // 年份区域点击
                if (y < yearBarHeight) {
                    val barWidth = width * 0.6f
                    val barStartX = (width - barWidth) / 2
                    val arrowLeftX = barStartX - 20
                    val arrowRightX = barStartX + barWidth + 20

                    if (x > arrowLeftX - 40 && x < arrowLeftX + 40) {
                        changeYearWithAnimation(-1)
                        return true
                    } else if (x > arrowRightX - 40 && x < arrowRightX + 40) {
                        changeYearWithAnimation(1)
                        return true
                    }
                }

                // 月份区域点击
                if (y > yearBarHeight && y < yearBarHeight + monthBarHeight) {
                    val centerX = width / 2f
                    if (x < centerX) {
                        animateMonthChange(-1)
                    } else {
                        animateMonthChange(1)
                    }
                    return true
                }

                // 日历格子点击
                if (y >= gridStartY && y < gridStartY + cellSize * 6) {
                    val col = ((x - startX) / cellSize).toInt()
                    val row = ((y - gridStartY) / cellSize).toInt()

                    if (col in 0..6 && row in 0..5) {
                        val day = getDayAtPosition(row, col)
                        if (day > 0) {
                            selectedDay = day
                            onDateSelectedListener?.invoke(currentYear, currentMonth, day)
                            invalidate()
                        }
                    }
                }
                return true
            }
        })
    }

    fun setDrawerContent(tasks: List<DrawerTaskItem>, diaries: List<DrawerDiaryItem>, paintings: List<DrawerPaintingItem>) {
        drawerTasks = tasks
        drawerDiaries = diaries
        drawerPaintings = paintings
        calculateDrawerContentHeight()
        drawerScrollOffset = 0f
        invalidate()
    }

    private fun calculateDrawerContentHeight() {
        var height = 60f
        height += DRAWER_LINE_HEIGHT
        height += 24f
        height += DRAWER_LINE_HEIGHT
        if (drawerTasks.isNotEmpty()) {
            height += drawerTasks.size * (DRAWER_LINE_HEIGHT - 8)
        } else {
            height += DRAWER_LINE_HEIGHT
        }
        height += 20f
        height += DRAWER_LINE_HEIGHT
        if (drawerDiaries.isNotEmpty()) {
            drawerDiaries.forEach { diary ->
                height += DRAWER_LINE_HEIGHT - 8
            }
        } else {
            height += DRAWER_LINE_HEIGHT
        }
        height += 20f
        height += DRAWER_LINE_HEIGHT
        if (drawerPaintings.isNotEmpty()) {
            height += drawerPaintings.size * (DRAWER_LINE_HEIGHT - 8)
        } else {
            height += DRAWER_LINE_HEIGHT
        }
        height += 40f
        drawerMaxScroll = maxOf(0f, height - drawerHeight)
    }

    fun setOnDrawerStateChangedListener(listener: (Boolean) -> Unit) {
        onDrawerStateChangedListener = listener
    }

    fun closeDrawer() {
        animateDrawer(0f)
    }

    private fun animateDrawer(targetProgress: Float) {
        drawerAnimator?.cancel()
        drawerAnimator = ValueAnimator.ofFloat(drawerOpenProgress, targetProgress).apply {
            duration = 250
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                drawerOpenProgress = it.animatedValue as Float
                requestLayout()
                invalidate()
            }
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onDrawerStateChangedListener?.invoke(drawerOpenProgress > 0.5f)
                }
                override fun onAnimationCancel(animation: android.animation.Animator) {}
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
            })
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        yearBarHeight = h * 0.1f
        monthBarHeight = h * 0.1f
        weekBarHeight = h * 0.08f
        calendarTop = yearBarHeight + monthBarHeight + weekBarHeight
        drawerHeight = h * 0.4f

        // 日历可用高度
        val availableHeight = h - calendarTop - 20f
        val calendarHeight = availableHeight - (drawerHeight * drawerOpenProgress)

        // 确保最小高度
        val safeCalendarHeight = maxOf(calendarHeight, 300f)

        val maxCellSizeByWidth = w / 7f
        val maxCellSizeByHeight = safeCalendarHeight / 6f
        cellSize = minOf(maxCellSizeByWidth, maxCellSizeByHeight).coerceAtLeast(30f)

        val totalWidth = cellSize * 7
        startX = (w - totalWidth) / 2

        val totalHeight = cellSize * 6
        gridStartY = calendarTop + (safeCalendarHeight - totalHeight) / 2
        startY = gridStartY

        // 把手区域 - 扩大范围，更容易拖动
        handleRect.set(0f, height - 80f, width.toFloat(), height.toFloat())

        android.util.Log.d("CustomCalendar", "onSizeChanged: handleRect=$handleRect")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data

        val primaryLightColor = Color.argb(255,
            (Color.red(primaryColor) + 40).coerceAtMost(255),
            (Color.green(primaryColor) + 40).coerceAtMost(255),
            (Color.blue(primaryColor) + 40).coerceAtMost(255))

        val textPrimaryColor = ContextCompat.getColor(context, R.color.text_primary)
        val textSecondaryColor = ContextCompat.getColor(context, R.color.text_secondary)

        customTypeface?.let {
            textPaint.typeface = it
        }

        drawYearScrollBar(canvas, primaryColor, textPrimaryColor)
        drawMonthTitle(canvas, primaryColor, textPrimaryColor)
        drawWeekDays(canvas, textSecondaryColor)

        if (isAnimating) {
            drawCalendarWithAnimation(canvas, primaryColor, primaryLightColor, textPrimaryColor)
        } else {
            drawCalendarGrid(canvas, primaryColor, primaryLightColor, textPrimaryColor)
        }

        drawDrawer(canvas, primaryColor, textPrimaryColor, textSecondaryColor)
    }

    private fun drawDrawer(canvas: Canvas, primaryColor: Int, textPrimaryColor: Int, textSecondaryColor: Int) {
        if (drawerHeight <= 0) return

        val drawerTop = height - drawerHeight * drawerOpenProgress
        val drawerBottom = height.toFloat()

        // 始终绘制把手
        drawHandle(canvas, primaryColor)

        if (drawerOpenProgress <= 0.01f) return

        bgPaint.color = ContextCompat.getColor(context, R.color.card_background)
        bgPaint.style = Paint.Style.FILL
        canvas.drawRect(0f, drawerTop, width.toFloat(), drawerBottom, bgPaint)

        canvas.save()
        canvas.clipRect(0f, drawerTop, width.toFloat(), drawerBottom)

        var currentY = drawerTop + 50f + drawerScrollOffset
        val padding = 20f
        val lineHeight = DRAWER_LINE_HEIGHT

        // 绘制关闭按钮（右上角）
        val closeX = width - 50f
        val closeY = drawerTop + 35f
        val closeRadius = 18f

        // 绘制圆形背景
        bgPaint.color = primaryColor
        bgPaint.alpha = 100
        canvas.drawCircle(closeX, closeY, closeRadius, bgPaint)
        bgPaint.alpha = 255

        // 绘制 X 符号
        textPaint.textSize = 28f
        textPaint.color = primaryColor
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("✕", closeX, closeY + 8, textPaint)
        textPaint.textAlign = Paint.Align.LEFT

        // 日期
        val selectedDateStr = "$currentYear-${currentMonth.toString().padStart(2, '0')}-${selectedDay.toString().padStart(2, '0')}"
        textPaint.textSize = DRAWER_DATE_SIZE
        textPaint.isFakeBoldText = true
        textPaint.color = textPrimaryColor
        canvas.drawText(DateUtils.formatDate(selectedDateStr), padding, currentY, textPaint)
        textPaint.isFakeBoldText = false
        currentY += lineHeight

        // 分割线
        bgPaint.color = textSecondaryColor
        bgPaint.alpha = 80
        canvas.drawRect(padding, currentY - 16, width.toFloat() - padding, currentY - 12, bgPaint)
        bgPaint.alpha = 255
        currentY += 30

        // 已完成任务
        textPaint.textSize = DRAWER_TITLE_SIZE
        textPaint.color = primaryColor
        canvas.drawText("📋 已完成任务", padding, currentY, textPaint)
        currentY += lineHeight

        if (drawerTasks.isNotEmpty()) {
            drawerTasks.forEach { task ->
                textPaint.color = textPrimaryColor
                textPaint.textSize = DRAWER_CONTENT_SIZE
                val displayTitle = if (task.title.length > 25) task.title.substring(0, 25) + "..." else task.title
                canvas.drawText("✅ $displayTitle", padding + 10f, currentY, textPaint)
                currentY += lineHeight - 8
            }
        } else {
            textPaint.textSize = DRAWER_CONTENT_SIZE
            textPaint.color = textSecondaryColor
            canvas.drawText("暂无已完成任务", padding, currentY, textPaint)
            currentY += lineHeight
        }
        currentY += 12

        // 分割线
        bgPaint.color = textSecondaryColor
        bgPaint.alpha = 80
        canvas.drawRect(padding, currentY - 16, width.toFloat() - padding, currentY - 12, bgPaint)
        bgPaint.alpha = 255
        currentY += 30

        // 日记
        textPaint.textSize = DRAWER_TITLE_SIZE
        textPaint.color = primaryColor
        canvas.drawText("📝 日记", padding, currentY, textPaint)
        currentY += lineHeight

        if (drawerDiaries.isNotEmpty()) {
            drawerDiaries.forEach { diary ->
                textPaint.textSize = DRAWER_CONTENT_SIZE
                textPaint.color = textPrimaryColor
                val title = diary.title?.takeIf { it.isNotEmpty() } ?: "无题"
                val displayTitle = if (title.length > 20) title.substring(0, 20) + "..." else title
                canvas.drawText("${diary.moodIcon} $displayTitle", padding, currentY, textPaint)
                currentY += lineHeight - 8
            }
        } else {
            textPaint.textSize = DRAWER_CONTENT_SIZE
            textPaint.color = textSecondaryColor
            canvas.drawText("暂无日记", padding, currentY, textPaint)
            currentY += lineHeight
        }
        currentY += 12

        // 分割线
        bgPaint.color = textSecondaryColor
        bgPaint.alpha = 80
        canvas.drawRect(padding, currentY - 16, width.toFloat() - padding, currentY - 12, bgPaint)
        bgPaint.alpha = 255
        currentY += 30

        // 绘画记录
        textPaint.textSize = DRAWER_TITLE_SIZE
        textPaint.color = primaryColor
        canvas.drawText("🎨 绘画记录", padding, currentY, textPaint)
        currentY += lineHeight

        if (drawerPaintings.isNotEmpty()) {
            drawerPaintings.forEach { painting ->
                textPaint.textSize = DRAWER_CONTENT_SIZE
                textPaint.color = textPrimaryColor
                val displayTitle = if (painting.title.length > 25) painting.title.substring(0, 25) + "..." else painting.title
                canvas.drawText("🖼️ $displayTitle", padding, currentY, textPaint)
                currentY += lineHeight - 8
            }
        } else {
            textPaint.textSize = DRAWER_CONTENT_SIZE
            textPaint.color = textSecondaryColor
            canvas.drawText("暂无绘画记录", padding, currentY, textPaint)
        }

        canvas.restore()
    }

    private fun drawHandle(canvas: Canvas, primaryColor: Int) {
        // 绘制把手背景（半透明）
        bgPaint.color = primaryColor
        bgPaint.alpha = 80
        canvas.drawRect(0f, height - 70f, width.toFloat(), height.toFloat(), bgPaint)
        bgPaint.alpha = 255

        // 绘制把手横条
        bgPaint.color = primaryColor
        bgPaint.alpha = 220
        val handleBarWidth = 100f
        val handleBarHeight = 6f
        val handleBarX = (width - handleBarWidth) / 2
        val handleBarY = height - 35f
        canvas.drawRoundRect(handleBarX, handleBarY - handleBarHeight / 2, handleBarX + handleBarWidth, handleBarY + handleBarHeight / 2, 8f, 8f, bgPaint)
        bgPaint.alpha = 255

        // 提示文字
        textPaint.textSize = 22f
        textPaint.color = ContextCompat.getColor(context, R.color.text_secondary)
        textPaint.textAlign = Paint.Align.CENTER
        val tipText = if (drawerOpenProgress > 0.5f) "↓ 向下拖动关闭" else "↑ 向上拖动打开"
        canvas.drawText(tipText, width / 2f, height - 20f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchY = event.y

                // 检测是否点击了关闭按钮（抽屉打开时）
                if (drawerOpenProgress > 0.01f) {
                    val closeX = width - 50f
                    val drawerTop = height - drawerHeight * drawerOpenProgress
                    val closeY = drawerTop + 35f
                    val closeRadius = 22f

                    // 计算点击位置与关闭按钮的距离
                    val dx = event.x - closeX
                    val dy = event.y - closeY
                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                    if (distance < closeRadius) {
                        // 点击关闭按钮，关闭抽屉
                        animateDrawer(0f)
                        return true
                    }
                }

                // 检测是否在把手区域
                val isInHandleArea = event.y > height - 80f

                if (isInHandleArea) {
                    isDraggingDrawer = true
                    isScrollingContent = false
                    return true
                }

                // 检测是否在抽屉内容区域（抽屉打开时）
                if (drawerOpenProgress > 0.1f && event.y > height - drawerHeight * drawerOpenProgress) {
                    isScrollingContent = true
                    isDraggingDrawer = false
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDraggingDrawer) {
                    val deltaY = event.y - lastTouchY
                    val newProgress = (drawerOpenProgress - deltaY / drawerHeight).coerceIn(0f, 1f)
                    if (newProgress != drawerOpenProgress) {
                        drawerOpenProgress = newProgress
                        requestLayout()
                        invalidate()
                    }
                    lastTouchY = event.y
                    return true
                } else if (isScrollingContent) {
                    val deltaY = event.y - lastTouchY
                    drawerScrollOffset += deltaY
                    drawerScrollOffset = drawerScrollOffset.coerceIn(-drawerMaxScroll, 0f)
                    lastTouchY = event.y
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isDraggingDrawer) {
                    val shouldOpen = drawerOpenProgress > 0.3f
                    animateDrawer(if (shouldOpen) 1f else 0f)
                    isDraggingDrawer = false
                    return true
                } else if (isScrollingContent) {
                    isScrollingContent = false
                    return true
                }
            }
        }
        return gestureDetector.onTouchEvent(event)
    }

    private fun getDayAtPosition(row: Int, col: Int): Int {
        val firstDayOfWeek = DateUtils.getFirstDayOfMonth(currentYear, currentMonth)
        val day = row * 7 + col - firstDayOfWeek + 2
        val daysInMonth = DateUtils.getDaysInMonth(currentYear, currentMonth)
        return if (day in 1..daysInMonth) day else -1
    }

    private fun animateMonthChange(delta: Int) {
        if (isAnimating) return
        var newMonth = currentMonth + delta
        var newYear = currentYear
        if (newMonth > 12) { newMonth = 1; newYear++ }
        else if (newMonth < 1) { newMonth = 12; newYear-- }
        targetYear = newYear; targetMonth = newMonth
        isAnimating = true; animationProgress = 0f
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener { animationProgress = it.animatedValue as Float; invalidate() }
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    isAnimating = false; currentYear = targetYear; currentMonth = targetMonth
                    selectedDay = 1; onMonthChangedListener?.invoke(currentYear, currentMonth); invalidate()
                }
                override fun onAnimationCancel(animation: android.animation.Animator) { isAnimating = false }
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
            })
            start()
        }
    }

    private fun changeYearWithAnimation(delta: Int) {
        if (isAnimating) return
        targetYear = currentYear + delta; targetMonth = currentMonth
        isAnimating = true; animationProgress = 0f
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener { animationProgress = it.animatedValue as Float; invalidate() }
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    isAnimating = false; currentYear = targetYear
                    selectedDay = 1; onMonthChangedListener?.invoke(currentYear, currentMonth); invalidate()
                }
                override fun onAnimationCancel(animation: android.animation.Animator) { isAnimating = false }
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
            })
            start()
        }
    }

    private fun drawCalendarWithAnimation(canvas: Canvas, primaryColor: Int, primaryLightColor: Int, textColor: Int) {
        val offset = (1 - animationProgress) * width
        canvas.save(); canvas.translate(-offset, 0f)
        drawMonthGrid(canvas, currentYear, currentMonth, primaryColor, primaryLightColor, textColor, true)
        canvas.restore()
        canvas.save(); canvas.translate(width - offset, 0f)
        drawMonthGrid(canvas, targetYear, targetMonth, primaryColor, primaryLightColor, textColor, false)
        canvas.restore()
    }

    private fun drawMonthGrid(canvas: Canvas, year: Int, month: Int, primaryColor: Int, primaryLightColor: Int, textColor: Int, isCurrent: Boolean) {
        val firstDayOfWeek = DateUtils.getFirstDayOfMonth(year, month)
        val daysInMonth = DateUtils.getDaysInMonth(year, month)
        textPaint.textSize = cellSize * 0.35f
        textPaint.textAlign = Paint.Align.CENTER

        for (row in 0..5) {
            for (col in 0..6) {
                val left = startX + col * cellSize
                val top = startY + row * cellSize
                val right = left + cellSize
                val bottom = top + cellSize
                val centerX = left + cellSize / 2
                val centerY = top + cellSize / 2 + cellSize * 0.05f
                val day = row * 7 + col - firstDayOfWeek + 2
                if (day in 1..daysInMonth) {
                    val dateStr = "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
                    val markData = markedDates[dateStr]
                    when (currentMode) {
                        0 -> {
                            if (markData != null) {
                                if (markData.hasPainting && markData.hasDiary) {
                                    bgPaint.color = primaryColor; bgPaint.alpha = 100
                                    canvas.drawRect(left + 2, top + 2, right - 2, bottom - 2, bgPaint)
                                } else if (markData.hasPainting || markData.hasDiary) {
                                    bgPaint.color = primaryLightColor; bgPaint.alpha = 100
                                    canvas.drawRect(left + 2, top + 2, right - 2, bottom - 2, bgPaint)
                                }
                            }
                        }
                        1 -> {
                            if (markData != null && markData.hasPainting) {
                                bgPaint.color = primaryLightColor; bgPaint.alpha = 100
                                canvas.drawRect(left + 2, top + 2, right - 2, bottom - 2, bgPaint)
                            }
                        }
                        2 -> {
                            val mood = diaryMoods[dateStr]
                            if (mood != null) {
                                textPaint.textSize = cellSize * 0.4f
                                canvas.drawText(mood.icon, centerX, centerY + cellSize * 0.1f, textPaint)
                            }
                        }
                    }
                    if (isCurrent && day == selectedDay) {
                        borderPaint.color = primaryColor; borderPaint.strokeWidth = 4f
                        canvas.drawRect(left + 2, top + 2, right - 2, bottom - 2, borderPaint)
                    }
                    if (currentMode != 2 || diaryMoods[dateStr] == null) {
                        textPaint.textSize = cellSize * 0.35f; textPaint.color = textColor
                        canvas.drawText(day.toString(), centerX, centerY, textPaint)
                    }
                }
            }
        }
    }

    private fun drawCalendarGrid(canvas: Canvas, primaryColor: Int, primaryLightColor: Int, textColor: Int) {
        drawMonthGrid(canvas, currentYear, currentMonth, primaryColor, primaryLightColor, textColor, true)
    }

    private fun drawYearScrollBar(canvas: Canvas, primaryColor: Int, textColor: Int) {
        val y = yearBarHeight / 2
        val barWidth = width * 0.6f
        val barStartX = (width - barWidth) / 2
        bgPaint.color = Color.argb(30, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))
        bgPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(barStartX - 20, y - 20, barStartX + barWidth + 20, y + 30, 25f, 25f, bgPaint)
        textPaint.textSize = 36f; textPaint.color = textColor; textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("$currentYear", width / 2f, y + 8, textPaint)
        textPaint.textSize = 40f; textPaint.color = primaryColor
        canvas.drawText("◀", barStartX - 20, y + 8, textPaint)
        canvas.drawText("▶", barStartX + barWidth + 20, y + 8, textPaint)
    }

    private fun drawMonthTitle(canvas: Canvas, primaryColor: Int, textColor: Int) {
        val y = yearBarHeight + monthBarHeight / 2
        textPaint.textSize = 32f; textPaint.color = textColor; textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("${currentMonth}月", width / 2f, y + 8, textPaint)
        textPaint.textSize = 24f; textPaint.color = primaryColor; textPaint.alpha = 150
        canvas.drawText("◀ 滑动切换月份 ▶", width / 2f, y + 40, textPaint)
        textPaint.alpha = 255
    }

    private fun drawWeekDays(canvas: Canvas, textColor: Int) {
        val weekDays = arrayOf("一", "二", "三", "四", "五", "六", "日")
        val y = yearBarHeight + monthBarHeight + weekBarHeight / 2
        textPaint.textSize = cellSize * 0.3f; textPaint.color = textColor; textPaint.textAlign = Paint.Align.CENTER
        for (i in 0..6) {
            val x = startX + cellSize * i + cellSize / 2
            canvas.drawText(weekDays[i], x, y + 8, textPaint)
        }
    }

    private var currentMode = 0
    fun setMode(mode: Int) { currentMode = mode; invalidate() }
    fun setMarkedDates(dates: Map<String, MarkData>) { markedDates.clear(); markedDates.putAll(dates); invalidate() }
    fun setDiaryMoods(moods: Map<String, Mood>) { diaryMoods.clear(); diaryMoods.putAll(moods); invalidate() }
    fun setSelectedDate(year: Int, month: Int, day: Int) { currentYear = year; currentMonth = month; selectedDay = day; invalidate() }
    fun setOnDateSelectedListener(listener: (year: Int, month: Int, day: Int) -> Unit) { onDateSelectedListener = listener }
    fun setOnMonthChangedListener(listener: (year: Int, month: Int) -> Unit) { onMonthChangedListener = listener }
}