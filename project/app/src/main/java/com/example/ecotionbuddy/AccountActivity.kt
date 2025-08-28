package com.example.ecotionbuddy

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.ecotionbuddy.databinding.ActivityAkunBinding
import com.example.ecotionbuddy.utils.formatWithDots

class AccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAkunBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAkunBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupUI()
        loadUserData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.account_title)
    }

    private fun setupUI() {
        // Setup menu item click listeners
        binding.menuActivityHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        binding.menuEditProfile.setOnClickListener {
            // Navigate to edit profile (placeholder)
        }

        binding.menuSettings.setOnClickListener {
            // Navigate to settings (placeholder)
        }

        binding.menuPrivacyPolicy.setOnClickListener {
            // Navigate to privacy policy (placeholder)
        }

        binding.menuLogout.setOnClickListener {
            // Handle logout (placeholder)
            finish()
        }
    }

    private fun loadUserData() {
        // Load user data from repository
        // For demo purposes, using static data
        binding.apply {
            userName.text = "Muhammad Rafli"
            userEmail.text = "rafli@email.com"
            // FIX: Pass the raw integer directly to let the string resource handle formatting.
            userPoints.text = getString(R.string.total_points_format, 150000)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}