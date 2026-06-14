package com.lfc.consumer.ui.home

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.lfc.consumer.data.model.PostCommentsUiState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.data.model.PostCommentDto
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.data.model.displayName
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

@Composable
fun PostDetailScreen(
    post: PostDto?,
    commentsUi: PostCommentsUiState,
    currentUserLabel: String,
    currentUserAvatarUrl: String? = null,
    isLoading: Boolean,
    isSocialSubmitting: Boolean,
    onBack: () -> Unit,
    onLike: () -> Unit,
    onFavorite: () -> Unit,
    onSubmitComment: (String, Int?, Uri?) -> Unit,
    onLikeComment: (PostCommentDto) -> Unit = {},
    onLoadMoreComments: () -> Unit = {},
    onLoadMoreReplies: (Int) -> Unit = {},
    onCommentSortChange: (String) -> Unit = {},
    onAuthorClick: (Int) -> Unit = {},
    onFollowToggle: () -> Unit = {},
    isAuthorFollowing: Boolean = false,
    currentUserId: Int? = null,
    onProductClick: (Int) -> Unit = {},
    isPromotionSubmitting: Boolean = false,
    onBoost: (() -> Unit)? = null,
    postBoostActionLabel: String = "擦亮笔记",
    postBoostActiveHint: String = "擦亮期间将在推荐流优先展示",
    postBoostPriceHint: String? = null,
    postBoostBidHint: String? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onOffShelf: (() -> Unit)? = null,
    onOnShelf: (() -> Unit)? = null,
) {
    var commentInput by remember { mutableStateOf("") }
    var replyToCommentId by remember { mutableStateOf<Int?>(null) }
    var replyToLabel by remember { mutableStateOf<String?>(null) }
    var showComposer by remember { mutableStateOf(false) }
    var pendingCommentImageUri by remember { mutableStateOf<Uri?>(null) }
    var openComposerPickImage by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    fun openComposer(replyId: Int? = null, replyLabel: String? = null, pickImage: Boolean = false) {
        replyToCommentId = replyId
        replyToLabel = replyLabel
        openComposerPickImage = pickImage
        showComposer = true
    }

    fun dismissComposer() {
        keyboardController?.hide()
        focusManager.clearFocus()
        showComposer = false
        replyToCommentId = null
        replyToLabel = null
        pendingCommentImageUri = null
        openComposerPickImage = false
    }

    fun submitComment() {
        val content = commentInput.trim()
        if (content.isBlank() && pendingCommentImageUri == null) return
        onSubmitComment(content, replyToCommentId, pendingCommentImageUri)
        commentInput = ""
        dismissComposer()
    }

    BackHandler(enabled = showComposer) {
        dismissComposer()
    }

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
                val authorAvatarUrl = post.author?.avatarUrl
                val images = post.images.orEmpty()
                val displayTitle = postDisplayTitle(post)
                val displayBody = postDisplayBody(post)
                val product = post.product
                val isSelf = currentUserId != null && currentUserId == post.authorId
                val commentThreads = remember(commentsUi) { buildCommentThreads(commentsUi) }
                val commentIndex = remember(commentsUi.comments) { commentsUi.comments.associateBy { it.id } }
                val sortOrder = commentSortOrder(commentsUi.sort)
                val listState = rememberLazyListState()
                val mentionCandidates = remember(post.id, commentsUi.comments) {
                    buildCommentMentionCandidates(
                        postAuthorId = post.authorId,
                        postAuthorLabel = authorLabel,
                        comments = commentsUi.comments,
                    )
                }

                LaunchedEffect(listState, commentsUi.hasMore, commentsUi.isLoadingMore, commentThreads.size) {
                    snapshotFlow {
                        val layoutInfo = listState.layoutInfo
                        val total = layoutInfo.totalItemsCount
                        val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        total > 0 && lastVisible >= total - 3
                    }.collect { nearEnd ->
                        if (nearEnd && commentsUi.hasMore && !commentsUi.isLoadingMore && !commentsUi.isInitialLoading) {
                            onLoadMoreComments()
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    PostDetailHeader(
                        authorLabel = authorLabel,
                        authorAvatarUrl = authorAvatarUrl,
                        authorId = post.authorId,
                        currentUserId = currentUserId,
                        isAuthorFollowing = isAuthorFollowing,
                        onBack = onBack,
                        onAuthorClick = onAuthorClick,
                        onFollowToggle = onFollowToggle,
                        isSelf = isSelf,
                        isOffShelf = !post.isVisible,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        onOffShelf = onOffShelf,
                        onOnShelf = onOnShelf,
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
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
                                    latitude = post.latitude,
                                    longitude = post.longitude,
                                )
                            }

                            if (product != null) {
                                item {
                                    PostProductLinkCard(
                                        post = post,
                                        product = product,
                                        onClick = { onProductClick(post.id) },
                                    )
                                }
                            }

                            if (isSelf && onBoost != null) {
                                item {
                                    PromotionOwnerActionCard(
                                        promotion = post.promotion,
                                        actionLabel = postBoostActionLabel,
                                        activeHint = postBoostActiveHint,
                                        cooldownHint = post.promotion?.nextAvailableAt?.let {
                                            "冷却中，下次可擦亮：$it"
                                        },
                                        priceHint = postBoostPriceHint,
                                        bidHint = postBoostBidHint,
                                        isSubmitting = isPromotionSubmitting,
                                        onAction = onBoost,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    )
                                }
                            }

                            item {
                                PostCommentsHeader(
                                    commentCount = post.commentCount,
                                    sortOrder = sortOrder,
                                    onSortToggle = { onCommentSortChange(nextCommentSort(commentsUi.sort)) },
                                )
                            }
                            item {
                                PostQuickCommentTrigger(
                                    userLabel = currentUserLabel,
                                    userAvatarUrl = currentUserAvatarUrl,
                                    onClick = { openComposer() },
                                    onImageClick = { openComposer(pickImage = true) },
                                )
                            }

                            when {
                                commentsUi.isInitialLoading -> {
                                    item { PostCommentsLoadingState() }
                                }
                                commentThreads.isEmpty() -> {
                                    item { PostCommentsEmptyState() }
                                }
                                else -> {
                                    items(
                                        items = commentThreads,
                                        key = { it.root.id },
                                    ) { thread ->
                                        val isFirstComment = thread.root.id == commentThreads.first().root.id &&
                                            commentsUi.sort == "default"
                                        PostCommentThreadItem(
                                            thread = thread,
                                            postAuthorId = post.authorId,
                                            commentIndex = commentIndex,
                                            isFirstComment = isFirstComment,
                                            onReplyTo = { comment ->
                                                openComposer(
                                                    replyId = comment.id,
                                                    replyLabel = comment.author?.displayName()
                                                        ?: "同学${comment.userId}",
                                                )
                                            },
                                            onLikeComment = onLikeComment,
                                            onLoadMoreReplies = { onLoadMoreReplies(thread.root.id) },
                                        )
                                    }
                                }
                            }

                            if (commentThreads.isNotEmpty() && (commentsUi.isLoadingMore || commentsUi.hasMore)) {
                                item {
                                    PostCommentsLoadMoreFooter(isLoading = commentsUi.isLoadingMore)
                                }
                            }

                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }

                        if (showComposer) {
                            PostCommentScrim(onDismiss = ::dismissComposer)
                        }
                    }

                    Box(modifier = Modifier.imePadding()) {
                        if (showComposer) {
                            PostCommentExpandedPanel(
                                replyToLabel = replyToLabel,
                                value = commentInput,
                                onValueChange = { commentInput = it },
                                pendingImageUri = pendingCommentImageUri,
                                onPendingImageChange = { pendingCommentImageUri = it },
                                requestPickImageOnOpen = openComposerPickImage,
                                onPickImageRequestHandled = { openComposerPickImage = false },
                                mentionCandidates = mentionCandidates,
                                isSubmitting = isSocialSubmitting,
                                onSubmit = ::submitComment,
                            )
                        } else {
                            PostDetailInteractionBar(
                                likeCount = post.likeCount,
                                favoriteCount = post.favoriteCount,
                                commentCount = post.commentCount,
                                isLiked = post.isLiked,
                                isFavorited = post.isFavorited,
                                isSubmitting = isSocialSubmitting,
                                onCommentClick = { openComposer() },
                                onLike = onLike,
                                onFavorite = onFavorite,
                            )
                        }
                    }
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
    authorAvatarUrl: String?,
    authorId: Int,
    currentUserId: Int?,
    isAuthorFollowing: Boolean,
    onBack: () -> Unit,
    onAuthorClick: (Int) -> Unit,
    onFollowToggle: () -> Unit,
    isSelf: Boolean = false,
    isOffShelf: Boolean = false,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onOffShelf: (() -> Unit)? = null,
    onOnShelf: (() -> Unit)? = null,
) {
    XhsDetailAuthorHeader(
        authorLabel = authorLabel,
        authorId = authorId,
        authorAvatarUrl = authorAvatarUrl,
        onBack = onBack,
        onAuthorClick = onAuthorClick,
    ) {
        if (!isSelf) {
            XhsDetailFollowButton(
                isFollowing = isAuthorFollowing,
                onClick = onFollowToggle,
            )
        } else if (onEdit != null && onDelete != null && onOffShelf != null && onOnShelf != null) {
            OwnerContentManageButton(
                showShelfActions = true,
                isOffShelf = isOffShelf,
                contentLabel = "笔记",
                onEdit = onEdit,
                onDelete = onDelete,
                onOffShelf = onOffShelf,
                onOnShelf = onOnShelf,
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
    latitude: Double? = null,
    longitude: Double? = null,
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
            text = "发布于 ${formatXhsTime(createdAt)}",
            fontSize = 12.sp,
            color = XhsTextSecondary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        XhsDistanceLabel(
            targetLatitude = latitude,
            targetLongitude = longitude,
            fontSize = 12.sp,
            iconSize = 13.dp,
        )
    }
}
