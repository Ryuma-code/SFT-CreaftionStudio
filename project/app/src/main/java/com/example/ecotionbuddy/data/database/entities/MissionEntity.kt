package com.example.ecotionbuddy.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ecotionbuddy.data.models.WasteCategory
import com.example.ecotionbuddy.data.models.MissionDifficulty

@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val category: WasteCategory,
    val pointsReward: Int,
    val targetAmount: Double,
    val currentProgress: Double,
    val deadline: Long,
    val imageUrl: String,
    val isCompleted: Boolean,
    val completedDate: Long?,
    val difficulty: MissionDifficulty
)