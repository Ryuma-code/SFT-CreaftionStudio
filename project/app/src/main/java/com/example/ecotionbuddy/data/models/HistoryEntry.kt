package com.example.ecotionbuddy.data.models

data class HistoryEntry(
    val id: String = "",
    val type: HistoryType = HistoryType.MISSION_COMPLETED,
    val title: String = "",
    val description: String = "",
    val pointsEarned: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String = "",
    val category: WasteCategory? = null
)

enum class HistoryType(val displayName: String) {
    MISSION_COMPLETED("Misi Selesai"),
    WASTE_SCANNED("Sampah Dipindai"),
    POINTS_EARNED("Poin Diperoleh"),
    LEVEL_UP("Naik Level"),
    ACHIEVEMENT_UNLOCKED("Pencapaian Dibuka")
}