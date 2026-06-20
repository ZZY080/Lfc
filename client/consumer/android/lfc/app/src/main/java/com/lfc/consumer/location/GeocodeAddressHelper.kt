package com.lfc.consumer.location

import android.util.LruCache
import com.lfc.consumer.util.ApiBaseUrl
import com.lfc.consumer.data.model.PlaceSuggestionDto
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class ReverseGeocodeResult(
    val address: String,
    val latitude: Double,
    val longitude: Double,
)

private data class ReverseGeocodeResponseDto(
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
)

private data class PlaceSearchResponseDto(
    val items: List<PlaceSuggestionDto>?,
    val hasMore: Boolean?,
)

object GeocodeAddressHelper {
    private val cache = LruCache<String, ReverseGeocodeResult>(200)
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    suspend fun reverseGeocode(latitude: Double, longitude: Double): String? {
        return reverseGeocodeDetail(latitude, longitude)?.address
    }

    suspend fun reverseGeocodeDetail(
        latitude: Double,
        longitude: Double,
    ): ReverseGeocodeResult? {
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
                    val parsed = gson.fromJson(body, ReverseGeocodeResponseDto::class.java)
                    val address = parsed.address?.trim().takeIf { !it.isNullOrBlank() }
                    if (address == null) return@withContext null
                    val result = ReverseGeocodeResult(
                        address = address,
                        latitude = parsed.latitude ?: latitude,
                        longitude = parsed.longitude ?: longitude,
                    )
                    cache.put(cacheKey, result)
                    result
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun searchPlaces(
        keyword: String,
        page: Int = 1,
        limit: Int = 20,
        latitude: Double? = null,
        longitude: Double? = null,
        city: String? = null,
    ): Pair<List<PlaceSuggestionDto>, Boolean> {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) {
            return emptyList<PlaceSuggestionDto>() to false
        }

        return withContext(Dispatchers.IO) {
            try {
                val url = buildString {
                    append(ApiBaseUrl.resolve())
                    append("consumer/geocode/search")
                    append("?keyword=").append(java.net.URLEncoder.encode(trimmed, "UTF-8"))
                    append("&page=").append(page)
                    append("&limit=").append(limit)
                    if (latitude != null && longitude != null) {
                        append("&latitude=").append(latitude)
                        append("&longitude=").append(longitude)
                    }
                    city?.trim()?.takeIf { it.isNotBlank() && it != "同城" }?.let { cityLabel ->
                        append("&city=").append(java.net.URLEncoder.encode(cityLabel, "UTF-8"))
                    }
                }
                val request = Request.Builder().url(url).get().build()
                http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext emptyList<PlaceSuggestionDto>() to false
                    }
                    val body = response.body?.string().orEmpty()
                    val parsed = gson.fromJson(body, PlaceSearchResponseDto::class.java)
                    val items = parsed.items.orEmpty()
                    val hasMore = parsed.hasMore == true
                    items to hasMore
                }
            } catch (_: Exception) {
                emptyList<PlaceSuggestionDto>() to false
            }
        }
    }
}