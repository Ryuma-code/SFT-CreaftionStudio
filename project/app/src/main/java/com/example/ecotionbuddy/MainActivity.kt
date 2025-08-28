package com.example.ecotionbuddy

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ecotionbuddy.databinding.ActivityMainBinding
import com.example.ecotionbuddy.utils.formatWithDots

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
            // Navigate to mission details or start mission flow
            startActivity(Intent(this, ScanActivity::class.java))
        }
        
        binding.cardPoints.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }
    
    private fun loadUserData() {
        // For demo purposes, using static data
        binding.apply {
            tvGreeting.text = getString(R.string.greeting_format, "Rafli")
            tvPoints.text = 150000.formatWithDots()
            tvMissionTitle.text = getString(R.string.mission_collect_plastic)
            tvMissionDesc.text = getString(R.string.mission_description_plastic)
            tvDaysLeft.text = resources.getQuantityString(R.plurals.plural_days_left, 30, 30)
        }
    }
}