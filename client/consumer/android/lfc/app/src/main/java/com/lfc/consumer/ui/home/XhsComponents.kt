package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

private val coverGradients = listOf(
    listOf(Color(0xFFFF9A9E), Color(0xFFFAD0C4)),
    listOf(Color(0xFFA18CD1), Color(0xFFFBC2EB)),
    listOf(Color(0xFF84FAB0), Color(0xFF8FD3F4)),
    listOf(Color(0xFFFFE259), Color(0xFFFFA751)),
    listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
    listOf(Color(0xFFF093FB), Color(0xFFF5576C)),
)

fun coverGradientForId(id: Int): Brush {
    val colors = coverGradients[id % coverGradients.size]
    return Brush.linearGradient(colors)
}

fun cardHeightForId(id: Int) = (140 + (id % 4) * 36).dp

@Composable
fun XhsFeedCard(
    title: String,
    subtitle: String,
    authorLabel: String,
    id: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeightForId(id))
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(coverGradientForId(id)),
                contentAlignment = Alignment.BottomStart,
            ) {
                Text(
                    text = title,
                    modifier = Modifier.padding(12.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = XhsTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(coverGradientForId(id.hashCode())),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = authorLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = XhsTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun XhsBottomBar(
    selectedTab: Int,
    unreadCount: Int = 0,
    onTabSelected: (Int) -> Unit,
    onPublishClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            XhsNavItem(
                label = "首页",
                icon = Icons.Default.Home,
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
            )
            XhsNavItem(
                label = "活动",
                icon = Icons.Default.Event,
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
            )
            Box(
                modifier = Modifier
                    .offset(y = (-8).dp)
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(XhsRed)
                    .clickable(onClick = onPublishClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "发布",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
            XhsNavItem(
                label = "消息",
                icon = Icons.Default.Chat,
                selected = selectedTab == 2,
                badgeCount = unreadCount,
                onClick = { onTabSelected(2) },
            )
            XhsNavItem(
                label = "我的",
                icon = Icons.Default.Person,
                selected = selectedTab == 3,
                onClick = { onTabSelected(3) },
            )
        }
    }
}

@Composable
private fun XhsNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    badgeCount: Int = 0,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .defaultMinSize(minWidth = 52.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) XhsRed else XhsTextSecondary,
                modifier = Modifier.size(22.dp),
            )
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .offset(x = 4.dp, y = (-2).dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(XhsRed),
                )
            }
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (selected) XhsRed else XhsTextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
fun XhsProfileAvatar(
    label: String,
    modifier: Modifier = Modifier,
    size: Int = 72,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(coverGradientForId(label.hashCode())),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.take(1).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size / 2.5).sp,
        )
    }
}

@Composable
fun XhsStatItem(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = XhsTextPrimary,
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = XhsTextSecondary,
        )
    }
}
