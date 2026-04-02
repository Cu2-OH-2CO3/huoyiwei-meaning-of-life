package com.memoria.meaningoflife.ui.lunch

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.memoria.meaningoflife.data.database.lunch.DishEntity
import com.memoria.meaningoflife.databinding.ItemDishManageBinding
import com.memoria.meaningoflife.model.SpicyLevel

class DishManageAdapter(
    private val onToggleActive: (DishEntity) -> Unit,
    private val onEdit: (DishEntity) -> Unit,
    private val onDelete: (DishEntity) -> Unit
) : ListAdapter<DishEntity, DishManageAdapter.DishViewHolder>(DishDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DishViewHolder {
        val binding = ItemDishManageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DishViewHolder(binding, onToggleActive, onEdit, onDelete)
    }

    override fun onBindViewHolder(holder: DishViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DishViewHolder(
        private val binding: ItemDishManageBinding,
        private val onToggleActive: (DishEntity) -> Unit,
        private val onEdit: (DishEntity) -> Unit,
        private val onDelete: (DishEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(dish: DishEntity) {
            val spicy = SpicyLevel.fromValue(dish.spicyLevel)

            binding.tvName.text = dish.name
            binding.tvCuisine.text = dish.cuisine
            binding.tvSpicy.text = "${spicy.icon} ${spicy.text}"

            // 设置辣度文字颜色为主题色
            val primaryColor = binding.root.context.getColor(com.memoria.meaningoflife.R.color.primary)
            binding.tvSpicy.setTextColor(primaryColor)

            binding.switchActive.isChecked = dish.isActive
            binding.switchActive.setOnCheckedChangeListener { _, isChecked ->
                onToggleActive(dish.copy(isActive = isChecked))
            }

            binding.btnEdit.setOnClickListener {
                onEdit(dish)
            }

            binding.btnDelete.setOnClickListener {
                onDelete(dish)
            }
        }
    }

    class DishDiffCallback : DiffUtil.ItemCallback<DishEntity>() {
        override fun areItemsTheSame(oldItem: DishEntity, newItem: DishEntity): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: DishEntity, newItem: DishEntity): Boolean = oldItem == newItem
    }
}