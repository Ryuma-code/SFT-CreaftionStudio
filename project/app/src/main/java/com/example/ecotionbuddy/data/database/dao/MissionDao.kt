package com.example.ecotionbuddy.data.database.dao

import androidx.room.*
import androidx.lifecycle.LiveData
import com.example.ecotionbuddy.data.database.entities.MissionEntity
import com.example.ecotionbuddy.data.models.WasteCategory

@Dao
interface MissionDao {
    
    @Query("SELECT * FROM missions WHERE isCompleted = 0 ORDER BY deadline ASC")
    fun getActiveMissions(): LiveData<List<MissionEntity>>
    
    @Query("SELECT * FROM missions WHERE isCompleted = 1 ORDER BY completedDate DESC")
    fun getCompletedMissions(): LiveData<List<MissionEntity>>
    
    @Query("SELECT * FROM missions WHERE id = :missionId")
    suspend fun getMission(missionId: String): MissionEntity?
    
    @Query("SELECT * FROM missions WHERE category = :category AND isCompleted = 0")
    fun getMissionsByCategory(category: WasteCategory): LiveData<List<MissionEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMission(mission: MissionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMissions(missions: List<MissionEntity>)
    
    @Update
    suspend fun updateMission(mission: MissionEntity)
    
    @Query("UPDATE missions SET currentProgress = :progress WHERE id = :missionId")
    suspend fun updateProgress(missionId: String, progress: Double)
    
    @Query("UPDATE missions SET isCompleted = 1, completedDate = :completedDate WHERE id = :missionId")
    suspend fun completeMission(missionId: String, completedDate: Long)
    
    @Delete
    suspend fun deleteMission(mission: MissionEntity)
}