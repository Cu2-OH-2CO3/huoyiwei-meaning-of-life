package com.memoria.meaningoflife.ui.painting

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.TypedValue
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.databinding.ActivityGoalManageBinding
import com.memoria.meaningoflife.data.database.painting.GoalEntity
import com.memoria.meaningoflife.data.repository.PaintingRepository
import com.memoria.meaningoflife.ui.BaseActivity
import com.memoria.meaningoflife.utils.DateUtils
import kotlinx.coroutines.launch
import java.util.*

class GoalManageActivity : BaseActivity() {

    private lateinit var binding: ActivityGoalManageBinding
    private lateinit var adapter: GoalListAdapter
    private val repository = PaintingRepository(com.memoria.meaningoflife.MeaningOfLifeApp.instance.database)
    private var selectedStartDate = DateUtils.getCurrentDate()
    private var selectedTargetDate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoalManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "目标管理"

        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data
        supportActionBar?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(primaryColor))

        // 设置 FAB 按钮颜色为主题色
        binding.fabAdd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primaryColor))

        setupRecyclerView()
        setupClickListeners()
        loadGoals()
    }

    private fun setupRecyclerView() {
        adapter = GoalListAdapter(
            onGoalClick = { goal ->
                // 点击目标，可以显示详情或编辑
                showEditGoalDialog(goal)
            },
            onGoalEdit = { goal ->
                // 编辑目标
                showEditGoalDialog(goal)
            },
            onGoalComplete = { goal ->
                lifecycleScope.launch {
                    repository.completeGoal(goal.id)
                    loadGoals()
                    Toast.makeText(this@GoalManageActivity, "恭喜完成目标！", Toast.LENGTH_SHORT).show()
                }
            },
            onGoalDelete = { goal ->
                AlertDialog.Builder(this)
                    .setTitle("删除目标")
                    .setMessage("确定要删除「${goal.title}」吗？")
                    .setPositiveButton("删除") { _, _ ->
                        lifecycleScope.launch {
                            repository.deleteGoal(goal)
                            loadGoals()
                            Toast.makeText(this@GoalManageActivity, "目标已删除", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@GoalManageActivity)
            adapter = this@GoalManageActivity.adapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAdd.setOnClickListener {
            showAddGoalDialog()
        }
    }

    private fun loadGoals() {
        lifecycleScope.launch {
            val goals = repository.getAllGoals()
            adapter.submitList(goals)

            if (goals.isEmpty()) {
                binding.tvEmpty.visibility = android.view.View.VISIBLE
                binding.recyclerView.visibility = android.view.View.GONE
            } else {
                binding.tvEmpty.visibility = android.view.View.GONE
                binding.recyclerView.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun showAddGoalDialog() {
        val dialogBinding = android.view.LayoutInflater.from(this)
            .inflate(com.memoria.meaningoflife.R.layout.dialog_add_goal, null)

        val etTitle = dialogBinding.findViewById<android.widget.EditText>(com.memoria.meaningoflife.R.id.etTitle)
        val etDescription = dialogBinding.findViewById<android.widget.EditText>(com.memoria.meaningoflife.R.id.etDescription)
        val etTargetValue = dialogBinding.findViewById<android.widget.EditText>(com.memoria.meaningoflife.R.id.etTargetValue)
        val rgType = dialogBinding.findViewById<android.widget.RadioGroup>(com.memoria.meaningoflife.R.id.rgType)
        val btnStartDate = dialogBinding.findViewById<android.widget.Button>(com.memoria.meaningoflife.R.id.btnStartDate)
        val btnTargetDate = dialogBinding.findViewById<android.widget.Button>(com.memoria.meaningoflife.R.id.btnTargetDate)

        btnStartDate.text = "开始日期: ${selectedStartDate}"
        btnStartDate.setOnClickListener {
            showDatePicker { date ->
                selectedStartDate = date
                btnStartDate.text = "开始日期: $date"
            }
        }

        btnTargetDate.setOnClickListener {
            showDatePicker { date ->
                selectedTargetDate = date
                btnTargetDate.text = "目标日期: $date"
            }
        }

        AlertDialog.Builder(this)
            .setTitle("添加目标")
            .setView(dialogBinding)
            .setPositiveButton("保存") { _, _ ->
                val title = etTitle.text.toString().trim()
                val description = etDescription.text.toString().trim()
                val targetValueStr = etTargetValue.text.toString().trim()
                val targetType = if (rgType.checkedRadioButtonId == com.memoria.meaningoflife.R.id.rbWorkCount) 0 else 1

                if (title.isEmpty()) {
                    Toast.makeText(this, "请输入目标名称", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (targetValueStr.isEmpty()) {
                    Toast.makeText(this, "请输入目标值", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (selectedTargetDate.isEmpty()) {
                    Toast.makeText(this, "请选择目标日期", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val targetValue = targetValueStr.toIntOrNull()
                if (targetValue == null) {
                    Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val goal = GoalEntity(
                    title = title,
                    description = description,
                    targetDate = selectedTargetDate,
                    startDate = selectedStartDate,
                    targetType = targetType,
                    targetValue = targetValue,
                    currentValue = 0,
                    status = 0
                )

                lifecycleScope.launch {
                    repository.insertGoal(goal)
                    loadGoals()
                    Toast.makeText(this@GoalManageActivity, "目标已添加", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEditGoalDialog(goal: GoalEntity) {
        val dialogBinding = android.view.LayoutInflater.from(this)
            .inflate(com.memoria.meaningoflife.R.layout.dialog_edit_goal, null)

        val etTitle = dialogBinding.findViewById<android.widget.EditText>(R.id.etTitle)
        val etDescription = dialogBinding.findViewById<android.widget.EditText>(R.id.etDescription)
        val etTargetValue = dialogBinding.findViewById<android.widget.EditText>(R.id.etTargetValue)
        val rgType = dialogBinding.findViewById<android.widget.RadioGroup>(R.id.rgType)
        val btnStartDate = dialogBinding.findViewById<android.widget.Button>(R.id.btnStartDate)
        val btnTargetDate = dialogBinding.findViewById<android.widget.Button>(R.id.btnTargetDate)

        // 预填数据
        etTitle.setText(goal.title)
        etDescription.setText(goal.description)
        etTargetValue.setText(goal.targetValue.toString())

        // 设置目标类型
        if (goal.targetType == 0) {
            rgType.check(R.id.rbWorkCount)
        } else {
            rgType.check(R.id.rbDuration)
        }

        var startDate = goal.startDate
        var targetDate = goal.targetDate

        btnStartDate.text = "开始日期: ${startDate}"
        btnStartDate.setOnClickListener {
            showDatePicker { date ->
                startDate = date
                btnStartDate.text = "开始日期: $date"
            }
        }

        btnTargetDate.text = "目标日期: ${targetDate}"
        btnTargetDate.setOnClickListener {
            showDatePicker { date ->
                targetDate = date
                btnTargetDate.text = "目标日期: $date"
            }
        }

        AlertDialog.Builder(this)
            .setTitle("编辑目标")
            .setView(dialogBinding)
            .setPositiveButton("保存") { _, _ ->
                val title = etTitle.text.toString().trim()
                val description = etDescription.text.toString().trim()
                val targetValueStr = etTargetValue.text.toString().trim()
                val targetType = if (rgType.checkedRadioButtonId == R.id.rbWorkCount) 0 else 1

                if (title.isEmpty()) {
                    Toast.makeText(this, "请输入目标名称", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (targetValueStr.isEmpty()) {
                    Toast.makeText(this, "请输入目标值", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val targetValue = targetValueStr.toIntOrNull()
                if (targetValue == null) {
                    Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val updatedGoal = goal.copy(
                    title = title,
                    description = description,
                    targetDate = targetDate,
                    startDate = startDate,
                    targetType = targetType,
                    targetValue = targetValue
                )

                lifecycleScope.launch {
                    repository.updateGoal(updatedGoal)
                    loadGoals()
                    Toast.makeText(this@GoalManageActivity, "目标已更新", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val date = "$year-${(month + 1).toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
                onDateSelected(date)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}