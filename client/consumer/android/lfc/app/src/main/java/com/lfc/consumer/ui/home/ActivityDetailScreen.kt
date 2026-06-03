package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.ui.theme.XhsBackground
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

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
                val authorLabel = activity.author?.studentId ?: "同学${activity.authorId}"
                val participants = activity.participants.orEmpty()
                val participantLabels = participants.map {
                    it.user?.studentId ?: "同学${it.userId}"
                }
                val images = activity.images.orEmpty()
                val isSelf = currentUserId != null && currentUserId == activity.authorId
                Column(modifier = Modifier.fillMaxSize()) {
                    XhsDetailAuthorHeader(
                        authorLabel = authorLabel,
                        authorId = activity.authorId,
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
                        if (images.isNotEmpty()) {
                            XhsDetailImageCarousel(
                                images = images,
                                contentDescription = activity.title,
                                aspectRatio = 0.75f,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .background(coverGradientForId(activity.id)),
                            )
                        }
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = activity.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = XhsTextPrimary,
                                lineHeight = 26.sp,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activityStatusLabel(activity.status),
                                fontSize = 13.sp,
                                color = XhsTextSecondary,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            shadowElevation = 1.dp,
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                XhsDetailInfoRow(
                                    icon = {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = XhsTextSecondary,
                                        )
                                    },
                                    text = activity.location,
                                )
                                XhsDetailInfoRow(
                                    icon = {
                                        Icon(
                                            Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = XhsTextSecondary,
                                        )
                                    },
                                    text = formatActivityTime(activity.startTime, activity.endTime),
                                )
                            }
                        }
                        if (participantLabels.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Text(
                                    "已报名同学",
                                    fontWeight = FontWeight.Bold,
                                    color = XhsTextPrimary,
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                XhsParticipantAvatars(labels = participantLabels)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = "活动详情",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = XhsTextPrimary,
                            )
                            if (activity.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = activity.description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = XhsTextPrimary,
                                    lineHeight = 24.sp,
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "发布于 ${formatXhsTime(activity.createdAt)}",
                                fontSize = 12.sp,
                                color = XhsTextSecondary,
                            )
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                    XhsActivityDetailBottomBar(
                        participantCount = participants.size,
                        maxParticipants = activity.maxParticipants,
                        isJoining = isJoining,
                        onJoin = onJoin,
                    )
                }
            }
        }
    }
}

private fun activityStatusLabel(status: String): String = when (status.uppercase()) {
    "PENDING" -> "待审核"
    "APPROVED" -> "进行中"
    "REJECTED" -> "已拒绝"
    else -> status
}

private fun formatActivityTime(start: String, end: String): String {
    val startShort = start.replace("T", " ").take(16)
    val endShort = end.replace("T", " ").take(16)
    return "$startShort ~ $endShort"
}
