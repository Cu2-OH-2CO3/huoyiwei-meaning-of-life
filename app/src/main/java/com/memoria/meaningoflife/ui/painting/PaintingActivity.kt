package com.memoria.meaningoflife.ui.painting

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.databinding.ActivityPaintingBinding
import com.memoria.meaningoflife.ui.BaseActivity

class PaintingActivity : BaseActivity() {

    private lateinit var binding: ActivityPaintingBinding
    private lateinit var viewModel: PaintingViewModel
    private lateinit var adapter: WorkListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaintingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.painting_title)

        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data
        supportActionBar?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(primaryColor))

        binding.fabAdd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primaryColor))
        binding.btnGoalManage.setTextColor(primaryColor)
        binding.btnGoalManage.setBackgroundResource(R.drawable.button_outline_primary)
        binding.btnStatistics.setTextColor(primaryColor)
        binding.btnStatistics.setBackgroundResource(R.drawable.button_outline_primary)

        setupViewModel()
        setupRecyclerView()
        setupClickListeners()
        observeData()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[PaintingViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = WorkListAdapter { work ->
            val intent = Intent(this, WorkDetailActivity::class.java)
            intent.putExtra("work_id", work.id)
            startActivity(intent)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@PaintingActivity)
            adapter = this@PaintingActivity.adapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, AddWorkActivity::class.java))
        }

        binding.btnGoalManage.setOnClickListener {
            startActivity(Intent(this, GoalManageActivity::class.java))
        }

        binding.btnStatistics.setOnClickListener {
            Toast.makeText(this, "统计功能开发中", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeData() {
        viewModel.allWorks.observe(this) { works ->
            adapter.submitList(works)

            if (works.isEmpty()) {
                binding.tvEmpty.visibility = android.view.View.VISIBLE
                binding.recyclerView.visibility = android.view.View.GONE
            } else {
                binding.tvEmpty.visibility = android.view.View.GONE
                binding.recyclerView.visibility = android.view.View.VISIBLE
            }
        }

        viewModel.totalDuration.observe(this) { duration ->
            val hours = duration / 60
            val minutes = duration % 60
            binding.tvTotalDuration.text = if (hours > 0) {
                "总时长: ${hours}小时${minutes}分钟"
            } else {
                "总时长: ${minutes}分钟"
            }
        }

        viewModel.currentGoal.observe(this) { goal ->
            if (goal != null) {
                binding.tvGoalProgress.visibility = android.view.View.VISIBLE
                binding.tvGoalProgress.text = "当前目标: ${goal.title} - 进度 ${goal.currentValue}/${goal.targetValue}"
            } else {
                binding.tvGoalProgress.visibility = android.view.View.GONE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}