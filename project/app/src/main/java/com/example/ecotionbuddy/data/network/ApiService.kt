package com.example.ecotionbuddy.data.network

import com.example.ecotionbuddy.data.network.dto.AndroidEventDto
import com.example.ecotionbuddy.data.network.dto.EventsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("events")
    suspend fun postAndroidEvent(@Body event: AndroidEventDto): Map<String, Any?>

    @GET("events/latest")
    suspend fun getLatestEvents(@Query("limit") limit: Int = 50): EventsResponse
}
