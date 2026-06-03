package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.data.model.ChatMessageDto
import com.lfc.consumer.data.model.ConversationDto
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

@Composable
fun ChatScreen(
    conversation: ConversationDto?,
    messages: List<ChatMessageDto>,
    currentUserId: Int?,
    isLoading: Boolean,
    isSending: Boolean,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            XhsProfileAvatar(
                label = conversation?.peerStudentId ?: "?",
                size = 34,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = conversation?.peerStudentId ?: "私信",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = XhsTextPrimary,
            )
        }
        HorizontalDivider(color = Color(0xFFEEEEEE))

        when {
            isLoading -> XhsDetailLoading(Modifier.weight(1f))
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("打个招呼吧", color = XhsTextSecondary)
                            }
                        }
                    } else {
                        items(messages, key = { it.id }) { message ->
                            ChatBubble(
                                message = message,
                                isMine = message.senderId == currentUserId,
                            )
                        }
                    }
                }
            }
        }

        Surface(color = Color.White, shadowElevation = 4.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF5F5F5))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        enabled = !isSending,
                        textStyle = TextStyle(fontSize = 15.sp, color = XhsTextPrimary),
                        cursorBrush = SolidColor(XhsRed),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (input.isEmpty()) {
                                Text("发消息...", color = XhsTextSecondary, fontSize = 15.sp)
                            }
                            inner()
                        },
                    )
                }
                IconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            onSend(input.trim())
                            input = ""
                        }
                    },
                    enabled = input.isNotBlank() && !isSending,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "发送",
                        tint = if (input.isNotBlank()) XhsRed else XhsTextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessageDto,
    isMine: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
    ) {
        if (!isMine) {
            XhsProfileAvatar(
                label = message.sender?.studentId ?: "同学",
                size = 32,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMine) 16.dp else 4.dp,
                            bottomEnd = if (isMine) 4.dp else 16.dp,
                        ),
                    )
                    .background(if (isMine) XhsRed else Color.White)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = message.content,
                    color = if (isMine) Color.White else XhsTextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                )
            }
            Text(
                text = formatRelativeTime(message.createdAt),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
                fontSize = 11.sp,
                color = XhsTextSecondary,
            )
        }
    }
}
