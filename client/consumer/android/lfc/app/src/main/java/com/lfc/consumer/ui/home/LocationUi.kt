package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.location.ActivityLocation
import com.lfc.consumer.location.AmapLocationHelper
import com.lfc.consumer.location.formatDistanceCompact
import com.lfc.consumer.location.formatDistanceLabel
import com.lfc.consumer.location.formatFeedLocationChipAddress
import com.lfc.consumer.location.hasValidCoordinate
import com.lfc.consumer.ui.theme.XhsTextSecondary

val LocalUserLocation = compositionLocalOf<ActivityLocation?> { null }

@Composable
fun XhsDistanceLabel(
    targetLatitude: Double?,
    targetLongitude: Double?,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 10.sp,
    iconSize: Dp = 11.dp,
    tint: Color = XhsTextSecondary,
) {
    val userLocation = LocalUserLocation.current
    val label = formatDistanceLabel(
        userLat = userLocation?.latitude,
        userLng = userLocation?.longitude,
        targetLat = targetLatitude,
        targetLng = targetLongitude,
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.NearMe,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
        Text(
            text = label,
            fontSize = fontSize,
            color = tint,
            modifier = Modifier.padding(start = 2.dp),
            maxLines = 1,
        )
    }
}

/** 小红书风格：封面底部通栏胶囊 地址 | 距离 */
@Composable
fun XhsFeedLocationOverlay(
    address: String?,
    latitude: Double?,
    longitude: Double?,
    modifier: Modifier = Modifier,
) {
    if (!hasValidCoordinate(latitude, longitude) && address.isNullOrBlank()) return

    var resolvedAddress by remember(address, latitude, longitude) {
        mutableStateOf(address)
    }
    LaunchedEffect(address, latitude, longitude) {
        if (!address.isNullOrBlank()) {
            resolvedAddress = address
            return@LaunchedEffect
        }
        if (!hasValidCoordinate(latitude, longitude)) return@LaunchedEffect
        AmapLocationHelper.reverseGeocode(latitude!!, longitude!!)
            .onSuccess { resolvedAddress = it }
    }

    val userLocation = LocalUserLocation.current
    val addressText = formatFeedLocationChipAddress(resolvedAddress)
    val distanceText = formatDistanceCompact(
        userLat = userLocation?.latitude,
        userLng = userLocation?.longitude,
        targetLat = latitude,
        targetLng = longitude,
    )
    if (addressText.isBlank() && distanceText.isNullOrBlank()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 4.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(8.dp),
        )
        if (addressText.isNotBlank()) {
            Text(
                text = addressText,
                color = Color.White,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 1.dp),
            )
        }
        if (addressText.isNotBlank() && !distanceText.isNullOrBlank()) {
            Text(
                text = " | ",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 8.sp,
            )
        }
        if (!distanceText.isNullOrBlank()) {
            Text(
                text = distanceText,
                color = Color.White,
                fontSize = 8.sp,
                maxLines = 1,
            )
        }
    }
}
