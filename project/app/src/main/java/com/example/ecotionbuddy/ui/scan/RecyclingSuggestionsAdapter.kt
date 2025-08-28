package com.example.ecotionbuddy.ui.scan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ecotionbuddy.data.models.RecyclingSuggestion
import com.example.ecotionbuddy.databinding.ItemRecyclingSuggestionBinding

class RecyclingSuggestionsAdapter : ListAdapter<RecyclingSuggestion, RecyclingSuggestionsAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecyclingSuggestionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ViewHolder(private val binding: ItemRecyclingSuggestionBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(suggestion: RecyclingSuggestion) {
            binding.apply {
                tvSuggestionTitle.text = suggestion.title
                tvSuggestionDescription.text = suggestion.description
                chipDifficulty.text = suggestion.difficulty
                chipTime.text = suggestion.estimatedTime
                
                btnViewDetails.setOnClickListener {
                    // Navigate to detailed tutorial
                }
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<RecyclingSuggestion>() {
        override fun areItemsTheSame(oldItem: RecyclingSuggestion, newItem: RecyclingSuggestion): Boolean {
            return oldItem.title == newItem.title
        }
        
        override fun areContentsTheSame(oldItem: RecyclingSuggestion, newItem: RecyclingSuggestion): Boolean {
            return oldItem == newItem
        }
    }
}