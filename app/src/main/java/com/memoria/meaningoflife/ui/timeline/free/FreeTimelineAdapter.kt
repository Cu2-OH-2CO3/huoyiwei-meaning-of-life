// 路径：ui/timeline/free/FreeTimelineAdapter.kt
package com.memoria.meaningoflife.ui.timeline.free

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.data.database.timeline.TimelineEventEntity
import com.memoria.meaningoflife.databinding.ItemTimelineEventBinding
import java.text.SimpleDateFormat
import java.util.*

class FreeTimelineAdapter(
    private val onEventClick: (TimelineEventEntity) -> Unit,
    private val onEventLongClick: (TimelineEventEntity) -> Boolean,
    private val onCheckboxClick: (TimelineEventEntity) -> Unit
) : ListAdapter<TimelineEventEntity, FreeTimelineAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemTimelineEventBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        if (position != RecyclerView.NO_POSITION && position < itemCount) {
            holder.bind(getItem(position))
        }
    }

    inner class EventViewHolder(
        private val binding: ItemTimelineEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: TimelineEventEntity) {
            binding.tvTitle.text = event.title
            binding.chkComplete.isChecked = event.isCompleted

            // 设置时间显示
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())

            val timeStr = if (event.isAllDay) {
                "全天"
            } else {
                val startTime = timeFormat.format(Date(event.startTime))
                val endTime = event.endTime?.let { timeFormat.format(Date(it)) }
                if (endTime != null) "$startTime - $endTime" else startTime
            }

            val dateStr = dateFormat.format(Date(event.startTime))
            binding.tvTime.text = "$dateStr $timeStr"

            // 设置位置
            if (!event.location.isNullOrEmpty()) {
                binding.tvLocation.text = event.location
                binding.tvLocation.visibility = android.view.View.VISIBLE
            } else {
                binding.tvLocation.visibility = android.view.View.GONE
            }

            // 设置类型颜色
            val typeColor = getTypeColor(event.eventType.name)
            binding.viewTypeIndicator.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, typeColor)
            )

            // 已完成的任务添加删除线
            if (event.isCompleted) {
                binding.tvTitle.paintFlags = binding.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvTitle.alpha = 0.6f
            } else {
                binding.tvTitle.paintFlags = binding.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvTitle.alpha = 1f
            }

            binding.chkComplete.setOnCheckedChangeListener(null)
            binding.chkComplete.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != event.isCompleted) {
                    onCheckboxClick(event)
                }
            }

            binding.root.setOnClickListener { onEventClick(event) }
            binding.root.setOnLongClickListener { onEventLongClick(event) }
        }

        private fun getTypeColor(type: String): Int {
            return when (type) {
                "COURSE" -> R.color.type_course
                "TASK" -> R.color.type_task
                "DIARY" -> R.color.type_diary
                "PAINTING" -> R.color.type_painting
                "LUNCH" -> R.color.type_lunch
                else -> R.color.type_custom
            }
        }
    }

    class EventDiffCallback : DiffUtil.ItemCallback<TimelineEventEntity>() {
        override fun areItemsTheSame(oldItem: TimelineEventEntity, newItem: TimelineEventEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TimelineEventEntity, newItem: TimelineEventEntity): Boolean =
            oldItem == newItem
    }
}