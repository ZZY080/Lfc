package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.lfc.consumer.data.model.ConversationDto
import com.lfc.consumer.data.model.MessagesUiState
import com.lfc.consumer.data.model.NotificationDto
import com.lfc.consumer.data.model.peerDisplayName
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsRedContainer
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun ConversationListScreen(
    messagesState: MessagesUiState,
    unreadCount: Int,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onConversationClick: (ConversationDto) -> Unit,
    onNotificationClick: (NotificationDto) -> Unit,
    onViewAllNotifications: () -> Unit,
    onMarkAllNotificationsRead: () -> Unit,
    onDeleteNotification: (NotificationDto) -> Unit,
    onDeleteConversation: (ConversationDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    val conversations = messagesState.conversations
    val notifications = messagesState.notificationPreview
    val notificationUnread = messagesState.notificationUnreadCount
    var pendingNotificationDelete by remember { mutableStateOf<NotificationDto?>(null) }
    var pendingConversationDelete by remember { mutableStateOf<ConversationDto?>(null) }

    pendingNotificationDelete?.let { notification ->
        DeleteConfirmDialog(
            title = "删除通知",
            message = "删除后无法恢复，确定要删除这条通知吗？",
            onDismiss = { pendingNotificationDelete = null },
            onConfirm = {
                onDeleteNotification(notification)
                pendingNotificationDelete = null
            },
        )
    }

    pendingConversationDelete?.let { conversation ->
        DeleteConfirmDialog(
            title = "删除会话",
            message = "删除后将从列表移除，对方发来新消息时会再次出现。确定删除吗？",
            onDismiss = { pendingConversationDelete = null },
            onConfirm = {
                onDeleteConversation(conversation)
                pendingConversationDelete = null
            },
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("消息", fontWeight = FontWeight.Bold, color = XhsTextPrimary, fontSize = 20.sp)
                if (unreadCount > 0) {
                    Text(
                        text = " ($unreadCount)",
                        color = XhsRed,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (notificationUnread > 0) {
                TextButton(onClick = onMarkAllNotificationsRead) {
                    Text("通知已读", color = XhsRed)
                }
            }
        }

        MessagesListContent(
            messagesState = messagesState,
            onRefresh = onRefresh,
            onLoadMore = onLoadMore,
            onConversationClick = onConversationClick,
            onNotificationClick = onNotificationClick,
            onViewAllNotifications = onViewAllNotifications,
            onDeleteNotification = { pendingNotificationDelete = it },
            onDeleteConversation = { pendingConversationDelete = it },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun MessagesListContent(
    messagesState: MessagesUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onConversationClick: (ConversationDto) -> Unit,
    onNotificationClick: (NotificationDto) -> Unit,
    onViewAllNotifications: () -> Unit,
    onDeleteNotification: (NotificationDto) -> Unit,
    onDeleteConversation: (ConversationDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    val conversations = messagesState.conversations
    val notifications = messagesState.notificationPreview
    val listState = rememberLazyListState()
    val latestState by rememberUpdatedState(messagesState)
    val pullRefreshState = rememberPullRefreshState(
        refreshing = messagesState.isRefreshing,
        onRefresh = onRefresh,
    )
    val showViewAllNotifications = messagesState.notificationTotal > notifications.size

    LaunchedEffect(messagesState.listResetNonce) {
        if (messagesState.listResetNonce > 0 && listState.layoutInfo.totalItemsCount > 0) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(
        listState,
        messagesState.hasMore,
        messagesState.isLoadingMore,
        messagesState.isRefreshing,
    ) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to info.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (lastVisible, total) ->
                val state = latestState
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
            messagesState.isInitialLoading &&
                conversations.isEmpty() &&
                notifications.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = XhsRed, modifier = Modifier.size(28.dp))
                }
            }
            conversations.isEmpty() && notifications.isEmpty() && !messagesState.isRefreshing -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = XhsTextSecondary,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("暂无消息", color = XhsTextSecondary)
                    }
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    if (notifications.isNotEmpty() || messagesState.notificationTotal > 0) {
                        item(key = "notification-section-title") {
                            SectionTitle("系统通知")
                        }
                        items(notifications, key = { "notification-${it.id}" }) { notification ->
                            SwipeRevealDeleteItem(
                                onDeleteClick = { onDeleteNotification(notification) },
                            ) {
                                NotificationListItem(
                                    notification = notification,
                                    onClick = { onNotificationClick(notification) },
                                )
                            }
                        }
                        if (showViewAllNotifications) {
                            item(key = "notification-view-all") {
                                Text(
                                    text = "查看全部 ${messagesState.notificationTotal} 条通知",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = onViewAllNotifications)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    color = XhsRed,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        item(key = "notification-divider") {
                            HorizontalDivider(
                                modifier = Modifier.padding(top = 8.dp),
                                thickness = 6.dp,
                                color = Color(0xFFF5F5F5),
                            )
                        }
                    }

                    item(key = "conversation-section-title") {
                        SectionTitle("私信")
                    }

                    if (conversations.isEmpty()) {
                        item(key = "conversation-empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("暂无私信，可在笔记详情页联系作者", color = XhsTextSecondary, fontSize = 14.sp)
                            }
                        }
                    } else {
                        items(conversations, key = { it.id }) { conversation ->
                            SwipeRevealDeleteItem(
                                onDeleteClick = { onDeleteConversation(conversation) },
                            ) {
                                ConversationListItem(
                                    conversation = conversation,
                                    onClick = { onConversationClick(conversation) },
                                )
                            }
                        }
                    }

                    if (messagesState.isLoadingMore) {
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
                    } else if (!messagesState.hasMore && conversations.isNotEmpty()) {
                        item(key = "no-more") {
                            Text(
                                text = "没有更多了",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                textAlign = TextAlign.Center,
                                color = XhsTextSecondary,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = messagesState.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = XhsRed,
        )
    }
}

@Composable
internal fun DeleteConfirmDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, fontWeight = FontWeight.Bold, color = XhsTextPrimary)
        },
        text = {
            Text(message, color = XhsTextSecondary, fontSize = 14.sp)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除", color = XhsRed, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = XhsTextSecondary)
            }
        },
    )
}

@Composable
internal fun SwipeRevealDeleteItem(
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val deleteWidth = 72.dp
    val density = LocalDensity.current
    val deleteWidthPx = with(density) { deleteWidth.toPx() }
    var offsetX by remember { mutableFloatStateOf(0f) }

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(XhsRed),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                modifier = Modifier
                    .width(deleteWidth)
                    .fillMaxHeight()
                    .clickable {
                        offsetX = 0f
                        onDeleteClick()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("删除", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(deleteWidthPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            offsetX = if (offsetX <= -deleteWidthPx / 2f) {
                                -deleteWidthPx
                            } else {
                                0f
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount).coerceIn(-deleteWidthPx, 0f)
                        },
                    )
                },
            color = Color.White,
        ) {
            content()
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = XhsTextPrimary,
    )
}

@Composable
private fun ConversationListItem(
    conversation: ConversationDto,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        XhsProfileAvatar(
            label = conversation.peerDisplayName(),
            size = 48,
            avatarUrl = conversation.peerAvatarUrl,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = conversation.peerDisplayName(),
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Medium,
                    color = XhsTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatConversationTime(conversation.lastMessageAt ?: conversation.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = XhsTextSecondary,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = conversation.lastMessageContent ?: "开始聊天吧",
                style = MaterialTheme.typography.bodySmall,
                color = XhsTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (conversation.unreadCount > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(XhsRed),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
internal fun NotificationListItem(
    notification: NotificationDto,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(XhsRedContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                notificationTypeIcon(notification.type),
                contentDescription = null,
                tint = XhsRed,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.title,
                fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                color = XhsTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = notification.content,
                style = MaterialTheme.typography.bodySmall,
                color = XhsTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!notification.isRead) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(XhsRed),
            )
        }
    }
}

private fun notificationTypeIcon(type: String): ImageVector = when (type.uppercase()) {
    "ACTIVITY_APPROVED" -> Icons.Default.CheckCircle
    "ACTIVITY_REJECTED" -> Icons.Default.Campaign
    "ACTIVITY_JOIN" -> Icons.Default.PersonAdd
    "ACTIVITY_SUBMITTED" -> Icons.Default.Event
    else -> Icons.Default.Notifications
}

private fun formatConversationTime(iso: String): String {
    val normalized = iso.replace("T", " ").take(16)
    return if (normalized.length >= 10) normalized.substring(5, 10).replace("-", "/") else normalized
}
