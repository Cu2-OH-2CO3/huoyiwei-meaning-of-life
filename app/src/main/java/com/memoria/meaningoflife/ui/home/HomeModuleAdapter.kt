package com.memoria.meaningoflife.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.memoria.meaningoflife.databinding.ItemHomeModuleBinding
import com.memoria.meaningoflife.utils.CardColorManager

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

            // 根据模块ID应用不同的卡片颜色
            val color = when (module.id) {
                "painting" -> CardColorManager.getPaintingCardColor(binding.root.context)
                "diary" -> CardColorManager.getDiaryCardColor(binding.root.context)
                "lunch" -> CardColorManager.getLunchCardColor(binding.root.context)
                else -> CardColorManager.getBackupCardColor(binding.root.context)
            }
            binding.cardView.setCardBackgroundColor(color)

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