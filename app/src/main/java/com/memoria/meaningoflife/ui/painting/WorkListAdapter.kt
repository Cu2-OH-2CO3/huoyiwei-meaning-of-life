package com.memoria.meaningoflife.ui.painting

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.memoria.meaningoflife.data.database.painting.WorkEntity
import com.memoria.meaningoflife.data.repository.PaintingRepository
import com.memoria.meaningoflife.databinding.ItemWorkBinding
import com.memoria.meaningoflife.utils.DateUtils
import kotlinx.coroutines.runBlocking

class WorkListAdapter(
    private val onItemClick: (WorkEntity) -> Unit
) : ListAdapter<WorkEntity, WorkListAdapter.WorkViewHolder>(WorkDiffCallback()) {

    private val repository = PaintingRepository(com.memoria.meaningoflife.MeaningOfLifeApp.instance.database)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkViewHolder {
        val binding = ItemWorkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WorkViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: WorkViewHolder, position: Int) {
        holder.bind(getItem(position), repository)
    }

    class WorkViewHolder(
        private val binding: ItemWorkBinding,
        private val onItemClick: (WorkEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(work: WorkEntity, repository: PaintingRepository) {
            binding.tvTitle.text = work.title
            binding.tvDate.text = DateUtils.formatDate(work.createdDate)
            binding.tvDuration.text = DateUtils.formatDuration(work.totalDuration)

            // 优先使用最新节点的图片
            var imagePath = work.finalImagePath

            runBlocking {
                val nodes = repository.getNodesByWorkId(work.id)
                if (nodes.isNotEmpty()) {
                    // 按 nodeOrder 排序，取最后一个（最新节点）
                    val latestNode = nodes.maxByOrNull { it.nodeOrder }
                    if (latestNode?.imagePath != null) {
                        imagePath = latestNode.imagePath
                    }
                }
            }

            if (!imagePath.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(imagePath)
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