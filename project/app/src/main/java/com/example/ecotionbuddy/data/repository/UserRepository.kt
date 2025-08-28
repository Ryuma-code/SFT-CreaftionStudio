package com.example.ecotionbuddy.data.repository

import androidx.lifecycle.LiveData
import com.example.ecotionbuddy.data.database.dao.UserDao
import com.example.ecotionbuddy.data.database.entities.UserEntity
import com.example.ecotionbuddy.data.models.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao
) {
    
    fun getUser(userId: String): LiveData<UserEntity?> = userDao.getUser(userId)
    
    suspend fun getUserSync(userId: String): User? {
        return userDao.getUserSync(userId)?.let { entity ->
            User(
                id = entity.id,
                name = entity.name,
                email = entity.email,
                points = entity.points,
                level = entity.level,
                profileImageUrl = entity.profileImageUrl,
                joinDate = entity.joinDate,
                totalMissionsCompleted = entity.totalMissionsCompleted,
                totalWasteCollected = entity.totalWasteCollected
            )
        }
    }
    
    suspend fun createUser(user: User) {
        val entity = UserEntity(
            id = user.id,
            name = user.name,
            email = user.email,
            points = user.points,
            level = user.level,
            profileImageUrl = user.profileImageUrl,
            joinDate = user.joinDate,
            totalMissionsCompleted = user.totalMissionsCompleted,
            totalWasteCollected = user.totalWasteCollected
        )
        userDao.insertUser(entity)
    }
    
    suspend fun updateUser(user: User) {
        val entity = UserEntity(
            id = user.id,
            name = user.name,
            email = user.email,
            points = user.points,
            level = user.level,
            profileImageUrl = user.profileImageUrl,
            joinDate = user.joinDate,
            totalMissionsCompleted = user.totalMissionsCompleted,
            totalWasteCollected = user.totalWasteCollected
        )
        userDao.updateUser(entity)
    }
    
    suspend fun addPoints(userId: String, points: Int) {
        userDao.addPoints(userId, points)
    }
    
    suspend fun incrementMissionsCompleted(userId: String) {
        userDao.incrementMissionsCompleted(userId)
    }
    
    suspend fun addWasteCollected(userId: String, amount: Double) {
        userDao.addWasteCollected(userId, amount)
    }
}