package com.hcmus.forumus_admin.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit client for API communication with the backend server.
 */
object RetrofitClient {
    
    // TODO: Replace with your actual backend server URL
    // For local testing: http://10.0.2.2:8080/ (Android emulator)
    // For local testing: http://localhost:8080/ (physical device on same network)
    // For production: https://your-backend-url.com/
    private const val BASE_URL = "http://10.0.2.2:8081/"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    val emailApi: EmailApiService by lazy {
        retrofit.create(EmailApiService::class.java)
    }
}
