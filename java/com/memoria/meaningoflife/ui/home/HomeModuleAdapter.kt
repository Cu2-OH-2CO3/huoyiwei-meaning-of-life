package com.memoria.meaningoflife.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.databinding.ItemHomeModuleBinding
import com.memoria.meaningoflife.utils.CardColorManager

class HomeModuleAdapter(
    private val onModuleClick: (HomeModule) -> Unit,
    private val onDeleteClick: (HomeModule) -> Unit
) : ListAdapter<HomeModule, HomeModuleAdapter.ModuleViewHolder>(ModuleDiffCallback()) {

    private var isEditMode = false

    fun setEditMode(editMode: Boolean) {
        isEditMode = editMode
        notifyItemRangeChanged(0, itemCount)
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

            // 显示角标
            if (module.badgeCount > 0 && !isEditMode) {
                binding.badge.visibility = View.VISIBLE
                binding.badge.text = module.badgeCount.toString()
            } else {
                binding.badge.visibility = View.GONE
            }

            // 获取卡片颜色（返回的是 Int 类型）
            val color = when (module.id) {
                "painting" -> CardColorManager.getPaintingCardColorHex(binding.root.context)
                "diary" -> CardColorManager.getDiaryCardColorHex(binding.root.context)
                "lunch" -> CardColorManager.getLunchCardColorHex(binding.root.context)
                "task" -> CardColorManager.getTaskCardColorHex(binding.root.context)
                else -> CardColorManager.getBackupCardColorHex(binding.root.context)
            }

            // 设置卡片背景色 - color 已经是 Int 类型，可以直接使用
            (binding.root as? CardView)?.setCardBackgroundColor(color)

            binding.btnDelete.visibility = if (isEditMode) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                if (!isEditMode) {
                    onModuleClick(module)
                }
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