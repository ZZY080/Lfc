package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.data.model.ActivityParticipantDto
import com.lfc.consumer.data.model.displayName
import com.lfc.consumer.data.model.isPaidActivity
import com.lfc.consumer.data.model.promotionBadge
import com.lfc.consumer.location.AmapNavigationHelper
import com.lfc.consumer.ui.theme.XhsDivider
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsRedContainer
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

@Composable
fun ActivityDetailScreen(
    activity: ActivityDto?,
    isLoading: Boolean,
    isJoining: Boolean,
    isPaymentProcessing: Boolean = false,
    currentUserId: Int? = null,
    isAuthorFollowing: Boolean = false,
    onBack: () -> Unit,
    onJoin: () -> Unit,
    onAuthorClick: (Int) -> Unit = {},
    onFollowToggle: () -> Unit = {},
    onLike: () -> Unit = {},
    onFavorite: () -> Unit = {},
    isSocialSubmitting: Boolean = false,
    isPromotionSubmitting: Boolean = false,
    onPromote: (() -> Unit)? = null,
    activityPromoteActionLabel: String = "推广活动",
    activityPromoteActiveHint: String = "推广期间将在活动 Tab 优先展示",
    activityPromotePriceHint: String? = null,
    activityPromoteBidHint: String? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onOffShelf: (() -> Unit)? = null,
    onOnShelf: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        when {
            isLoading -> XhsDetailLoading(Modifier.fillMaxSize(), style = DetailSkeletonStyle.Activity)
            activity == null -> XhsDetailEmpty("活动不存在或已删除", Modifier.fillMaxSize())
            else -> {
                val context = LocalContext.current
                val authorLabel = activity.author?.displayName() ?: "同学${activity.authorId}"
                val participants = activity.participants.orEmpty()
                val images = activity.images.orEmpty()
                val isSelf = currentUserId != null && currentUserId == activity.authorId
                val isPaid = activity.isPaidActivity()
                var showNavigationSheet by remember { mutableStateOf(false) }

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
                        } else if (
                            onEdit != null &&
                            onDelete != null &&
                            onOffShelf != null &&
                            onOnShelf != null
                        ) {
                            val status = activity.status.uppercase()
                            OwnerContentManageButton(
                                showShelfActions = status == "APPROVED" || status == "OFF_SHELF",
                                isOffShelf = status == "OFF_SHELF",
                                contentLabel = "活动",
                                onEdit = onEdit,
                                onDelete = onDelete,
                                onOffShelf = onOffShelf,
                                onOnShelf = onOnShelf,
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
                            status = activity.status,
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))

                            ActivityDetailTitleSection(
                                title = activity.title,
                                status = activity.status,
                                isPaid = isPaid,
                                fee = activity.fee,
                                promotionBadge = activity.promotion?.badge?.takeIf {
                                    activity.promotion?.isActive == true
                                },
                            )

                            if (isSelf && onPromote != null) {
                                Spacer(modifier = Modifier.height(14.dp))
                                PromotionOwnerActionCard(
                                    promotion = activity.promotion,
                                    actionLabel = activityPromoteActionLabel,
                                    activeHint = activityPromoteActiveHint,
                                    cooldownHint = activity.promotion?.nextAvailableAt?.let {
                                        "冷却中，下次可推广：$it"
                                    },
                                    priceHint = activityPromotePriceHint,
                                    bidHint = activityPromoteBidHint,
                                    isSubmitting = isPromotionSubmitting,
                                    onAction = onPromote,
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            ActivityDetailInfoCard(
                                startTime = activity.startTime,
                                endTime = activity.endTime,
                                location = activity.location,
                                latitude = activity.latitude,
                                longitude = activity.longitude,
                                participantCount = participants.size,
                                maxParticipants = activity.maxParticipants,
                                isPaid = isPaid,
                                fee = activity.fee,
                                onNavigateClick = {
                                    if (activity.location.isNotBlank()) {
                                        showNavigationSheet = true
                                    }
                                },
                            )

                            NavigationTravelModeSheet(
                                visible = showNavigationSheet,
                                onDismiss = { showNavigationSheet = false },
                                onModeSelected = { mode ->
                                    AmapNavigationHelper.openNavigation(
                                        context = context,
                                        name = activity.location,
                                        latitude = activity.latitude,
                                        longitude = activity.longitude,
                                        mode = mode,
                                    )
                                },
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            ActivityParticipantsSection(
                                participants = participants,
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            ActivityDetailDescriptionSection(
                                description = activity.description.orEmpty(),
                                createdAt = activity.createdAt,
                            )

                            Spacer(modifier = Modifier.height(96.dp))
                        }
                    }

                    ActivityDetailBottomBar(
                        participantCount = participants.size,
                        maxParticipants = activity.maxParticipants,
                        fee = activity.fee,
                        isJoined = activity.isJoined,
                        isSelf = isSelf,
                        isJoining = isJoining,
                        isPaymentProcessing = isPaymentProcessing,
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
    status: String,
) {
    if (images.isNotEmpty()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            XhsDetailImageCarousel(
                images = images,
                contentDescription = activity.title,
                aspectRatio = 1.05f,
                indicatorStyle = CarouselIndicatorStyle.Dots,
                indicatorBottomPadding = 12.dp,
            )
            ActivityDetailHeroOverlay(
                activity = activity,
                status = status,
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(coverGradientForId(activity.id)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Event,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(44.dp),
            )
            ActivityDetailHeroOverlay(
                activity = activity,
                status = status,
            )
        }
    }
}

@Composable
private fun BoxScope.ActivityDetailHeroOverlay(
    activity: ActivityDto,
    status: String,
) {
    Text(
        text = profileActivityStatusLabel(status),
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
    )

    activity.promotionBadge()?.let { badge ->
        XhsPromotionBadge(
            label = badge,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
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
    promotionBadge: String? = null,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!promotionBadge.isNullOrBlank()) {
            XhsPromotionBadge(label = promotionBadge)
        }
        ActivityMetaChip(
            text = if (isPaid) formatPriceYuan(fee) else "免费",
            background = if (isPaid) XhsRedContainer else Color(0xFFF0F7F4),
            textColor = if (isPaid) XhsRed else Color(0xFF1B9B55),
        )
        ActivityMetaChip(
            text = profileActivityStatusLabel(status),
            background = Color(0xFFF5F5F5),
            textColor = XhsTextSecondary,
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        color = XhsTextPrimary,
        lineHeight = 30.sp,
    )
}

@Composable
private fun ActivityMetaChip(
    text: String,
    background: Color,
    textColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = background,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ActivityDetailInfoCard(
    startTime: String,
    endTime: String,
    location: String,
    latitude: Double?,
    longitude: Double?,
    participantCount: Int,
    maxParticipants: Int,
    isPaid: Boolean,
    fee: String?,
    onNavigateClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFAFAFA),
        border = BorderStroke(0.5.dp, XhsDivider),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
            ActivityInfoRow(
                icon = Icons.Default.Schedule,
                label = "时间",
                onClick = null,
            ) {
                Text(
                    text = formatActivityDateTimeForDisplay(startTime),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = XhsTextPrimary,
                    lineHeight = 22.sp,
                )
                Text(
                    text = "至 ${formatActivityDateTimeForDisplay(endTime)}",
                    fontSize = 13.sp,
                    color = XhsTextSecondary,
                    lineHeight = 20.sp,
                )
            }

            HorizontalDivider(color = Color(0xFFEEEEEE))

            ActivityInfoRow(
                icon = Icons.Default.LocationOn,
                label = "地点",
                actionLabel = if (location.isNotBlank()) "导航" else null,
                onClick = if (location.isNotBlank()) onNavigateClick else null,
            ) {
                Text(
                    text = location.ifBlank { "待定" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = XhsTextPrimary,
                    lineHeight = 22.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                XhsDistanceLabel(
                    targetLatitude = latitude,
                    targetLongitude = longitude,
                    fontSize = 12.sp,
                    iconSize = 13.dp,
                )
            }

            HorizontalDivider(color = Color(0xFFEEEEEE))

            ActivityInfoRow(
                icon = Icons.Default.Event,
                label = "报名",
                onClick = null,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = buildString {
                            append("$participantCount 人已报名")
                            if (maxParticipants > 0) {
                                append(" · 限额 $maxParticipants 人")
                            }
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = XhsTextPrimary,
                    )
                    Text(
                        text = if (isPaid) formatPriceYuan(fee) else "免费参加",
                        fontSize = 13.sp,
                        color = if (isPaid) XhsRed else Color(0xFF1B9B55),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (maxParticipants > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (participantCount.toFloat() / maxParticipants).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = XhsRed,
                        trackColor = Color(0xFFFFE8E8),
                        strokeCap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    actionLabel: String? = null,
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
            )
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = XhsRed,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, fontSize = 12.sp, color = XhsTextSecondary)
                if (actionLabel != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = actionLabel,
                            fontSize = 12.sp,
                            color = XhsRed,
                            fontWeight = FontWeight.Medium,
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = XhsRed,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun ActivityParticipantsSection(
    participants: List<ActivityParticipantDto>,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "已报名",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = XhsTextPrimary,
        )
        Text(
            text = "${participants.size} 人",
            fontSize = 13.sp,
            color = XhsTextSecondary,
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (participants.isEmpty()) {
        Text(
            text = "暂无人报名，快来抢占第一个名额",
            fontSize = 14.sp,
            color = XhsTextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF8F8F8))
                .padding(vertical = 20.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    } else {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 4.dp),
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
        modifier = Modifier.width(56.dp),
    ) {
        XhsProfileAvatar(
            label = label,
            size = 48,
            avatarUrl = avatarUrl,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
        )
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
) {
    Text(
        text = "活动详情",
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = XhsTextPrimary,
    )
    Spacer(modifier = Modifier.height(10.dp))
    if (description.isNotBlank()) {
        Text(
            text = description,
            fontSize = 15.sp,
            color = XhsTextPrimary,
            lineHeight = 24.sp,
        )
    } else {
        Text(
            text = "发起人暂未补充活动介绍",
            fontSize = 14.sp,
            color = XhsTextSecondary,
            lineHeight = 22.sp,
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "发布于 ${formatXhsTime(createdAt)}",
        fontSize = 12.sp,
        color = XhsTextSecondary,
    )
}

@Composable
private fun ActivityDetailBottomBar(
    participantCount: Int,
    maxParticipants: Int,
    fee: String?,
    isJoined: Boolean,
    isSelf: Boolean,
    isJoining: Boolean,
    isPaymentProcessing: Boolean,
    likeCount: Int,
    favoriteCount: Int,
    isLiked: Boolean,
    isFavorited: Boolean,
    isSocialSubmitting: Boolean,
    onLike: () -> Unit,
    onFavorite: () -> Unit,
    onJoin: () -> Unit,
) {
    val joinBusy = isJoining || isPaymentProcessing
    val isPaid = fee?.toDoubleOrNull()?.let { it > 0 } == true
    val joinLabel = when {
        isJoined -> "已报名"
        isPaid -> "支付 ${formatPriceYuan(fee)}"
        else -> "免费报名"
    }
    val buttonColors = when {
        isJoined -> ButtonDefaults.buttonColors(
            containerColor = Color(0xFFF0F0F0),
            disabledContainerColor = Color(0xFFF0F0F0),
            contentColor = XhsTextSecondary,
            disabledContentColor = XhsTextSecondary,
        )
        else -> ButtonDefaults.buttonColors(containerColor = XhsRed)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 6.dp,
        color = Color.White,
    ) {
        Column {
            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                XhsDetailSocialChip(
                    icon = {
                        Icon(
                            if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "点赞",
                            tint = if (isLiked) XhsRed else XhsTextPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    count = likeCount,
                    onClick = onLike,
                    enabled = !isSocialSubmitting && !joinBusy,
                )
                XhsDetailSocialChip(
                    icon = {
                        Icon(
                            if (isFavorited) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "收藏",
                            tint = if (isFavorited) Color(0xFFFFB800) else XhsTextPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    count = favoriteCount,
                    onClick = onFavorite,
                    enabled = !isSocialSubmitting && !joinBusy,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (!isSelf) {
                    Button(
                        onClick = onJoin,
                        enabled = !joinBusy && !isJoined,
                        colors = buttonColors,
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .width(140.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    ) {
                        if (joinBusy) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(joinLabel, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$participantCount 人已报名",
                            fontWeight = FontWeight.SemiBold,
                            color = XhsTextPrimary,
                            fontSize = 14.sp,
                        )
                        if (maxParticipants > 0) {
                            Text(
                                text = "限额 $maxParticipants 人",
                                fontSize = 12.sp,
                                color = XhsTextSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}
