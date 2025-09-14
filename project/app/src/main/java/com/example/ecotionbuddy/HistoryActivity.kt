package com.example.ecotionbuddy

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import com.example.ecotionbuddy.data.models.HistoryEntry
import com.example.ecotionbuddy.data.models.HistoryType
import com.example.ecotionbuddy.data.models.WasteCategory
import com.example.ecotionbuddy.databinding.ActivityRiwayatBinding
import com.example.ecotionbuddy.ui.history.HistoryAdapter
import com.example.ecotionbuddy.utils.PreferencesManager
import com.example.ecotionbuddy.data.network.RetrofitClient

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRiwayatBinding
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRiwayatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        setupRecyclerView()
        loadHistoryData()
    }
    
    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter()
        binding.historyRecyclerView.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(this@HistoryActivity)
        }
    }
    
    private fun loadHistoryData() {
        val preferencesManager = PreferencesManager(this)
        val userId = preferencesManager.userId ?: "guest"
        
        binding.progressBar.visibility = View.VISIBLE
        binding.historyRecyclerView.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getUserHistory(userId)
                val historyEntries = response.history.map { item ->
                    HistoryEntry(
                        id = item.id,
                        type = mapStringToHistoryType(item.type),
                        title = item.title,
                        description = item.description,
                        pointsEarned = item.pointsEarned,
                        timestamp = item.timestamp,
                        category = mapStringToWasteCategory(item.category)
                    )
                }
                
                historyAdapter.submitList(historyEntries)
                binding.progressBar.visibility = View.GONE
                binding.historyRecyclerView.visibility = View.VISIBLE
                
            } catch (e: Exception) {
                // Show error or fallback to sample data
                loadFallbackData()
                binding.progressBar.visibility = View.GONE
                binding.historyRecyclerView.visibility = View.VISIBLE
            }
        }
    }
    
    private fun mapStringToHistoryType(typeString: String): HistoryType {
        return when (typeString) {
            "waste_scanned" -> HistoryType.WASTE_SCANNED
            "mission_completed" -> HistoryType.MISSION_COMPLETED
            "level_up" -> HistoryType.LEVEL_UP
            else -> HistoryType.WASTE_SCANNED
        }
    }
    
    private fun mapStringToWasteCategory(categoryString: String): WasteCategory? {
        return when (categoryString.lowercase()) {
            "plastic" -> WasteCategory.PLASTIC
            "paper" -> WasteCategory.PAPER
            "glass" -> WasteCategory.GLASS
            "metal" -> WasteCategory.METAL
            "cardboard" -> WasteCategory.PAPER
            "organic" -> WasteCategory.ORGANIC
            else -> null
        }
    }
    
    private fun loadFallbackData() {
        val fallbackHistory = listOf(
            HistoryEntry(
                id = "fallback_1",
                type = HistoryType.WASTE_SCANNED,
                title = "No history available",
                description = "Start scanning waste to see your activity history",
                pointsEarned = 0,
                timestamp = System.currentTimeMillis()
            )
        )
        historyAdapter.submitList(fallbackHistory)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}