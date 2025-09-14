package com.example.ecotionbuddy.utils

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("ecotion_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    }
    
    var userId: String?
        get() = sharedPreferences.getString(KEY_USER_ID, null)
        set(value) = sharedPreferences.edit().putString(KEY_USER_ID, value).apply()
    
    var userName: String?
        get() = sharedPreferences.getString(KEY_USER_NAME, null)
        set(value) = sharedPreferences.edit().putString(KEY_USER_NAME, value).apply()
    
    var userEmail: String?
        get() = sharedPreferences.getString(KEY_USER_EMAIL, null)
        set(value) = sharedPreferences.edit().putString(KEY_USER_EMAIL, value).apply()
    
    var isFirstLaunch: Boolean
        get() = sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()
    
    var notificationsEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()
    
    fun isLoggedIn(): Boolean {
        return !userId.isNullOrEmpty()
    }
    
    fun saveUserData(userId: String, userName: String) {
        this.userId = userId
        this.userName = userName
    }
    
    fun logout() {
        userId = null
        userName = null
        userEmail = null
    }
    
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }
}