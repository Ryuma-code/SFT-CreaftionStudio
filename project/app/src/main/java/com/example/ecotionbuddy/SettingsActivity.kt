package com.example.ecotionbuddy

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ecotionbuddy.databinding.ActivitySettingsBinding
import com.example.ecotionbuddy.data.network.RetrofitClient
import com.example.ecotionbuddy.utils.PreferencesManager
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(this)
        setupToolbar()
        setupViews()
        loadUserSettings()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Pengaturan"
    }

    private fun setupViews() {
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.notificationsEnabled = isChecked
            Toast.makeText(this, if (isChecked) "Notifikasi diaktifkan" else "Notifikasi dinonaktifkan", Toast.LENGTH_SHORT).show()
        }

        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.btnPrivacyPolicy.setOnClickListener {
            showPrivacyPolicy()
        }

        binding.btnAbout.setOnClickListener {
            showAboutDialog()
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        binding.btnDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmation()
        }
    }

    private fun loadUserSettings() {
        binding.switchNotifications.isChecked = preferencesManager.notificationsEnabled
        
        val userName = preferencesManager.userName ?: "User"
        val userEmail = preferencesManager.userEmail ?: "user@email.com"
        
        binding.tvUserName.text = userName
        binding.tvUserEmail.text = userEmail
    }

    private fun showEditProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val etName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etName)
        val etEmail = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEmail)
        
        etName.setText(preferencesManager.userName)
        etEmail.setText(preferencesManager.userEmail)

        AlertDialog.Builder(this)
            .setTitle("Edit Profil")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val newName = etName.text.toString().trim()
                val newEmail = etEmail.text.toString().trim()
                
                if (newName.isNotEmpty() && newEmail.isNotEmpty()) {
                    updateProfile(newName, newEmail)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updateProfile(name: String, email: String) {
        lifecycleScope.launch {
            try {
                // Update locally
                preferencesManager.userName = name
                preferencesManager.userEmail = email
                
                // Update UI
                binding.tvUserName.text = name
                binding.tvUserEmail.text = email
                
                Toast.makeText(this@SettingsActivity, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Gagal memperbarui profil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        
        AlertDialog.Builder(this)
            .setTitle("Ubah Password")
            .setView(dialogView)
            .setPositiveButton("Ubah") { _, _ ->
                Toast.makeText(this, "Fitur ubah password akan segera tersedia", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showPrivacyPolicy() {
        AlertDialog.Builder(this)
            .setTitle("Kebijakan Privasi")
            .setMessage("""
                EcotionBuddy berkomitmen untuk melindungi privasi pengguna.
                
                Data yang kami kumpulkan:
                - Informasi profil pengguna
                - Aktivitas pemindaian sampah
                - Riwayat misi dan pencapaian
                
                Data ini digunakan untuk:
                - Meningkatkan pengalaman pengguna
                - Menyediakan rekomendasi yang relevan
                - Melacak progress lingkungan
                
                Kami tidak membagikan data pribadi kepada pihak ketiga tanpa persetujuan.
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Tentang EcotionBuddy")
            .setMessage("""
                EcotionBuddy v1.0
                
                Aplikasi edukasi dan manajemen sampah yang membantu Anda:
                - Mengidentifikasi jenis sampah
                - Mendapatkan saran daur ulang
                - Menyelesaikan misi lingkungan
                - Melacak kontribusi terhadap lingkungan
                
                Dikembangkan dengan ❤️ untuk bumi yang lebih hijau.
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Apakah Anda yakin ingin keluar dari akun?")
            .setPositiveButton("Ya") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun performLogout() {
        preferencesManager.logout()
        
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showDeleteAccountConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Hapus Akun")
            .setMessage("PERINGATAN: Tindakan ini akan menghapus semua data Anda secara permanen dan tidak dapat dibatalkan. Apakah Anda yakin?")
            .setPositiveButton("Hapus") { _, _ ->
                performDeleteAccount()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun performDeleteAccount() {
        lifecycleScope.launch {
            try {
                // Clear local data
                preferencesManager.clearAll()
                
                Toast.makeText(this@SettingsActivity, "Akun berhasil dihapus", Toast.LENGTH_SHORT).show()
                
                val intent = Intent(this@SettingsActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Gagal menghapus akun: ${e.message}", Toast.LENGTH_SHORT).show()
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
