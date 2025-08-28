package com.example.ecotionbuddy

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ecotionbuddy.data.models.HistoryEntry
import com.example.ecotionbuddy.data.models.HistoryType
import com.example.ecotionbuddy.data.models.WasteCategory
import com.example.ecotionbuddy.databinding.ActivityRiwayatBinding
import com.example.ecotionbuddy.ui.history.HistoryAdapter

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
        loadSampleData()
    }
    
    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter()
        binding.historyRecyclerView.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(this@HistoryActivity)
        }
    }
    
    private fun loadSampleData() {
        val sampleHistory = listOf(
            HistoryEntry(
                id = "1",
                type = HistoryType.MISSION_COMPLETED,
                title = "Kumpulkan Sampah Plastik",
                description = "Berhasil mengumpulkan 5kg sampah plastik",
                pointsEarned = 1000,
                timestamp = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000L),
                category = WasteCategory.PLASTIC
            ),
            HistoryEntry(
                id = "2",
                type = HistoryType.WASTE_SCANNED,
                title = "Pemindaian Botol Plastik",
                description = "Berhasil memindai dan mendapat saran daur ulang",
                pointsEarned = 50,
                timestamp = System.currentTimeMillis() - (5 * 24 * 60 * 60 * 1000L),
                category = WasteCategory.PLASTIC
            ),
            HistoryEntry(
                id = "3",
                type = HistoryType.LEVEL_UP,
                title = "Naik ke Level 5",
                description = "Selamat! Anda telah mencapai level 5",
                pointsEarned = 500,
                timestamp = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            )
        )
        
        historyAdapter.submitList(sampleHistory)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}