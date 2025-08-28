package com.example.ecotionbuddy.data.models

data class WasteDetection(
    val id: String = "",
    val imageUrl: String = "",
    val detectedCategory: WasteCategory = WasteCategory.PLASTIC,
    val confidence: Float = 0f,
    val suggestions: List<RecyclingSuggestion> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val location: String = "",
    val pointsEarned: Int = 0
)

data class RecyclingSuggestion(
    val title: String = "",
    val description: String = "",
    val steps: List<String> = emptyList(),
    val difficulty: String = "Mudah",
    val estimatedTime: String = "",
    val materialsNeeded: List<String> = emptyList()
)