package com.example.ecotionbuddy.data.network.dto

data class AndroidEventDto(
    val userId: String,
    val action: String,
    val binId: String? = null,
    val payload: Map<String, String>? = null
)
