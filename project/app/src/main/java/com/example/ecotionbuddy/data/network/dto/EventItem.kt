package com.example.ecotionbuddy.data.network.dto

// Generic event item matching backend's events collection documents

data class EventItem(
    val _id: String? = null,
    val origin: String? = null,
    val ts: String? = null,

    // Android-origin fields
    val userId: String? = null,
    val action: String? = null,
    val binId: String? = null,
    val payload: Map<String, Any?>? = null,

    // IoT-origin fields
    val deviceId: String? = null,
    val label: String? = null,
    val confidence: Double? = null,
    val imageUrl: String? = null,
    val extra: Map<String, Any?>? = null
)
