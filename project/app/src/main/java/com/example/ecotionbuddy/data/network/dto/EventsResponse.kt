package com.example.ecotionbuddy.data.network.dto

// Matches backend /events/latest response structure
// { "items": [ ... ], "count": 12 }

data class EventsResponse(
    val items: List<EventItem>,
    val count: Int
)
