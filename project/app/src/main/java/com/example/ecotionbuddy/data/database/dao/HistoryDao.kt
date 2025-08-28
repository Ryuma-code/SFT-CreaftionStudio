package com.example.ecotionbuddy.data.database.dao

import androidx.room.*
import androidx.lifecycle.LiveData
import com.example.ecotionbuddy.data.database.entities.HistoryEntity
import com.example.ecotionbuddy.data.models.HistoryType

@Dao
interface HistoryDao {
    
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): LiveData<List<HistoryEntity>>
    
    @Query("SELECT * FROM history WHERE type = :type ORDER BY timestamp DESC")
    fun getHistoryByType(type: HistoryType): LiveData<List<HistoryEntity>>
    
    @Query("SELECT * FROM history WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getHistoryByDateRange(startTime: Long, endTime: Long): LiveData<List<HistoryEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)
    
    @Query("SELECT SUM(pointsEarned) FROM history WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getPointsInDateRange(startTime: Long, endTime: Long): Int?
    
    @Query("DELETE FROM history WHERE timestamp < :cutoffTime")
    suspend fun deleteOldHistory(cutoffTime: Long)
}