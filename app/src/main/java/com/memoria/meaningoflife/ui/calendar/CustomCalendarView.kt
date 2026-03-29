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

    private var gestureDetector: GestureDetector
    private var onDateSelectedListener: ((year: Int, month: Int, day: Int) -> Unit)? = null
    private var onMonthChangedListener: ((year: Int, month: Int) -> Unit)? = null

    private var customTypeface: Typeface? = null

    private var animator: ValueAnimator? = null
    private var animationProgress = 0f
    private var isAnimating = false
    private var targetYear = 0
    private var targetMonth = 0

    data class MarkData(
        val hasPainting: Boolean,
        val hasDiary: Boolean
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
                if (Math.abs(diffX) > 150) {
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

                if (y > yearBarHeight && y < yearBarHeight + monthBarHeight) {
                    val centerX = width / 2f
                    if (x < centerX) {
                        animateMonthChange(-1)
                    } else {
                        animateMonthChange(1)
                    }
                    return true
                }

                if (y > calendarTop) {
                    val col = ((x - startX) / cellSize).toInt()
                    val row = ((y - calendarTop) / cellSize).toInt()

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

    private fun animateMonthChange(delta: Int) {
        if (isAnimating) return

        var newMonth = currentMonth + delta
        var newYear = currentYear

        if (newMonth > 12) {
            newMonth = 1
            newYear++
        } else if (newMonth < 1) {
            newMonth = 12
            newYear--
        }

        targetYear = newYear
        targetMonth = newMonth

        isAnimating = true
        animationProgress = 0f

        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                animationProgress = it.animatedValue as Float
                invalidate()
            }
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    isAnimating = false
                    currentYear = targetYear
                    currentMonth = targetMonth
                    selectedDay = 1
                    onMonthChangedListener?.invoke(currentYear, currentMonth)
                    invalidate()
                }
                override fun onAnimationCancel(animation: android.animation.Animator) {
                    isAnimating = false
                }
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
            })
            start()
        }
    }

    private fun changeYearWithAnimation(delta: Int) {
        if (isAnimating) return

        targetYear = currentYear + delta
        targetMonth = currentMonth

        isAnimating = true
        animationProgress = 0f

        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                animationProgress = it.animatedValue as Float
                invalidate()
            }
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    isAnimating = false
                    currentYear = targetYear
                    selectedDay = 1
                    onMonthChangedListener?.invoke(currentYear, currentMonth)
                    invalidate()
                }
                override fun onAnimationCancel(animation: android.animation.Animator) {
                    isAnimating = false
                }
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
            })
            start()
        }
    }

    private fun getDayAtPosition(row: Int, col: Int): Int {
        val firstDayOfWeek = DateUtils.getFirstDayOfMonth(currentYear, currentMonth)
        val day = row * 7 + col - firstDayOfWeek + 2
        val daysInMonth = DateUtils.getDaysInMonth(currentYear, currentMonth)
        return if (day in 1..daysInMonth) day else -1
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        yearBarHeight = h * 0.1f
        monthBarHeight = h * 0.1f
        weekBarHeight = h * 0.08f
        calendarTop = yearBarHeight + monthBarHeight + weekBarHeight

        val calendarHeight = h - calendarTop - 20f
        val maxCellSizeByWidth = w / 7f
        val maxCellSizeByHeight = calendarHeight / 6f
        cellSize = minOf(maxCellSizeByWidth, maxCellSizeByHeight)

        val totalWidth = cellSize * 7
        startX = (w - totalWidth) / 2

        val totalHeight = cellSize * 6
        startY = calendarTop + (calendarHeight - totalHeight) / 2
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 从主题获取当前主题色
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data

        // 获取浅色主题色（用于单项完成）
        val primaryLightColor = Color.argb(255,
            Color.red(primaryColor) + 40,
            Color.green(primaryColor) + 40,
            Color.blue(primaryColor) + 40)

        val textPrimaryColor = resources.getColor(R.color.text_primary, null)
        val textSecondaryColor = resources.getColor(R.color.text_secondary, null)

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
    }

    private fun drawCalendarWithAnimation(canvas: Canvas, primaryColor: Int, primaryLightColor: Int, textColor: Int) {
        val offset = (1 - animationProgress) * width

        canvas.save()
        canvas.translate(-offset, 0f)
        drawMonthGrid(canvas, currentYear, currentMonth, primaryColor, primaryLightColor, textColor, true)
        canvas.restore()

        canvas.save()
        canvas.translate(width - offset, 0f)
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
                                    bgPaint.color = primaryColor
                                    bgPaint.alpha = 100
                                    canvas.drawRect(left + 2, top + 2, right - 2, bottom - 2, bgPaint)
                                } else if (markData.hasPainting || markData.hasDiary) {
                                    bgPaint.color = primaryLightColor
                                    bgPaint.alpha = 100
                                    canvas.drawRect(left + 2, top + 2, right - 2, bottom - 2, bgPaint)
                                }
                            }
                        }
                        1 -> {
                            if (markData != null && markData.hasPainting) {
                                bgPaint.color = primaryLightColor
                                bgPaint.alpha = 100
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
                        borderPaint.color = primaryColor
                        borderPaint.strokeWidth = 4f
                        canvas.drawRect(left + 2, top + 2, right - 2, bottom - 2, borderPaint)
                    }

                    if (currentMode != 2 || diaryMoods[dateStr] == null) {
                        textPaint.textSize = cellSize * 0.35f
                        textPaint.color = textColor
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

        textPaint.textSize = 36f
        textPaint.color = textColor
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("$currentYear", width / 2f, y + 8, textPaint)

        textPaint.textSize = 40f
        textPaint.color = primaryColor
        canvas.drawText("◀", barStartX - 20, y + 8, textPaint)
        canvas.drawText("▶", barStartX + barWidth + 20, y + 8, textPaint)
    }

    private fun drawMonthTitle(canvas: Canvas, primaryColor: Int, textColor: Int) {
        val y = yearBarHeight + monthBarHeight / 2
        textPaint.textSize = 32f
        textPaint.color = textColor
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("${currentMonth}月", width / 2f, y + 8, textPaint)

        textPaint.textSize = 24f
        textPaint.color = primaryColor
        textPaint.alpha = 150
        canvas.drawText("◀ 滑动切换月份 ▶", width / 2f, y + 40, textPaint)
        textPaint.alpha = 255
    }

    private fun drawWeekDays(canvas: Canvas, textColor: Int) {
        val weekDays = arrayOf("一", "二", "三", "四", "五", "六", "日")
        val y = yearBarHeight + monthBarHeight + weekBarHeight / 2
        textPaint.textSize = cellSize * 0.3f
        textPaint.color = textColor
        textPaint.textAlign = Paint.Align.CENTER

        for (i in 0..6) {
            val x = startX + cellSize * i + cellSize / 2
            canvas.drawText(weekDays[i], x, y + 8, textPaint)
        }
    }

    private var currentMode = 0

    fun setMode(mode: Int) {
        currentMode = mode
        invalidate()
    }

    fun setMarkedDates(dates: Map<String, MarkData>) {
        markedDates.clear()
        markedDates.putAll(dates)
        invalidate()
    }

    fun setDiaryMoods(moods: Map<String, Mood>) {
        diaryMoods.clear()
        diaryMoods.putAll(moods)
        invalidate()
    }

    fun setSelectedDate(year: Int, month: Int, day: Int) {
        currentYear = year
        currentMonth = month
        selectedDay = day
        invalidate()
    }

    fun setOnDateSelectedListener(listener: (year: Int, month: Int, day: Int) -> Unit) {
        onDateSelectedListener = listener
    }

    fun setOnMonthChangedListener(listener: (year: Int, month: Int) -> Unit) {
        onMonthChangedListener = listener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }
}