package com.example.ecotionbuddy

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ecotionbuddy.databinding.ActivityRegisterBinding
import com.example.ecotionbuddy.data.network.RetrofitClient
import com.example.ecotionbuddy.utils.PreferencesManager
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferencesManager = PreferencesManager(this)
        setupViews()
    }
    
    private fun setupViews() {
        binding.btnRegister.setOnClickListener {
            val userId = binding.etUserId.text.toString().trim()
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            
            if (validateInput(userId, name, email)) {
                performRegistration(userId, name, email)
            }
        }
        
        binding.btnBackToLogin.setOnClickListener {
            finish()
        }
    }
    
    private fun validateInput(userId: String, name: String, email: String): Boolean {
        if (userId.isEmpty()) {
            binding.etUserId.error = "User ID tidak boleh kosong"
            return false
        }
        
        if (name.isEmpty()) {
            binding.etName.error = "Nama tidak boleh kosong"
            return false
        }
        
        if (email.isEmpty()) {
            binding.etEmail.error = "Email tidak boleh kosong"
            return false
        }
        
        if (userId.length < 3) {
            binding.etUserId.error = "User ID minimal 3 karakter"
            return false
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Format email tidak valid"
            return false
        }
        
        return true
    }
    
    private fun performRegistration(userId: String, name: String, email: String) {
        binding.btnRegister.isEnabled = false
        binding.progressBar.visibility = android.view.View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.registerUser(
                    com.example.ecotionbuddy.data.network.UserRegistrationRequest(
                        userId = userId,
                        name = name,
                        email = email
                    )
                )
                
                // Save user data locally
                preferencesManager.saveUserData(userId, name)
                preferencesManager.userEmail = email
                
                Toast.makeText(this@RegisterActivity, "Registrasi berhasil! Selamat datang, $name", Toast.LENGTH_SHORT).show()
                
                // Navigate to main activity
                val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                
            } catch (e: Exception) {
                Toast.makeText(this@RegisterActivity, "Registrasi gagal: ${e.message}", Toast.LENGTH_LONG).show()
                binding.btnRegister.isEnabled = true
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }
}
