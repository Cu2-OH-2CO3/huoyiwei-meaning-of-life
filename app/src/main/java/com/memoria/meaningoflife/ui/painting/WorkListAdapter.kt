package com.memoria.meaningoflife.ui.painting

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.memoria.meaningoflife.data.database.painting.WorkEntity
import com.memoria.meaningoflife.databinding.ItemWorkBinding
import com.memoria.meaningoflife.utils.DateUtils

class WorkListAdapter(
    private val onItemClick: (WorkEntity) -> Unit
) : ListAdapter<WorkEntity, WorkListAdapter.WorkViewHolder>(WorkDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkViewHolder {
        val binding = ItemWorkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WorkViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: WorkViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class WorkViewHolder(
        private val binding: ItemWorkBinding,
        private val onItemClick: (WorkEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(work: WorkEntity) {
            binding.tvTitle.text = work.title
            binding.tvDate.text = DateUtils.formatDate(work.createdDate)
            binding.tvDuration.text = DateUtils.formatDuration(work.totalDuration)

            // 加载缩略图
            if (!work.finalImagePath.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(work.finalImagePath)
                    .centerCrop()
                    .into(binding.ivThumbnail)
            } else {
                binding.ivThumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            // 标签显示
            val tags = work.tags?.let {
                com.memoria.meaningoflife.utils.JsonHelper.jsonToTags(it)
            } ?: emptyList()

            if (tags.isNotEmpty()) {
                binding.tvTags.text = tags.joinToString(" · ")
                binding.tvTags.visibility = android.view.View.VISIBLE
            } else {
                binding.tvTags.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener {
                onItemClick(work)
            }
        }
    }

    class WorkDiffCallback : DiffUtil.ItemCallback<WorkEntity>() {
        override fun areItemsTheSame(oldItem: WorkEntity, newItem: WorkEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WorkEntity, newItem: WorkEntity): Boolean {
            return oldItem == newItem
        }
    }
}