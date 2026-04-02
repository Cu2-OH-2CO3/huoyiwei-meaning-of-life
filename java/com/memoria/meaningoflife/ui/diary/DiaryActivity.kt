package com.memoria.meaningoflife.ui.diary

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.PopupMenu
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.databinding.ActivityDiaryBinding
import com.memoria.meaningoflife.ui.BaseActivity
import com.memoria.meaningoflife.utils.JsonHelper

class DiaryActivity : BaseActivity() {

    private lateinit var binding: ActivityDiaryBinding
    private lateinit var viewModel: DiaryViewModel
    private lateinit var adapter: DiaryListAdapter
    private var currentTag: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.diary_title)

        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data
        supportActionBar?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(primaryColor))

        binding.fabAdd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primaryColor))
        binding.btnTagFilter.setTextColor(primaryColor)
        binding.btnTagFilter.setBackgroundResource(R.drawable.button_outline_primary)
        binding.btnStatistics.setTextColor(primaryColor)
        binding.btnStatistics.setBackgroundResource(R.drawable.button_outline_primary)

        setupViewModel()
        setupRecyclerView()
        setupClickListeners()
        observeData()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[DiaryViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = DiaryListAdapter { diary ->
            val intent = Intent(this, DiaryDetailActivity::class.java)
            intent.putExtra("diary_id", diary.id)
            startActivity(intent)
        }

        binding.diaryRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@DiaryActivity)
            adapter = this@DiaryActivity.adapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, DiaryWriteActivity::class.java))
        }

        binding.btnTagFilter.setOnClickListener {
            showTagFilterPopup()
        }

        binding.btnStatistics.setOnClickListener {
            startActivity(Intent(this, DiaryStatisticsActivity::class.java))
        }
    }

    private fun showTagFilterPopup() {
        val popupMenu = PopupMenu(this, binding.btnTagFilter)
        popupMenu.menu.add(0, 0, 0, "全部")

        val allDiaries = viewModel.allDiaries.value ?: emptyList()
        val allTags = mutableSetOf<String>()
        allDiaries.forEach { diary ->
            diary.tags?.let { tagsJson ->
                val tags = JsonHelper.jsonToTags(tagsJson)
                allTags.addAll(tags)
            }
        }

        var order = 1
        allTags.sorted().forEach { tag ->
            popupMenu.menu.add(0, order, order, tag)
            order++
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            val selectedTag = menuItem.title.toString()
            if (selectedTag == "全部") {
                currentTag = null
                binding.btnTagFilter.text = "全部 ▼"
            } else {
                currentTag = selectedTag
                binding.btnTagFilter.text = "$selectedTag ▼"
            }
            filterDiariesByTag()
            true
        }

        popupMenu.show()
    }

    private fun filterDiariesByTag() {
        val allDiaries = viewModel.allDiaries.value ?: emptyList()

        val filteredDiaries = if (currentTag == null) {
            allDiaries
        } else {
            allDiaries.filter { diary ->
                diary.tags?.let { tagsJson ->
                    val tags = JsonHelper.jsonToTags(tagsJson)
                    tags.contains(currentTag)
                } ?: false
            }
        }

        adapter.submitList(filteredDiaries)

        if (filteredDiaries.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.diaryRecyclerView.visibility = View.GONE
            if (currentTag != null) {
                binding.tvEmpty.text = "没有「${currentTag}」标签的日记"
            } else {
                binding.tvEmpty.text = "暂无日记，点击右下角写一篇吧"
            }
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.diaryRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun observeData() {
        viewModel.allDiaries.observe(this) { diaries ->
            if (currentTag == null) {
                adapter.submitList(diaries)
                if (diaries.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.diaryRecyclerView.visibility = View.GONE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    binding.diaryRecyclerView.visibility = View.VISIBLE
                }
            } else {
                filterDiariesByTag()
            }
        }

        viewModel.consecutiveDays.observe(this) { days ->
            binding.tvConsecutiveDays.text = "连续写日记 $days 天"
        }

        viewModel.monthCount.observe(this) { count ->
            binding.tvMonthCount.text = "本月 $count 篇"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()
    }
}