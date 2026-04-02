package com.memoria.meaningoflife.ui.painting

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.memoria.meaningoflife.data.database.painting.GoalEntity
import com.memoria.meaningoflife.databinding.ItemGoalBinding
import com.memoria.meaningoflife.utils.DateUtils

class GoalListAdapter(
    private val onGoalClick: (GoalEntity) -> Unit,
    private val onGoalEdit: (GoalEntity) -> Unit,
    private val onGoalComplete: (GoalEntity) -> Unit,
    private val onGoalDelete: (GoalEntity) -> Unit
) : ListAdapter<GoalEntity, GoalListAdapter.GoalViewHolder>(GoalDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val binding = ItemGoalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GoalViewHolder(binding, onGoalClick, onGoalComplete, onGoalDelete)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class GoalViewHolder(
        private val binding: ItemGoalBinding,
        private val onGoalClick: (GoalEntity) -> Unit,
        private val onGoalComplete: (GoalEntity) -> Unit,
        private val onGoalDelete: (GoalEntity) -> Unit

    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(goal: GoalEntity) {
            binding.tvTitle.text = goal.title
            binding.tvDescription.text = goal.description ?: "无描述"

            val targetText = when (goal.targetType) {
                0 -> "目标: ${goal.targetValue} 幅作品"
                else -> "目标: ${goal.targetValue} 分钟 (${goal.targetValue / 60}小时${goal.targetValue % 60}分钟)"
            }
            binding.tvTarget.text = targetText

            val currentText = when (goal.targetType) {
                0 -> "进度: ${goal.currentValue}/${goal.targetValue} 幅"
                else -> "进度: ${goal.currentValue}/${goal.targetValue} 分钟 (${goal.currentValue / 60}小时${goal.currentValue % 60}分钟)"
            }
            binding.tvProgress.text = currentText

            binding.tvDate.text = "截止: ${DateUtils.formatDate(goal.targetDate)}"

            // 计算进度百分比
            val progress = if (goal.targetValue > 0) {
                (goal.currentValue.toFloat() / goal.targetValue.toFloat()) * 100
            } else 0f
            binding.progressBar.progress = progress.toInt()

            // 检查是否已完成
            val isCompleted = goal.currentValue >= goal.targetValue

            if (isCompleted) {
                // 已完成的目标，显示绿色完成状态
                binding.btnComplete.text = "✓ 已完成"
                binding.btnComplete.isEnabled = false
                binding.progressBar.progressTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#4CAF50")
                )
            } else {
                binding.btnComplete.text = "完成"
                binding.btnComplete.isEnabled = true
            }

            binding.root.setOnClickListener { onGoalClick(goal) }
            binding.btnComplete.setOnClickListener { onGoalComplete(goal) }
            binding.btnDelete.setOnClickListener { onGoalDelete(goal) }
        }

        private fun showEditGoalDialog(goal: GoalEntity) {
            // 复用添加目标的对话框，预填数据
            binding.tvTitle.text = goal.title
            binding.tvDescription.text = goal.description ?: "无描述"

            val targetText = when (goal.targetType) {
                0 -> "目标: ${goal.targetValue} 幅作品"
                else -> "目标: ${goal.targetValue} 分钟 (${goal.targetValue / 60}小时${goal.targetValue % 60}分钟)"
            }
            binding.tvTarget.text = targetText

            val currentText = when (goal.targetType) {
                0 -> "进度: ${goal.currentValue}/${goal.targetValue} 幅"
                else -> "进度: ${goal.currentValue}/${goal.targetValue} 分钟 (${goal.currentValue / 60}小时${goal.currentValue % 60}分钟)"
            }
            binding.tvProgress.text = currentText

            binding.tvDate.text = "截止: ${DateUtils.formatDate(goal.targetDate)}"

            // 计算进度百分比
            val progress = if (goal.targetValue > 0) {
                (goal.currentValue.toFloat() / goal.targetValue.toFloat()) * 100
            } else 0f
            binding.progressBar.progress = progress.toInt()

            // 检查是否已完成
            val isCompleted = goal.currentValue >= goal.targetValue

            if (isCompleted) {
                // 已完成的目标，显示绿色完成状态
                binding.btnComplete.text = "✓ 已完成"
                binding.btnComplete.isEnabled = false
                binding.progressBar.progressTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#4CAF50")
                )
            } else {
                binding.btnComplete.text = "完成"
                binding.btnComplete.isEnabled = true
            }

            binding.root.setOnClickListener { onGoalClick(goal) }
            binding.btnComplete.setOnClickListener { onGoalComplete(goal) }
            binding.btnDelete.setOnClickListener { onGoalDelete(goal) }
        }
    }

    class GoalDiffCallback : DiffUtil.ItemCallback<GoalEntity>() {
        override fun areItemsTheSame(oldItem: GoalEntity, newItem: GoalEntity): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: GoalEntity, newItem: GoalEntity): Boolean = oldItem == newItem
    }
}