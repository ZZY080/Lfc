package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.lfc.consumer.data.model.NotificationDto
import com.lfc.consumer.ui.theme.XhsBackground
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsRedContainer
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

@Composable
fun MessageDetailScreen(
    message: NotificationDto?,
    isLoading: Boolean,
    onBack: () -> Unit,
    onActivityClick: (Int) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text(
                text = "消息详情",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = XhsTextPrimary,
            )
        }
        HorizontalDivider(color = Color(0xFFEEEEEE))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(XhsBackground)
                .navigationBarsPadding(),
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        color = XhsRed,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                    )
                }
                message == null -> {
                    Text(
                        text = "消息不存在或已删除",
                        color = XhsTextSecondary,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = XhsRedContainer,
                        ) {
                            Text(
                                text = messageTypeLabel(message.type),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = XhsRed,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = message.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = XhsTextPrimary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatDetailTime(message.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = XhsTextSecondary,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = XhsTextPrimary,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                        )
                        if (message.relatedType == "ACTIVITY" && message.relatedId != null) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onActivityClick(message.relatedId) },
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                shadowElevation = 0.5.dp,
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "关联活动",
                                        fontWeight = FontWeight.Bold,
                                        color = XhsTextPrimary,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "点击查看活动详情",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = XhsRed,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun messageTypeLabel(type: String): String = when (type.uppercase()) {
    "SYSTEM" -> "系统通知"
    "ACTIVITY_SUBMITTED" -> "活动提交"
    "ACTIVITY_APPROVED" -> "审核通过"
    "ACTIVITY_REJECTED" -> "审核未通过"
    "ACTIVITY_JOIN" -> "活动报名"
    else -> "通知"
}

private fun formatDetailTime(iso: String): String =
    iso.replace("T", " ").take(19)
