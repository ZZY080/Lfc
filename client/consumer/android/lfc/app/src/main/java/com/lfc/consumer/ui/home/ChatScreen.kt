package com.lfc.consumer.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lfc.consumer.data.model.ChatMessageDto
import com.lfc.consumer.data.model.ConversationDto
import com.lfc.consumer.data.model.parseChatProductPayload
import com.lfc.consumer.data.model.displayName
import com.lfc.consumer.data.model.peerDisplayName
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

private val chatBackground = Color(0xFFF3F3F3)
private val peerBubbleColor = Color.White
private val inputBackground = Color(0xFFF5F5F5)

@Composable
fun ChatScreen(
    conversation: ConversationDto?,
    messages: List<ChatMessageDto>,
    currentUserId: Int?,
    currentUserLabel: String = "我",
    currentUserAvatarUrl: String? = null,
    isLoading: Boolean,
    isSending: Boolean,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onSendMedia: (Uri, String) -> Unit = { _, _ -> },
    onProductClick: (Int) -> Unit = {},
) {
    var input by remember { mutableStateOf("") }
    var showAttachmentPanel by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val peerLabel = conversation?.peerDisplayName() ?: "私信"
    val peerAvatarUrl = conversation?.peerAvatarUrl

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let {
            showAttachmentPanel = false
            onSendMedia(it, "IMAGE")
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let {
            showAttachmentPanel = false
            onSendMedia(it, "VIDEO")
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(chatBackground)
            .imePadding(),
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
                label = peerLabel,
                size = 34,
                avatarUrl = peerAvatarUrl,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = peerLabel,
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
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("打个招呼吧", color = XhsTextSecondary, fontSize = 14.sp)
                            }
                        }
                    } else {
                        items(messages, key = { it.id }) { message ->
                            ChatBubble(
                                message = message,
                                isMine = message.senderId == currentUserId,
                                myLabel = currentUserLabel,
                                myAvatarUrl = currentUserAvatarUrl,
                                peerLabel = peerLabel,
                                peerAvatarUrl = peerAvatarUrl,
                                onProductClick = onProductClick,
                            )
                        }
                    }
                }
            }
        }

        ChatInputDock(
            input = input,
            onInputChange = { input = it },
            isSending = isSending,
            showAttachmentPanel = showAttachmentPanel,
            onToggleAttachmentPanel = {
                showAttachmentPanel = !showAttachmentPanel
                if (showAttachmentPanel) {
                    keyboardController?.hide()
                }
            },
            onSend = {
                if (input.isNotBlank() && !isSending) {
                    onSend(input.trim())
                    input = ""
                    showAttachmentPanel = false
                }
            },
            onPickImage = {
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onPickVideo = {
                videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            },
        )
    }
}

@Composable
private fun ChatInputDock(
    input: String,
    onInputChange: (String) -> Unit,
    isSending: Boolean,
    showAttachmentPanel: Boolean,
    onToggleAttachmentPanel: () -> Unit,
    onSend: () -> Unit,
    onPickImage: () -> Unit,
    onPickVideo: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding(),
    ) {
        if (showAttachmentPanel) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                ChatAttachmentAction(
                    icon = Icons.Default.Image,
                    label = "图片",
                    onClick = onPickImage,
                )
                ChatAttachmentAction(
                    icon = Icons.Default.Videocam,
                    label = "视频",
                    onClick = onPickVideo,
                )
            }
            HorizontalDivider(color = Color(0xFFF0F0F0))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            IconButton(
                onClick = onToggleAttachmentPanel,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "更多",
                    tint = if (showAttachmentPanel) XhsRed else XhsTextPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(inputBackground)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                BasicTextField(
                    value = input,
                    onValueChange = onInputChange,
                    enabled = !isSending,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        color = XhsTextPrimary,
                        lineHeight = 22.sp,
                    ),
                    cursorBrush = SolidColor(XhsRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 38.dp),
                    decorationBox = { inner ->
                        if (input.isEmpty()) {
                            Text("发消息...", color = XhsTextSecondary, fontSize = 15.sp)
                        }
                        inner()
                    },
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            if (isSending) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = XhsRed,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                }
            } else if (input.isNotBlank()) {
                Text(
                    text = "发送",
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onSend)
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    color = XhsRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            } else {
                Spacer(modifier = Modifier.width(40.dp))
            }
        }
    }
}

@Composable
private fun ChatAttachmentAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = XhsTextPrimary, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, fontSize = 12.sp, color = XhsTextSecondary)
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessageDto,
    isMine: Boolean,
    myLabel: String,
    myAvatarUrl: String?,
    peerLabel: String,
    peerAvatarUrl: String?,
    onProductClick: (Int) -> Unit,
) {
    val avatarLabel = when {
        isMine -> message.sender?.displayName() ?: myLabel
        else -> message.sender?.displayName() ?: peerLabel
    }
    val avatarUrl = when {
        isMine -> message.sender?.avatarUrl ?: myAvatarUrl
        else -> message.sender?.avatarUrl ?: peerAvatarUrl
    }
    val bubbleShapeMine = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = 18.dp,
        bottomEnd = 4.dp,
    )
    val bubbleShapePeer = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = 4.dp,
        bottomEnd = 18.dp,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isMine) {
            XhsProfileAvatar(label = avatarLabel, size = 36, avatarUrl = avatarUrl)
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 260.dp),
        ) {
            when (message.messageType.uppercase()) {
                "PRODUCT" -> {
                    val product = parseChatProductPayload(message.content)
                    if (product != null) {
                        ChatProductMessageBubble(
                            product = product,
                            isMine = isMine,
                            bubbleShape = if (isMine) bubbleShapeMine else bubbleShapePeer,
                            onClick = { onProductClick(product.postId) },
                        )
                    } else {
                        Text(
                            text = "[商品消息]",
                            modifier = Modifier.padding(8.dp),
                            color = XhsTextSecondary,
                            fontSize = 13.sp,
                        )
                    }
                }
                "IMAGE" -> {
                    Surface(
                        shape = if (isMine) bubbleShapeMine else bubbleShapePeer,
                        color = Color.Transparent,
                        shadowElevation = if (isMine) 0.dp else 1.dp,
                    ) {
                        AsyncImage(
                            model = message.content,
                            contentDescription = "图片消息",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .widthIn(max = 220.dp)
                                .height(160.dp)
                                .clip(if (isMine) bubbleShapeMine else bubbleShapePeer),
                        )
                    }
                }
                "VIDEO" -> {
                    Surface(
                        shape = if (isMine) bubbleShapeMine else bubbleShapePeer,
                        color = Color(0xFF1E1E1E),
                        shadowElevation = if (isMine) 0.dp else 1.dp,
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(min = 180.dp, max = 220.dp)
                                .height(120.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "视频",
                                tint = Color.White,
                                modifier = Modifier.size(42.dp),
                            )
                        }
                    }
                }
                else -> {
                    if (isMine) {
                        Surface(shape = bubbleShapeMine, color = XhsRed) {
                            Text(
                                text = message.content,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                color = Color.White,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                            )
                        }
                    } else {
                        Surface(shape = bubbleShapePeer, color = peerBubbleColor, shadowElevation = 1.dp) {
                            Text(
                                text = message.content,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                color = XhsTextPrimary,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                            )
                        }
                    }
                }
            }
            Text(
                text = formatRelativeTime(message.createdAt),
                modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp),
                fontSize = 10.sp,
                color = XhsTextSecondary.copy(alpha = 0.85f),
            )
        }

        if (isMine) {
            Spacer(modifier = Modifier.width(8.dp))
            XhsProfileAvatar(label = avatarLabel, size = 36, avatarUrl = avatarUrl)
        }
    }
}

@Composable
private fun ChatProductMessageBubble(
    product: com.lfc.consumer.data.model.ChatProductPayload,
    isMine: Boolean,
    bubbleShape: RoundedCornerShape,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .widthIn(max = 240.dp)
            .clickable(onClick = onClick),
        shape = bubbleShape,
        color = Color.White,
        shadowElevation = 1.dp,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(coverGradientForId(product.postId)),
            ) {
                product.coverUrl?.let { coverUrl ->
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = product.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Text(
                    text = "商品",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    color = Color.White,
                    fontSize = 10.sp,
                )
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = product.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = XhsTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatPriceYuan(product.price),
                    color = XhsRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                product.category?.let { category ->
                    Text(
                        text = productCategoryLabel(category),
                        fontSize = 11.sp,
                        color = XhsTextSecondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
