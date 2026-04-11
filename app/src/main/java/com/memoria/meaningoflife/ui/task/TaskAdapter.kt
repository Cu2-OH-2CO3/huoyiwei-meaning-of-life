// 路径：com/memoria/meaningoflife/ui/task/TaskAdapter.kt
package com.memoria.meaningoflife.ui.task

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.data.database.task.TaskEntity
import com.memoria.meaningoflife.data.database.task.TaskPriority
import com.memoria.meaningoflife.databinding.ItemTaskBinding
import com.memoria.meaningoflife.utils.DateUtils
 import com.memoria.meaningoflife.utils.ThemeManager

class TaskAdapter(
    private val onTaskClick: (TaskEntity) -> Unit,
    private val onTaskLongClick: (TaskEntity) -> Unit,
    private val onCheckboxClick: (TaskEntity) -> Unit
) : ListAdapter<TaskEntity, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    private var filterType = TaskListFragment.FilterType.ALL

    fun setFilter(filter: TaskListFragment.FilterType) {
        filterType = filter
        // 实际筛选在 ViewModel 层处理，这里只是通知
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(
        private val binding: ItemTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: TaskEntity) {
            binding.tvTitle.text = task.title
            binding.checkbox.isChecked = task.completedAt != null

            // 已完成的任务添加删除线
            if (task.completedAt != null) {
                binding.tvTitle.paintFlags = binding.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvTitle.alpha = 0.6f
            } else {
                binding.tvTitle.paintFlags = binding.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvTitle.alpha = 1f
            }

            // 显示截止日期
            task.deadline?.let { deadline ->
                val dateStr = DateUtils.formatDate(deadline)
                val isOverdue = deadline < System.currentTimeMillis() && task.completedAt == null
                binding.tvDeadline.text = "DEADLINE  $dateStr"
                binding.tvDeadline.visibility = View.VISIBLE
                if (isOverdue) {
                    binding.tvDeadline.setTextColor(
                        ContextCompat.getColor(binding.root.context, R.color.nd_accent)
                    )
                } else {
                    binding.tvDeadline.setTextColor(
                        ContextCompat.getColor(binding.root.context, R.color.nd_text_secondary)
                    )
                }
            } ?: run {
                binding.tvDeadline.visibility = View.GONE
            }

            // 显示优先级标签
            val priority = task.getPriority()
            val priorityText = when (priority) {
                TaskPriority.URGENT_IMPORTANT -> "[ PRIORITY: U+I ]"
                TaskPriority.URGENT_NOT_IMPORTANT -> "[ PRIORITY: U ]"
                TaskPriority.NOT_URGENT_IMPORTANT -> "[ PRIORITY: I ]"
                TaskPriority.NOT_URGENT_NOT_IMPORTANT -> "[ PRIORITY: NORMAL ]"
            }
            binding.tvPriority.text = priorityText
            binding.tvPriority.setTextColor(getPriorityColor(priority, binding.root.context))

            binding.checkbox.setOnCheckedChangeListener(null)
            binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != (task.completedAt != null)) {
                    onCheckboxClick(task)
                }
            }

            binding.root.setOnClickListener { onTaskClick(task) }
            binding.root.setOnLongClickListener {
                onTaskLongClick(task)
                true
            }
        }
    }

    private fun getPriorityColor(priority: TaskPriority, context: android.content.Context): Int {
        return when (priority) {
            TaskPriority.URGENT_IMPORTANT -> ThemeManager.resolvePrimaryColor(context)
            TaskPriority.URGENT_NOT_IMPORTANT -> ContextCompat.getColor(context, R.color.nd_text_primary)
            TaskPriority.NOT_URGENT_IMPORTANT -> ContextCompat.getColor(context, R.color.nd_text_secondary)
            TaskPriority.NOT_URGENT_NOT_IMPORTANT -> ContextCompat.getColor(context, R.color.nd_text_disabled)
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<TaskEntity>() {
        override fun areItemsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean = oldItem == newItem
    }
}