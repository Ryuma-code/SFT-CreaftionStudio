package com.example.ecotionbuddy

// File: ChatbotActivity.kt

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ecotionbuddy.databinding.ActivityChatbotBinding // Ganti dengan package project Anda

class ChatbotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatbotBinding
    private lateinit var chatAdapter: ChatAdapter
    private val messageList = mutableListOf<ChatMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatbotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupSendButton()

        // Menampilkan pesan selamat datang dari bot saat pertama kali dibuka
        showInitialBotMessage()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.chatRecyclerView.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@ChatbotActivity)
        }
    }

    private fun showInitialBotMessage() {
        // Tunda pesan bot agar terlihat natural
        Handler(Looper.getMainLooper()).postDelayed({
            addMessageToChat(
                "Halo! Saya AI Ecotion. Ada yang bisa saya bantu terkait pengelolaan sampah?",
                Sender.BOT
            )
        }, 1000) // Tunda 1 detik
    }

    private fun setupSendButton() {
        binding.sendButton.setOnClickListener {
            val userMessage = binding.messageInput.text.toString().trim()
            if (userMessage.isNotEmpty()) {
                // Tambahkan pesan user ke chat
                addMessageToChat(userMessage, Sender.USER)
                binding.messageInput.text?.clear()

                // Simulasikan balasan dari bot
                simulateBotResponse()
            }
        }
    }

    private fun addMessageToChat(message: String, sender: Sender) {
        // Tambahkan pesan baru ke list
        messageList.add(ChatMessage(message, sender))
        // Kirim list yang sudah diupdate ke adapter
        chatAdapter.submitList(messageList.toList()) // Kirim salinan list
        // Scroll ke pesan paling bawah
        binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
    }

    private fun simulateBotResponse() {
        // Tunda balasan bot agar terasa seperti sedang berpikir
        Handler(Looper.getMainLooper()).postDelayed({
            addMessageToChat("Terima kasih atas pertanyaan Anda. Saya sedang memprosesnya...", Sender.BOT)
        }, 1500) // Tunda 1.5 detik
    }

    // Fungsi untuk tombol kembali di toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}