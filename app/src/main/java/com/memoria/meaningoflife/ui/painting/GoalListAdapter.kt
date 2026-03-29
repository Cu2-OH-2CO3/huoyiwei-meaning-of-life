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
            binding.tvTarget.text = "目标: ${goal.targetValue} ${if (goal.targetType == 0) "幅作品" else "分钟"}"
            binding.tvProgress.text = "进度: ${goal.currentValue}/${goal.targetValue}"
            binding.tvDate.text = "截止: ${DateUtils.formatDate(goal.targetDate)}"

            // 进度条
            val progress = if (goal.targetValue > 0) {
                (goal.currentValue.toFloat() / goal.targetValue.toFloat()) * 100
            } else 0f
            binding.progressBar.progress = progress.toInt()

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