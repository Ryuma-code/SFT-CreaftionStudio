package com.example.ecotionbuddy.data.network.gemini

// Minimal DTOs for Google AI Studio (Gemini) generateContent v1beta
// API: POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key=API_KEY

data class GeminiPart(
    val text: String
)

data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

data class GeminiRequest(
    val contents: List<GeminiContent>
)

data class GeminiCandidate(
    val content: GeminiContent
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)
