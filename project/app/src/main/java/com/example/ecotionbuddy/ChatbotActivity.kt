package com.example.ecotionbuddy

// File: ChatbotActivity.kt

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ecotionbuddy.databinding.ActivityChatbotBinding // Ganti dengan package project Anda
import androidx.lifecycle.lifecycleScope
import com.example.ecotionbuddy.BuildConfig
import com.example.ecotionbuddy.data.network.gemini.GeminiClient
import com.example.ecotionbuddy.data.network.gemini.GeminiContent
import com.example.ecotionbuddy.data.network.gemini.GeminiPart
import com.example.ecotionbuddy.data.network.gemini.GeminiRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatbotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatbotBinding
    private lateinit var chatAdapter: ChatAdapter
    private val messageList = mutableListOf<ChatMessage>()

    // Model Gemini dan prompt sistem yang fokus pada keberlanjutan lingkungan & manajemen sampah
    private val geminiModel = "gemini-1.5-flash"
    private val systemPrompt = """
        You are EcoBuddy, an expert assistant focused on environmental sustainability and waste management.
        Objectives:
        - Give practical, localized, respectful guidance on how to reduce, reuse, and recycle.
        - Teach source separation (organic, plastic, paper, glass, metal, e-waste) and contamination risks.
        - Explain why a material belongs to a bin type and common mistakes to avoid.
        - Encourage behavior change with actionable tips and short checklists.
        - When uncertain, ask clarifying questions (e.g., material type, residue, labels).
        Constraints:
        - Keep answers concise and structured with short bullets.
        - Prefer simple language; avoid jargon.
        - If asked about program points or bin compatibility, refer to the app’s session flow and local rules.
    """.trimIndent()

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

                // Panggil Gemini untuk mendapatkan balasan
                chatWithGemini()
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

    private fun chatWithGemini() {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            addMessageToChat("Konfigurasi API key belum diatur. Tambahkan GEMINI_API_KEY di local.properties.", Sender.BOT)
            return
        }

        // Tampilkan pesan "sedang mengetik"
        addMessageToChat("Memikirkan jawaban terbaik untukmu...", Sender.BOT)

        // Susun percakapan untuk Gemini: gabungkan prompt sistem + riwayat chat
        val contents = buildContentsFromHistory()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = GeminiRequest(contents = contents)
                val resp = GeminiClient.api.generateContent(
                    model = geminiModel,
                    apiKey = apiKey,
                    body = request
                )

                val text = resp.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text
                    ?.trim()
                    ?: "Maaf, saya tidak menemukan jawaban saat ini. Coba tanyakan dengan cara berbeda."

                withContext(Dispatchers.Main) {
                    addMessageToChat(text, Sender.BOT)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    addMessageToChat("Terjadi kesalahan memanggil model: ${e.localizedMessage}", Sender.BOT)
                }
            }
        }
    }

    private fun buildContentsFromHistory(): List<GeminiContent> {
        val list = mutableListOf<GeminiContent>()
        // Masukkan prompt sistem sebagai konteks awal
        list += GeminiContent(role = "user", parts = listOf(GeminiPart(text = systemPrompt)))
        // Masukkan riwayat percakapan: mapping Sender.USER -> role "user", Sender.BOT -> role "model"
        messageList.forEach { msg ->
            val role = if (msg.sender == Sender.USER) "user" else "model"
            list += GeminiContent(role = role, parts = listOf(GeminiPart(text = msg.message)))
        }
        return list
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