package com.memoria.meaningoflife.ui.diary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.model.Mood
import com.memoria.meaningoflife.utils.DateUtils

class DiaryCalendarFragment : Fragment() {

    private lateinit var viewModel: DiaryViewModel
    private lateinit var tvContent: TextView
    private lateinit var tvMood: TextView
    private lateinit var tvDate: TextView
    private lateinit var cardPreview: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        // 添加日历视图
        val calendarView = CalendarView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        rootView.addView(calendarView)

        // 添加预览卡片
        cardPreview = android.widget.FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 16
            }
            visibility = View.GONE

            val cardInner = androidx.cardview.widget.CardView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                radius = 8f
                elevation = 2f
                setCardBackgroundColor(resources.getColor(R.color.card_background, null))
            }

            val innerLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
            }

            tvDate = TextView(requireContext()).apply {
                text = ""
                textSize = 14f
                setTextColor(resources.getColor(R.color.text_secondary, null))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = 8
                layoutParams = params
            }

            tvMood = TextView(requireContext()).apply {
                text = ""
                textSize = 14f
                setTextColor(resources.getColor(R.color.accent, null))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = 8
                layoutParams = params
            }

            tvContent = TextView(requireContext()).apply {
                text = ""
                textSize = 14f
                setTextColor(resources.getColor(R.color.text_primary, null))
                maxLines = 3
                ellipsize = android.text.TextUtils.TruncateAt.END
            }

            innerLayout.addView(tvDate)
            innerLayout.addView(tvMood)
            innerLayout.addView(tvContent)
            cardInner.addView(innerLayout)
            addView(cardInner)
        }
        rootView.addView(cardPreview)

        // 设置日历监听
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val dateStr = "$year-${(month + 1).toString().padStart(2, '0')}-${dayOfMonth.toString().padStart(2, '0')}"
            loadDiaryForDate(dateStr)
        }

        viewModel = ViewModelProvider(requireActivity())[DiaryViewModel::class.java]

        // 加载今天的日记
        val today = java.util.Calendar.getInstance()
        val todayStr = "${today.get(java.util.Calendar.YEAR)}-${(today.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')}-${today.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')}"
        loadDiaryForDate(todayStr)

        return rootView
    }

    private fun loadDiaryForDate(date: String) {
        val diaries = viewModel.allDiaries.value ?: return
        val diary = diaries.find { it.createdDate == date }

        if (diary != null) {
            val mood = Mood.fromValue(diary.mood)
            tvContent.text = diary.content.take(200) + if (diary.content.length > 200) "..." else ""
            tvMood.text = "${mood.icon} ${mood.text}"
            tvDate.text = DateUtils.formatDate(date)
            cardPreview.visibility = View.VISIBLE

            cardPreview.setOnClickListener {
                val intent = android.content.Intent(requireContext(), DiaryDetailActivity::class.java)
                intent.putExtra("diary_id", diary.id)
                startActivity(intent)
            }
        } else {
            cardPreview.visibility = View.GONE
        }
    }
}