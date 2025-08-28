package com.example.ecotionbuddy.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.ecotionbuddy.data.database.dao.UserDao
import com.example.ecotionbuddy.data.database.dao.MissionDao
import com.example.ecotionbuddy.data.database.dao.HistoryDao
import com.example.ecotionbuddy.data.database.entities.UserEntity
import com.example.ecotionbuddy.data.database.entities.MissionEntity
import com.example.ecotionbuddy.data.database.entities.HistoryEntity

@Database(
    entities = [UserEntity::class, MissionEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class EcotionDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun missionDao(): MissionDao
    abstract fun historyDao(): HistoryDao
    
    companion object {
        @Volatile
        private var INSTANCE: EcotionDatabase? = null
        
        fun getDatabase(context: Context): EcotionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EcotionDatabase::class.java,
                    "ecotion_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}