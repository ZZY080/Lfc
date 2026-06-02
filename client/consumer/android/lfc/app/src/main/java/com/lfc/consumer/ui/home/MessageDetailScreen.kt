package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lfc.consumer.data.model.MessageDto
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsRedContainer
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailScreen(
    message: MessageDto?,
    isLoading: Boolean,
    onBack: () -> Unit,
    onActivityClick: (Int) -> Unit = {},
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("消息详情", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        when {
            isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    CircularProgressIndicator(color = XhsRed, modifier = Modifier.padding(24.dp))
                }
            }
            message == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                ) {
                    Text("消息不存在或已删除", color = XhsTextSecondary)
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
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
                            color = MaterialTheme.colorScheme.surfaceVariant,
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
