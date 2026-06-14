package com.lfc.consumer.location

import android.util.LruCache
import com.lfc.consumer.util.ApiBaseUrl
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

private data class ReverseGeocodeResponse(val address: String?)

object GeocodeAddressHelper {
    private val cache = LruCache<String, String>(200)
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    suspend fun reverseGeocode(latitude: Double, longitude: Double): String? {
        val cacheKey = "%.4f,%.4f".format(latitude, longitude)
        cache.get(cacheKey)?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val url = buildString {
                    append(ApiBaseUrl.resolve())
                    append("consumer/geocode/reverse")
                    append("?latitude=").append(latitude)
                    append("&longitude=").append(longitude)
                }
                val request = Request.Builder().url(url).get().build()
                http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body?.string().orEmpty()
                    val parsed = gson.fromJson(body, ReverseGeocodeResponse::class.java)
                    val address = parsed.address?.trim().takeIf { !it.isNullOrBlank() }
                    if (address != null) {
                        cache.put(cacheKey, address)
                    }
                    address
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}
