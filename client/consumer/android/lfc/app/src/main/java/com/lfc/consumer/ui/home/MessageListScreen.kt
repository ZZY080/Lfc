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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.lfc.consumer.data.model.MessageDto
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsRedContainer
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen(
    messages: List<MessageDto>,
    unreadCount: Int,
    onMessageClick: (MessageDto) -> Unit,
    onMarkAllRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            if (unreadCount > 0) {
                TextButton(onClick = onMarkAllRead) {
                    Text("全部已读", color = XhsRed)
                }
            }
        }

        if (messages.isEmpty()) {
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
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageListItem(
                        message = message,
                        onClick = { onMessageClick(message) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageListItem(
    message: MessageDto,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(XhsRedContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                messageTypeIcon(message.type),
                contentDescription = null,
                tint = XhsRed,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = message.title,
                    fontWeight = if (message.isRead) FontWeight.Normal else FontWeight.Bold,
                    color = XhsTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatMessageTime(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = XhsTextSecondary,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodySmall,
                color = XhsTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!message.isRead) {
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

private fun messageTypeIcon(type: String): ImageVector = when (type.uppercase()) {
    "ACTIVITY_APPROVED" -> Icons.Default.CheckCircle
    "ACTIVITY_REJECTED" -> Icons.Default.Campaign
    "ACTIVITY_JOIN" -> Icons.Default.PersonAdd
    "ACTIVITY_SUBMITTED" -> Icons.Default.Event
    else -> Icons.Default.Notifications
}

private fun formatMessageTime(iso: String): String {
    val normalized = iso.replace("T", " ").take(16)
    return if (normalized.length >= 10) normalized.substring(5, 10).replace("-", "/") else normalized
}
