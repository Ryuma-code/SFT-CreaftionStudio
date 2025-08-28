package com.example.ecotionbuddy.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val email: String,
    val points: Int,
    val level: Int,
    val profileImageUrl: String,
    val joinDate: Long,
    val totalMissionsCompleted: Int,
    val totalWasteCollected: Double
)