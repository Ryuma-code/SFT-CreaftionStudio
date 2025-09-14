package com.example.ecotionbuddy

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ecotionbuddy.databinding.ActivityLoginBinding
import com.example.ecotionbuddy.utils.PreferencesManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferencesManager = PreferencesManager(this)
        
        // Check if user is already logged in
        if (preferencesManager.isLoggedIn()) {
            navigateToMain()
            return
        }
        
        setupViews()
    }
    
    private fun setupViews() {
        binding.btnLogin.setOnClickListener {
            val userId = binding.etUserId.text.toString().trim()
            val name = binding.etName.text.toString().trim()
            
            if (validateInput(userId, name)) {
                performLogin(userId, name)
            }
        }
        
        binding.btnSkipLogin.setOnClickListener {
            // For demo purposes, create a guest user
            val guestUserId = "guest-${System.currentTimeMillis()}"
            performLogin(guestUserId, "Guest User")
        }
        
        binding.btnRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun validateInput(userId: String, name: String): Boolean {
        if (userId.isEmpty()) {
            binding.etUserId.error = "User ID tidak boleh kosong"
            return false
        }
        
        if (name.isEmpty()) {
            binding.etName.error = "Nama tidak boleh kosong"
            return false
        }
        
        if (userId.length < 3) {
            binding.etUserId.error = "User ID minimal 3 karakter"
            return false
        }
        
        return true
    }
    
    private fun performLogin(userId: String, name: String) {
        lifecycleScope.launch {
            try {
                // Save user data locally
                preferencesManager.saveUserData(userId, name)
                
                Toast.makeText(this@LoginActivity, "Login berhasil! Selamat datang, $name", Toast.LENGTH_SHORT).show()
                navigateToMain()
                
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Login gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
