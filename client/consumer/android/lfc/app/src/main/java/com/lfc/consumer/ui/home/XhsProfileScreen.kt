package com.lfc.consumer.ui.home

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.data.model.ProfileCommentDto
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

enum class XhsProfileMode { Self, Other }

data class XhsProfileData(
    val userId: Int,
    val lfcNo: String,
    val displayName: String,
    val bio: String = "莲峰校园 · 记录校园生活",
    val avatarUrl: String? = null,
    val coverUrl: String? = null,
    val postCount: Int = 0,
    val followingCount: Int = 0,
    val followerCount: Int = 0,
    val likeAndFavoriteCount: Int = 0,
    val posts: List<PostDto> = emptyList(),
    val activities: List<ActivityDto> = emptyList(),
    val participationCount: Int = 0,
    val isFollowing: Boolean = false,
    val showCommentsPublic: Boolean = false,
    val showFavoritesPublic: Boolean = false,
    val showLikesPublic: Boolean = false,
)

private val profileTabLabels = listOf("笔记", "活动", "评论", "收藏", "赞")

private fun XhsProfileData.isTabLocked(tabIndex: Int): Boolean = when (tabIndex) {
    2 -> !showCommentsPublic
    3 -> !showFavoritesPublic
    4 -> !showLikesPublic
    else -> false
}

private fun XhsProfileData.isTabAccessible(mode: XhsProfileMode, tabIndex: Int): Boolean {
    if (mode == XhsProfileMode.Self || tabIndex !in 2..4) return true
    return when (tabIndex) {
        2 -> showCommentsPublic
        3 -> showFavoritesPublic
        4 -> showLikesPublic
        else -> true
    }
}

private val profileCoverDark = Color(0xFF1A2329)
private val topBarHeight = 48.dp

fun formatProfileDate(iso: String): String {
    val normalized = iso.replace("T", " ").take(10)
    return if (normalized.length >= 10) normalized else iso
}

@Composable
fun XhsProfileScreen(
    profile: XhsProfileData?,
    mode: XhsProfileMode,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    profileFavoritePosts: List<PostDto> = emptyList(),
    profileLikedPosts: List<PostDto> = emptyList(),
    profileComments: List<ProfileCommentDto> = emptyList(),
    isLibraryLoading: Boolean = false,
    onBack: (() -> Unit)? = null,
    onPostClick: (Int) -> Unit,
    onActivityClick: (Int) -> Unit = {},
    onMessage: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    onEditProfile: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onShowQr: (() -> Unit)? = null,
    onScanProfile: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
    onGoToMessages: (() -> Unit)? = null,
    unreadCount: Int = 0,
    onFollowToggle: (() -> Unit)? = null,
    onTabSelected: ((Int) -> Unit)? = null,
    onEditPost: ((PostDto) -> Unit)? = null,
    onDeletePost: ((Int) -> Unit)? = null,
    onEditActivity: ((ActivityDto) -> Unit)? = null,
    onDeleteActivity: ((Int) -> Unit)? = null,
    onOpenSideMenu: (() -> Unit)? = null,
    selectedContentTab: Int? = null,
    onContentTabChange: ((Int) -> Unit)? = null,
) {
    var internalTab by remember { mutableIntStateOf(0) }
    val selectedTab = selectedContentTab ?: internalTab
    fun updateSelectedTab(tab: Int) {
        if (onContentTabChange != null) {
            onContentTabChange(tab)
        } else {
            internalTab = tab
        }
    }
    val view = LocalView.current

    DisposableEffect(Unit) {
        if (!view.isInEditMode) {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            val previous = controller.isAppearanceLightStatusBars
            controller.isAppearanceLightStatusBars = false
            onDispose { controller.isAppearanceLightStatusBars = previous }
        } else {
            onDispose { }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
    ) {
        when {
            isLoading -> XhsDetailLoading(Modifier.fillMaxSize())
            profile == null -> XhsDetailEmpty("用户不存在", Modifier.fillMaxSize())
            else -> {
                val gridState = rememberLazyStaggeredGridState()
                val isCollapsed by remember {
                    derivedStateOf {
                        gridState.firstVisibleItemIndex > 0 ||
                            (gridState.firstVisibleItemIndex == 0 &&
                                gridState.firstVisibleItemScrollOffset > 120)
                    }
                }
                val showStickyTabs by remember {
                    derivedStateOf { gridState.firstVisibleItemIndex >= 1 }
                }

                LazyVerticalStaggeredGrid(
                    state = gridState,
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalItemSpacing = 2.dp,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        XhsProfileHeaderBlock(
                            profile = profile,
                            mode = mode,
                            onMessage = onMessage,
                            onFollowToggle = onFollowToggle,
                            selectedTab = selectedTab,
                            onTabSelected = {
                                updateSelectedTab(it)
                                onTabSelected?.invoke(it)
                            },
                            onSearch = onSearch,
                            showInlineTabs = !showStickyTabs,
                            onShowMyQr = if (mode == XhsProfileMode.Self) onShowQr else null,
                        )
                    }

                    when (selectedTab) {
                        0 -> renderNotesTab(
                            posts = profile.posts,
                            authorLabel = profile.displayName,
                            avatarUrl = profile.avatarUrl,
                            isSelf = mode == XhsProfileMode.Self,
                            onPostClick = onPostClick,
                            onEditPost = onEditPost,
                            onDeletePost = onDeletePost,
                        )
                        1 -> renderActivitiesTab(
                            activities = profile.activities,
                            isSelf = mode == XhsProfileMode.Self,
                            onActivityClick = onActivityClick,
                            onEditActivity = onEditActivity,
                            onDeleteActivity = onDeleteActivity,
                        )
                        2, 3, 4 -> {
                            if (profile.isTabAccessible(mode, selectedTab)) {
                                when (selectedTab) {
                                    2 -> renderCommentsTab(
                                        comments = profileComments,
                                        isLoading = isLibraryLoading,
                                        onPostClick = onPostClick,
                                    )
                                    3 -> renderPostLibraryTab(
                                        posts = profileFavoritePosts,
                                        authorLabel = profile.displayName,
                                        avatarUrl = profile.avatarUrl,
                                        emptyHint = "还没有收藏内容",
                                        isLoading = isLibraryLoading,
                                        onPostClick = onPostClick,
                                    )
                                    else -> renderPostLibraryTab(
                                        posts = profileLikedPosts,
                                        authorLabel = profile.displayName,
                                        avatarUrl = profile.avatarUrl,
                                        emptyHint = "还没有赞过内容",
                                        isLoading = isLibraryLoading,
                                        onPostClick = onPostClick,
                                    )
                                }
                            } else {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    XhsProfileLockedHint(isSelf = false)
                                }
                            }
                        }
                    }
                }

                XhsProfileTopOverlay(
                    profile = profile,
                    mode = mode,
                    isCollapsed = isCollapsed || showStickyTabs,
                    onBack = onBack,
                    onMessage = onMessage,
                    onEditProfile = onEditProfile,
                    onShare = onShare,
                    onScanProfile = onScanProfile,
                    onMenuClick = if (mode == XhsProfileMode.Self) onOpenSideMenu else null,
                )

                if (showStickyTabs) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                    ) {
                        Spacer(
                            modifier = Modifier
                                .statusBarsPadding()
                                .height(topBarHeight),
                        )
                        XhsProfileTabBar(
                            profile = profile,
                            selectedTab = selectedTab,
                            onTabSelected = {
                                updateSelectedTab(it)
                                onTabSelected?.invoke(it)
                            },
                            onSearch = onSearch,
                            modifier = Modifier.background(Color.White),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSideMenuOverlay(
    visible: Boolean,
    profile: XhsProfileData?,
    unreadCount: Int,
    onDismiss: () -> Unit,
    onScan: () -> Unit,
    onShowMyQr: () -> Unit,
    onSettings: () -> Unit,
    onEditProfile: () -> Unit,
    onShare: () -> Unit,
    onSearch: () -> Unit,
    onLogout: () -> Unit,
    onSelectProfileTab: (Int) -> Unit,
    onGoToMessages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var holdOverlay by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            holdOverlay = true
        }
    }

    if (!visible && !holdOverlay) return

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        ProfileSideMenu(
            visible = visible,
            profile = profile,
            unreadCount = unreadCount,
            onDismiss = onDismiss,
            onScan = onScan,
            onShowMyQr = onShowMyQr,
            onSettings = onSettings,
            onEditProfile = onEditProfile,
            onShare = onShare,
            onSearch = onSearch,
            onLogout = onLogout,
            onSelectProfileTab = onSelectProfileTab,
            onGoToMessages = onGoToMessages,
            onFullyHidden = { holdOverlay = false },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope.renderNotesTab(
    posts: List<PostDto>,
    authorLabel: String,
    avatarUrl: String? = null,
    isSelf: Boolean,
    onPostClick: (Int) -> Unit,
    onEditPost: ((PostDto) -> Unit)?,
    onDeletePost: ((Int) -> Unit)?,
) {
    if (posts.isEmpty()) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 56.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("还没有发布笔记", color = XhsTextSecondary, fontSize = 14.sp)
            }
        }
    } else {
        items(posts, key = { it.id }) { post ->
            var showMenu by remember(post.id) { mutableStateOf(false) }

            Box(
                modifier = if (isSelf && onEditPost != null && onDeletePost != null) {
                    Modifier.combinedClickable(
                        onClick = { onPostClick(post.id) },
                        onLongClick = { showMenu = true },
                    )
                } else {
                    Modifier.clickable { onPostClick(post.id) }
                },
            ) {
                XhsProfileFeedCard(
                    post = post,
                    authorLabel = authorLabel,
                    isPinned = isSelf && post.id == posts.firstOrNull()?.id,
                )
                if (isSelf && onEditPost != null && onDeletePost != null) {
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("编辑") },
                            onClick = {
                                showMenu = false
                                onEditPost(post)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("删除") },
                            onClick = {
                                showMenu = false
                                onDeletePost(post.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope.renderPostLibraryTab(
    posts: List<PostDto>,
    authorLabel: String,
    avatarUrl: String?,
    emptyHint: String,
    isLoading: Boolean,
    onPostClick: (Int) -> Unit,
) {
    if (isLoading) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.CircularProgressIndicator(color = XhsRed)
            }
        }
    } else if (posts.isEmpty()) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 56.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(emptyHint, color = XhsTextSecondary, fontSize = 14.sp)
            }
        }
    } else {
        items(posts, key = { it.id }) { post ->
            Box(modifier = Modifier.clickable { onPostClick(post.id) }) {
                XhsProfileFeedCard(
                    post = post,
                    authorLabel = post.author?.studentId ?: authorLabel,
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope.renderCommentsTab(
    comments: List<ProfileCommentDto>,
    isLoading: Boolean,
    onPostClick: (Int) -> Unit,
) {
    if (isLoading) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.CircularProgressIndicator(color = XhsRed)
            }
        }
    } else if (comments.isEmpty()) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 56.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("还没有发表评论", color = XhsTextSecondary, fontSize = 14.sp)
            }
        }
    } else {
        items(comments, key = { it.id }) { comment ->
            val post = comment.post
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPostClick(comment.postId) },
                color = Color.White,
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        comment.content,
                        fontSize = 14.sp,
                        color = XhsTextPrimary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = post?.title ?: "查看原笔记",
                        fontSize = 12.sp,
                        color = XhsTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatProfileDate(comment.createdAt),
                        fontSize = 11.sp,
                        color = XhsTextSecondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope.renderActivitiesTab(
    activities: List<ActivityDto>,
    isSelf: Boolean,
    onActivityClick: (Int) -> Unit,
    onEditActivity: ((ActivityDto) -> Unit)?,
    onDeleteActivity: ((Int) -> Unit)?,
) {
    if (activities.isEmpty()) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 56.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("还没有发布活动", color = XhsTextSecondary, fontSize = 14.sp)
            }
        }
    } else {
        items(activities, key = { it.id }, span = { StaggeredGridItemSpan.FullLine }) { activity ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onActivityClick(activity.id) },
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 72.dp, height = 64.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(coverGradientForId(activity.id)),
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp),
                    ) {
                        Text(
                            activity.title,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 14.sp,
                        )
                        Text(
                            activity.location,
                            fontSize = 12.sp,
                            color = XhsTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun XhsProfileHeaderBlock(
    profile: XhsProfileData,
    mode: XhsProfileMode,
    onMessage: (() -> Unit)?,
    onFollowToggle: (() -> Unit)?,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onSearch: (() -> Unit)?,
    showInlineTabs: Boolean,
    onShowMyQr: (() -> Unit)? = null,
) {
    val coverUrl = profile.coverUrl ?: profile.posts.firstNotNullOfOrNull { it.images?.firstOrNull() }
    val coverOverlay = Brush.verticalGradient(
        colors = listOf(
            Color.Black.copy(alpha = 0.15f),
            Color.Black.copy(alpha = 0.35f),
            Color.Black.copy(alpha = 0.72f),
        ),
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(248.dp),
        ) {
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF3D5360),
                                    Color(0xFF243038),
                                    profileCoverDark,
                                ),
                            ),
                        ),
                )
            }
            Box(modifier = Modifier.fillMaxSize().background(coverOverlay))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = topBarHeight + 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    XhsProfileAvatar(
                        label = profile.displayName,
                        size = 68,
                        avatarUrl = profile.avatarUrl,
                    )
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text(
                            text = profile.displayName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "莲峰号：${profile.lfcNo}",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp,
                            )
                            if (onShowMyQr != null) {
                                IconButton(
                                    onClick = onShowMyQr,
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        Icons.Default.QrCode2,
                                        contentDescription = "我的二维码",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    XhsProfileStatWhite(count = profile.followingCount, label = "关注")
                    XhsProfileStatWhite(count = profile.followerCount, label = "粉丝")
                    XhsProfileStatWhite(count = profile.likeAndFavoriteCount, label = "获赞与收藏")
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = profile.bio,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (mode == XhsProfileMode.Other && onFollowToggle != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        onClick = onFollowToggle,
                        shape = RoundedCornerShape(16.dp),
                        color = if (profile.isFollowing) {
                            Color.White.copy(alpha = 0.18f)
                        } else {
                            XhsRed
                        },
                    ) {
                        Text(
                            text = if (profile.isFollowing) "已关注" else "+ 关注",
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                    }
                } else if (mode == XhsProfileMode.Self) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        XhsProfileTagChip("校园用户")
                        if (profile.participationCount > 0) {
                            XhsProfileTagChip("已报名 ${profile.participationCount}")
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(profileCoverDark)
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            XhsProfileSummaryCard(
                title = "笔记",
                value = "${profile.postCount}",
                modifier = Modifier.weight(1f),
            )
            XhsProfileSummaryCard(
                title = "活动",
                value = "${profile.activities.size}",
                modifier = Modifier.weight(1f),
            )
            if (mode == XhsProfileMode.Other && onMessage != null) {
                XhsProfileSummaryCard(
                    title = "私信",
                    value = "联系",
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onMessage),
                )
            } else {
                XhsProfileSummaryCard(
                    title = "报名",
                    value = "${profile.participationCount}",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-10).dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(Color.White),
        ) {
            if (showInlineTabs) {
                XhsProfileTabBar(
                    profile = profile,
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    onSearch = onSearch,
                )
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun XhsProfileTopOverlay(
    profile: XhsProfileData,
    mode: XhsProfileMode,
    isCollapsed: Boolean,
    onBack: (() -> Unit)?,
    onMessage: (() -> Unit)?,
    onEditProfile: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onScanProfile: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isCollapsed) profileCoverDark else Color.Transparent)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(topBarHeight)
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (mode == XhsProfileMode.Other && onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            } else {
                IconButton(onClick = { onMenuClick?.invoke() }) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "菜单",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            if (isCollapsed) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    XhsProfileAvatar(
                        label = profile.displayName,
                        size = 26,
                        avatarUrl = profile.avatarUrl,
                    )
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            if (mode == XhsProfileMode.Self) {
                Surface(
                    onClick = { onEditProfile?.invoke() },
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.28f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "编辑主页",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { onScanProfile?.invoke() }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = "扫一扫",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp),
                    )
                }
            } else if (onMessage != null) {
                Surface(
                    onClick = onMessage,
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.28f),
                ) {
                    Text(
                        text = "私信",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                    )
                }
            }

            IconButton(onClick = { onShare?.invoke() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Share, contentDescription = "分享", tint = Color.White, modifier = Modifier.size(19.dp))
            }
        }
    }
}

@Composable
private fun XhsProfileTabBar(
    profile: XhsProfileData,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                profileTabLabels.forEachIndexed { index, label ->
                    val selected = selectedTab == index
                    val locked = profile.isTabLocked(index)
                    Column(
                        modifier = Modifier
                            .clickable { onTabSelected(index) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (locked) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = XhsTextSecondary.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(10.dp)
                                        .padding(end = 2.dp),
                                )
                            }
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) XhsTextPrimary else XhsTextSecondary,
                            )
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .width(if (selected) 22.dp else 0.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (selected) XhsRed else Color.Transparent),
                        )
                    }
                }
            }
            IconButton(onClick = { onSearch?.invoke() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Search, contentDescription = "搜索", tint = XhsTextSecondary, modifier = Modifier.size(18.dp))
            }
        }
        HorizontalDivider(color = Color(0xFFEFEFEF), thickness = 0.5.dp)
    }
}

@Composable
private fun XhsProfileLockedHint(isSelf: Boolean = true) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 56.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = XhsTextSecondary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isSelf) "仅自己可见" else "该用户设置了隐私",
                color = XhsTextSecondary,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun XhsProfileStatWhite(count: Int, label: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = formatProfileStatCount(count),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
        Text(
            text = " $label",
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 1.dp),
        )
    }
}

private fun formatProfileStatCount(count: Int): String = when {
    count < 10000 -> count.toString()
    else -> String.format("%.1fw", count / 10000f)
}

@Composable
private fun XhsProfileTagChip(text: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.14f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun XhsProfileSummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(0.dp),
        color = Color.White.copy(alpha = 0.08f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp)
            Text(
                value,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
