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
    private var privacyShown = false
    @Volatile
    private var privacyAgreed = false

    /** 展示高德隐私政策（不表示用户已同意） */
    fun showPrivacyPolicy(context: Context) {
        if (privacyShown) return
        synchronized(this) {
            if (privacyShown) return
            AMapLocationClient.updatePrivacyShow(context.applicationContext, true, true)
            privacyShown = true
        }
    }

    /** 用户同意隐私政策后调用 */
    fun agreePrivacy(context: Context) {
        showPrivacyPolicy(context)
        if (privacyAgreed) return
        synchronized(this) {
            if (privacyAgreed) return
            AMapLocationClient.updatePrivacyAgree(context.applicationContext, true)
            privacyAgreed = true
        }
    }

    fun isPrivacyAgreed(): Boolean = privacyAgreed

    suspend fun getCurrentLocation(context: Context): Result<ActivityLocation> {
        if (BuildConfig.AMAP_API_KEY.isBlank()) {
            return Result.failure(IllegalStateException("请先在 gradle.properties 配置 AMAP_API_KEY"))
        }

        if (!privacyAgreed) {
            return Result.failure(IllegalStateException("使用定位前需先同意隐私政策"))
        }
        showPrivacyPolicy(context)

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
        }.mapCatching { baseLocation ->
            refineLocationWithReverseGeocode(baseLocation)
        }
    }

    private suspend fun refineLocationWithReverseGeocode(
        baseLocation: ActivityLocation,
    ): ActivityLocation {
        val refined = GeocodeAddressHelper.reverseGeocodeDetail(
            baseLocation.latitude,
            baseLocation.longitude,
        )
        if (refined == null) {
            return baseLocation
        }
        return ActivityLocation(
            address = refined.address,
            latitude = refined.latitude,
            longitude = refined.longitude,
        )
    }

    suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double,
    ): Result<String> {
        val address = GeocodeAddressHelper.reverseGeocode(latitude, longitude)
        return if (!address.isNullOrBlank()) {
            Result.success(address)
        } else {
            Result.failure(IllegalStateException("逆地理编码无结果"))
        }
    }

    private fun formatLocation(location: AMapLocation): ActivityLocation {
        val latitude = location.latitude
        val longitude = location.longitude
        val address = buildFullAddress(location)
        return ActivityLocation(
            address = address,
            latitude = latitude,
            longitude = longitude,
        )
    }

    private fun buildFullAddress(location: AMapLocation): String {
        val province = location.province.normalizedAmapText()
        val city = location.city.normalizedAmapText()
        val district = location.district.normalizedAmapText()
        val street = location.street.normalizedAmapText()
        val streetNum = location.streetNum.normalizedAmapText()
        val poi = location.poiName.normalizedAmapText()
        val aoi = location.aoiName.normalizedAmapText()
        val rawAddress = location.address.normalizedAmapText()

        val streetLine = listOf(street, streetNum)
            .filterNot { it.isNullOrBlank() }
            .joinToString("")
        val adminLine = dedupeAddressParts(listOf(province, city, district))
            .joinToString("")

        val baseAddress = when {
            !rawAddress.isNullOrBlank() -> rawAddress
            adminLine.isNotBlank() || streetLine.isNotBlank() -> adminLine + streetLine
            else -> null
        }

        val placeLabel = listOfNotNull(poi, aoi)
            .distinct()
            .joinToString("·")
            .takeIf { it.isNotBlank() }

        return when {
            placeLabel != null && !baseAddress.isNullOrBlank() && !baseAddress.contains(placeLabel) ->
                "$placeLabel（$baseAddress）"
            placeLabel != null && baseAddress.isNullOrBlank() -> placeLabel
            !baseAddress.isNullOrBlank() -> baseAddress
            else -> "纬度 ${"%.5f".format(location.latitude)}, 经度 ${"%.5f".format(location.longitude)}"
        }
    }

    private fun dedupeAddressParts(parts: List<String?>): List<String> {
        val result = mutableListOf<String>()
        for (part in parts) {
            val value = part?.trim().orEmpty()
            if (value.isBlank()) continue
            if (result.any { existing -> existing.contains(value) || value.contains(existing) }) {
                continue
            }
            result.add(value)
        }
        return result
    }

    private fun String?.normalizedAmapText(): String? {
        val value = this?.trim().orEmpty()
        if (value.isBlank() || value.equals("null", ignoreCase = true)) {
            return null
        }
        return value
    }
}
