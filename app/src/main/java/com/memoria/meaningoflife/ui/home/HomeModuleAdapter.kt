package com.memoria.meaningoflife.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.memoria.meaningoflife.databinding.ItemHomeModuleBinding

class HomeModuleAdapter(
    private val onModuleClick: (HomeModule) -> Unit,
    private val onDeleteClick: (HomeModule) -> Unit
) : ListAdapter<HomeModule, HomeModuleAdapter.ModuleViewHolder>(ModuleDiffCallback()) {

    private var isEditMode = false

    fun setEditMode(editMode: Boolean) {
        isEditMode = editMode
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        val binding = ItemHomeModuleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ModuleViewHolder(binding, onModuleClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        holder.bind(getItem(position), isEditMode)
    }

    class ModuleViewHolder(
        private val binding: ItemHomeModuleBinding,
        private val onModuleClick: (HomeModule) -> Unit,
        private val onDeleteClick: (HomeModule) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(module: HomeModule, isEditMode: Boolean) {
            binding.tvTitle.text = module.title
            binding.tvStats.text = module.statsText
            binding.ivIcon.setImageResource(module.iconRes)
            // 设置卡片背景颜色 - 使用 setCardBackgroundColor 而不是 setBackgroundColor
            val cardView = binding.root as? androidx.cardview.widget.CardView
            cardView?.setCardBackgroundColor(module.color)

            // 删除按钮显示
            binding.btnDelete.visibility = if (isEditMode) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                onModuleClick(module)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(module)
            }
        }
    }

    class ModuleDiffCallback : DiffUtil.ItemCallback<HomeModule>() {
        override fun areItemsTheSame(oldItem: HomeModule, newItem: HomeModule): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: HomeModule, newItem: HomeModule): Boolean = oldItem == newItem
    }
}