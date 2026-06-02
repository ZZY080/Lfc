package com.lfc.consumer.data

import android.util.Log
import com.lfc.consumer.BuildConfig
import com.lfc.consumer.data.api.LfcApiService
import com.lfc.consumer.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val TAG = "ApiClient"

    fun createApiService(tokenManager: TokenManager): LfcApiService {
        Log.d(TAG, "API base URL: ${BuildConfig.API_BASE_URL}")
        val authInterceptor = Interceptor { chain ->
            val token = runBlocking { tokenManager.getToken() }
            val request = if (!token.isNullOrBlank()) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LfcApiService::class.java)
    }
}
