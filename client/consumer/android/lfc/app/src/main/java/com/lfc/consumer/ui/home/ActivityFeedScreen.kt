package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lfc.consumer.data.model.displayName
import com.lfc.consumer.data.model.isPaidActivity
import com.lfc.consumer.data.model.promotionBadge
import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.data.model.ActivityFeedUiState
import com.lfc.consumer.ui.theme.XhsBackground
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary
import kotlinx.coroutines.flow.distinctUntilChanged

private val activityCoverHeight = 168.dp

@Composable
fun ActivityFeedScreen(
    feedState: ActivityFeedUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onJoin: (Int) -> Unit,
    onActivityClick: (Int) -> Unit,
    currentUserId: Int? = null,
    isPaymentProcessing: Boolean = false,
    payingActivityId: Int? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XhsBackground),
    ) {
        XhsPageTitle("校园活动")
        Text(
            text = "推广内容已明确标注，优先展示不代表官方背书",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 6.dp),
            color = XhsTextSecondary,
            fontSize = 11.sp,
        )
        ActivityFeedContent(
            feedState = feedState,
            onRefresh = onRefresh,
            onLoadMore = onLoadMore,
            onJoin = onJoin,
            onActivityClick = onActivityClick,
            currentUserId = currentUserId,
            isPaymentProcessing = isPaymentProcessing,
            payingActivityId = payingActivityId,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityFeedContent(
    feedState: ActivityFeedUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onJoin: (Int) -> Unit,
    onActivityClick: (Int) -> Unit,
    currentUserId: Int?,
    isPaymentProcessing: Boolean,
    payingActivityId: Int?,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val pullRefreshState = rememberPullToRefreshState()
    val activityCount = feedState.activities.size
    val latestFeedState by rememberUpdatedState(feedState)

    LaunchedEffect(listState, activityCount) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible
        }
            .distinctUntilChanged()
            .collect { lastVisible ->
                val state = latestFeedState
                val count = state.activities.size
                if (
                    count > 0 &&
                    lastVisible >= count - 1 &&
                    lastVisible < count &&
                    state.hasMore &&
                    !state.isLoadingMore &&
                    !state.isRefreshing &&
                    !state.isInitialLoading
                ) {
                    onLoadMore()
                }
            }
    }

    PullToRefreshBox(
        state = pullRefreshState,
        isRefreshing = feedState.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                isRefreshing = feedState.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                color = XhsRed,
            )
        },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when {
                feedState.isInitialLoading && feedState.activities.isEmpty() -> {
                    item(key = "loading") {
                        ActivityFeedSkeleton(itemCount = 2)
                    }
                }
                feedState.activities.isEmpty() -> {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("暂无活动，点击 + 发布第一个活动吧", color = XhsTextSecondary)
                        }
                    }
                }
                else -> {
                    items(feedState.activities, key = { it.id }) { activity ->
                        val isJoinBusy = isPaymentProcessing &&
                            (payingActivityId == null || payingActivityId == activity.id)
                        ActivityCard(
                            activity = activity,
                            isSelf = currentUserId != null && currentUserId == activity.authorId,
                            isJoinBusy = isJoinBusy,
                            onClick = { onActivityClick(activity.id) },
                            onJoin = { onJoin(activity.id) },
                        )
                    }
                    if (feedState.isLoadingMore) {
                        item(key = "loading-more") {
                            SkeletonLoadMoreFooter()
                        }
                    } else if (!feedState.hasMore) {
                        item(key = "end") {
                            Text(
                                "— 已经到底了 —",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                color = XhsTextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityCard(
    activity: ActivityDto,
    isSelf: Boolean,
    isJoinBusy: Boolean,
    onClick: () -> Unit,
    onJoin: () -> Unit,
) {
    val coverUrl = activity.images?.firstOrNull()
    val participantCount = activity.participants?.size ?: 0
    val joinLabel = when {
        activity.isJoined -> "已报名"
        activity.isPaidActivity() -> "支付 ${formatActivityFeeLabel(activity.fee)}"
        else -> "免费报名"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(activityCoverHeight)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(coverGradientForId(activity.id)),
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = activity.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Text(
                    text = profileActivityStatusLabel(activity.status),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.42f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    color = Color.White,
                    fontSize = 10.sp,
                )

                activity.promotionBadge()?.let { badge ->
                    XhsPromotionBadge(
                        label = badge,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                    )
                }

                if (participantCount > 0) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.42f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Event,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp),
                        )
                        Text(
                            text = formatLikeCount(participantCount),
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(start = 2.dp),
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(
                    text = activity.title,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = XhsTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (activity.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = activity.description,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = XhsTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = XhsTextSecondary,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = activity.location,
                        fontSize = 13.sp,
                        color = XhsTextSecondary,
                        modifier = Modifier.padding(start = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                XhsDistanceLabel(
                    targetLatitude = activity.latitude,
                    targetLongitude = activity.longitude,
                    fontSize = 13.sp,
                    iconSize = 15.dp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = XhsTextSecondary,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = formatActivityTime(activity.startTime, activity.endTime),
                        fontSize = 13.sp,
                        color = XhsTextSecondary,
                        modifier = Modifier.padding(start = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val authorLabel = activity.author?.displayName() ?: "同学${activity.authorId}"
                    XhsProfileAvatar(
                        label = authorLabel,
                        size = 20,
                        avatarUrl = activity.author?.avatarUrl,
                    )
                    Text(
                        text = authorLabel,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 6.dp),
                        fontSize = 12.sp,
                        color = XhsTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        if (activity.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (activity.isLiked) XhsRed else XhsTextSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp),
                    )
                    if (activity.likeCount > 0) {
                        Text(
                            text = formatLikeCount(activity.likeCount),
                            modifier = Modifier.padding(start = 2.dp),
                            fontSize = 12.sp,
                            color = if (activity.isLiked) XhsRed else XhsTextSecondary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val count = activity.participants?.size ?: 0
                    Column {
                        Text(
                            text = "$count 人已报名",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = XhsRed,
                        )
                        Text(
                            text = buildString {
                                if (activity.maxParticipants > 0) append("限额 ${activity.maxParticipants} 人 · ")
                                append(if (activity.isPaidActivity()) formatActivityFeeLabel(activity.fee) else "免费")
                            },
                            fontSize = 12.sp,
                            color = XhsTextSecondary,
                        )
                    }
                    if (!isSelf) {
                        Button(
                            onClick = onJoin,
                            enabled = !activity.isJoined && !isJoinBusy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activity.isJoined) Color(0xFFE8E8E8) else XhsRed,
                                disabledContainerColor = Color(0xFFE8E8E8),
                                disabledContentColor = XhsTextSecondary,
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(38.dp),
                        ) {
                            if (isJoinBusy && !activity.isJoined) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(
                                    text = joinLabel,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activity.isJoined) XhsTextSecondary else Color.White,
                                )
                            }
                        }
                    }
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
