package com.memoria.meaningoflife.ui.diary

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.memoria.meaningoflife.data.database.diary.DiaryEntity
import com.memoria.meaningoflife.databinding.ItemDiaryBinding
import com.memoria.meaningoflife.model.Mood
import com.memoria.meaningoflife.model.Weather
import com.memoria.meaningoflife.utils.DateUtils
import com.memoria.meaningoflife.utils.JsonHelper

class DiaryListAdapter(
    private val onItemClick: (DiaryEntity) -> Unit
) : ListAdapter<DiaryEntity, DiaryListAdapter.DiaryViewHolder>(DiaryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
        val binding = ItemDiaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DiaryViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiaryViewHolder(
        private val binding: ItemDiaryBinding,
        private val onItemClick: (DiaryEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(diary: DiaryEntity) {
            // 修复：处理可能为 null 的 mood 和 weather
            val moodValue = diary.mood ?: 2  // 默认 NORMAL
            val weatherValue = diary.weather ?: 0  // 默认 SUNNY

            val mood = Mood.fromValue(moodValue)
            val weather = Weather.fromValue(weatherValue)

            binding.tvDate.text = DateUtils.formatDate(diary.createdDate)
            binding.tvMood.text = "${mood.icon} ${mood.text}"
            binding.tvWeather.text = "${weather.icon} ${weather.text}"

            // 标题或内容预览
            if (!diary.title.isNullOrEmpty()) {
                binding.tvTitle.text = diary.title
                binding.tvTitle.visibility = android.view.View.VISIBLE
                binding.tvContent.text = diary.content.take(100) + if (diary.content.length > 100) "..." else ""
            } else {
                binding.tvTitle.visibility = android.view.View.GONE
                binding.tvContent.text = diary.content.take(100) + if (diary.content.length > 100) "..." else ""
            }

            // 图片数量
            val images = diary.images?.let { JsonHelper.jsonToImages(it) } ?: emptyList()
            if (images.isNotEmpty()) {
                binding.tvImageCount.text = "📷 ${images.size}张图片"
                binding.tvImageCount.visibility = android.view.View.VISIBLE
            } else {
                binding.tvImageCount.visibility = android.view.View.GONE
            }

            // 标签
            val tags = diary.tags?.let { JsonHelper.jsonToTags(it) } ?: emptyList()
            if (tags.isNotEmpty()) {
                binding.tvTags.text = tags.joinToString(" · ")
                binding.tvTags.visibility = android.view.View.VISIBLE
            } else {
                binding.tvTags.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener {
                onItemClick(diary)
            }
        }
    }

    class DiaryDiffCallback : DiffUtil.ItemCallback<DiaryEntity>() {
        override fun areItemsTheSame(oldItem: DiaryEntity, newItem: DiaryEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DiaryEntity, newItem: DiaryEntity): Boolean {
            return oldItem == newItem
        }
    }
}