package com.memoria.meaningoflife.ui.calendar

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.data.database.task.TaskPriority
import com.memoria.meaningoflife.model.Mood
import com.memoria.meaningoflife.utils.DateUtils

class CalendarFragment : Fragment() {

    private lateinit var viewModel: CalendarViewModel
    private lateinit var customCalendarView: CustomCalendarView
    private var currentMode = 0
    private var customTypeface: Typeface? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
            setBackgroundColor(ContextCompat.getColor(context, R.color.background))
        }

        try {
            customTypeface = ResourcesCompat.getFont(requireContext(), R.font.lxgw)
        } catch (e: Exception) {
            customTypeface = null
        }

        // 模式切换按钮
        val chipGroup = ChipGroup(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 8)
            }
            isSingleSelection = true
        }

        val chipAll = Chip(requireContext()).apply {
            id = View.generateViewId()
            text = "全部"
            isCheckable = true
            tag = 0
            customTypeface?.let { typeface = it }
        }
        val chipPainting = Chip(requireContext()).apply {
            id = View.generateViewId()
            text = "绘画"
            isCheckable = true
            tag = 1
            customTypeface?.let { typeface = it }
        }
        val chipDiary = Chip(requireContext()).apply {
            id = View.generateViewId()
            text = "日记"
            isCheckable = true
            tag = 2
            customTypeface?.let { typeface = it }
        }

        chipGroup.addView(chipAll)
        chipGroup.addView(chipPainting)
        chipGroup.addView(chipDiary)
        chipAll.isChecked = true
        rootView.addView(chipGroup)

        // 日历视图
        customCalendarView = CustomCalendarView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            setOnDateSelectedListener { year, month, day ->
                val date = "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
                viewModel.loadDayDetail(date, currentMode)
                viewModel.loadDrawerContent(date)
            }
            setOnMonthChangedListener { year, month ->
                val startDate = "$year-${month.toString().padStart(2, '0')}-01"
                val endDate = DateUtils.getLastDayOfMonth(year, month)
                viewModel.loadMarkedDates(startDate, endDate)
                viewModel.loadDiaryMoods(startDate, endDate)
            }
        }
        rootView.addView(customCalendarView)

        // 初始化 ViewModel
        viewModel = ViewModelProvider(this)[CalendarViewModel::class.java]

        // 设置模式切换监听
        chipGroup.setOnCheckedChangeListener { _, checkedId ->
            currentMode = when (checkedId) {
                chipAll.id -> 0
                chipPainting.id -> 1
                chipDiary.id -> 2
                else -> 0
            }
            customCalendarView.setMode(currentMode)

            val calendar = java.util.Calendar.getInstance()
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH) + 1
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
            val currentDate = "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
            viewModel.loadDayDetail(currentDate, currentMode)
            viewModel.loadDrawerContent(currentDate)
        }

        // 加载当月数据
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val startDate = "$year-${month.toString().padStart(2, '0')}-01"
        val endDate = DateUtils.getLastDayOfMonth(year, month)
        viewModel.loadMarkedDates(startDate, endDate)
        viewModel.loadDiaryMoods(startDate, endDate)

        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val currentDate = "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
        viewModel.loadDayDetail(currentDate, currentMode)
        viewModel.loadDrawerContent(currentDate)

        // 观察标记数据
        viewModel.markedDatesData.observe(viewLifecycleOwner) { data ->
            customCalendarView.setMarkedDates(data)
        }

        // 观察日记心情数据
        viewModel.diaryMoodsData.observe(viewLifecycleOwner) { moods ->
            customCalendarView.setDiaryMoods(moods)
        }

        // 观察抽屉内容数据 - 统一更新
        viewModel.drawerTasks.observe(viewLifecycleOwner) { _ ->
            updateDrawerContent()
        }
        viewModel.drawerDiaries.observe(viewLifecycleOwner) { _ ->
            updateDrawerContent()
        }
        viewModel.drawerPaintings.observe(viewLifecycleOwner) { paintings ->
            android.util.Log.d("CalendarFragment", "绘画记录更新: count=${paintings.size}")
            updateDrawerContent()
        }

        return rootView
    }

    private fun updateDrawerContent() {
        val tasks = viewModel.drawerTasks.value ?: emptyList()
        val diaries = viewModel.drawerDiaries.value ?: emptyList()
        val paintings = viewModel.drawerPaintings.value ?: emptyList()

        android.util.Log.d("CalendarFragment", "updateDrawerContent: tasks=${tasks.size}, diaries=${diaries.size}, paintings=${paintings.size}")

        val drawerTasks = tasks.map { task ->
            val priorityColor = when (task.getPriority()) {
                TaskPriority.URGENT_IMPORTANT -> ContextCompat.getColor(requireContext(), R.color.task_urgent_important)
                TaskPriority.URGENT_NOT_IMPORTANT -> ContextCompat.getColor(requireContext(), R.color.task_urgent_not_important)
                TaskPriority.NOT_URGENT_IMPORTANT -> ContextCompat.getColor(requireContext(), R.color.task_not_urgent_important)
                TaskPriority.NOT_URGENT_NOT_IMPORTANT -> ContextCompat.getColor(requireContext(), R.color.task_not_urgent_not_important)
            }
            CustomCalendarView.DrawerTaskItem(
                id = task.id,
                title = task.title,
                priority = "",
                priorityColor = priorityColor,
                deadline = task.deadline?.let { DateUtils.formatDate(it) }
            )
        }

        val drawerDiaries = diaries.map { diary ->
            CustomCalendarView.DrawerDiaryItem(
                id = diary.id,
                title = diary.title,
                content = diary.content.take(50),
                moodIcon = Mood.fromValue(diary.mood).icon
            )
        }

        val drawerPaintings = paintings.map { painting ->
            CustomCalendarView.DrawerPaintingItem(
                id = painting.id,
                title = painting.title,
                thumbnailPath = painting.finalImagePath
            )
        }

        customCalendarView.setDrawerContent(drawerTasks, drawerDiaries, drawerPaintings)
    }
}