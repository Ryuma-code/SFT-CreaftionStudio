package com.example.ecotionbuddy.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ecotionbuddy.data.models.HistoryEntry
import com.example.ecotionbuddy.databinding.ItemMissionHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter : ListAdapter<HistoryEntry, HistoryAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMissionHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ViewHolder(private val binding: ItemMissionHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(historyEntry: HistoryEntry) {
            binding.apply {
                missionTitle.text = historyEntry.title
                
                // Set category chip
                historyEntry.category?.let { category ->
                    chipCategory.text = category.displayName
                    chipCategory.visibility = android.view.View.VISIBLE
                } ?: run {
                    chipCategory.visibility = android.view.View.GONE
                }
                
                // Set points chip
                if (historyEntry.pointsEarned > 0) {
                    chipPoints.text = "+${historyEntry.pointsEarned} Poin"
                    chipPoints.visibility = android.view.View.VISIBLE
                } else {
                    chipPoints.visibility = android.view.View.GONE
                }
                
                // Format completion date
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                val formattedDate = dateFormat.format(Date(historyEntry.timestamp))
                completionDate.text = "Selesai: $formattedDate"
                
                // Set placeholder image (in real app, load from historyEntry.imageUrl)
                missionImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<HistoryEntry>() {
        override fun areItemsTheSame(oldItem: HistoryEntry, newItem: HistoryEntry): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: HistoryEntry, newItem: HistoryEntry): Boolean {
            return oldItem == newItem
        }
    }
}