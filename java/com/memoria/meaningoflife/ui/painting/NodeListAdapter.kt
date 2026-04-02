package com.memoria.meaningoflife.ui.painting

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.memoria.meaningoflife.data.database.painting.NodeEntity
import com.memoria.meaningoflife.databinding.ItemNodeBinding
import com.memoria.meaningoflife.utils.DateUtils

class NodeListAdapter(
    private val onNodeClick: (NodeEntity) -> Unit,
    private val onNodeDelete: (NodeEntity) -> Unit
) : ListAdapter<NodeEntity, NodeListAdapter.NodeViewHolder>(NodeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NodeViewHolder {
        val binding = ItemNodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NodeViewHolder(binding, onNodeClick, onNodeDelete)
    }

    override fun onBindViewHolder(holder: NodeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NodeViewHolder(
        private val binding: ItemNodeBinding,
        private val onNodeClick: (NodeEntity) -> Unit,
        private val onNodeDelete: (NodeEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(node: NodeEntity) {
            binding.tvOrder.text = "节点 ${node.nodeOrder + 1}"
            binding.tvDuration.text = DateUtils.formatDuration(node.duration)
            binding.tvNote.text = node.note ?: "无留言"

            // 加载节点图片
            if (!node.imagePath.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(node.imagePath)
                    .centerCrop()
                    .into(binding.ivImage)
            } else {
                binding.ivImage.setImageResource(android.R.drawable.ic_menu_camera)
            }

            binding.root.setOnClickListener {
                onNodeClick(node)
            }

            binding.btnDelete.setOnClickListener {
                onNodeDelete(node)
            }
        }
    }

    class NodeDiffCallback : DiffUtil.ItemCallback<NodeEntity>() {
        override fun areItemsTheSame(oldItem: NodeEntity, newItem: NodeEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NodeEntity, newItem: NodeEntity): Boolean {
            return oldItem == newItem
        }
    }
}