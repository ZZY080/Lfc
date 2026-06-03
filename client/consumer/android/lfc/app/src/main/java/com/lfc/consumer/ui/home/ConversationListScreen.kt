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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.data.model.ConversationDto
import com.lfc.consumer.data.model.NotificationDto
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsRedContainer
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

@Composable
fun ConversationListScreen(
    conversations: List<ConversationDto>,
    notifications: List<NotificationDto>,
    unreadCount: Int,
    onConversationClick: (ConversationDto) -> Unit,
    onNotificationClick: (NotificationDto) -> Unit,
    onMarkAllNotificationsRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val notificationUnread = notifications.count { !it.isRead }

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

        if (conversations.isEmpty() && notifications.isEmpty()) {
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
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 8.dp)) {
                if (notifications.isNotEmpty()) {
                    item {
                        SectionTitle("系统通知")
                    }
                    items(notifications, key = { "notification-${it.id}" }) { notification ->
                        NotificationListItem(
                            notification = notification,
                            onClick = { onNotificationClick(notification) },
                        )
                    }
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 8.dp),
                            thickness = 6.dp,
                            color = androidx.compose.ui.graphics.Color(0xFFF5F5F5),
                        )
                    }
                }

                item {
                    SectionTitle("私信")
                }

                if (conversations.isEmpty()) {
                    item {
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
                        ConversationListItem(
                            conversation = conversation,
                            onClick = { onConversationClick(conversation) },
                        )
                    }
                }
            }
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
        XhsProfileAvatar(label = conversation.peerStudentId, size = 48)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = conversation.peerStudentId,
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
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun NotificationListItem(
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
