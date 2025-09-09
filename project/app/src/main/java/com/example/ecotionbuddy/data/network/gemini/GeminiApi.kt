package com.example.ecotionbuddy.data.network.gemini

import com.example.ecotionbuddy.data.network.gemini.GeminiRequest
import com.example.ecotionbuddy.data.network.gemini.GeminiResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body body: GeminiRequest
    ): GeminiResponse
}
