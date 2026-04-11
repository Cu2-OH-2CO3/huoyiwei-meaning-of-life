// 路径：ui/timeline/schedule/ScheduleFragment.kt
package com.memoria.meaningoflife.ui.timeline.schedule

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.data.database.timeline.CourseEntity
import com.memoria.meaningoflife.data.database.timeline.EventType
import com.memoria.meaningoflife.data.repository.TimelineRepository
import com.memoria.meaningoflife.databinding.FragmentScheduleBinding
import com.memoria.meaningoflife.MeaningOfLifeApp
import com.memoria.meaningoflife.utils.PreferenceManager
import com.memoria.meaningoflife.utils.ThemeManager
import kotlinx.coroutines.launch
import java.util.*

class ScheduleFragment : Fragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ScheduleViewModel
    private lateinit var repository: TimelineRepository
    private lateinit var scheduleWeekView: ScheduleWeekView
    private var currentWeek = 1
    private var totalWeeks = 16
    private var showNonCourseEvents = false
    private lateinit var dayNameViews: List<TextView>
    private lateinit var dayDateViews: List<TextView>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = TimelineRepository(MeaningOfLifeApp.instance.database)
        viewModel = ViewModelProvider(this, ScheduleViewModelFactory(repository))[ScheduleViewModel::class.java]

        setupWeekView()
        setupDateRow()
        setupWeekNavigator()
        setupVisibilitySwitch()
        setupFab()
        observeData()
        loadCurrentSemester()
    }

    private fun setupWeekView() {
        scheduleWeekView = binding.scheduleWeekView
        scheduleWeekView.setOnCourseClickListener { course ->
            showCourseDetailDialog(course)
        }
        scheduleWeekView.setOnCourseLongClickListener { course ->
            showCourseOptionsDialog(course)
        }
    }

    private fun setupWeekNavigator() {
        binding.btnPrevWeek.setOnClickListener {
            if (currentWeek > 1) {
                currentWeek--
                updateWeekDisplay()
                loadWeekCourses()
            }
        }

        binding.btnNextWeek.setOnClickListener {
            if (currentWeek < totalWeeks) {
                currentWeek++
                updateWeekDisplay()
                loadWeekCourses()
            }
        }
    }

    private fun setupDateRow() {
        dayNameViews = listOf(
            binding.tvDayName1,
            binding.tvDayName2,
            binding.tvDayName3,
            binding.tvDayName4,
            binding.tvDayName5,
            binding.tvDayName6,
            binding.tvDayName7
        )
        dayDateViews = listOf(
            binding.tvDayDate1,
            binding.tvDayDate2,
            binding.tvDayDate3,
            binding.tvDayDate4,
            binding.tvDayDate5,
            binding.tvDayDate6,
            binding.tvDayDate7
        )
    }

    private fun setupVisibilitySwitch() {
        showNonCourseEvents = PreferenceManager.isScheduleNonCourseVisible(requireContext())
        ThemeManager.tintSwitch(binding.switchShowNonCourseEvents)
        binding.switchShowNonCourseEvents.isChecked = showNonCourseEvents
        binding.switchShowNonCourseEvents.setOnCheckedChangeListener { _, isChecked ->
            showNonCourseEvents = isChecked
            PreferenceManager.setScheduleNonCourseVisible(requireContext(), isChecked)
            loadWeekCourses()
        }
    }

    private fun setupFab() {
        binding.fabAddCourse.setOnClickListener {
            startActivity(Intent(requireContext(), CourseEditActivity::class.java))
        }

        binding.fabImportIcs.setOnClickListener {
            startActivity(Intent(requireContext(), IcsImportActivity::class.java))
        }
    }

    private fun updateWeekDisplay() {
        val weekText = String.format(Locale.getDefault(), "第 %d 周 / 共 %d 周", currentWeek, totalWeeks)
        binding.tvWeekInfo.text = weekText
    }

    // 修复 loadWeekCourses 方法
    private fun loadWeekCourses() {
        lifecycleScope.launch {
            val semester = repository.getCurrentSemester()
            if (semester != null) {
                val semesterCourses = repository.getCoursesBySemester(semester.id)
                val weekRange = getCurrentWeekRange(semester.startDate, currentWeek)
                scheduleWeekView.setWeekStartDate(weekRange.first)
                updateDateRow(weekRange.first)
                val filteredCourses = semesterCourses.filter { course ->
                    shouldShowCourse(course, currentWeek)
                }.toMutableList()

                if (showNonCourseEvents) {
                    val nonCourseEvents = repository.getEventsInRange(weekRange.first, weekRange.second)
                        .filter { event ->
                            event.eventType != EventType.COURSE &&
                                    event.startTime in weekRange.first..weekRange.second
                        }
                    filteredCourses.addAll(nonCourseEvents.map { mapEventToVirtualCourse(it, currentWeek) })
                }

                val sorted = filteredCourses.sortedWith(compareBy<CourseEntity> { it.weekDay }.thenBy { it.startTime })
                scheduleWeekView.setCourses(sorted, currentWeek, semesterCourses)
            }
        }
    }

    private fun getCurrentWeekRange(semesterStartDate: Long, week: Int): Pair<Long, Long> {
        val weekStart = semesterStartDate + (week - 1L) * 7L * 24L * 3600_000L
        val weekEnd = weekStart + 7L * 24L * 3600_000L - 1L
        return weekStart to weekEnd
    }

    private fun updateDateRow(weekStartMillis: Long) {
        val weekNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        val today = Calendar.getInstance()
        val highlightBg = ContextCompat.getColor(requireContext(), R.color.nd_surface_raised)
        val transparent = android.graphics.Color.TRANSPARENT
        val accent = ContextCompat.getColor(requireContext(), R.color.nd_accent)
        val secondary = ContextCompat.getColor(requireContext(), R.color.nd_text_secondary)
        val primary = ContextCompat.getColor(requireContext(), R.color.nd_text_primary)

        val base = Calendar.getInstance().apply { timeInMillis = weekStartMillis }
        binding.tvMonthLabel.text = "${base.get(Calendar.MONTH) + 1}月"

        for (i in 0 until 7) {
            val c = Calendar.getInstance().apply {
                timeInMillis = weekStartMillis
                add(Calendar.DAY_OF_YEAR, i)
            }

            dayNameViews[i].text = weekNames[i]
            dayDateViews[i].text = c.get(Calendar.DAY_OF_MONTH).toString()

            val isToday = today.get(Calendar.YEAR) == c.get(Calendar.YEAR) &&
                    today.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR)

            dayDateViews[i].setBackgroundColor(if (isToday) highlightBg else transparent)
            dayDateViews[i].setTextColor(if (isToday) accent else primary)
            dayDateViews[i].typeface = if (isToday) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            dayNameViews[i].setTextColor(if (isToday) accent else secondary)
        }
    }

    private fun mapEventToVirtualCourse(
        event: com.memoria.meaningoflife.data.database.timeline.TimelineEventEntity,
        week: Int
    ): CourseEntity {
        val calendar = Calendar.getInstance().apply { timeInMillis = event.startTime }
        val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 7
        }

        val startTimeOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 3600000L +
                calendar.get(Calendar.MINUTE) * 60000L

        val endMillis = event.endTime ?: (event.startTime + 60 * 60 * 1000L)
        val endCalendar = Calendar.getInstance().apply { timeInMillis = endMillis }
        val endTimeOfDay = endCalendar.get(Calendar.HOUR_OF_DAY) * 3600000L +
                endCalendar.get(Calendar.MINUTE) * 60000L

        return CourseEntity(
            id = -event.id,
            name = event.title,
            teacher = "非课程事件",
            location = event.location,
            weekDay = dayOfWeek,
            startTime = startTimeOfDay,
            endTime = if (endTimeOfDay <= startTimeOfDay) startTimeOfDay + 60 * 60 * 1000L else endTimeOfDay,
            weekStart = week,
            weekEnd = week,
            remark = event.description,
            icsUid = event.icsUid,
            semesterId = null
        )
    }

    private fun isVirtualEventCourse(course: CourseEntity): Boolean {
        return course.id < 0
    }

    private fun shouldShowCourse(course: CourseEntity, week: Int): Boolean {
        return CourseWeekPattern.shouldShowInWeek(course, week)
    }

    private fun loadCurrentSemester() {
        lifecycleScope.launch {
            val semester = repository.getCurrentSemester()
            semester?.let {
                totalWeeks = it.totalWeeks
                currentWeek = calculateCurrentWeek(it.startDate, totalWeeks)
                updateWeekDisplay()
                loadWeekCourses()
            }
        }
    }

    private fun calculateCurrentWeek(semesterStartDate: Long, totalWeeks: Int): Int {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val semesterStart = Calendar.getInstance().apply {
            timeInMillis = semesterStartDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val diffDays = ((todayStart - semesterStart) / (24L * 3600L * 1000L)).toInt()
        val week = diffDays / 7 + 1
        return week.coerceIn(1, totalWeeks.coerceAtLeast(1))
    }

    override fun onResume() {
        super.onResume()
        // 从导入页返回后立即刷新课表，避免显示旧学期数据。
        loadCurrentSemester()
    }

    private fun observeData() {
        viewModel.allCourses.observe(viewLifecycleOwner) {
            // 统一走 loadWeekCourses，避免观察器覆盖当前周筛选与动态时间轴。
            loadWeekCourses()
        }
    }

    private fun showCourseDetailDialog(course: CourseEntity) {
        if (isVirtualEventCourse(course)) {
            val dialog = AlertDialog.Builder(requireContext())
                .setTitle("事件详情")
                .setMessage(course.remark ?: "无描述")
                .setPositiveButton("关闭", null)
                .show()
            tintDialogButtons(dialog)
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_course_detail, null)

        val tvName = dialogView.findViewById<TextView>(R.id.tv_course_name)
        val tvTeacher = dialogView.findViewById<TextView>(R.id.tv_teacher)
        val tvLocation = dialogView.findViewById<TextView>(R.id.tv_location)
        val tvTime = dialogView.findViewById<TextView>(R.id.tv_time)
        val tvWeekInfo = dialogView.findViewById<TextView>(R.id.tv_week_info)
        val tvRemark = dialogView.findViewById<TextView>(R.id.tv_remark)

        tvName.text = course.name
        tvTeacher.text = course.teacher ?: "未设置"
        tvLocation.text = course.location ?: "未设置"

        val startHour = course.startTime / 3600000
        val startMinute = (course.startTime % 3600000) / 60000
        val endHour = course.endTime / 3600000
        val endMinute = (course.endTime % 3600000) / 60000
        val dayName = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日").getOrElse(course.weekDay - 1) { "周${course.weekDay}" }
        val timeStr = String.format(Locale.getDefault(), "%s %02d:%02d - %02d:%02d", dayName, startHour, startMinute, endHour, endMinute)
        tvTime.text = timeStr

        tvWeekInfo.text = CourseWeekPattern.buildWeekInfoText(course)
        tvRemark.text = CourseWeekPattern.stripWeekMarker(course.remark) ?: "无"

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("课程详情")
            .setView(dialogView)
            .setPositiveButton("关闭", null)
            .setNeutralButton("编辑周次") { _, _ ->
                showWeekSelectionDialog(course)
            }
            .setNegativeButton("编辑") { _, _ ->
                val intent = Intent(requireContext(), CourseEditActivity::class.java)
                intent.putExtra("course_id", course.id)
                startActivity(intent)
            }
            .show()
        tintDialogButtons(dialog)
    }

    private fun showWeekSelectionDialog(course: CourseEntity) {
        val maxWeeks = totalWeeks.coerceAtLeast(course.weekEnd).coerceAtLeast(1)
        val items = Array(maxWeeks) { index -> "第${index + 1}周" }
        val selectedWeeks = CourseWeekPattern.resolveWeeks(course, maxWeeks).toMutableSet()
        val checked = BooleanArray(maxWeeks) { index -> (index + 1) in selectedWeeks }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("编辑上课周次")
            .setMultiChoiceItems(items, checked) { _, which, isChecked ->
                val week = which + 1
                if (isChecked) selectedWeeks.add(week) else selectedWeeks.remove(week)
            }
            .setPositiveButton("保存") { _, _ ->
                if (selectedWeeks.isEmpty()) {
                    Toast.makeText(requireContext(), "请至少选择一周", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val sortedWeeks = selectedWeeks.toSortedSet()
                val updatedCourse = course.copy(
                    weekStart = sortedWeeks.first(),
                    weekEnd = sortedWeeks.last(),
                    isOddWeek = false,
                    isEvenWeek = false,
                    remark = CourseWeekPattern.mergeRemarkWithWeeks(
                        CourseWeekPattern.stripWeekMarker(course.remark),
                        sortedWeeks
                    )
                )

                lifecycleScope.launch {
                    repository.updateCourse(updatedCourse)
                    Toast.makeText(requireContext(), "周次已更新", Toast.LENGTH_SHORT).show()
                    loadWeekCourses()
                }
            }
            .setNegativeButton("取消", null)
            .show()
        tintDialogButtons(dialog)
    }

    private fun showCourseOptionsDialog(course: CourseEntity) {
        if (isVirtualEventCourse(course)) {
            Toast.makeText(requireContext(), "非课程事件请在里模式中编辑", Toast.LENGTH_SHORT).show()
            return
        }

        val options = listOf("编辑", "删除")

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(course.name)
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(requireContext(), CourseEditActivity::class.java)
                        intent.putExtra("course_id", course.id)
                        startActivity(intent)
                    }
                    1 -> {
                        val deleteDialog = AlertDialog.Builder(requireContext())
                            .setTitle("删除课程")
                            .setMessage("确定要删除「${course.name}」吗？")
                            .setPositiveButton("删除") { _, _ ->
                                lifecycleScope.launch {
                                    repository.deleteCourse(course.id)
                                    Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show()
                                    loadWeekCourses()
                                }
                            }
                            .setNegativeButton("取消", null)
                            .show()
                        tintDialogButtons(deleteDialog)
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
        tintDialogButtons(dialog)
    }

    private fun tintDialogButtons(dialog: AlertDialog) {
        val primary = ThemeManager.resolvePrimaryColor(requireContext())
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(primary)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(primary)
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(primary)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}