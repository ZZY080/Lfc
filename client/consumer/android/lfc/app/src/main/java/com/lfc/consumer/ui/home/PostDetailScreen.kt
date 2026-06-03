package com.lfc.consumer.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.data.model.PostCommentDto
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.data.model.displayName
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

@Composable
fun PostDetailScreen(
    post: PostDto?,
    comments: List<PostCommentDto>,
    currentUserLabel: String,
    isLoading: Boolean,
    isCommentsLoading: Boolean,
    isSocialSubmitting: Boolean,
    onBack: () -> Unit,
    onLike: () -> Unit,
    onFavorite: () -> Unit,
    onSubmitComment: (String, Int?) -> Unit,
    onAuthorClick: (Int) -> Unit = {},
    onFollowToggle: () -> Unit = {},
    isAuthorFollowing: Boolean = false,
    currentUserId: Int? = null,
) {
    var commentInput by remember { mutableStateOf("") }
    var replyToCommentId by remember { mutableStateOf<Int?>(null) }
    var replyToLabel by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        when {
            isLoading -> XhsDetailLoading(Modifier.fillMaxSize())
            post == null -> XhsDetailEmpty("笔记不存在或已删除", Modifier.fillMaxSize())
            else -> {
                val authorLabel = post.author?.displayName() ?: "同学${post.authorId}"
                val images = post.images.orEmpty()
                val displayTitle = postDisplayTitle(post)
                val displayBody = postDisplayBody(post)

                Column(modifier = Modifier.fillMaxSize()) {
                    PostDetailHeader(
                        authorLabel = authorLabel,
                        authorId = post.authorId,
                        currentUserId = currentUserId,
                        isAuthorFollowing = isAuthorFollowing,
                        onBack = onBack,
                        onAuthorClick = onAuthorClick,
                        onFollowToggle = onFollowToggle,
                    )

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .imePadding(),
                    ) {
                        if (images.isNotEmpty()) {
                            item {
                                XhsDetailImageCarousel(
                                    images = images,
                                    contentDescription = displayTitle ?: post.title,
                                )
                            }
                        }

                        item {
                            PostDetailContentSection(
                                title = displayTitle,
                                body = displayBody,
                                createdAt = post.createdAt,
                                hasImages = images.isNotEmpty(),
                            )
                        }

                        item {
                            PostCommentsHeader(commentCount = post.commentCount)
                        }

                        item {
                            PostQuickCommentRow(
                                userLabel = currentUserLabel,
                                onClick = { /* focus handled by bottom bar */ },
                            )
                        }

                        if (isCommentsLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("加载评论中…", color = XhsTextSecondary, fontSize = 14.sp)
                                }
                            }
                        } else if (comments.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 28.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "还没有评论，快来抢沙发吧",
                                        color = XhsTextSecondary,
                                        fontSize = 14.sp,
                                    )
                                }
                            }
                        } else {
                            val topLevel = comments.filter { it.parentId == null }
                            items(topLevel, key = { it.id }) { comment ->
                                PostCommentItem(
                                    comment = comment,
                                    postAuthorId = post.authorId,
                                    replies = comments.filter { it.parentId == comment.id },
                                    onReply = {
                                        replyToCommentId = comment.id
                                        replyToLabel = comment.author?.studentId ?: "同学${comment.userId}"
                                    },
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }

                    if (replyToLabel != null) {
                        Surface(color = Color(0xFFF8F8F8)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "回复 $replyToLabel",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp,
                                    color = XhsTextSecondary,
                                )
                                Text(
                                    text = "取消",
                                    modifier = Modifier.clickable {
                                        replyToCommentId = null
                                        replyToLabel = null
                                    },
                                    fontSize = 13.sp,
                                    color = XhsRed,
                                )
                            }
                        }
                    }

                    XhsPostDetailBottomBar(
                        likeCount = post.likeCount,
                        favoriteCount = post.favoriteCount,
                        commentCount = post.commentCount,
                        isLiked = post.isLiked,
                        isFavorited = post.isFavorited,
                        commentInput = commentInput,
                        onCommentInputChange = { commentInput = it },
                        isSubmitting = isSocialSubmitting,
                        onLike = onLike,
                        onFavorite = onFavorite,
                        onSubmitComment = {
                            if (commentInput.isNotBlank()) {
                                onSubmitComment(commentInput.trim(), replyToCommentId)
                                commentInput = ""
                                replyToCommentId = null
                                replyToLabel = null
                            }
                        },
                    )
                }
            }
        }
    }
}

private fun postDisplayTitle(post: PostDto): String? {
    val title = post.title.trim()
    if (title.isBlank()) return null
    if (title in setOf("图片笔记", "校园笔记")) return null
    if (title == post.content.trim().take(30)) return null
    return title
}

private fun postDisplayBody(post: PostDto): String {
    return post.content.trim()
}

@Composable
private fun PostDetailHeader(
    authorLabel: String,
    authorId: Int,
    currentUserId: Int?,
    isAuthorFollowing: Boolean,
    onBack: () -> Unit,
    onAuthorClick: (Int) -> Unit,
    onFollowToggle: () -> Unit,
) {
    val isSelf = currentUserId != null && currentUserId == authorId
    XhsDetailAuthorHeader(
        authorLabel = authorLabel,
        authorId = authorId,
        onBack = onBack,
        onAuthorClick = onAuthorClick,
    ) {
        if (!isSelf) {
            XhsDetailFollowButton(
                isFollowing = isAuthorFollowing,
                onClick = onFollowToggle,
            )
        }

        IconButton(onClick = { }, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Default.Share,
                contentDescription = "分享",
                tint = XhsTextPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun PostDetailContentSection(
    title: String?,
    body: String,
    createdAt: String,
    hasImages: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = if (hasImages) 16.dp else 12.dp, bottom = 8.dp),
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = XhsTextPrimary,
                lineHeight = 28.sp,
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (body.isNotBlank()) {
            Text(
                text = body,
                fontSize = 16.sp,
                color = XhsTextPrimary,
                lineHeight = 26.sp,
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        Text(
            text = formatRelativeTime(createdAt),
            fontSize = 12.sp,
            color = XhsTextSecondary,
        )
    }
}

@Composable
private fun PostCommentsHeader(commentCount: Int) {
    HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 6.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "共 $commentCount 条评论",
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = XhsTextPrimary,
        )
        Icon(
            Icons.Outlined.Sort,
            contentDescription = "排序",
            tint = XhsTextSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun PostQuickCommentRow(
    userLabel: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        XhsProfileAvatar(label = userLabel, size = 32)
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFF5F5F5))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = "留下你的想法吧",
                modifier = Modifier.padding(horizontal = 14.dp),
                color = XhsTextSecondary,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun PostCommentItem(
    comment: PostCommentDto,
    postAuthorId: Int,
    replies: List<PostCommentDto>,
    onReply: () -> Unit,
) {
    val authorLabel = comment.author?.studentId ?: "同学${comment.userId}"
    val isAuthor = comment.userId == postAuthorId

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            XhsProfileAvatar(label = authorLabel, size = 34)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = authorLabel,
                        fontSize = 13.sp,
                        color = XhsTextSecondary,
                    )
                    if (isAuthor) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = XhsRed.copy(alpha = 0.1f),
                        ) {
                            Text(
                                text = "作者",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                fontSize = 10.sp,
                                color = XhsRed,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = comment.content,
                    fontSize = 15.sp,
                    color = XhsTextPrimary,
                    lineHeight = 22.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatRelativeTime(comment.createdAt),
                        fontSize = 11.sp,
                        color = XhsTextSecondary,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "回复",
                        modifier = Modifier.clickable(onClick = onReply),
                        fontSize = 12.sp,
                        color = XhsTextSecondary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Icon(
                Icons.Default.FavoriteBorder,
                contentDescription = "赞",
                tint = Color(0xFFCCCCCC),
                modifier = Modifier
                    .size(18.dp)
                    .padding(top = 2.dp),
            )
        }

        if (replies.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .padding(start = 44.dp, top = 10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFAFAFA))
                    .padding(10.dp),
            ) {
                replies.forEach { reply ->
                    PostReplyItem(
                        reply = reply,
                        postAuthorId = postAuthorId,
                    )
                    if (reply != replies.last()) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PostReplyItem(
    reply: PostCommentDto,
    postAuthorId: Int,
) {
    val replyLabel = reply.author?.studentId ?: "同学${reply.userId}"
    val isAuthor = reply.userId == postAuthorId

    Row {
        XhsProfileAvatar(label = replyLabel, size = 22)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(replyLabel, fontSize = 12.sp, color = XhsTextSecondary)
                if (isAuthor) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("作者", fontSize = 10.sp, color = XhsRed)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = reply.content,
                fontSize = 14.sp,
                color = XhsTextPrimary,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
fun XhsPostDetailBottomBar(
    likeCount: Int,
    favoriteCount: Int,
    commentCount: Int,
    isLiked: Boolean,
    isFavorited: Boolean,
    commentInput: String,
    onCommentInputChange: (String) -> Unit,
    isSubmitting: Boolean,
    onLike: () -> Unit,
    onFavorite: () -> Unit,
    onSubmitComment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 6.dp,
        color = Color.White,
    ) {
        Column {
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(Color(0xFFF5F5F5))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Create,
                        contentDescription = null,
                        tint = XhsTextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    BasicTextField(
                        value = commentInput,
                        onValueChange = onCommentInputChange,
                        enabled = !isSubmitting,
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp, color = XhsTextPrimary),
                        cursorBrush = SolidColor(XhsRed),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (commentInput.isEmpty()) {
                                    Text("说点什么...", color = XhsTextSecondary, fontSize = 14.sp)
                                }
                                inner()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                if (commentInput.isNotBlank()) {
                    IconButton(onClick = onSubmitComment, enabled = !isSubmitting) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "发送",
                            tint = XhsRed,
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                    BottomSocialChip(
                        icon = {
                            Icon(
                                if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "点赞",
                                tint = if (isLiked) XhsRed else XhsTextPrimary,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        count = likeCount,
                        onClick = onLike,
                        enabled = !isSubmitting,
                    )
                    BottomSocialChip(
                        icon = {
                            Icon(
                                if (isFavorited) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "收藏",
                                tint = if (isFavorited) Color(0xFFFFB800) else XhsTextPrimary,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        count = favoriteCount,
                        onClick = onFavorite,
                        enabled = !isSubmitting,
                    )
                    BottomSocialChip(
                        icon = {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = "评论",
                                tint = XhsTextPrimary,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        count = commentCount,
                        onClick = {},
                        enabled = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomSocialChip(
    icon: @Composable () -> Unit,
    count: Int,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp),
    ) {
        icon()
        if (count > 0) {
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = formatSocialCount(count),
                fontSize = 13.sp,
                color = XhsTextPrimary,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private fun formatSocialCount(count: Int): String = when {
    count <= 0 -> ""
    count < 10000 -> count.toString()
    else -> String.format("%.1fw", count / 10000f)
}

fun formatRelativeTime(iso: String): String {
    return try {
        val cleaned = iso.replace(" ", "T").substringBefore(".").substringBefore("+").take(19)
        val local = java.time.LocalDateTime.parse(cleaned)
        val instant = local.atZone(java.time.ZoneId.systemDefault()).toInstant()
        val now = java.time.Instant.now()
        val minutes = java.time.Duration.between(instant, now).toMinutes()
        when {
            minutes < 1 -> "刚刚"
            minutes < 60 -> "${minutes}分钟前"
            minutes < 60 * 24 -> "${minutes / 60}小时前"
            minutes < 60 * 24 * 7 -> "${minutes / (60 * 24)}天前"
            else -> formatXhsTime(iso).take(10)
        }
    } catch (_: Exception) {
        formatXhsTime(iso).take(16)
    }
}
