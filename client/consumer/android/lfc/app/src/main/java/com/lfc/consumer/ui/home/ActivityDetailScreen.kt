package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Groups
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.data.model.ActivityParticipantDto
import com.lfc.consumer.data.model.displayName
import com.lfc.consumer.data.model.isPaidActivity
import com.lfc.consumer.location.AmapNavigationHelper
import com.lfc.consumer.ui.theme.XhsBackground
import com.lfc.consumer.ui.theme.XhsDivider
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsRedContainer
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

private val sheetRadius = 20.dp
private val heroHeight = 300.dp

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
                val context = LocalContext.current
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
                            status = activity.status,
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = (-sheetRadius))
                                .clip(RoundedCornerShape(topStart = sheetRadius, topEnd = sheetRadius))
                                .background(Color.White)
                                .padding(top = 22.dp, bottom = 24.dp),
                        ) {
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
                                Spacer(modifier = Modifier.height(12.dp))
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
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            ActivityDetailSummaryStrip(
                                participantCount = participants.size,
                                maxParticipants = activity.maxParticipants,
                                isPaid = isPaid,
                                fee = activity.fee,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            ActivityDetailWhenWhereSection(
                                location = activity.location,
                                startTime = activity.startTime,
                                endTime = activity.endTime,
                                onNavigate = {
                                    AmapNavigationHelper.openNavigation(
                                        context = context,
                                        name = activity.location,
                                        latitude = activity.latitude,
                                        longitude = activity.longitude,
                                    )
                                },
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            ActivityParticipantsSection(
                                participants = participants,
                                maxParticipants = activity.maxParticipants,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )

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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight),
    ) {
        if (images.isNotEmpty()) {
            XhsDetailImageCarousel(
                images = images,
                contentDescription = activity.title,
                modifier = Modifier.fillMaxSize(),
                aspectRatio = 0.75f,
                indicatorStyle = CarouselIndicatorStyle.Counter,
                indicatorBottomPadding = 28.dp,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(coverGradientForId(activity.id)),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Default.Event,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = activity.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.18f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.42f),
                        ),
                    ),
                ),
        )

        ActivityStatusChip(
            status = status,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 14.dp),
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
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!promotionBadge.isNullOrBlank()) {
                XhsPromotionBadge(label = promotionBadge)
            }
            if (isPaid) {
                Surface(shape = RoundedCornerShape(20.dp), color = XhsRedContainer) {
                    Text(
                        text = formatPriceYuan(fee),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        color = XhsRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
            } else {
                Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFE8F8EF)) {
                    Text(
                        text = "免费参加",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        color = Color(0xFF1B9B55),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
            }
            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFF5F7FA)) {
                Text(
                    text = "校园活动",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    color = XhsTextSecondary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = XhsTextPrimary,
            lineHeight = 32.sp,
        )
    }
}

@Composable
private fun ActivityStatusChip(
    status: String,
    modifier: Modifier = Modifier,
) {
    val (label, bg, fg) = when (status.uppercase()) {
        "PENDING" -> Triple("待审核", Color(0xFFFFF3E0), Color(0xFFE65100))
        "APPROVED" -> Triple("进行中", Color(0xFFE8F4FD), Color(0xFF1565C0))
        "REJECTED" -> Triple("已拒绝", Color(0xFFF5F5F5), XhsTextSecondary)
        else -> Triple(status, Color(0x66000000), Color.White)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = bg,
        shadowElevation = if (status.uppercase() == "APPROVED") 0.dp else 2.dp,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            color = fg,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ActivityDetailSummaryStrip(
    participantCount: Int,
    maxParticipants: Int,
    isPaid: Boolean,
    fee: String?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFFF8F6),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, XhsRed.copy(alpha = 0.12f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SummaryMetricItem(
                label = "已报名",
                value = "${participantCount}人",
                modifier = Modifier.weight(1f),
            )
            VerticalDividerLite()
            SummaryMetricItem(
                label = "名额",
                value = if (maxParticipants > 0) "${maxParticipants}人" else "不限",
                modifier = Modifier.weight(1f),
            )
            VerticalDividerLite()
            SummaryMetricItem(
                label = "费用",
                value = if (isPaid) formatPriceYuan(fee) else "免费",
                valueColor = if (isPaid) XhsRed else Color(0xFF1B9B55),
                modifier = Modifier.weight(1f),
            )
        }
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

@Composable
private fun VerticalDividerLite() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(Color(0xFFFFE0DA)),
    )
}

@Composable
private fun SummaryMetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = XhsTextPrimary,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = valueColor,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = XhsTextSecondary,
        )
    }
}

@Composable
private fun ActivityDetailWhenWhereSection(
    location: String,
    startTime: String,
    endTime: String,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ActivityHighlightCard(
            icon = Icons.Default.Schedule,
            iconTint = Color(0xFF4A90E2),
            iconBg = Color(0xFFEAF3FC),
            title = "活动时间",
            onClick = null,
        ) {
            Text(
                text = formatActivityDateTimeForDisplay(startTime),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = XhsTextPrimary,
                lineHeight = 22.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "至 ${formatActivityDateTimeForDisplay(endTime)}",
                fontSize = 13.sp,
                color = XhsTextSecondary,
                lineHeight = 20.sp,
            )
        }

        ActivityHighlightCard(
            icon = Icons.Default.LocationOn,
            iconTint = Color(0xFFFF6B35),
            iconBg = Color(0xFFFFEDE6),
            title = "活动地点",
            actionLabel = if (location.isNotBlank()) "导航" else null,
            onClick = if (location.isNotBlank()) onNavigate else null,
        ) {
            Text(
                text = location.ifBlank { "待定" },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (location.isNotBlank()) Color(0xFF1565C0) else XhsTextSecondary,
                lineHeight = 22.sp,
            )
        }
    }
}

@Composable
private fun ActivityHighlightCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    actionLabel: String? = null,
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFAFAFA),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, XhsDivider),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title, fontSize = 12.sp, color = XhsTextSecondary)
                    if (actionLabel != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = actionLabel,
                                fontSize = 12.sp,
                                color = Color(0xFF4A90E2),
                                fontWeight = FontWeight.Medium,
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFF4A90E2),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                content()
            }
        }
    }
}

@Composable
private fun ActivityParticipantsSection(
    participants: List<ActivityParticipantDto>,
    maxParticipants: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, XhsDivider),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = null,
                        tint = XhsRed,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "已报名同学",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = XhsTextPrimary,
                    )
                }
                Surface(shape = RoundedCornerShape(12.dp), color = XhsRedContainer) {
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

            if (participants.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8F8F8))
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "暂无人报名，快来抢占第一个名额",
                        fontSize = 13.sp,
                        color = XhsTextSecondary,
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(end = 4.dp),
                ) {
                    items(participants, key = { it.id }) { participant ->
                        ActivityParticipantChip(participant = participant)
                    }
                }
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
                .border(2.dp, Color(0xFFFFE8E8), CircleShape),
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
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFFAFAFA),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        fontSize = 15.sp,
                        color = Color(0xFF333333),
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
                HorizontalDivider(color = Color(0xFFEFEFEF))
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "发布于 ${formatXhsTime(createdAt)}",
                    fontSize = 12.sp,
                    color = XhsTextSecondary,
                )
            }
        }
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
            containerColor = Color(0xFFEFEFEF),
            disabledContainerColor = Color(0xFFEFEFEF),
            contentColor = XhsTextSecondary,
            disabledContentColor = XhsTextSecondary,
        )
        else -> ButtonDefaults.buttonColors(containerColor = XhsRed)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 16.dp,
        color = Color.White,
        tonalElevation = 0.dp,
    ) {
        Column {
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
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
                            if (maxParticipants > 0) append("限额 $maxParticipants 人 · ")
                            append(if (isPaid) formatPriceYuan(fee) else "免费参加")
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
                    enabled = !isSocialSubmitting && !joinBusy,
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
                    enabled = !isSocialSubmitting && !joinBusy,
                )
                if (!isSelf) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onJoin,
                        enabled = !joinBusy && !isJoined,
                        colors = buttonColors,
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.height(44.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    ) {
                        if (joinBusy) {
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
    }
}
