package com.example.ecotionbuddy

// File: ChatMessage.kt

// Enum untuk membedakan pengirim dengan aman dan jelas
enum class Sender {
    USER,
    BOT
}

// Data class untuk menampung informasi satu pesan
data class ChatMessage(
    val message: String,
    val sender: Sender,
    val timestamp: Long = System.currentTimeMillis() // Opsional, untuk data unik
)