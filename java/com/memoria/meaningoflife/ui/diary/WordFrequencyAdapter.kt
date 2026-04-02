package com.memoria.meaningoflife.ui.diary

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.memoria.meaningoflife.databinding.ItemWordFrequencyBinding

class WordFrequencyAdapter(
    private val words: List<Map.Entry<String, Int>>
) : RecyclerView.Adapter<WordFrequencyAdapter.WordViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val binding = ItemWordFrequencyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        holder.bind(words[position])
    }

    override fun getItemCount() = words.size

    class WordViewHolder(private val binding: ItemWordFrequencyBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: Map.Entry<String, Int>) {
            binding.tvWord.text = entry.key
            binding.tvCount.text = "${entry.value}次"
        }
    }
}