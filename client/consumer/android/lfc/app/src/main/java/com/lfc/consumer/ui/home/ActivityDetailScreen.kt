package com.lfc.consumer.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.data.model.ActivityParticipantDto
import com.lfc.consumer.data.model.displayName
import com.lfc.consumer.data.model.isPaidActivity
import com.lfc.consumer.ui.theme.XhsBackground
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsRedContainer
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

private val sheetRadius = 18.dp
private val heroAspect = 0.82f

@Composable
fun ActivityDetailScreen(
    activity: ActivityDto?,
    isLoading: Boolean,
    isJoining: Boolean,
    currentUserId: Int? = null,
    isAuthorFollowing: Boolean = false,
    onBack: () -> Unit,
    onJoin: () -> Unit,
    onAuthorClick: (Int) -> Unit = {},
    onFollowToggle: () -> Unit = {},
    onLike: () -> Unit = {},
    onFavorite: () -> Unit = {},
    isSocialSubmitting: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(XhsBackground),
    ) {
        when {
            isLoading -> XhsDetailLoading(Modifier.fillMaxSize())
            activity == null -> XhsDetailEmpty("活动不存在或已删除", Modifier.fillMaxSize())
            else -> {
                val authorLabel = activity.author?.displayName() ?: "同学${activity.authorId}"
                val participants = activity.participants.orEmpty()
                val images = activity.images.orEmpty()
                val isSelf = currentUserId != null && currentUserId == activity.authorId
                val isPaid = activity.isPaidActivity()

                Column(modifier = Modifier.fillMaxSize()) {
                    XhsDetailAuthorHeader(
                        authorLabel = authorLabel,
                        authorId = activity.authorId,
                        authorAvatarUrl = activity.author?.avatarUrl,
                        onBack = onBack,
                        onAuthorClick = onAuthorClick,
                    ) {
                        if (!isSelf) {
                            XhsDetailFollowButton(
                                isFollowing = isAuthorFollowing,
                                onClick = onFollowToggle,
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        ActivityDetailHero(
                            activity = activity,
                            images = images,
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = (-sheetRadius))
                                .clip(RoundedCornerShape(topStart = sheetRadius, topEnd = sheetRadius))
                                .background(Color.White)
                                .padding(top = 20.dp, bottom = 24.dp),
                        ) {
                            ActivityDetailTitleSection(
                                title = activity.title,
                                status = activity.status,
                                isPaid = isPaid,
                                fee = activity.fee,
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            ActivityDetailInfoCard(
                                location = activity.location,
                                timeRange = formatActivityTime(activity.startTime, activity.endTime),
                                isPaid = isPaid,
                                fee = activity.fee,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )

                            if (participants.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                ActivityParticipantsSection(
                                    participants = participants,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = Color(0xFFF3F3F3),
                            )
                            Spacer(modifier = Modifier.height(18.dp))

                            ActivityDetailDescriptionSection(
                                description = activity.description,
                                createdAt = activity.createdAt,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )

                            Spacer(modifier = Modifier.height(88.dp))
                        }
                    }

                    ActivityDetailBottomBar(
                        participantCount = participants.size,
                        maxParticipants = activity.maxParticipants,
                        fee = activity.fee,
                        isJoined = activity.isJoined,
                        isSelf = isSelf,
                        isJoining = isJoining,
                        likeCount = activity.likeCount,
                        favoriteCount = activity.favoriteCount,
                        isLiked = activity.isLiked,
                        isFavorited = activity.isFavorited,
                        isSocialSubmitting = isSocialSubmitting,
                        onLike = onLike,
                        onFavorite = onFavorite,
                        onJoin = onJoin,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityDetailHero(
    activity: ActivityDto,
    images: List<String>,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
    ) {
        if (images.isNotEmpty()) {
            XhsDetailImageCarousel(
                images = images,
                contentDescription = activity.title,
                modifier = Modifier.fillMaxSize(),
                aspectRatio = heroAspect,
                indicatorStyle = CarouselIndicatorStyle.Counter,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(coverGradientForId(activity.id)),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.12f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f),
                        ),
                    ),
                ),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActivityDetailTitleSection(
    title: String,
    status: String,
    isPaid: Boolean,
    fee: String?,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActivityStatusChip(status = status)
            if (isPaid) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = XhsRedContainer,
                ) {
                    Text(
                        text = formatPriceYuan(fee),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        color = XhsRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFE8F8EF),
                ) {
                    Text(
                        text = "免费参加",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        color = Color(0xFF1B9B55),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = XhsTextPrimary,
            lineHeight = 30.sp,
        )
    }
}

@Composable
private fun ActivityStatusChip(status: String) {
    val (label, bg, fg) = when (status.uppercase()) {
        "PENDING" -> Triple("待审核", Color(0xFFFFF3E0), Color(0xFFE65100))
        "APPROVED" -> Triple("进行中", Color(0xFFE8F4FD), Color(0xFF1565C0))
        "REJECTED" -> Triple("已拒绝", Color(0xFFF5F5F5), XhsTextSecondary)
        else -> Triple(status, Color(0xFFF5F5F5), XhsTextSecondary)
    }
    Surface(shape = RoundedCornerShape(20.dp), color = bg) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            color = fg,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun ActivityDetailInfoCard(
    location: String,
    timeRange: String,
    isPaid: Boolean,
    fee: String?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFAFAFA),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ActivityInfoRow(
                icon = Icons.Default.LocationOn,
                iconTint = Color(0xFFFF6B35),
                iconBg = Color(0xFFFFEDE6),
                label = "活动地点",
                value = location,
            )
            Spacer(modifier = Modifier.height(14.dp))
            ActivityInfoRow(
                icon = Icons.Default.Schedule,
                iconTint = Color(0xFF4A90E2),
                iconBg = Color(0xFFEAF3FC),
                label = "活动时间",
                value = timeRange,
            )
            if (isPaid) {
                Spacer(modifier = Modifier.height(14.dp))
                ActivityInfoRow(
                    icon = Icons.Default.Event,
                    iconTint = XhsRed,
                    iconBg = XhsRedContainer,
                    label = "报名费用",
                    value = formatPriceYuan(fee),
                )
            }
        }
    }
}

@Composable
private fun ActivityInfoRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    label: String,
    value: String,
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, color = XhsTextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = XhsTextPrimary,
                lineHeight = 22.sp,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActivityParticipantsSection(
    participants: List<ActivityParticipantDto>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "已报名同学",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = XhsTextPrimary,
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = XhsRedContainer,
            ) {
                Text(
                    text = "${participants.size} 人",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = XhsRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(end = 8.dp),
        ) {
            items(participants, key = { it.id }) { participant ->
                ActivityParticipantChip(participant = participant)
            }
        }
    }
}

@Composable
private fun ActivityParticipantChip(participant: ActivityParticipantDto) {
    val label = participant.user?.displayName()
        ?: participant.user?.nickname?.takeIf { it.isNotBlank() }
        ?: "同学${participant.userId}"
    val avatarUrl = participant.user?.avatarUrl

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, Color.White, CircleShape),
        ) {
            XhsProfileAvatar(
                label = label,
                size = 52,
                avatarUrl = avatarUrl,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = XhsTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ActivityDetailDescriptionSection(
    description: String,
    createdAt: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "活动详情",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = XhsTextPrimary,
        )
        if (description.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = description,
                fontSize = 15.sp,
                color = Color(0xFF444444),
                lineHeight = 24.sp,
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "发布于 ${formatXhsTime(createdAt)}",
            fontSize = 12.sp,
            color = XhsTextSecondary,
        )
    }
}

@Composable
private fun ActivityDetailBottomBar(
    participantCount: Int,
    maxParticipants: Int,
    fee: String?,
    isJoined: Boolean,
    isSelf: Boolean,
    isJoining: Boolean,
    likeCount: Int,
    favoriteCount: Int,
    isLiked: Boolean,
    isFavorited: Boolean,
    isSocialSubmitting: Boolean,
    onLike: () -> Unit,
    onFavorite: () -> Unit,
    onJoin: () -> Unit,
) {
    val isPaid = fee?.toDoubleOrNull()?.let { it > 0 } == true
    val joinLabel = when {
        isSelf -> "我的活动"
        isJoined -> "已报名"
        isPaid -> "支付 ${formatPriceYuan(fee)}"
        else -> "免费报名"
    }
    val buttonColors = when {
        isSelf || isJoined -> ButtonDefaults.buttonColors(
            containerColor = Color(0xFFEFEFEF),
            disabledContainerColor = Color(0xFFEFEFEF),
            contentColor = XhsTextSecondary,
            disabledContentColor = XhsTextSecondary,
        )
        else -> ButtonDefaults.buttonColors(containerColor = XhsRed)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 12.dp,
        color = Color.White,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$participantCount 人已报名",
                    fontWeight = FontWeight.Bold,
                    color = XhsRed,
                    fontSize = 16.sp,
                )
                Text(
                    text = buildString {
                        if (maxParticipants > 0) append("限额 $maxParticipants 人")
                        if (maxParticipants > 0) append(" · ")
                        append(if (isPaid) formatPriceYuan(fee) else "免费")
                    },
                    fontSize = 12.sp,
                    color = XhsTextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            XhsDetailSocialChip(
                icon = {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "点赞",
                        tint = if (isLiked) XhsRed else XhsTextPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                },
                count = likeCount,
                onClick = onLike,
                enabled = !isSocialSubmitting && !isJoining,
            )
            XhsDetailSocialChip(
                icon = {
                    Icon(
                        if (isFavorited) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "收藏",
                        tint = if (isFavorited) Color(0xFFFFB800) else XhsTextPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                },
                count = favoriteCount,
                onClick = onFavorite,
                enabled = !isSocialSubmitting && !isJoining,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onJoin,
                enabled = !isJoining && !isSelf && !isJoined,
                colors = buttonColors,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.height(44.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                if (isJoining) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(joinLabel, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

private fun formatActivityTime(start: String, end: String): String {
    val startShort = start.replace("T", " ").take(16)
    val endShort = end.replace("T", " ").take(16)
    return "$startShort ~ $endShort"
}
