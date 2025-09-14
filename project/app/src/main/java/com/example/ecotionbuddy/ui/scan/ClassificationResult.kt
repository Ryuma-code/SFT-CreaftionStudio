package com.example.ecotionbuddy.ui.scan

data class ClassificationResult(
    val label: String,
    val confidence: Float,
    val category: String,
    val suggestions: List<String>,
    val points: Int
)

data class ClassificationResponse(
    val prediction: ClassificationResult
)
