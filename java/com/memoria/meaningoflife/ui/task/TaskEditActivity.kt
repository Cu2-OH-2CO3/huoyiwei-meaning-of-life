package com.memoria.meaningoflife.ui.task

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.ViewModelProvider
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.data.database.task.TaskEntity
import com.memoria.meaningoflife.databinding.ActivityTaskEditBinding
import com.memoria.meaningoflife.utils.DateUtils
import java.util.*

class TaskEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskEditBinding
    private lateinit var viewModel: TaskViewModel
    private var taskId: Long = 0
    private var existingTask: TaskEntity? = null
    private var selectedDeadline: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (intent.hasExtra("task_id")) "编辑任务" else "新建任务"

        viewModel = ViewModelProvider(this)[TaskViewModel::class.java]
        taskId = intent.getLongExtra("task_id", 0)

        setupViews()

        if (taskId != 0L) {
            loadTask()
        }
    }

    private fun setupViews() {
        // 紧急程度开关
        binding.switchUrgent.setOnCheckedChangeListener { _, isChecked ->
            binding.tvUrgentValue.text = if (isChecked) "紧急" else "不紧急"
        }

        // 重要程度开关
        binding.switchImportant.setOnCheckedChangeListener { _, isChecked ->
            binding.tvImportantValue.text = if (isChecked) "重要" else "不重要"
        }

        // 截止日期选择
        binding.btnSelectDeadline.setOnClickListener {
            showDatePicker()
        }

        binding.btnClearDeadline.setOnClickListener {
            selectedDeadline = null
            binding.tvDeadline.text = "未设置"
        }

        // 保存按钮
        binding.btnSave.setOnClickListener {
            saveTask()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        if (selectedDeadline != null) {
            calendar.timeInMillis = selectedDeadline!!
        }

        android.app.DatePickerDialog(
            this,
            { _, year, month, day ->
                android.app.TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        val selectedCalendar = Calendar.getInstance().apply {
                            set(year, month, day, hour, minute)
                        }
                        selectedDeadline = selectedCalendar.timeInMillis
                        binding.tvDeadline.text = DateUtils.formatDateTime(selectedDeadline!!)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadTask() {
        viewModel.getTaskById(taskId) { task ->
            existingTask = task
            task?.let {
                binding.etTitle.setText(it.title)
                binding.etDescription.setText(it.description ?: "")

                binding.switchUrgent.isChecked = it.isUrgent
                binding.switchImportant.isChecked = it.isImportant

                it.deadline?.let { deadline ->
                    selectedDeadline = deadline
                    binding.tvDeadline.text = DateUtils.formatDateTime(deadline)
                }
            }
        }
    }

    private fun saveTask() {
        val title = binding.etTitle.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, "请输入任务标题", Toast.LENGTH_SHORT).show()
            return
        }

        val description = binding.etDescription.text.toString().trim()
        val isUrgent = binding.switchUrgent.isChecked
        val isImportant = binding.switchImportant.isChecked

        val task = if (existingTask != null) {
            existingTask!!.copy(
                title = title,
                description = description.takeIf { it.isNotEmpty() },
                isUrgent = isUrgent,
                isImportant = isImportant,
                deadline = selectedDeadline
            )
        } else {
            TaskEntity(
                title = title,
                description = description.takeIf { it.isNotEmpty() },
                isUrgent = isUrgent,
                isImportant = isImportant,
                deadline = selectedDeadline
            )
        }

        if (existingTask != null) {
            viewModel.updateTask(task)
        } else {
            viewModel.insertTask(task)
        }

        Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}