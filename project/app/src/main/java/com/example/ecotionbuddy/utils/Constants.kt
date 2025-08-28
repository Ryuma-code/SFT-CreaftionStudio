package com.example.ecotionbuddy.utils

object Constants {
    
    // Points System
    const val POINTS_PER_SCAN = 50
    const val POINTS_PER_KG_PLASTIC = 200
    const val POINTS_PER_KG_ORGANIC = 100
    const val POINTS_PER_KG_PAPER = 150
    const val POINTS_PER_KG_METAL = 300
    const val POINTS_PER_KG_GLASS = 250
    const val POINTS_PER_KG_ELECTRONIC = 500
    const val POINTS_PER_KG_TEXTILE = 180
    
    // Level System
    const val POINTS_PER_LEVEL = 10000
    const val MAX_LEVEL = 50
    
    // Mission Types
    const val MISSION_TYPE_COLLECT = "collect"
    const val MISSION_TYPE_SCAN = "scan"
    const val MISSION_TYPE_RECYCLE = "recycle"
    const val MISSION_TYPE_EDUCATE = "educate"
    
    // AI Chat
    const val MAX_CHAT_HISTORY = 50
    const val CHAT_TIMEOUT_MS = 30000L
    
    // Camera
    const val IMAGE_CAPTURE_QUALITY = 85
    const val MAX_IMAGE_SIZE = 1024
    
    // Notifications
    const val NOTIFICATION_CHANNEL_ID = "ecotion_notifications"
    const val NOTIFICATION_CHANNEL_NAME = "EcotionBuddy Notifications"
    
    // Database
    const val DATABASE_VERSION = 1
    const val DATABASE_NAME = "ecotion_database"
}