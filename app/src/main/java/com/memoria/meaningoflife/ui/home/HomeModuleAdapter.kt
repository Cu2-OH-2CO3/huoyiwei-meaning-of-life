package com.memoria.meaningoflife.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.memoria.meaningoflife.databinding.ItemHomeModuleBinding
import com.memoria.meaningoflife.utils.BackgroundManager
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

            // 获取卡片颜色 - 根据模块类型从 CardColorManager 获取
            val color = when (module.id) {
                "painting" -> CardColorManager.getPaintingCardColorHex(binding.root.context)
                "diary" -> CardColorManager.getDiaryCardColorHex(binding.root.context)
                "lunch" -> CardColorManager.getLunchCardColorHex(binding.root.context)
                "task" -> CardColorManager.getTaskCardColorHex(binding.root.context)
                "timeline" -> CardColorManager.getTimelineCardColorHex(binding.root.context)
                else -> CardColorManager.getBackupCardColorHex(binding.root.context)
            }

            // 仅调整卡片背景透明度，不影响文字与图标透明度。
            val alphaPercent = BackgroundManager.getCardAlpha().coerceIn(0, 100)
            val alpha = (alphaPercent * 255f / 100f).toInt().coerceIn(0, 255)
            val colorWithAlpha = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
            (binding.root as CardView).setCardBackgroundColor(colorWithAlpha)

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