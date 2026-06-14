package com.lfc.consumer.location

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

fun hasValidCoordinate(latitude: Double?, longitude: Double?): Boolean {
    if (latitude == null || longitude == null) return false
    if (latitude == 0.0 && longitude == 0.0) return false
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return false
    return true
}

/** 返回两点间直线距离（米），坐标无效时返回 null */
fun distanceMeters(
    fromLat: Double,
    fromLng: Double,
    toLat: Double,
    toLng: Double,
): Double {
    val dLat = Math.toRadians(toLat - fromLat)
    val dLng = Math.toRadians(toLng - fromLng)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(fromLat)) * cos(Math.toRadians(toLat)) * sin(dLng / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return EARTH_RADIUS_METERS * c
}

fun formatDistanceMeters(meters: Double): String = when {
    meters < 100 -> "100m内"
    meters < 1000 -> "${meters.toInt()}m"
    meters < 10_000 -> String.format("%.1fkm", meters / 1000)
    else -> "${(meters / 1000).toInt()}km"
}

/**
 * @param userLat 当前用户纬度
 * @param userLng 当前用户经度
 * @param targetLat 目标纬度
 * @param targetLng 目标经度
 */
fun formatDistanceLabel(
    userLat: Double?,
    userLng: Double?,
    targetLat: Double?,
    targetLng: Double?,
): String {
    if (!hasValidCoordinate(userLat, userLng)) {
        return "定位中..."
    }
    if (!hasValidCoordinate(targetLat, targetLng)) {
        return "距离未知"
    }
    val meters = distanceMeters(userLat!!, userLng!!, targetLat!!, targetLng!!)
    return "距你 ${formatDistanceMeters(meters)}"
}

/** Feed 卡片紧凑距离，如 1.6km */
fun formatDistanceCompact(
    userLat: Double?,
    userLng: Double?,
    targetLat: Double?,
    targetLng: Double?,
): String? {
    if (!hasValidCoordinate(targetLat, targetLng)) return null
    if (!hasValidCoordinate(userLat, userLng)) return "定位中"
    val meters = distanceMeters(userLat!!, userLng!!, targetLat!!, targetLng!!)
    return formatDistanceMeters(meters)
}

/** 从定位地址提取城市 Tab 文案，如「上海」 */
fun extractFeedCityLabel(address: String?): String {
    val compact = address
        ?.trim()
        ?.removePrefix("中国")
        ?.trim()
        .orEmpty()
    if (compact.isBlank()) return "同城"
    val cityMatch = Regex("([\\u4e00-\\u9fa5]{2,10}市)").find(compact)
    return cityMatch?.groupValues?.get(1)?.removeSuffix("市") ?: "同城"
}

/** Feed 封面角标地址，如「川沙新镇·上海川沙」或「上海·浦东新区」 */
fun formatFeedLocationChipAddress(address: String?, maxLength: Int = 22): String {
    val compact = address
        ?.trim()
        ?.removePrefix("中国")
        ?.trim()
        .orEmpty()
    if (compact.isBlank()) return ""

    val mainPart = compact.substringBefore('（').substringBefore('(').trim()
    val display = when {
        mainPart.contains('·') -> {
            val parts = mainPart.split('·').map { it.trim() }.filter { it.isNotBlank() }
            when {
                parts.size >= 2 -> parts.take(2).joinToString("·")
                else -> parts.firstOrNull().orEmpty()
            }
        }
        else -> formatFeedLocationFromAdminAddress(mainPart)
    }

    return if (display.length <= maxLength) display else "${display.take(maxLength)}..."
}

private fun formatFeedLocationFromAdminAddress(address: String): String {
    val withoutProvince = removeProvincePrefix(address)
    val cityShort = extractCityShortName(withoutProvince)
    val town = Regex("([\\u4e00-\\u9fa5]{2,8}镇)").findAll(withoutProvince).lastOrNull()?.value

    if (town != null && cityShort.isNotBlank()) {
        val tailStart = withoutProvince.indexOf(town) + town.length
        val tail = withoutProvince.substring(tailStart).trim()
        val placeTail = buildString {
            append(cityShort)
            if (tail.isNotBlank()) append(tail.take(6))
        }
        return "$town·$placeTail"
    }

    val cityMatch = Regex("^([\\u4e00-\\u9fa5]{2,10})市").find(withoutProvince)
    if (cityMatch != null) {
        val city = cityMatch.groupValues[1]
        val rest = withoutProvince.removePrefix("${city}市").trim()
        return if (rest.isNotBlank()) "$city·$rest" else city
    }

    return withoutProvince
}

private fun extractCityShortName(address: String): String =
    Regex("([\\u4e00-\\u9fa5]{2,10}市)").find(address)?.groupValues?.get(1)?.removeSuffix("市").orEmpty()

private fun removeProvincePrefix(address: String): String {
    val cityIndex = address.indexOf('市')
    if (cityIndex < 0) return address
    val provinceIndex = address.lastIndexOf('省', cityIndex)
    return if (provinceIndex >= 0) {
        address.drop(provinceIndex + 1)
    } else {
        address
    }
}
