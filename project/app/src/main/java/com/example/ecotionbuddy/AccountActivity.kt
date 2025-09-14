package com.example.ecotionbuddy

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
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
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.menuPrivacyPolicy.setOnClickListener {
            // Navigate to privacy policy (placeholder)
        }

        binding.menuLogout.setOnClickListener {
            val preferencesManager = com.example.ecotionbuddy.utils.PreferencesManager(this)
            preferencesManager.logout()
            
            // Navigate back to login
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadUserData() {
        val preferencesManager = com.example.ecotionbuddy.utils.PreferencesManager(this)
        val userName = preferencesManager.userName ?: "User"
        val userEmail = preferencesManager.userEmail ?: "user@email.com"
        val userId = preferencesManager.userId ?: "guest"
        
        binding.apply {
            this.userName.text = userName
            this.userEmail.text = userEmail
            
            // Load points from backend
            loadUserPoints(userId)
        }
    }
    
    private fun loadUserPoints(userId: String) {
        lifecycleScope.launch {
            try {
                val userResponse = com.example.ecotionbuddy.data.network.RetrofitClient.api.getUser(userId)
                binding.userPoints.text = getString(R.string.total_points_format, userResponse.points)
            } catch (e: Exception) {
                binding.userPoints.text = getString(R.string.total_points_format, 0)
            }
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