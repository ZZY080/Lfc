package com.lfc.consumer.location

import android.content.Context
import android.content.Intent
import android.net.Uri

object AmapNavigationHelper {
    private const val AMAP_PACKAGE = "com.autonavi.minimap"
    private const val SOURCE_APP = "莲峰校园"

    fun openNavigation(
        context: Context,
        name: String,
        latitude: Double?,
        longitude: Double?,
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
            val nativeUri = Uri.parse(
                "androidamap://navi?sourceApplication=${Uri.encode(SOURCE_APP)}" +
                    "&poiname=$encodedName&lat=$lat&lon=$lon&dev=0&style=2",
            )
            if (launch(context, nativeUri, AMAP_PACKAGE)) {
                return true
            }
            val routeUri = Uri.parse(
                "androidamap://route?sourceApplication=${Uri.encode(SOURCE_APP)}" +
                    "&dlat=$lat&dlon=$lon&dname=$encodedName&dev=0&t=0",
            )
            if (launch(context, routeUri, AMAP_PACKAGE)) {
                return true
            }
            val webUri = Uri.parse(
                "https://uri.amap.com/navigation?to=$lon,$lat,$encodedName&mode=car&callnative=1",
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
