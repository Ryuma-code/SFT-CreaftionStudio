package com.example.ecotionbuddy.data.models

data class Mission(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: WasteCategory = WasteCategory.PLASTIC,
    val pointsReward: Int = 0,
    val targetAmount: Double = 0.0, // in kg
    val currentProgress: Double = 0.0,
    val deadline: Long = 0L,
    val imageUrl: String = "",
    val isCompleted: Boolean = false,
    val completedDate: Long? = null,
    val difficulty: MissionDifficulty = MissionDifficulty.EASY
)

enum class WasteCategory(val displayName: String, val color: String) {
    PLASTIC("Plastik", "#FF6B6B"),
    ORGANIC("Organik", "#4ECDC4"),
    PAPER("Kertas", "#45B7D1"),
    METAL("Logam", "#96CEB4"),
    GLASS("Kaca", "#FFEAA7"),
    ELECTRONIC("Elektronik", "#DDA0DD"),
    TEXTILE("Tekstil", "#98D8C8")
}

enum class MissionDifficulty(val displayName: String, val multiplier: Double) {
    EASY("Mudah", 1.0),
    MEDIUM("Sedang", 1.5),
    HARD("Sulit", 2.0)
}