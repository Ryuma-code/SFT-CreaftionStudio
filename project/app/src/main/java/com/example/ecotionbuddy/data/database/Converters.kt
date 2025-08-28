package com.example.ecotionbuddy.data.database

import androidx.room.TypeConverter
import com.example.ecotionbuddy.data.models.WasteCategory
import com.example.ecotionbuddy.data.models.MissionDifficulty
import com.example.ecotionbuddy.data.models.HistoryType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    
    @TypeConverter
    fun fromWasteCategory(category: WasteCategory): String = category.name
    
    @TypeConverter
    fun toWasteCategory(categoryName: String): WasteCategory = 
        WasteCategory.valueOf(categoryName)
    
    @TypeConverter
    fun fromMissionDifficulty(difficulty: MissionDifficulty): String = difficulty.name
    
    @TypeConverter
    fun toMissionDifficulty(difficultyName: String): MissionDifficulty = 
        MissionDifficulty.valueOf(difficultyName)
    
    @TypeConverter
    fun fromHistoryType(type: HistoryType): String = type.name
    
    @TypeConverter
    fun toHistoryType(typeName: String): HistoryType = 
        HistoryType.valueOf(typeName)
    
    @TypeConverter
    fun fromStringList(value: List<String>): String = Gson().toJson(value)
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }
}