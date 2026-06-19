package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.data.model.NotificationDto
import com.lfc.consumer.data.model.NotificationFeedUiState
import com.lfc.consumer.ui.theme.XhsBackground
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun NotificationListScreen(
    feedState: NotificationFeedUiState,
    notificationUnreadCount: Int,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onNotificationClick: (NotificationDto) -> Unit,
    onMarkAllNotificationsRead: () -> Unit,
    onDeleteNotification: (NotificationDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<NotificationDto?>(null) }

    pendingDelete?.let { notification ->
        DeleteConfirmDialog(
            title = "删除通知",
            message = "删除后无法恢复，确定要删除这条通知吗？",
            onDismiss = { pendingDelete = null },
            onConfirm = {
                onDeleteNotification(notification)
                pendingDelete = null
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Text(
                    text = "系统通知",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = XhsTextPrimary,
                )
            }
            if (notificationUnreadCount > 0) {
                TextButton(onClick = onMarkAllNotificationsRead) {
                    Text("全部已读", color = XhsRed)
                }
            }
        }
        HorizontalDivider(color = Color(0xFFEEEEEE))

        NotificationFeedContent(
            feedState = feedState,
            onRefresh = onRefresh,
            onLoadMore = onLoadMore,
            onNotificationClick = onNotificationClick,
            onDeleteNotification = { pendingDelete = it },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(XhsBackground)
                .navigationBarsPadding(),
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun NotificationFeedContent(
    feedState: NotificationFeedUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onNotificationClick: (NotificationDto) -> Unit,
    onDeleteNotification: (NotificationDto) -> Unit,
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
        when {
            feedState.isInitialLoading && feedState.notifications.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = XhsRed, modifier = Modifier.size(28.dp))
                }
            }
            feedState.notifications.isEmpty() && !feedState.isRefreshing -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = XhsTextSecondary,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("暂无系统通知", color = XhsTextSecondary)
                    }
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(feedState.notifications, key = { it.id }) { notification ->
                        SwipeRevealDeleteItem(
                            onDeleteClick = { onDeleteNotification(notification) },
                        ) {
                            NotificationListItem(
                                notification = notification,
                                onClick = { onNotificationClick(notification) },
                            )
                        }
                    }
                    if (feedState.isLoadingMore) {
                        item(key = "loading-more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    color = XhsRed,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    } else if (!feedState.hasMore && feedState.notifications.isNotEmpty()) {
                        item(key = "no-more") {
                            Text(
                                text = "没有更多了",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = XhsTextSecondary,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = feedState.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = XhsRed,
        )
    }
}
