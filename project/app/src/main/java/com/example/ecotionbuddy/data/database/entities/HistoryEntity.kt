package com.example.ecotionbuddy.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ecotionbuddy.data.models.HistoryType
import com.example.ecotionbuddy.data.models.WasteCategory

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey
    val id: String,
    val type: HistoryType,
    val title: String,
    val description: String,
    val pointsEarned: Int,
    val timestamp: Long,
    val imageUrl: String,
    val category: WasteCategory?
)