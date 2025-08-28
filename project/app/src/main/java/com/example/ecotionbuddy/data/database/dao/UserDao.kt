package com.example.ecotionbuddy.data.database.dao

import androidx.room.*
import androidx.lifecycle.LiveData
import com.example.ecotionbuddy.data.database.entities.UserEntity

@Dao
interface UserDao {
    
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUser(userId: String): LiveData<UserEntity?>
    
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserSync(userId: String): UserEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
    
    @Update
    suspend fun updateUser(user: UserEntity)
    
    @Query("UPDATE users SET points = points + :points WHERE id = :userId")
    suspend fun addPoints(userId: String, points: Int)
    
    @Query("UPDATE users SET totalMissionsCompleted = totalMissionsCompleted + 1 WHERE id = :userId")
    suspend fun incrementMissionsCompleted(userId: String)
    
    @Query("UPDATE users SET totalWasteCollected = totalWasteCollected + :amount WHERE id = :userId")
    suspend fun addWasteCollected(userId: String, amount: Double)
}