package com.lfc.consumer.location

import android.content.Context
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.lfc.consumer.BuildConfig
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

object AmapLocationHelper {
    @Volatile
    private var privacyInitialized = false

    fun ensurePrivacy(context: Context) {
        if (privacyInitialized) return
        synchronized(this) {
            if (privacyInitialized) return
            AMapLocationClient.updatePrivacyShow(context.applicationContext, true, true)
            AMapLocationClient.updatePrivacyAgree(context.applicationContext, true)
            privacyInitialized = true
        }
    }

    suspend fun getCurrentLocation(context: Context): Result<String> {
        if (BuildConfig.AMAP_API_KEY.isBlank()) {
            return Result.failure(IllegalStateException("请先在 gradle.properties 配置 AMAP_API_KEY"))
        }

        ensurePrivacy(context.applicationContext)

        return suspendCancellableCoroutine { continuation ->
            val client = AMapLocationClient(context.applicationContext)
            val option = AMapLocationClientOption().apply {
                locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                isOnceLocation = true
                isOnceLocationLatest = true
                isNeedAddress = true
                isMockEnable = false
                httpTimeOut = 20_000
            }
            client.setLocationOption(option)
            client.setLocationListener { location ->
                client.stopLocation()
                client.onDestroy()
                if (continuation.isCancelled) return@setLocationListener
                if (location != null && location.errorCode == AMapLocation.LOCATION_SUCCESS) {
                    continuation.resume(Result.success(formatLocation(location)))
                } else {
                    continuation.resume(
                        Result.failure(
                            IllegalStateException(location?.errorInfo?.ifBlank { null } ?: "定位失败"),
                        ),
                    )
                }
            }
            continuation.invokeOnCancellation {
                client.stopLocation()
                client.onDestroy()
            }
            client.startLocation()
        }
    }

    private fun formatLocation(location: AMapLocation): String {
        val poi = location.poiName?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
        val address = location.address?.takeIf { it.isNotBlank() }
        return when {
            poi != null && address != null && !address.contains(poi) -> "$poi（$address）"
            poi != null -> poi
            !address.isNullOrBlank() -> address
            else -> buildString {
                append(location.city.orEmpty())
                append(location.district.orEmpty())
                append(location.street.orEmpty())
                append(location.streetNum.orEmpty())
            }.ifBlank {
                "纬度 ${"%.5f".format(location.latitude)}, 经度 ${"%.5f".format(location.longitude)}"
            }
        }
    }
}
