package com.memoria.meaningoflife.ui.calendar

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.model.Mood
import com.memoria.meaningoflife.utils.DateUtils

class CalendarFragment : Fragment() {

    private lateinit var viewModel: CalendarViewModel
    private lateinit var tvDetailDate: TextView
    private lateinit var tvDetailPainting: TextView
    private lateinit var tvDetailDiary: TextView
    private lateinit var customCalendarView: CustomCalendarView
    private var currentMode = 0 // 0=全部, 1=绘画, 2=日记
    private var customTypeface: Typeface? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
            setBackgroundColor(resources.getColor(R.color.background, null))
        }

        // 加载字体
        try {
            customTypeface = ResourcesCompat.getFont(requireContext(), R.font.lxgw)
        } catch (e: Exception) {
            customTypeface = null
        }

        // 添加模式切换按钮
        val chipGroup = com.google.android.material.chip.ChipGroup(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 8)
            }
            isSingleSelection = true
        }

        val chipAll = com.google.android.material.chip.Chip(requireContext()).apply {
            id = View.generateViewId()
            text = "全部"
            isCheckable = true
            tag = 0
            customTypeface?.let { typeface = it }
        }
        val chipPainting = com.google.android.material.chip.Chip(requireContext()).apply {
            id = View.generateViewId()
            text = "绘画"
            isCheckable = true
            tag = 1
            customTypeface?.let { typeface = it }
        }
        val chipDiary = com.google.android.material.chip.Chip(requireContext()).apply {
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

        // 添加自定义日历视图
        customCalendarView = CustomCalendarView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            setOnDateSelectedListener { year, month, day ->
                val date = "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
                viewModel.loadDayDetail(date, currentMode)
            }
            setOnMonthChangedListener { year, month ->
                val startDate = "$year-${month.toString().padStart(2, '0')}-01"
                val endDate = DateUtils.getLastDayOfMonth(year, month)
                viewModel.loadMarkedDates(startDate, endDate)
                viewModel.loadDiaryMoods(startDate, endDate)
            }
        }
        rootView.addView(customCalendarView)

        // 添加详情区域卡片
        val detailCard = android.widget.ScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 8, 16, 16)
            }
        }

        val detailContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(resources.getColor(R.color.card_background, null))
            setBackgroundResource(R.drawable.card_background)
        }

        tvDetailDate = TextView(requireContext()).apply {
            text = "请选择日期"
            textSize = 16f
            setTextColor(resources.getColor(R.color.text_primary, null))
            customTypeface?.let { typeface = it }
            setTypeface(customTypeface, Typeface.BOLD)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = 12
            layoutParams = params
        }

        tvDetailPainting = TextView(requireContext()).apply {
            text = "无绘画记录"
            textSize = 14f
            setTextColor(resources.getColor(R.color.text_secondary, null))
            customTypeface?.let { typeface = it }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = 8
            layoutParams = params
        }

        tvDetailDiary = TextView(requireContext()).apply {
            text = "无日记记录"
            textSize = 14f
            setTextColor(resources.getColor(R.color.text_secondary, null))
            customTypeface?.let { typeface = it }
        }

        detailContainer.addView(tvDetailDate)
        detailContainer.addView(tvDetailPainting)
        detailContainer.addView(tvDetailDiary)
        detailCard.addView(detailContainer)
        rootView.addView(detailCard)

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

            // 刷新当前日期详情
            val calendar = java.util.Calendar.getInstance()
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH) + 1
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
            val currentDate = "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
            viewModel.loadDayDetail(currentDate, currentMode)
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

        // 观察标记数据
        viewModel.markedDatesData.observe(viewLifecycleOwner) { data ->
            customCalendarView.setMarkedDates(data)
        }

        // 观察日记心情数据
        viewModel.diaryMoodsData.observe(viewLifecycleOwner) { moods ->
            customCalendarView.setDiaryMoods(moods)
        }

        // 观察详情数据
        viewModel.selectedDayData.observe(viewLifecycleOwner) { dayData ->
            if (dayData == null) {
                tvDetailDate.text = "请选择日期"
                tvDetailPainting.text = "无记录"
                tvDetailDiary.text = "无记录"
                return@observe
            }

            tvDetailDate.text = DateUtils.formatDate(dayData.date)

            when (currentMode) {
                0 -> {
                    if (dayData.hasPainting) {
                        tvDetailPainting.text = "✅ 绘画：${dayData.paintingCount}幅作品"
                        tvDetailPainting.setTextColor(resources.getColor(R.color.calendar_painting, null))
                    } else {
                        tvDetailPainting.text = "❌ 无绘画记录"
                        tvDetailPainting.setTextColor(resources.getColor(R.color.text_secondary, null))
                    }

                    if (dayData.hasDiary) {
                        val moodText = dayData.diaryMood?.let { "${it.icon} ${it.text}" } ?: ""
                        tvDetailDiary.text = "✅ 日记：$moodText"
                        tvDetailDiary.setTextColor(resources.getColor(R.color.calendar_diary, null))
                    } else {
                        tvDetailDiary.text = "❌ 无日记"
                        tvDetailDiary.setTextColor(resources.getColor(R.color.text_secondary, null))
                    }
                }
                1 -> {
                    if (dayData.hasPainting) {
                        tvDetailPainting.text = "✅ 完成${dayData.paintingCount}幅作品"
                        tvDetailPainting.setTextColor(resources.getColor(R.color.calendar_painting, null))
                    } else {
                        tvDetailPainting.text = "❌ 无绘画记录"
                        tvDetailPainting.setTextColor(resources.getColor(R.color.text_secondary, null))
                    }
                    tvDetailDiary.text = ""
                }
                2 -> {
                    if (dayData.hasDiary) {
                        val moodText = dayData.diaryMood?.let { "${it.icon} ${it.text}" } ?: ""
                        tvDetailDiary.text = "✅ 已记录 $moodText"
                        tvDetailDiary.setTextColor(resources.getColor(R.color.calendar_diary, null))
                    } else {
                        tvDetailDiary.text = "❌ 无日记"
                        tvDetailDiary.setTextColor(resources.getColor(R.color.text_secondary, null))
                    }
                    tvDetailPainting.text = ""
                }
            }
        }

        return rootView
    }
}