package com.memoria.meaningoflife.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.memoria.meaningoflife.databinding.ItemQuoteBinding

class QuoteListAdapter(
    private val onEdit: (String, Int) -> Unit,
    private val onDelete: (Int) -> Unit
) : ListAdapter<String, QuoteListAdapter.QuoteViewHolder>(QuoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder {
        val binding = ItemQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QuoteViewHolder(binding, onEdit, onDelete)
    }

    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    class QuoteViewHolder(
        private val binding: ItemQuoteBinding,
        private val onEdit: (String, Int) -> Unit,
        private val onDelete: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(quote: String, position: Int) {
            binding.tvQuote.text = quote

            binding.btnEdit.setOnClickListener {
                onEdit(quote, position)
            }

            binding.btnDelete.setOnClickListener {
                onDelete(position)
            }
        }
    }

    class QuoteDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
    }
}