package com.lfc.consumer.data

import android.util.Log
import com.lfc.consumer.BuildConfig
import com.lfc.consumer.data.api.LfcApiService
import com.lfc.consumer.util.ApiBaseUrl
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

    /** 登录/注册/刷新等无需携带 accessToken 的接口 */
    fun createAuthApiService(): LfcApiService {
        return buildRetrofit(createBaseClient()).create(LfcApiService::class.java)
    }

    /** 业务接口：自动附带 Bearer Token，401 时静默刷新 */
    fun createApiService(tokenManager: TokenManager): LfcApiService {
        Log.d(TAG, "API base URL: ${ApiBaseUrl.resolve()} (configured: ${BuildConfig.API_BASE_URL})")
        val authApi = createAuthApiService()
        val client = createBaseClient()
            .newBuilder()
            .authenticator(BearerTokenAuthenticator(tokenManager, authApi))
            .addInterceptor(createAuthInterceptor(tokenManager))
            .build()
        return buildRetrofit(client).create(LfcApiService::class.java)
    }

    private fun createAuthInterceptor(tokenManager: TokenManager): Interceptor = Interceptor { chain ->
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

    private fun createBaseClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun buildRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiBaseUrl.resolve())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
