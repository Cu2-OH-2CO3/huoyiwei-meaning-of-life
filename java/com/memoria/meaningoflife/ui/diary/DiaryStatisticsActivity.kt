package com.memoria.meaningoflife.ui.diary

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.databinding.ActivityDiaryStatisticsBinding
import com.memoria.meaningoflife.ui.BaseActivity
import com.memoria.meaningoflife.utils.DateUtils
import java.util.*

class DiaryStatisticsActivity : BaseActivity() {

    private lateinit var binding: ActivityDiaryStatisticsBinding
    private lateinit var viewModel: DiaryViewModel
    private var currentRange = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiaryStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "日记统计"

        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data
        supportActionBar?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(primaryColor))

        viewModel = ViewModelProvider(this)[DiaryViewModel::class.java]

        setupRangeSpinner()
        setupClickListeners()

        viewModel.allDiaries.observe(this) {
            loadStatistics()
        }

        loadStatistics()
    }

    private fun setupRangeSpinner() {
        val ranges = arrayOf("本周", "本月", "全部")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ranges)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRange.adapter = adapter

        binding.spinnerRange.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentRange = position
                loadStatistics()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadStatistics() {
        val diaries = viewModel.allDiaries.value ?: return

        val filteredDiaries = when (currentRange) {
            0 -> getThisWeekDiaries(diaries)
            1 -> getThisMonthDiaries(diaries)
            else -> diaries
        }

        val totalCount = filteredDiaries.size
        val totalWords = filteredDiaries.sumOf { it.content.length }
        val averageWords = if (totalCount > 0) totalWords / totalCount else 0

        binding.tvTotalCount.text = "$totalCount 篇"
        binding.tvTotalWords.text = "$totalWords 字"
        binding.tvAverageWords.text = "$averageWords 字"

        val wordFrequency = generateWordFrequency(filteredDiaries)
        val topWords = wordFrequency.entries.sortedByDescending { it.value }.take(10)

        if (topWords.isEmpty()) {
            binding.tvEmptyWords.visibility = View.VISIBLE
            binding.recyclerViewWords.visibility = View.GONE
        } else {
            binding.tvEmptyWords.visibility = View.GONE
            binding.recyclerViewWords.visibility = View.VISIBLE
            val wordAdapter = WordFrequencyAdapter(topWords)
            binding.recyclerViewWords.layoutManager = LinearLayoutManager(this)
            binding.recyclerViewWords.adapter = wordAdapter
        }
    }

    private fun getThisWeekDiaries(diaries: List<com.memoria.meaningoflife.data.database.diary.DiaryEntity>): List<com.memoria.meaningoflife.data.database.diary.DiaryEntity> {
        val calendar = Calendar.getInstance()
        val today = calendar.time
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val startOfWeek = calendar.time

        return diaries.filter {
            val date = DateUtils.parseDate(it.createdDate)
            date != null && date >= startOfWeek && date <= today
        }
    }

    private fun getThisMonthDiaries(diaries: List<com.memoria.meaningoflife.data.database.diary.DiaryEntity>): List<com.memoria.meaningoflife.data.database.diary.DiaryEntity> {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val startOfMonth = "$year-${(month + 1).toString().padStart(2, '0')}-01"
        val endOfMonth = DateUtils.getLastDayOfMonth(year, month + 1)

        return diaries.filter {
            it.createdDate >= startOfMonth && it.createdDate <= endOfMonth
        }
    }

    private fun generateWordFrequency(diaries: List<com.memoria.meaningoflife.data.database.diary.DiaryEntity>): Map<String, Int> {
        val frequency = mutableMapOf<String, Int>()
        val stopWords = setOf("的", "了", "是", "在", "我", "有", "和", "就", "不", "人", "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这", "那", "又", "但", "而", "与", "或", "被", "把", "给", "让", "从", "向", "对", "为", "以", "于", "之", "等", "便", "并", "还")

        diaries.forEach { diary ->
            val words = diary.content.split(Regex("[，。！？；：\"“”‘’（）《》【】、\\s]+"))
            words.forEach { word ->
                if (word.length >= 2 && word !in stopWords && word.matches(Regex("[\\u4e00-\\u9fa5]+"))) {
                    frequency[word] = frequency.getOrDefault(word, 0) + 1
                }
            }
        }

        return frequency
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}