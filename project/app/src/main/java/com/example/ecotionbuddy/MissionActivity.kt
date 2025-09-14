package com.example.ecotionbuddy

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ecotionbuddy.databinding.ActivityMissionBinding
import com.example.ecotionbuddy.data.network.RetrofitClient
import com.example.ecotionbuddy.data.network.Mission
import com.example.ecotionbuddy.utils.PreferencesManager
import kotlinx.coroutines.launch

class MissionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMissionBinding
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var availableMissionsAdapter: MissionAdapter
    private lateinit var activeMissionsAdapter: MissionAdapter
    private lateinit var completedMissionsAdapter: MissionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(this)
        setupToolbar()
        setupRecyclerViews()
        loadMissions()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Misi"
    }

    private fun setupRecyclerViews() {
        availableMissionsAdapter = MissionAdapter { mission ->
            startMission(mission)
        }
        
        activeMissionsAdapter = MissionAdapter { mission ->
            // Show mission details or progress
            showMissionDetails(mission)
        }
        
        completedMissionsAdapter = MissionAdapter { mission ->
            showMissionDetails(mission)
        }

        binding.rvAvailableMissions.apply {
            adapter = availableMissionsAdapter
            layoutManager = LinearLayoutManager(this@MissionActivity)
        }

        binding.rvActiveMissions.apply {
            adapter = activeMissionsAdapter
            layoutManager = LinearLayoutManager(this@MissionActivity)
        }

        binding.rvCompletedMissions.apply {
            adapter = completedMissionsAdapter
            layoutManager = LinearLayoutManager(this@MissionActivity)
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadMissions()
        }
    }

    private fun loadMissions() {
        val userId = preferencesManager.userId ?: return
        
        binding.progressBar.visibility = View.VISIBLE
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                // Load available missions
                val availableMissions = RetrofitClient.api.getAvailableMissions()
                
                // Load user missions
                val userMissions = RetrofitClient.api.getUserMissions(userId)
                
                // Check mission progress
                RetrofitClient.api.checkMissionProgress(userId)
                
                // Update UI
                availableMissionsAdapter.submitList(availableMissions.missions.filter { available ->
                    userMissions.active_missions.none { active -> active.id == available.id }
                })
                
                activeMissionsAdapter.submitList(userMissions.active_missions)
                completedMissionsAdapter.submitList(userMissions.completed_missions)
                
                updateSectionVisibility(
                    availableMissions.missions.size,
                    userMissions.active_missions.size,
                    userMissions.completed_missions.size
                )

            } catch (e: Exception) {
                Toast.makeText(this@MissionActivity, "Gagal memuat misi: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun startMission(mission: Mission) {
        val userId = preferencesManager.userId ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.startMission(userId, mission.id)
                Toast.makeText(this@MissionActivity, "Misi dimulai: ${mission.title}", Toast.LENGTH_SHORT).show()
                loadMissions() // Refresh the list
            } catch (e: Exception) {
                Toast.makeText(this@MissionActivity, "Gagal memulai misi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showMissionDetails(mission: Mission) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(mission.title)
            .setMessage("""
                ${mission.description}
                
                Target: ${mission.target}
                Progress: ${mission.progress}/${mission.target}
                Reward: ${mission.reward_points} poin
                Durasi: ${mission.duration_days} hari
            """.trimIndent())
            .setPositiveButton("OK", null)
            .create()
        
        dialog.show()
    }

    private fun updateSectionVisibility(availableCount: Int, activeCount: Int, completedCount: Int) {
        binding.sectionAvailable.visibility = if (availableCount > 0) View.VISIBLE else View.GONE
        binding.sectionActive.visibility = if (activeCount > 0) View.VISIBLE else View.GONE
        binding.sectionCompleted.visibility = if (completedCount > 0) View.VISIBLE else View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
