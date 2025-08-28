package com.example.ecotionbuddy.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.example.ecotionbuddy.data.database.dao.MissionDao
import com.example.ecotionbuddy.data.database.entities.MissionEntity
import com.example.ecotionbuddy.data.models.Mission
import com.example.ecotionbuddy.data.models.WasteCategory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MissionRepository @Inject constructor(
    private val missionDao: MissionDao
) {
    
    fun getActiveMissions(): LiveData<List<Mission>> {
        return missionDao.getActiveMissions().map { entities ->
            entities.map { it.toMission() }
        }
    }
    
    fun getCompletedMissions(): LiveData<List<Mission>> {
        return missionDao.getCompletedMissions().map { entities ->
            entities.map { it.toMission() }
        }
    }
    
    fun getMissionsByCategory(category: WasteCategory): LiveData<List<Mission>> {
        return missionDao.getMissionsByCategory(category).map { entities ->
            entities.map { it.toMission() }
        }
    }
    
    suspend fun getMission(missionId: String): Mission? {
        return missionDao.getMission(missionId)?.toMission()
    }
    
    suspend fun insertMission(mission: Mission) {
        missionDao.insertMission(mission.toEntity())
    }
    
    suspend fun insertMissions(missions: List<Mission>) {
        missionDao.insertMissions(missions.map { it.toEntity() })
    }
    
    suspend fun updateMission(mission: Mission) {
        missionDao.updateMission(mission.toEntity())
    }
    
    suspend fun updateProgress(missionId: String, progress: Double) {
        missionDao.updateProgress(missionId, progress)
    }
    
    suspend fun completeMission(missionId: String) {
        missionDao.completeMission(missionId, System.currentTimeMillis())
    }
    
    private fun MissionEntity.toMission(): Mission {
        return Mission(
            id = id,
            title = title,
            description = description,
            category = category,
            pointsReward = pointsReward,
            targetAmount = targetAmount,
            currentProgress = currentProgress,
            deadline = deadline,
            imageUrl = imageUrl,
            isCompleted = isCompleted,
            completedDate = completedDate,
            difficulty = difficulty
        )
    }
    
    private fun Mission.toEntity(): MissionEntity {
        return MissionEntity(
            id = id,
            title = title,
            description = description,
            category = category,
            pointsReward = pointsReward,
            targetAmount = targetAmount,
            currentProgress = currentProgress,
            deadline = deadline,
            imageUrl = imageUrl,
            isCompleted = isCompleted,
            completedDate = completedDate,
            difficulty = difficulty
        )
    }
}