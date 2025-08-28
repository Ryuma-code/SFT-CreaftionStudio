package com.example.ecotionbuddy.data.models

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val points: Int = 0,
    val level: Int = 1,
    val profileImageUrl: String = "",
    val joinDate: Long = System.currentTimeMillis(),
    val totalMissionsCompleted: Int = 0,
    val totalWasteCollected: Double = 0.0 // in kg
)