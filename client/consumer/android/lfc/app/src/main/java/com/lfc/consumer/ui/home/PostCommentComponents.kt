package com.lfc.consumer.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.lfc.consumer.data.model.PostCommentDto
import com.lfc.consumer.data.model.PostCommentsUiState
import com.lfc.consumer.data.model.displayName
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary
import kotlinx.coroutines.delay

private val composerFieldBg = Color(0xFFF5F5F5)
private val commentMetaColor = Color(0xFF999999)

private val quickEmojis = listOf("😀", "😂", "🥰", "😭", "👍", "🙏", "❤️", "🔥")

private val extendedEmojis = listOf(
    "😊", "🙂", "😉", "😍", "🤔", "😅", "🥺", "😡",
    "👏", "💪", "✨", "🎉", "🌹", "💯", "👀", "🐶",
    "🍻", "☕", "🎵", "📚", "🏀", "⚽", "🚗", "✈️",
)

internal enum class CommentSortOrder {
    Default,
    Newest,
}

internal fun commentSortOrder(sort: String): CommentSortOrder =
    if (sort == "newest") CommentSortOrder.Newest else CommentSortOrder.Default

internal fun nextCommentSort(current: String): String =
    if (current == "newest") "default" else "newest"

@Composable
internal fun PostCommentsHeader(
    commentCount: Int,
    sortOrder: CommentSortOrder,
    onSortToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "共 $commentCount 条评论",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = XhsTextPrimary,
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.clickable(onClick = onSortToggle),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (sortOrder == CommentSortOrder.Newest) {
                Text(
                    text = "最新",
                    fontSize = 12.sp,
                    color = XhsRed,
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
            Icon(
                Icons.Outlined.Sort,
                contentDescription = "排序",
                tint = if (sortOrder == CommentSortOrder.Newest) XhsRed else XhsTextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
internal fun PostQuickCommentTrigger(
    userLabel: String,
    userAvatarUrl: String?,
    onClick: () -> Unit,
    onImageClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        XhsProfileAvatar(label = userLabel, size = 32, avatarUrl = userAvatarUrl)
        Spacer(modifier = Modifier.width(10.dp))
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            color = composerFieldBg,
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "有话要说，快来评论",
                    color = Color(0xFFAAAAAA),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onClick),
                )
                Icon(
                    Icons.Default.Image,
                    contentDescription = "图片",
                    tint = Color(0xFFBBBBBB),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onImageClick),
                )
            }
        }
    }
}

@Composable
internal fun PostCommentsLoadingState() {
    CommentListSkeleton()
}

@Composable
internal fun PostCommentsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            tint = Color(0xFFE0E0E0),
            modifier = Modifier.size(36.dp),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "还没有评论",
            fontSize = 14.sp,
            color = XhsTextSecondary,
        )
    }
}

@Composable
internal fun PostCommentThreadItem(
    thread: PostCommentThreadUi,
    postAuthorId: Int,
    commentIndex: Map<Int, PostCommentDto>,
    isFirstComment: Boolean,
    onReplyTo: (PostCommentDto) -> Unit,
    onLikeComment: (PostCommentDto) -> Unit,
    onLoadMoreReplies: () -> Unit,
) {
    val hiddenCount = (thread.replyCount - thread.replies.size).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        PostCommentRow(
            comment = thread.root,
            postAuthorId = postAuthorId,
            parentComment = null,
            avatarSize = 34,
            showFirstBadge = isFirstComment,
            onReply = { onReplyTo(thread.root) },
            onLike = { onLikeComment(thread.root) },
        )

        thread.replies.forEach { reply ->
            PostCommentRow(
                comment = reply,
                postAuthorId = postAuthorId,
                parentComment = commentIndex[reply.parentId],
                avatarSize = 24,
                showFirstBadge = false,
                onReply = { onReplyTo(reply) },
                onLike = { onLikeComment(reply) },
                modifier = Modifier.padding(start = 42.dp, top = 12.dp),
            )
        }

        when {
            thread.isLoadingReplies -> {
                Box(
                    modifier = Modifier
                        .padding(start = 42.dp, top = 10.dp)
                        .size(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF5B7C99),
                    )
                }
            }
            thread.hasMoreReplies && hiddenCount > 0 -> {
                Text(
                    text = "展开 $hiddenCount 条回复",
                    modifier = Modifier
                        .padding(start = 42.dp, top = 10.dp)
                        .clickable(onClick = onLoadMoreReplies),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF5B7C99),
                )
            }
        }
    }
}

@Composable
internal fun PostCommentsLoadMoreFooter(isLoading: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = XhsRed,
            )
        }
    }
}

@Composable
private fun PostCommentRow(
    comment: PostCommentDto,
    postAuthorId: Int,
    parentComment: PostCommentDto?,
    avatarSize: Int,
    showFirstBadge: Boolean,
    onReply: () -> Unit,
    onLike: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val authorLabel = comment.author?.displayName() ?: "同学${comment.userId}"
    val isAuthor = comment.userId == postAuthorId
    val parsed = CommentContentHelper.parse(comment.content)
    val displayContent = formatCommentContent(parsed.text, comment, parentComment)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        XhsProfileAvatar(
            label = authorLabel,
            size = avatarSize,
            avatarUrl = comment.author?.avatarUrl,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = authorLabel,
                    fontSize = 13.sp,
                    fontWeight = if (isAuthor) FontWeight.Medium else FontWeight.Normal,
                    color = if (isAuthor) XhsTextPrimary else XhsTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isAuthor) {
                    Spacer(modifier = Modifier.width(6.dp))
                    PostAuthorBadge()
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (displayContent.isNotBlank() && displayContent != "[图片]") {
                Text(
                    text = displayContent,
                    fontSize = 15.sp,
                    color = XhsTextPrimary,
                    lineHeight = 22.sp,
                )
            }
            parsed.imageUrl?.let { imageUrl ->
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "评论图片",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatRelativeTime(comment.createdAt),
                    fontSize = 11.sp,
                    color = commentMetaColor,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "回复",
                    modifier = Modifier.clickable(onClick = onReply),
                    fontSize = 12.sp,
                    color = commentMetaColor,
                )
                if (showFirstBadge) {
                    Spacer(modifier = Modifier.width(8.dp))
                    FirstCommentBadge()
                }
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(start = 8.dp, top = 2.dp)
                .clickable(onClick = onLike),
        ) {
            Icon(
                if (comment.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "点赞",
                tint = if (comment.isLiked) XhsRed else Color(0xFFCCCCCC),
                modifier = Modifier.size(16.dp),
            )
            if (comment.likeCount > 0) {
                Text(
                    text = if (comment.likeCount > 99) "99+" else comment.likeCount.toString(),
                    fontSize = 10.sp,
                    color = if (comment.isLiked) XhsRed else commentMetaColor,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

private fun formatCommentContent(
    text: String,
    comment: PostCommentDto,
    parentComment: PostCommentDto?,
): String {
    if (parentComment != null && parentComment.id == comment.parentId) {
        val parentName = parentComment.author?.displayName() ?: "同学${parentComment.userId}"
        return "回复 $parentName：$text"
    }
    return text
}

@Composable
private fun PostAuthorBadge() {
    Surface(
        shape = RoundedCornerShape(3.dp),
        color = Color(0xFFFFECEE),
    ) {
        Text(
            text = "作者",
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = XhsRed,
            lineHeight = 12.sp,
        )
    }
}

@Composable
private fun FirstCommentBadge() {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFFF0F0F0),
    ) {
        Text(
            text = "首评",
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            fontSize = 10.sp,
            color = XhsTextSecondary,
        )
    }
}

@Composable
fun PostCommentScrim(onDismiss: () -> Unit) {
    val alpha by animateFloatAsState(
        targetValue = 0.45f,
        animationSpec = tween(durationMillis = 200),
        label = "scrimAlpha",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = alpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCommentExpandedPanel(
    replyToLabel: String?,
    value: String,
    onValueChange: (String) -> Unit,
    pendingImageUri: Uri?,
    onPendingImageChange: (Uri?) -> Unit,
    requestPickImageOnOpen: Boolean,
    onPickImageRequestHandled: () -> Unit,
    mentionCandidates: List<CommentMentionCandidate>,
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
    maxLength: Int = 500,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var showMentionSheet by remember { mutableStateOf(false) }
    var showExtendedEmojis by remember { mutableStateOf(false) }
    var showQuickEmojis by remember { mutableStateOf(true) }
    val mentionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            onPendingImageChange(uri)
        }
    }

    fun launchImagePicker() {
        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    LaunchedEffect(Unit) {
        delay(80)
        focusRequester.requestFocus()
    }

    LaunchedEffect(requestPickImageOnOpen) {
        if (requestPickImageOnOpen) {
            delay(120)
            launchImagePicker()
            onPickImageRequestHandled()
        }
    }

    if (showMentionSheet && mentionCandidates.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showMentionSheet = false },
            sheetState = mentionSheetState,
            containerColor = Color.White,
        ) {
            Text(
                text = "选择要 @ 的人",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = XhsTextPrimary,
            )
            mentionCandidates.forEach { candidate ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            appendMention(onValueChange, value, candidate.label, maxLength)
                            showMentionSheet = false
                            focusRequester.requestFocus()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    XhsProfileAvatar(label = candidate.label, size = 36)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = candidate.label,
                        fontSize = 15.sp,
                        color = XhsTextPrimary,
                    )
                }
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            .background(Color.White),
    ) {
        if (replyToLabel != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "回复 $replyToLabel",
                    fontSize = 13.sp,
                    color = XhsTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(
                    bottom = if (replyToLabel != null) 4.dp else 12.dp,
                    top = if (replyToLabel != null) 0.dp else 12.dp,
                )
                .clip(RoundedCornerShape(10.dp))
                .background(composerFieldBg),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                BasicTextField(
                    value = value,
                    onValueChange = { next ->
                        if (next.length <= maxLength) onValueChange(next)
                    },
                    enabled = !isSubmitting,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = XhsTextPrimary,
                        lineHeight = 24.sp,
                    ),
                    cursorBrush = SolidColor(XhsRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = if (pendingImageUri == null) 96.dp else 56.dp)
                        .focusRequester(focusRequester),
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    text = "说点什么...",
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 16.sp,
                                )
                            }
                            inner()
                        }
                    },
                )

                pendingImageUri?.let { uri ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Box {
                        AsyncImage(
                            model = uri,
                            contentDescription = "待发送图片",
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "移除图片",
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable { onPendingImageChange(null) }
                                .padding(2.dp),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 2.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CommentComposerToolIcon(Icons.Default.Image, "图片", onClick = ::launchImagePicker)
            CommentComposerToolIcon(Icons.Default.AlternateEmail, "@") {
                if (mentionCandidates.isEmpty()) {
                    appendMentionToken(onValueChange, value, maxLength)
                } else {
                    keyboardController?.hide()
                    showMentionSheet = true
                }
            }
            CommentComposerToolIcon(
                icon = Icons.Default.EmojiEmotions,
                description = "表情",
                active = showQuickEmojis,
            ) {
                showQuickEmojis = !showQuickEmojis
                showExtendedEmojis = false
                if (showQuickEmojis) {
                    keyboardController?.hide()
                } else {
                    focusRequester.requestFocus()
                }
            }
            CommentComposerToolIcon(
                icon = Icons.Default.Add,
                description = "更多表情",
                active = showExtendedEmojis,
            ) {
                showExtendedEmojis = !showExtendedEmojis
                showQuickEmojis = false
                keyboardController?.hide()
            }
            Spacer(modifier = Modifier.weight(1f))
            val canSend = (value.isNotBlank() || pendingImageUri != null) && !isSubmitting
            val sendBg by animateColorAsState(
                targetValue = if (canSend) XhsRed else Color(0xFFFADADD),
                label = "sendBg",
            )
            Surface(
                onClick = { if (canSend) onSubmit() },
                enabled = canSend,
                shape = RoundedCornerShape(18.dp),
                color = sendBg,
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = "发送",
                            color = if (canSend) Color.White else Color(0xFFEEA0A0),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }

        if (showQuickEmojis) {
            CommentEmojiRow(
                emojis = quickEmojis,
                value = value,
                maxLength = maxLength,
                onValueChange = onValueChange,
            )
        }

        if (showExtendedEmojis) {
            CommentExtendedEmojiPanel(
                emojis = extendedEmojis,
                value = value,
                maxLength = maxLength,
                onValueChange = onValueChange,
            )
        }

        Spacer(
            modifier = Modifier
                .navigationBarsPadding()
                .height(6.dp),
        )
    }
}

@Composable
private fun CommentEmojiRow(
    emojis: List<String>,
    value: String,
    maxLength: Int,
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        emojis.forEach { emoji ->
            Text(
                text = emoji,
                fontSize = 30.sp,
                modifier = Modifier
                    .clickable {
                        appendEmoji(onValueChange, value, emoji, maxLength)
                    }
                    .padding(vertical = 2.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommentExtendedEmojiPanel(
    emojis: List<String>,
    value: String,
    maxLength: Int,
    onValueChange: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        emojis.forEach { emoji ->
            Text(
                text = emoji,
                fontSize = 28.sp,
                modifier = Modifier
                    .clickable {
                        appendEmoji(onValueChange, value, emoji, maxLength)
                    }
                    .padding(4.dp),
            )
        }
    }
}

private fun appendEmoji(
    onValueChange: (String) -> Unit,
    value: String,
    emoji: String,
    maxLength: Int,
) {
    if (value.length + emoji.length <= maxLength) {
        onValueChange(value + emoji)
    }
}

private fun appendMentionToken(
    onValueChange: (String) -> Unit,
    value: String,
    maxLength: Int,
) {
    val token = "@"
    if (value.length + token.length <= maxLength) {
        onValueChange(value + token)
    }
}

private fun appendMention(
    onValueChange: (String) -> Unit,
    value: String,
    label: String,
    maxLength: Int,
) {
    val token = "@$label "
    if (value.length + token.length <= maxLength) {
        onValueChange(value + token)
    }
}

@Composable
private fun CommentComposerToolIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (active) XhsRed else Color(0xFF666666),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun PostDetailInteractionBar(
    likeCount: Int,
    favoriteCount: Int,
    commentCount: Int,
    isLiked: Boolean,
    isFavorited: Boolean,
    isSubmitting: Boolean,
    onCommentClick: () -> Unit,
    onLike: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 6.dp,
        color = Color.White,
    ) {
        Column {
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onCommentClick,
                        ),
                    shape = RoundedCornerShape(22.dp),
                    color = composerFieldBg,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Create,
                            contentDescription = null,
                            tint = Color(0xFFB0B0B0),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "说点什么...",
                            color = Color(0xFFAAAAAA),
                            fontSize = 14.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                PostDetailBarAction(
                    icon = {
                        Icon(
                            if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "点赞",
                            tint = if (isLiked) XhsRed else XhsTextPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    caption = if (likeCount > 0) formatSocialCount(likeCount) else "点赞",
                    captionColor = if (likeCount > 0) XhsTextPrimary else XhsTextSecondary,
                    onClick = onLike,
                    enabled = !isSubmitting,
                )
                PostDetailBarAction(
                    icon = {
                        Icon(
                            if (isFavorited) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "收藏",
                            tint = if (isFavorited) Color(0xFFFFB800) else XhsTextPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    caption = if (favoriteCount > 0) formatSocialCount(favoriteCount) else "收藏",
                    captionColor = if (favoriteCount > 0) XhsTextPrimary else XhsTextSecondary,
                    onClick = onFavorite,
                    enabled = !isSubmitting,
                )
                PostDetailBarAction(
                    icon = {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = "评论",
                            tint = XhsTextPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    caption = if (commentCount > 0) formatSocialCount(commentCount) else null,
                    captionColor = XhsTextPrimary,
                    onClick = onCommentClick,
                    enabled = !isSubmitting,
                )
            }
        }
    }
}

@Composable
private fun PostDetailBarAction(
    icon: @Composable () -> Unit,
    caption: String?,
    captionColor: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        icon()
        if (!caption.isNullOrBlank()) {
            Text(
                text = caption,
                fontSize = 12.sp,
                color = captionColor,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

private fun formatSocialCount(count: Int): String =
    if (count > 99) "99+" else count.toString()
