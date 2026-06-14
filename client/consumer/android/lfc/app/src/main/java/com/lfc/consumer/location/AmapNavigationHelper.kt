package com.lfc.consumer.location

import android.content.Context
import android.content.Intent
import android.net.Uri

enum class NavigationTravelMode(
    val label: String,
    val routeType: Int,
    val webMode: String,
    val openFeatureName: String? = null,
    val rideType: String? = null,
) {
    DRIVE("驾车", 0, "car"),
    WALK("步行", 2, "walk", "OnFootNavi"),
    RIDE("骑行", 3, "ride", "OnRideNavi", "bike"),
    BUS("公交", 1, "bus"),
}

object AmapNavigationHelper {
    private const val AMAP_PACKAGE = "com.autonavi.minimap"
    private const val SOURCE_APP = "莲峰校园"

    fun openNavigation(
        context: Context,
        name: String,
        latitude: Double?,
        longitude: Double?,
        mode: NavigationTravelMode = NavigationTravelMode.DRIVE,
    ): Boolean {
        val label = name.trim().ifBlank { "活动地点" }
        val encodedName = Uri.encode(label)
        val hasCoordinate = latitude != null &&
            longitude != null &&
            latitude != 0.0 &&
            longitude != 0.0

        if (hasCoordinate) {
            val lat = latitude!!
            val lon = longitude!!
            val candidates = buildCoordinateCandidates(lat, lon, encodedName, mode)
            for (uri in candidates) {
                if (launch(context, uri, AMAP_PACKAGE)) {
                    return true
                }
            }
            val webUri = Uri.parse(
                "https://uri.amap.com/navigation?to=$lon,$lat,$encodedName" +
                    "&mode=${mode.webMode}&callnative=1",
            )
            return launch(context, webUri)
        }

        val keywordUri = Uri.parse(
            "androidamap://keywordNavi?sourceApplication=${Uri.encode(SOURCE_APP)}" +
                "&keyword=$encodedName&style=2",
        )
        if (launch(context, keywordUri, AMAP_PACKAGE)) {
            return true
        }
        val searchUri = Uri.parse("https://uri.amap.com/search?keyword=$encodedName&callnative=1")
        return launch(context, searchUri)
    }

    private fun buildCoordinateCandidates(
        lat: Double,
        lon: Double,
        encodedName: String,
        mode: NavigationTravelMode,
    ): List<Uri> {
        val source = Uri.encode(SOURCE_APP)
        val routePlanUri = buildRoutePlanUri(source, lat, lon, encodedName, mode)
        return when (mode) {
            NavigationTravelMode.DRIVE -> listOf(
                Uri.parse(
                    "androidamap://navi?sourceApplication=$source" +
                        "&poiname=$encodedName&lat=$lat&lon=$lon&dev=0&style=2",
                ),
                routePlanUri,
            )
            NavigationTravelMode.WALK,
            NavigationTravelMode.RIDE,
            -> {
                val featureUri = buildOpenFeatureUri(source, lat, lon, mode)
                if (featureUri != null) listOf(featureUri, routePlanUri) else listOf(routePlanUri)
            }
            NavigationTravelMode.BUS -> listOf(routePlanUri)
        }
    }

    private fun buildRoutePlanUri(
        source: String,
        lat: Double,
        lon: Double,
        encodedName: String,
        mode: NavigationTravelMode,
    ): Uri {
        val rideTypeParam = mode.rideType?.let { "&rideType=$it" }.orEmpty()
        return Uri.parse(
            "amapuri://route/plan/?sourceApplication=$source" +
                "&dlat=$lat&dlon=$lon&dname=$encodedName&dev=0&t=${mode.routeType}$rideTypeParam",
        )
    }

    private fun buildOpenFeatureUri(
        source: String,
        lat: Double,
        lon: Double,
        mode: NavigationTravelMode,
    ): Uri? {
        val featureName = mode.openFeatureName ?: return null
        val rideTypeParam = mode.rideType?.let { "&rideType=$it" }.orEmpty()
        return Uri.parse(
            "amapuri://openFeature?featureName=$featureName$rideTypeParam" +
                "&sourceApplication=$source&lat=$lat&lon=$lon&dev=0",
        )
    }

    private fun launch(context: Context, uri: Uri, packageName: String? = null): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                packageName?.let { setPackage(it) }
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
