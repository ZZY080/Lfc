package com.lfc.consumer.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val menuPanelWidth = 300.dp
private const val menuOpenDurationMs = 340
private const val menuCloseDurationMs = 280
private const val menuScrimAlpha = 0.52f
private val menuDividerColor = Color(0xFFF0F0F0)

@Composable
fun ProfileSideMenu(
    visible: Boolean,
    profile: XhsProfileData?,
    unreadCount: Int = 0,
    onDismiss: () -> Unit,
    onScan: () -> Unit,
    onShowMyQr: () -> Unit,
    onSettings: () -> Unit,
    onOrders: () -> Unit,
    onBindAlipay: () -> Unit,
    onEditProfile: () -> Unit,
    onShare: () -> Unit,
    onSearch: () -> Unit,
    onLogout: () -> Unit,
    onSelectProfileTab: (Int) -> Unit,
    onGoToMessages: () -> Unit,
    onFullyHidden: () -> Unit = {},
) {
    val density = LocalDensity.current
    val panelWidthPx = with(density) { menuPanelWidth.toPx() }
    val scrimAlpha = remember { Animatable(0f) }
    val panelOffsetX = remember(panelWidthPx) { Animatable(-panelWidthPx) }
    var hasOpened by remember { mutableStateOf(false) }

    LaunchedEffect(visible, panelWidthPx) {
        if (visible) {
            hasOpened = true
            coroutineScope {
                launch {
                    scrimAlpha.animateTo(
                        targetValue = menuScrimAlpha,
                        animationSpec = tween(menuOpenDurationMs, easing = FastOutSlowInEasing),
                    )
                }
                launch {
                    panelOffsetX.snapTo(-panelWidthPx)
                    panelOffsetX.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(menuOpenDurationMs, easing = FastOutSlowInEasing),
                    )
                }
            }
        } else if (hasOpened) {
            coroutineScope {
                launch {
                    scrimAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(menuCloseDurationMs, easing = FastOutSlowInEasing),
                    )
                }
                launch {
                    panelOffsetX.animateTo(
                        targetValue = -panelWidthPx,
                        animationSpec = tween(menuCloseDurationMs, easing = FastOutSlowInEasing),
                    )
                }
            }
            hasOpened = false
            onFullyHidden()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha.value))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxHeight()
                .width(menuPanelWidth)
                .graphicsLayer { translationX = panelOffsetX.value }
                .background(Color.White)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            ProfileSideMenuHeader(profile = profile)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                ProfileSideMenuRow(
                    icon = Icons.Default.Home,
                    title = "我的笔记",
                    subtitle = profile?.let { "${it.postCount} 篇" },
                    onClick = {
                        onSelectProfileTab(0)
                        onDismiss()
                    },
                )
                ProfileSideMenuDivider()
                ProfileSideMenuRow(
                    icon = Icons.Default.Event,
                    title = "我的活动",
                    subtitle = profile?.let { "${it.activities.size} 个" },
                    onClick = {
                        onSelectProfileTab(1)
                        onDismiss()
                    },
                )
                ProfileSideMenuDivider()
                ProfileSideMenuRow(
                    icon = Icons.Default.FavoriteBorder,
                    title = "我的收藏",
                    onClick = {
                        onSelectProfileTab(3)
                        onDismiss()
                    },
                )
                ProfileSideMenuDivider()
                ProfileSideMenuRow(
                    icon = Icons.Default.Favorite,
                    title = "我的赞",
                    onClick = {
                        onSelectProfileTab(4)
                        onDismiss()
                    },
                )

                ProfileSideMenuSectionGap()

                ProfileSideMenuRow(
                    icon = Icons.AutoMirrored.Filled.Message,
                    title = "我的消息",
                    badge = if (unreadCount > 0) unreadCount.coerceAtMost(99).toString() else null,
                    onClick = {
                        onGoToMessages()
                        onDismiss()
                    },
                )

                ProfileSideMenuSectionGap()

                ProfileSideMenuRow(
                    icon = Icons.Default.ReceiptLong,
                    title = "我的订单",
                    onClick = {
                        onOrders()
                        onDismiss()
                    },
                )
                ProfileSideMenuDivider()

                ProfileSideMenuRow(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "收款账号",
                    onClick = {
                        onBindAlipay()
                        onDismiss()
                    },
                )
                ProfileSideMenuDivider()

                ProfileSideMenuRow(
                    icon = Icons.Outlined.Edit,
                    title = "编辑资料",
                    onClick = {
                        onEditProfile()
                        onDismiss()
                    },
                )
                ProfileSideMenuDivider()
                ProfileSideMenuRow(
                    icon = Icons.Default.Search,
                    title = "搜索笔记",
                    onClick = {
                        onSearch()
                        onDismiss()
                    },
                )
                ProfileSideMenuDivider()
                ProfileSideMenuRow(
                    icon = Icons.Default.QrCode2,
                    title = "我的二维码",
                    onClick = {
                        onShowMyQr()
                        onDismiss()
                    },
                )
                ProfileSideMenuDivider()
                ProfileSideMenuRow(
                    icon = Icons.Default.Share,
                    title = "分享主页",
                    onClick = {
                        onShare()
                        onDismiss()
                    },
                )
            }

            ProfileSideMenuBottomBar(
                onScan = {
                    onScan()
                    onDismiss()
                },
                onSettings = {
                    onSettings()
                    onDismiss()
                },
                onLogout = {
                    onLogout()
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun ProfileSideMenuHeader(profile: XhsProfileData?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        if (profile != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                XhsProfileAvatar(
                    label = profile.displayName,
                    size = 52,
                    avatarUrl = profile.avatarUrl,
                )
                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text(
                        text = profile.displayName,
                        color = XhsTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "莲峰号：${profile.lfcNo}",
                        color = XhsTextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        } else {
            Text(
                text = "我的主页",
                color = XhsTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }
    }
    HorizontalDivider(color = menuDividerColor)
}

@Composable
private fun ProfileSideMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    badge: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = XhsTextPrimary,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = title,
            fontSize = 15.sp,
            color = XhsTextPrimary,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
        )
        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(XhsRed)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = badge,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        } else if (subtitle != null) {
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = XhsTextSecondary,
            )
        }
    }
}

@Composable
private fun ProfileSideMenuDivider() {
    HorizontalDivider(
        color = menuDividerColor,
        modifier = Modifier.padding(start = 56.dp),
    )
}

@Composable
private fun ProfileSideMenuSectionGap() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(Color.White),
    )
}

@Composable
private fun ProfileSideMenuBottomBar(
    onScan: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(color = menuDividerColor)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ProfileSideMenuBottomAction(
                icon = Icons.Default.QrCodeScanner,
                label = "扫一扫",
                onClick = onScan,
            )
            ProfileSideMenuBottomAction(
                icon = Icons.Outlined.Settings,
                label = "设置",
                onClick = onSettings,
            )
            ProfileSideMenuBottomAction(
                icon = Icons.AutoMirrored.Filled.Logout,
                label = "退出",
                onClick = onLogout,
            )
        }
    }
}

@Composable
private fun ProfileSideMenuBottomAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = XhsTextPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = XhsTextSecondary,
        )
    }
}
