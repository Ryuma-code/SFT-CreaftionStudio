package com.example.ecotionbuddy.data.network.gemini

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object GeminiClient {
    private fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        builder.addInterceptor(logging)
        return builder.build()
    }

    val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(provideOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApi::class.java)
    }
}
