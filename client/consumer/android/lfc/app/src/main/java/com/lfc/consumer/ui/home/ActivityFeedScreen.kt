package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.data.model.ActivityFeedUiState
import com.lfc.consumer.ui.theme.XhsBackground
import com.lfc.consumer.ui.theme.XhsDivider
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary
import kotlinx.coroutines.flow.distinctUntilChanged

private val activityCoverSize = 96.dp

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
        ActivityFeedContent(
            feedState = feedState,
            onRefresh = onRefresh,
            onLoadMore = onLoadMore,
            onJoin = onJoin,
            onActivityClick = onActivityClick,
            currentUserId = currentUserId,
            isPaymentProcessing = isPaymentProcessing,
            payingActivityId = payingActivityId,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
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
    val latestFeedState by rememberUpdatedState(feedState)
    val pullRefreshState = rememberPullRefreshState(
        refreshing = feedState.isRefreshing,
        onRefresh = onRefresh,
    )

    LaunchedEffect(feedState.listResetNonce) {
        if (feedState.listResetNonce > 0 && listState.layoutInfo.totalItemsCount > 0) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(feedState.activities.size, feedState.isRefreshing) {
        if (!feedState.isRefreshing && feedState.activities.isNotEmpty()) {
            val maxIndex = listState.layoutInfo.totalItemsCount - 1
            if (maxIndex >= 0 && listState.firstVisibleItemIndex > maxIndex) {
                listState.scrollToItem(0)
            }
        }
    }

    LaunchedEffect(listState, feedState.hasMore, feedState.isLoadingMore, feedState.isRefreshing) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to info.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (lastVisible, total) ->
                val state = latestFeedState
                if (
                    total > 0 &&
                    lastVisible >= total - 3 &&
                    state.hasMore &&
                    !state.isLoadingMore &&
                    !state.isRefreshing &&
                    !state.isInitialLoading
                ) {
                    onLoadMore()
                }
            }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            when {
                feedState.isInitialLoading && feedState.activities.isEmpty() -> {
                    item(key = "loading") {
                        ActivityFeedSkeleton(itemCount = 4)
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
                        ActivityListItem(
                            activity = activity,
                            isSelf = currentUserId != null && currentUserId == activity.authorId,
                            isJoinBusy = isJoinBusy,
                            onClick = { onActivityClick(activity.id) },
                            onJoin = { onJoin(activity.id) },
                        )
                        HorizontalDivider(
                            color = XhsDivider,
                            thickness = 0.5.dp,
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

        LfcPullRefreshIndicator(
            refreshing = feedState.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = Color.White,
        )
    }
}

@Composable
private fun ActivityListItem(
    activity: ActivityDto,
    isSelf: Boolean,
    isJoinBusy: Boolean,
    onClick: () -> Unit,
    onJoin: () -> Unit,
) {
    val coverUrl = activity.images?.firstOrNull()
    val organizerLabel = activity.author?.displayName() ?: "同学${activity.authorId}"
    val (actionLabel, canJoin) = activityFeedActionLabel(activity, isSelf)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(activityCoverSize)
                .clip(RoundedCornerShape(6.dp))
                .background(coverGradientForId(activity.id)),
        ) {
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = activity.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    Icons.Default.Event,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(28.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.title,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                color = XhsTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatActivityDateRangeForList(activity.startTime, activity.endTime),
                fontSize = 13.sp,
                color = XhsTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = XhsTextSecondary,
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(14.dp),
                )
                Text(
                    text = activity.location.ifBlank { "地点待定" },
                    modifier = Modifier.padding(start = 4.dp),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = XhsTextSecondary,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = organizerLabel,
                    modifier = Modifier.weight(1f),
                    fontSize = 13.sp,
                    color = XhsTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                ActivityFeedActionChip(
                    label = actionLabel,
                    enabled = canJoin && !isJoinBusy,
                    isLoading = isJoinBusy && canJoin,
                    onClick = onJoin,
                )
            }
        }
    }
}

@Composable
private fun ActivityFeedActionChip(
    label: String,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (enabled) XhsRed else XhsRed.copy(alpha = 0.65f)
    val textColor = if (enabled) XhsRed else XhsRed.copy(alpha = 0.65f)

    Box(
        modifier = Modifier
            .padding(start = 8.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled && !isLoading, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = XhsRed,
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = label,
                fontSize = 12.sp,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun activityFeedActionLabel(activity: ActivityDto, isSelf: Boolean): Pair<String, Boolean> {
    if (isActivityEnded(activity.endTime)) {
        return "活动已结束" to false
    }
    when (activity.status.uppercase()) {
        "OFF_SHELF" -> return "已下架" to false
        "PENDING" -> return "待审核" to false
        "REJECTED" -> return "已拒绝" to false
    }
    if (activity.isJoined) {
        return "已报名" to false
    }
    if (isSelf) {
        return profileActivityStatusLabel(activity.status) to false
    }
    return when {
        activity.isPaidActivity() -> "支付 ${formatActivityFeeLabel(activity.fee)}" to true
        else -> "立即报名" to true
    }
}
