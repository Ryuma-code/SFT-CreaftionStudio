package com.example.ecotionbuddy

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.ecotionbuddy.databinding.ActivityMainBinding
import com.example.ecotionbuddy.utils.formatWithDots
import com.example.ecotionbuddy.utils.PreferencesManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        setupClickListeners()
        loadUserData()
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Already on home
                    true
                }
                R.id.navigation_ask_ai -> {
                    startActivity(Intent(this, ChatbotActivity::class.java))
                    true
                }
                R.id.navigation_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    true
                }
                R.id.navigation_account -> {
                    startActivity(Intent(this, AccountActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnTanyaAI.setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
        }
        
        binding.btnScanWaste.setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java))
        }
        
        binding.btnStartMission.setOnClickListener {
            startActivity(Intent(this, MissionActivity::class.java))
        }
        
        binding.cardPoints.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        
        binding.btnScanQR.setOnClickListener {
            startActivity(Intent(this, QRScanActivity::class.java))
        }
    }
    
    private fun loadUserData() {
        val preferencesManager = PreferencesManager(this)
        val userName = preferencesManager.userName ?: "User"
        val userId = preferencesManager.userId ?: "guest"
        
        binding.apply {
            tvGreeting.text = getString(R.string.greeting_format, userName.split(" ").first())
            
            // Load user points from backend
            loadUserPoints(userId)
            
            // Load missions from backend
            loadFeaturedMission()
        }
    }
    
    private fun loadUserPoints(userId: String) {
        // Use coroutines to fetch user data from backend
        lifecycleScope.launch {
            try {
                val userResponse = com.example.ecotionbuddy.data.network.RetrofitClient.api.getUser(userId)
                binding.tvPoints.text = userResponse.points.formatWithDots()
            } catch (e: Exception) {
                // Fallback to default points if API fails
                binding.tvPoints.text = "0"
            }
        }
    }
    
    private fun loadFeaturedMission() {
        // For now use static mission, can be enhanced to load from backend
        binding.apply {
            tvMissionTitle.text = getString(R.string.mission_collect_plastic)
            tvMissionDesc.text = getString(R.string.mission_description_plastic)
            tvDaysLeft.text = resources.getQuantityString(R.plurals.plural_days_left, 30, 30)
        }
    }
}