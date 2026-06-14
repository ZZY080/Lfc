package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.data.model.hasOnSaleProduct
import com.lfc.consumer.data.model.promotionBadge
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.location.hasValidCoordinate
import com.lfc.consumer.data.model.displayName
import com.lfc.consumer.ui.theme.XhsDivider
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

private fun gradientIndexForId(id: Int, size: Int): Int = Math.floorMod(id, size)

fun coverGradientForId(id: Int): Brush {
    val colors = coverGradients[gradientIndexForId(id, coverGradients.size)]
    return Brush.linearGradient(colors)
}

fun cardHeightForId(id: Int) = (160 + gradientIndexForId(id, 4) * 28).dp

@Composable
fun XhsFeedCard(
    post: PostDto,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    XhsProfileFeedCard(
        post = post,
        authorLabel = post.author?.displayName() ?: "同学${post.authorId}",
        avatarUrl = post.author?.avatarUrl,
        modifier = modifier.clickable(onClick = onClick),
    )
}

@Composable
fun XhsProfileFeedCard(
    post: PostDto,
    authorLabel: String,
    avatarUrl: String? = null,
    isPinned: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val coverUrl = post.images?.firstOrNull()
    val aspectRatio = 0.68f + gradientIndexForId(post.id, 4) * 0.06f

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 0.dp,
    ) {
        Column {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)),
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = post.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspectRatio),
                    )
                } else {
                    ProfileTextNoteCover(
                        content = post.content.ifBlank { post.title },
                        id = post.id,
                    )
                }

                if (isPinned) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(XhsRed)
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) {
                        Text("置顶", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                post.promotionBadge()?.let { badge ->
                    XhsPromotionBadge(
                        label = badge,
                        modifier = Modifier
                            .align(if (isPinned) Alignment.TopEnd else Alignment.TopStart)
                            .padding(6.dp),
                    )
                }

                if (hasValidCoordinate(post.latitude, post.longitude) ||
                    !post.location.isNullOrBlank()
                ) {
                    XhsFeedLocationOverlay(
                        address = post.location,
                        latitude = post.latitude,
                        longitude = post.longitude,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 3.dp, vertical = 3.dp),
                    )
                }
            }

            Text(
                text = post.title,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                color = XhsTextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val footerAuthor = post.author?.displayName() ?: authorLabel
                val footerAvatar = post.author?.avatarUrl ?: avatarUrl
                XhsProfileAvatar(
                    label = footerAuthor,
                    size = 18,
                    avatarUrl = footerAvatar,
                )
                Text(
                    text = footerAuthor,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 5.dp),
                    fontSize = 10.sp,
                    color = XhsTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (post.isLiked) XhsRed else XhsTextSecondary.copy(alpha = 0.75f),
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = formatLikeCount(post.likeCount),
                    modifier = Modifier.padding(start = 2.dp),
                    fontSize = 10.sp,
                    color = if (post.isLiked) XhsRed else XhsTextSecondary,
                )
            }
        }
    }
}

@Composable
fun XhsProfileActivityCard(
    activity: ActivityDto,
    authorLabel: String,
    avatarUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    val coverUrl = activity.images?.firstOrNull()
    val aspectRatio = 0.68f + gradientIndexForId(activity.id, 4) * 0.06f
    val participantCount = activity.participants?.size ?: 0

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 0.dp,
    ) {
        Column {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)),
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = activity.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspectRatio),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspectRatio)
                            .background(coverGradientForId(activity.id)),
                    )
                }

                Text(
                    text = profileActivityStatusLabel(activity.status),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.42f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    color = Color.White,
                    fontSize = 9.sp,
                )

                activity.promotionBadge()?.let { badge ->
                    XhsPromotionBadge(
                        label = badge,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                    )
                }

                if (participantCount > 0) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.42f))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Event,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp),
                        )
                        Text(
                            text = formatLikeCount(participantCount),
                            color = Color.White,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(start = 2.dp),
                        )
                    }
                }
            }

            Text(
                text = activity.title,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                color = XhsTextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp,
            )
            XhsDistanceLabel(
                targetLatitude = activity.latitude,
                targetLongitude = activity.longitude,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val footerAuthor = activity.author?.displayName() ?: authorLabel
                val footerAvatar = activity.author?.avatarUrl ?: avatarUrl
                XhsProfileAvatar(
                    label = footerAuthor,
                    size = 16,
                    avatarUrl = footerAvatar,
                )
                Text(
                    text = footerAuthor,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                    fontSize = 9.sp,
                    color = XhsTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    if (activity.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (activity.isLiked) XhsRed else XhsTextSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.size(11.dp),
                )
                if (activity.likeCount > 0) {
                    Text(
                        text = formatLikeCount(activity.likeCount),
                        modifier = Modifier.padding(start = 2.dp),
                        fontSize = 9.sp,
                        color = if (activity.isLiked) XhsRed else XhsTextSecondary,
                    )
                }
            }
        }
    }
}

fun profileActivityStatusLabel(status: String): String = when (status.uppercase()) {
    "PENDING" -> "待审核"
    "APPROVED" -> "进行中"
    "REJECTED" -> "已拒绝"
    "OFF_SHELF" -> "已下架"
    else -> status
}

@Composable
private fun ProfileTextNoteCover(
    content: String,
    id: Int,
) {
    val pastelColors = listOf(
        Color(0xFFF8F3E8),
        Color(0xFFEDF5F0),
        Color(0xFFF3EFF8),
        Color(0xFFF5F0EB),
    )
    val bg = pastelColors[gradientIndexForId(id, pastelColors.size)]

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f + gradientIndexForId(id, 3) * 0.06f)
            .background(bg)
            .padding(12.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Text(
            text = content,
            color = XhsTextPrimary.copy(alpha = 0.88f),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun TextNoteCover(
    content: String,
    id: Int,
    cornerRadius: androidx.compose.ui.unit.Dp = 12.dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius))
            .background(coverGradientForId(id))
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .padding(12.dp),
        ) {
            Text(
                text = content.ifBlank { "纯文字笔记" },
                color = XhsTextPrimary,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

fun formatLikeCount(count: Int): String = when {
    count <= 0 -> "0"
    count < 10000 -> count.toString()
    else -> String.format("%.1fw", count / 10000f)
}

@Composable
private fun ProfileFeedCoverBadge(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .padding(6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.42f))
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun ProfileFeedViewBadge(
    viewCount: Int,
    modifier: Modifier = Modifier,
) {
    if (viewCount <= 0) return
    ProfileFeedCoverBadge(modifier = modifier) {
        Icon(
            Icons.Default.Visibility,
            contentDescription = "浏览量",
            tint = Color.White,
            modifier = Modifier.size(10.dp),
        )
        Text(
            text = formatLikeCount(viewCount),
            color = Color.White,
            fontSize = 9.sp,
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}

@Composable
fun XhsBottomBar(
    selectedTab: Int,
    unreadCount: Int = 0,
    onTabSelected: (Int) -> Unit,
    onPublishClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selectedTab == 3) {
                    Color.White.copy(alpha = 0.94f)
                } else {
                    Color.White
                },
            ),
    ) {
        HorizontalDivider(color = XhsDivider, thickness = 0.5.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                XhsNavItem(
                    label = "首页",
                    icon = Icons.Default.Home,
                    selected = selectedTab == 0,
                    onClick = { onTabSelected(0) },
                    modifier = Modifier.weight(1f),
                )
                XhsNavItem(
                    label = "市集",
                    icon = Icons.Default.Event,
                    selected = selectedTab == 1,
                    onClick = { onTabSelected(1) },
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    XhsPublishTabButton(onClick = onPublishClick)
                }
                XhsNavItem(
                    label = "消息",
                    icon = Icons.Default.Chat,
                    selected = selectedTab == 2,
                    badgeCount = unreadCount,
                    onClick = { onTabSelected(2) },
                    modifier = Modifier.weight(1f),
                )
                XhsNavItem(
                    label = "我",
                    icon = Icons.Default.Person,
                    selected = selectedTab == 3,
                    onClick = { onTabSelected(3) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun XhsPublishTabButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .offset(y = (-10).dp)
            .size(48.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp), clip = false),
        shape = RoundedCornerShape(16.dp),
        color = XhsRed,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.Add,
                contentDescription = "发布",
                tint = Color.White,
                modifier = Modifier.size(28.dp),
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
    modifier: Modifier = Modifier,
) {
    val iconTint = if (selected) XhsTextPrimary else Color(0xFFB8B8B8)
    val labelColor = if (selected) XhsTextPrimary else Color(0xFFB8B8B8)

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(if (selected) 24.dp else 22.dp),
            )
            if (badgeCount > 0) {
                Surface(
                    modifier = Modifier.offset(x = 6.dp, y = (-4).dp),
                    shape = CircleShape,
                    color = XhsRed,
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 10.sp,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = if (selected) 11.sp else 10.sp,
            color = labelColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
fun XhsProfileAvatar(
    label: String,
    modifier: Modifier = Modifier,
    size: Int = 72,
    avatarUrl: String? = null,
) {
    if (!avatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size.dp)
                .clip(CircleShape),
        )
        return
    }

    val initial = resolveAvatarInitial(label, size)
    val fontSize = avatarInitialFontSize(size, initial.length)

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(coverGradientForId(label.hashCode())),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            lineHeight = fontSize,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
            modifier = Modifier.padding(horizontal = (size * 0.08f).dp),
        )
    }
}

private const val DEFAULT_AVATAR_INITIAL = "莲"

private fun resolveAvatarInitial(label: String, @Suppress("UNUSED_PARAMETER") avatarSizeDp: Int): String {
    val trimmed = label.trim()
    if (trimmed.isEmpty()) return DEFAULT_AVATAR_INITIAL
    if (trimmed.startsWith("莲峰校园") || trimmed.startsWith("同学")) {
        return DEFAULT_AVATAR_INITIAL
    }
    return trimmed.first().toString()
}

private fun avatarInitialFontSize(avatarSizeDp: Int, @Suppress("UNUSED_PARAMETER") initialLength: Int): TextUnit {
    return when {
        avatarSizeDp <= 16 -> 8.sp
        avatarSizeDp <= 20 -> 9.sp
        avatarSizeDp <= 28 -> 11.sp
        avatarSizeDp <= 40 -> 14.sp
        else -> (avatarSizeDp * 0.38f).sp
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
