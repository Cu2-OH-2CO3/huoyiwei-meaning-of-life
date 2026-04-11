// 路径：ui/timeline/schedule/CourseEditActivity.kt
package com.memoria.meaningoflife.ui.timeline.schedule

import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.data.database.timeline.CourseEntity
import com.memoria.meaningoflife.data.repository.TimelineRepository
import com.memoria.meaningoflife.MeaningOfLifeApp
import com.memoria.meaningoflife.ui.BaseActivity
import com.memoria.meaningoflife.utils.ThemeManager
import kotlinx.coroutines.launch
import java.util.*

class CourseEditActivity : BaseActivity() {

    private lateinit var repository: TimelineRepository

    private lateinit var etName: EditText
    private lateinit var etTeacher: EditText
    private lateinit var etLocation: EditText
    private lateinit var spinnerWeekDay: Spinner
    private lateinit var tvStartTime: TextView
    private lateinit var tvEndTime: TextView
    private lateinit var etWeekStart: EditText
    private lateinit var etWeekEnd: EditText
    private lateinit var gridWeekButtons: GridLayout
    private lateinit var etRemark: EditText
    private lateinit var btnSave: Button

    private var courseId: Long = 0
    private var existingCourse: CourseEntity? = null
    private var startTimeMillis: Long = 8 * 3600000L  // 默认 08:00
    private var endTimeMillis: Long = 10 * 3600000L   // 默认 10:00
    private var selectorTotalWeeks: Int = 16
    private val selectedWeeks = sortedSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_edit)
        ThemeManager.applyTheme(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        repository = TimelineRepository(MeaningOfLifeApp.instance.database)
        courseId = intent.getLongExtra("course_id", 0)

        setupViews()
        setupTimePickers()
        setupWeekButtons(selectorTotalWeeks)

        if (courseId == 0L) {
            lifecycleScope.launch {
                val semesterWeeks = repository.getCurrentSemester()?.totalWeeks ?: 16
                selectorTotalWeeks = semesterWeeks.coerceAtLeast(1)
                setupWeekButtons(selectorTotalWeeks)
            }
        }

        if (courseId != 0L) {
            supportActionBar?.title = "编辑课程"
            loadCourse()
        } else {
            supportActionBar?.title = "添加课程"
        }

        btnSave.setOnClickListener { saveCourse() }
    }

    private fun setupViews() {
        etName = findViewById(R.id.et_course_name)
        etTeacher = findViewById(R.id.et_teacher)
        etLocation = findViewById(R.id.et_location)
        spinnerWeekDay = findViewById(R.id.spinner_week_day)
        tvStartTime = findViewById(R.id.tv_start_time)
        tvEndTime = findViewById(R.id.tv_end_time)
        etWeekStart = findViewById(R.id.et_week_start)
        etWeekEnd = findViewById(R.id.et_week_end)
        gridWeekButtons = findViewById(R.id.grid_week_buttons)
        etRemark = findViewById(R.id.et_remark)
        btnSave = findViewById(R.id.btn_save)

        val weekDays = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        spinnerWeekDay.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, weekDays)

        etWeekStart.setText("1")
        etWeekEnd.setText("16")

        btnSave.backgroundTintList = ColorStateList.valueOf(ThemeManager.resolvePrimaryColor(this))
        btnSave.setTextColor(resolveThemeColor(com.google.android.material.R.attr.colorOnPrimary))
    }

    private fun setupTimePickers() {
        tvStartTime.setOnClickListener {
            showTimePicker(true)
        }

        tvEndTime.setOnClickListener {
            showTimePicker(false)
        }

        updateTimeDisplay()
    }

    private fun showTimePicker(isStart: Boolean) {
        val currentTime = if (isStart) startTimeMillis else endTimeMillis
        val hour = (currentTime / 3600000).toInt()
        val minute = ((currentTime % 3600000) / 60000).toInt()

        TimePickerDialog(this, { _, hourOfDay, minuteOfHour ->
            if (isStart) {
                startTimeMillis = hourOfDay * 3600000L + minuteOfHour * 60000L
            } else {
                endTimeMillis = hourOfDay * 3600000L + minuteOfHour * 60000L
            }
            updateTimeDisplay()
        }, hour, minute, true).show()
    }

    // 修改 updateTimeDisplay 方法，使用本地化格式
    private fun updateTimeDisplay() {
        tvStartTime.text = formatTime(startTimeMillis)
        tvEndTime.text = formatTime(endTimeMillis)
    }

    private fun formatTime(timeInMillis: Long): String {
        val hour = timeInMillis / 3600000
        val minute = (timeInMillis % 3600000) / 60000
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }



    private fun loadCourse() {
        lifecycleScope.launch {
            existingCourse = repository.getCourseById(courseId)  // 使用 repository 直接调用
            existingCourse?.let { course ->
                val semesterWeeks = repository.getCurrentSemester()?.totalWeeks ?: 16
                selectorTotalWeeks = maxOf(semesterWeeks, course.weekEnd, 1)
                setupWeekButtons(selectorTotalWeeks)

                etName.setText(course.name)
                etTeacher.setText(course.teacher ?: "")
                etLocation.setText(course.location ?: "")
                spinnerWeekDay.setSelection(course.weekDay - 1)
                startTimeMillis = course.startTime
                endTimeMillis = course.endTime
                updateTimeDisplay()
                etWeekStart.setText(course.weekStart.toString())
                etWeekEnd.setText(course.weekEnd.toString())
                selectedWeeks.clear()
                CourseWeekPattern.parseExplicitWeeks(course.remark)
                    ?.filter { it in 1..selectorTotalWeeks }
                    ?.let { selectedWeeks.addAll(it) }
                updateWeekButtonsState()
                etRemark.setText(CourseWeekPattern.stripWeekMarker(course.remark) ?: "")
            }
        }
    }

    private fun saveCourse() {
        val name = etName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入课程名称", Toast.LENGTH_SHORT).show()
            return
        }

        val weekDay = spinnerWeekDay.selectedItemPosition + 1
        var weekStart = etWeekStart.text.toString().toIntOrNull() ?: 1
        var weekEnd = etWeekEnd.text.toString().toIntOrNull() ?: 16
        if (weekEnd < weekStart) {
            val temp = weekStart
            weekStart = weekEnd
            weekEnd = temp
        }
        val explicitWeeks = selectedWeeks.toSortedSet()

        if (explicitWeeks.isNotEmpty()) {
            weekStart = explicitWeeks.minOrNull() ?: weekStart
            weekEnd = explicitWeeks.maxOrNull() ?: weekEnd
        }

        if (startTimeMillis >= endTimeMillis) {
            Toast.makeText(this, "结束时间必须晚于开始时间", Toast.LENGTH_SHORT).show()
            return
        }

        val plainRemark = etRemark.text.toString().takeIf { it.isNotBlank() }
        val mergedRemark = CourseWeekPattern.mergeRemarkWithWeeks(
            plainRemark = plainRemark,
            explicitWeeks = explicitWeeks.takeIf { it.isNotEmpty() }
        )

        val course = if (existingCourse != null) {
            existingCourse!!.copy(
                name = name,
                teacher = etTeacher.text.toString().takeIf { it.isNotEmpty() },
                location = etLocation.text.toString().takeIf { it.isNotEmpty() },
                weekDay = weekDay,
                startTime = startTimeMillis,
                endTime = endTimeMillis,
                weekStart = weekStart,
                weekEnd = weekEnd,
                isOddWeek = false,
                isEvenWeek = false,
                remark = mergedRemark
            )
        } else {
            CourseEntity(
                name = name,
                teacher = etTeacher.text.toString().takeIf { it.isNotEmpty() },
                location = etLocation.text.toString().takeIf { it.isNotEmpty() },
                weekDay = weekDay,
                startTime = startTimeMillis,
                endTime = endTimeMillis,
                weekStart = weekStart,
                weekEnd = weekEnd,
                isOddWeek = false,
                isEvenWeek = false,
                remark = mergedRemark
            )
        }

        lifecycleScope.launch {
            if (existingCourse != null) {
                repository.updateCourse(course)
                Toast.makeText(this@CourseEditActivity, "更新成功", Toast.LENGTH_SHORT).show()
            } else {
                repository.insertCourse(course)
                Toast.makeText(this@CourseEditActivity, "添加成功", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupWeekButtons(totalWeeks: Int) {
        gridWeekButtons.removeAllViews()
        gridWeekButtons.columnCount = 5

        val buttonSize = dpToPx(52)
        val buttonMargin = dpToPx(6)

        for (week in 1..totalWeeks) {
            val button = MaterialButton(this).apply {
                text = week.toString()
                tag = week
                isAllCaps = false
                textSize = 14f
                insetTop = 0
                insetBottom = 0
                minWidth = 0
                minHeight = 0
                minimumWidth = 0
                minimumHeight = 0
                cornerRadius = buttonSize / 2
                strokeWidth = dpToPx(1)
                isSingleLine = true
                maxLines = 1
                ellipsize = null
                setPadding(0, 0, 0, 0)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = buttonSize
                    height = buttonSize
                    setMargins(buttonMargin, buttonMargin, buttonMargin, buttonMargin)
                }
                setOnClickListener {
                    val selectedWeek = tag as Int
                    if (selectedWeek in selectedWeeks) {
                        selectedWeeks.remove(selectedWeek)
                    } else {
                        selectedWeeks.add(selectedWeek)
                    }
                    updateWeekButtonsState()
                }
            }
            gridWeekButtons.addView(button)
        }

        updateWeekButtonsState()
    }

    private fun updateWeekButtonsState() {
        val primary = ThemeManager.resolvePrimaryColor(this)
        val onPrimary = resolveThemeColor(com.google.android.material.R.attr.colorOnPrimary)
        val surface = resolveThemeColor(com.google.android.material.R.attr.colorSurface)
        val onSurface = resolveThemeColor(com.google.android.material.R.attr.colorOnSurface)

        for (index in 0 until gridWeekButtons.childCount) {
            val child = gridWeekButtons.getChildAt(index) as? MaterialButton ?: continue
            val week = child.tag as? Int ?: continue
            val selected = week in selectedWeeks

            if (selected) {
                child.backgroundTintList = ColorStateList.valueOf(primary)
                child.strokeColor = ColorStateList.valueOf(primary)
                child.setTextColor(onPrimary)
            } else {
                child.backgroundTintList = ColorStateList.valueOf(surface)
                child.strokeColor = ColorStateList.valueOf(primary)
                child.setTextColor(onSurface)
            }
        }
    }

    private fun resolveThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return when {
            typedValue.resourceId != 0 -> ContextCompat.getColor(this, typedValue.resourceId)
            typedValue.data != 0 -> typedValue.data
            else -> ThemeManager.resolvePrimaryColor(this)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}