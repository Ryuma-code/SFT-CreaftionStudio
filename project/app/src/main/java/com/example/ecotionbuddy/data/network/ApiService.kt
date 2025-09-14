package com.example.ecotionbuddy.data.network

import com.example.ecotionbuddy.data.network.dto.AndroidEventDto
import com.example.ecotionbuddy.data.network.dto.EventsResponse
import com.example.ecotionbuddy.ui.scan.ClassificationResponse
import okhttp3.RequestBody
import retrofit2.http.*
import retrofit2.Response

interface ApiService {
    @POST("events")
    suspend fun postAndroidEvent(@Body event: AndroidEventDto): Map<String, Any?>

    @GET("events/latest")
    suspend fun getLatestEvents(@Query("limit") limit: Int = 50): EventsResponse
    
    @GET("users/{userId}")
    suspend fun getUser(@Path("userId") userId: String): UserResponse
    
    @POST("classify")
    suspend fun classifyImage(@Body imageBytes: ByteArray): ClassificationResponse
    
    @POST("session/start")
    suspend fun startSession(@Body request: StartSessionRequest): SessionResponse
    
    @POST("session/end")
    suspend fun endSession(@Body request: EndSessionRequest): Response<SessionResponse>
    
    @GET("users/{userId}/history")
    suspend fun getUserHistory(@Path("userId") userId: String, @Query("limit") limit: Int = 50): UserHistoryResponse
    
    @POST("users/register")
    suspend fun registerUser(@Body request: UserRegistrationRequest): UserRegistrationResponse
    
    @GET("missions")
    suspend fun getAvailableMissions(): MissionsResponse
    
    @GET("users/{userId}/missions")
    suspend fun getUserMissions(@Path("userId") userId: String): UserMissionsResponse
    
    @POST("users/{userId}/missions/{missionId}/start")
    suspend fun startMission(@Path("userId") userId: String, @Path("missionId") missionId: String): MissionStartResponse
    
    @POST("users/{userId}/missions/check_progress")
    suspend fun checkMissionProgress(@Path("userId") userId: String): MissionProgressResponse
}

data class UserResponse(
    val userId: String,
    val points: Int,
    val claimsCount: Int,
    val recentClaims: List<Map<String, Any>>
)

data class StartSessionRequest(
    val userId: String,
    val binId: String,
    val countdownMs: Int = 3000,
    val deviceId: String? = null
)

data class SessionResponse(
    val status: String,
    val sessionId: String,
    val deviceId: String
)

data class EndSessionRequest(
    val sessionId: String,
    val reason: String? = null
)

data class UserHistoryResponse(
    val history: List<HistoryItem>
)

data class HistoryItem(
    val id: String,
    val type: String,
    val title: String,
    val description: String,
    val pointsEarned: Int,
    val timestamp: Long,
    val category: String
)

data class UserRegistrationRequest(
    val userId: String,
    val name: String,
    val email: String
)

data class UserRegistrationResponse(
    val status: String,
    val message: String,
    val user: UserInfo
)

data class UserInfo(
    val userId: String,
    val name: String,
    val email: String,
    val points: Int,
    val level: Int
)

data class MissionsResponse(
    val missions: List<Mission>
)

data class Mission(
    val id: String,
    val title: String,
    val description: String,
    val type: String,
    val target: Int,
    val reward_points: Int,
    val duration_days: Int,
    val requirements: Map<String, Any>,
    val progress: Int = 0,
    val started_at: String? = null,
    val expires_at: String? = null
)

data class UserMissionsResponse(
    val active_missions: List<Mission>,
    val completed_missions: List<Mission>,
    val expired_missions: List<Mission>
)

data class MissionStartResponse(
    val status: String,
    val message: String,
    val mission: Mission
)

data class MissionProgressResponse(
    val completed_missions: List<Mission>,
    val updated_missions: List<Mission>,
    val points_earned: Int
)
