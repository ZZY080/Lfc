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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PersonAddAlt1
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.snapshotFlow
import com.lfc.consumer.data.model.ProfileTabUiState
import com.lfc.consumer.data.model.ProfileTabsUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.data.model.ProfileCommentDto
import com.lfc.consumer.data.model.displayName
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
    val alipayBound: Boolean = false,
    val alipayLoginIdMasked: String? = null,
)

private val profileTabLabels = listOf("笔记", "活动", "评论", "收藏", "赞")

private const val PROFILE_FAVORITES_TAB = 3
private const val PROFILE_LIKES_TAB = 4

sealed interface ProfileLibraryFeedItem {
    data class PostItem(val post: PostDto) : ProfileLibraryFeedItem
    data class ActivityItem(val activity: ActivityDto) : ProfileLibraryFeedItem
}

fun combineProfileLibraryTabState(
    posts: ProfileTabUiState,
    activities: ProfileTabUiState,
): ProfileTabUiState = ProfileTabUiState(
    page = maxOf(posts.page, activities.page),
    hasMore = posts.hasMore || activities.hasMore,
    isRefreshing = posts.isRefreshing || activities.isRefreshing,
    isLoadingMore = posts.isLoadingMore || activities.isLoadingMore,
    isInitialLoading = posts.isInitialLoading || activities.isInitialLoading,
)

fun profileTabUiStateFor(
    selectedTab: Int,
    profileTabs: ProfileTabsUiState,
): ProfileTabUiState {
    return when (selectedTab) {
        PROFILE_FAVORITES_TAB -> combineProfileLibraryTabState(
            posts = profileTabs.tabs.getOrElse(PROFILE_FAVORITES_TAB) { ProfileTabUiState() },
            activities = profileTabs.favoriteActivities,
        )
        PROFILE_LIKES_TAB -> combineProfileLibraryTabState(
            posts = profileTabs.tabs.getOrElse(PROFILE_LIKES_TAB) { ProfileTabUiState() },
            activities = profileTabs.likedActivities,
        )
        else -> profileTabs.tabs.getOrElse(selectedTab) { ProfileTabUiState() }
    }
}

fun buildProfileLibraryFeed(
    posts: List<PostDto>,
    activities: List<ActivityDto>,
): List<ProfileLibraryFeedItem> {
    return (posts.map { ProfileLibraryFeedItem.PostItem(it) } +
        activities.map { ProfileLibraryFeedItem.ActivityItem(it) })
        .sortedByDescending { item ->
            when (item) {
                is ProfileLibraryFeedItem.PostItem -> item.post.createdAt
                is ProfileLibraryFeedItem.ActivityItem -> item.activity.createdAt
            }
        }
}

private fun ProfileLibraryFeedItem.stableKey(): String = when (this) {
    is ProfileLibraryFeedItem.PostItem -> "post-${post.id}"
    is ProfileLibraryFeedItem.ActivityItem -> "activity-${activity.id}"
}

private data class ProfileTabItem(val label: String, val contentIndex: Int)

private fun profileTabsFor(mode: XhsProfileMode): List<ProfileTabItem> = when (mode) {
    XhsProfileMode.Self -> profileTabLabels.mapIndexed { index, label ->
        ProfileTabItem(label, index)
    }
    XhsProfileMode.Other -> listOf(
        ProfileTabItem("笔记", 0),
        ProfileTabItem("活动", 1),
        ProfileTabItem("收藏", PROFILE_FAVORITES_TAB),
    )
}

private fun XhsProfileData.isTabLocked(tabIndex: Int): Boolean = when (tabIndex) {
    2 -> !showCommentsPublic
    PROFILE_FAVORITES_TAB -> !showFavoritesPublic
    PROFILE_LIKES_TAB -> !showLikesPublic
    else -> false
}

private fun XhsProfileData.isTabAccessible(mode: XhsProfileMode, tabIndex: Int): Boolean {
    if (mode == XhsProfileMode.Self || tabIndex !in 2..PROFILE_LIKES_TAB) return true
    return when (tabIndex) {
        2 -> showCommentsPublic
        PROFILE_FAVORITES_TAB -> showFavoritesPublic
        PROFILE_LIKES_TAB -> showLikesPublic
        else -> true
    }
}

private fun profileTabEmptyMessage(tabIndex: Int): String = when (tabIndex) {
    0 -> "还没有发布笔记"
    1 -> "还没有发布活动"
    2 -> "还没有发表评论"
    PROFILE_FAVORITES_TAB -> "还没有收藏内容"
    PROFILE_LIKES_TAB -> "还没有赞过内容"
    else -> ""
}

private fun isProfileTabContentEmpty(
    tabIndex: Int,
    profileNotes: List<PostDto>,
    profileActivities: List<ActivityDto>,
    profileComments: List<ProfileCommentDto>,
    profileFavoritePosts: List<PostDto>,
    profileFavoriteActivities: List<ActivityDto>,
    profileLikedPosts: List<PostDto>,
    profileLikedActivities: List<ActivityDto>,
): Boolean = when (tabIndex) {
    0 -> profileNotes.isEmpty()
    1 -> profileActivities.isEmpty()
    2 -> profileComments.isEmpty()
    PROFILE_FAVORITES_TAB -> profileFavoritePosts.isEmpty() && profileFavoriteActivities.isEmpty()
    PROFILE_LIKES_TAB -> profileLikedPosts.isEmpty() && profileLikedActivities.isEmpty()
    else -> true
}

private val profileCoverDark = Color(0xFF1A2329)
private val profileGridHorizontalPadding = 8.dp
private val profileGridItemSpacing = 8.dp
private val topBarHeight = 48.dp

private fun Modifier.breakOutHorizontalPadding(padding: Dp): Modifier = layout { measurable, constraints ->
    val paddingPx = padding.roundToPx()
    val extraWidth = paddingPx * 2
    val placeable = measurable.measure(
        Constraints(
            minWidth = 0,
            maxWidth = constraints.maxWidth + extraWidth,
            minHeight = 0,
            maxHeight = constraints.maxHeight,
        ),
    )
    layout(constraints.maxWidth, placeable.height) {
        placeable.placeRelative(-paddingPx, 0)
    }
}

fun formatProfileDate(iso: String): String {
    val normalized = iso.replace("T", " ").take(10)
    return if (normalized.length >= 10) normalized else iso
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XhsProfileScreen(
    profile: XhsProfileData?,
    mode: XhsProfileMode,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    profileNotes: List<PostDto> = emptyList(),
    profileActivities: List<ActivityDto> = emptyList(),
    profileFavoritePosts: List<PostDto> = emptyList(),
    profileFavoriteActivities: List<ActivityDto> = emptyList(),
    profileLikedPosts: List<PostDto> = emptyList(),
    profileLikedActivities: List<ActivityDto> = emptyList(),
    profileComments: List<ProfileCommentDto> = emptyList(),
    tabUiState: ProfileTabUiState = ProfileTabUiState(),
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    onPostClick: (Int) -> Unit,
    onActivityClick: (Int) -> Unit = {},
    onMessage: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    onEditProfile: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onShowQr: (() -> Unit)? = null,
    onScanProfile: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
    onBindAlipay: (() -> Unit)? = null,
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
    fun selectTab(tab: Int) {
        if (onContentTabChange != null) {
            onContentTabChange(tab)
        } else {
            internalTab = tab
            onTabSelected?.invoke(tab)
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
                val tabLocked = selectedTab in 2..PROFILE_LIKES_TAB &&
                    !profile.isTabAccessible(mode, selectedTab)
                val isTabEmpty = isProfileTabContentEmpty(
                    tabIndex = selectedTab,
                    profileNotes = profileNotes,
                    profileActivities = profileActivities,
                    profileComments = profileComments,
                    profileFavoritePosts = profileFavoritePosts,
                    profileFavoriteActivities = profileFavoriteActivities,
                    profileLikedPosts = profileLikedPosts,
                    profileLikedActivities = profileLikedActivities,
                )
                val showTabPlaceholder = tabLocked ||
                    tabUiState.isInitialLoading ||
                    (isTabEmpty && !tabUiState.isLoadingMore)
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

                LaunchedEffect(selectedTab) {
                    gridState.scrollToItem(0)
                }

                LaunchedEffect(gridState, tabUiState.hasMore, tabUiState.isLoadingMore, selectedTab) {
                    snapshotFlow {
                        val info = gridState.layoutInfo
                        val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                        lastVisible to info.totalItemsCount
                    }
                        .distinctUntilChanged()
                        .collect { (lastVisible, total) ->
                            if (
                                !tabLocked &&
                                total > 0 &&
                                lastVisible >= total - 3 &&
                                tabUiState.hasMore &&
                                !tabUiState.isLoadingMore &&
                                !tabUiState.isRefreshing
                            ) {
                                onLoadMore()
                            }
                        }
                }

                val pullRefreshState = rememberPullToRefreshState()

                PullToRefreshBox(
                    state = pullRefreshState,
                    isRefreshing = tabUiState.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            isRefreshing = tabUiState.isRefreshing,
                            state = pullRefreshState,
                            modifier = Modifier.align(Alignment.TopCenter),
                            color = XhsRed,
                        )
                    },
                ) {
                    LazyVerticalStaggeredGrid(
                        state = gridState,
                        columns = StaggeredGridCells.Fixed(2),
                        contentPadding = PaddingValues(
                            start = profileGridHorizontalPadding,
                            end = profileGridHorizontalPadding,
                            bottom = 16.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(profileGridItemSpacing),
                        verticalItemSpacing = profileGridItemSpacing,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .breakOutHorizontalPadding(profileGridHorizontalPadding),
                            ) {
                                XhsProfileHeaderBlock(
                                    profile = profile,
                                    mode = mode,
                                    onMessage = onMessage,
                                    onFollowToggle = onFollowToggle,
                                    onShare = onShare,
                                    selectedTab = selectedTab,
                                    onTabSelected = { selectTab(it) },
                                    showInlineTabs = showTabPlaceholder || !showStickyTabs,
                                    onShowMyQr = if (mode == XhsProfileMode.Self) onShowQr else null,
                                )
                            }
                        }

                        if (mode == XhsProfileMode.Self && !profile.alipayBound && onBindAlipay != null) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                AlipaySetupBanner(
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                    onBindClick = onBindAlipay,
                                )
                            }
                        }

                        if (showTabPlaceholder) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                ProfileTabPlaceholderItem(
                                    tabLocked = tabLocked,
                                    isInitialLoading = tabUiState.isInitialLoading,
                                    emptyMessage = profileTabEmptyMessage(selectedTab),
                                )
                            }
                        } else when (selectedTab) {
                            0 -> renderNotesTab(
                                posts = profileNotes,
                                authorLabel = profile.displayName,
                                avatarUrl = profile.avatarUrl,
                                isSelf = mode == XhsProfileMode.Self,
                                onPostClick = onPostClick,
                                onEditPost = onEditPost,
                                onDeletePost = onDeletePost,
                            )
                            1 -> renderActivitiesTab(
                                activities = profileActivities,
                                authorLabel = profile.displayName,
                                avatarUrl = profile.avatarUrl,
                                isSelf = mode == XhsProfileMode.Self,
                                onActivityClick = onActivityClick,
                                onEditActivity = onEditActivity,
                                onDeleteActivity = onDeleteActivity,
                            )
                            2 -> renderCommentsTab(
                                comments = profileComments,
                                onPostClick = onPostClick,
                            )
                            3 -> renderLibraryTab(
                                items = buildProfileLibraryFeed(
                                    posts = profileFavoritePosts,
                                    activities = profileFavoriteActivities,
                                ),
                                onPostClick = onPostClick,
                                onActivityClick = onActivityClick,
                            )
                            4 -> renderLibraryTab(
                                items = buildProfileLibraryFeed(
                                    posts = profileLikedPosts,
                                    activities = profileLikedActivities,
                                ),
                                onPostClick = onPostClick,
                                onActivityClick = onActivityClick,
                            )
                        }

                        if (tabUiState.isLoadingMore) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        color = XhsRed,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                XhsProfileTopOverlay(
                    profile = profile,
                    mode = mode,
                    isCollapsed = !showTabPlaceholder && (isCollapsed || showStickyTabs),
                    onBack = onBack,
                    onMessage = onMessage,
                    onEditProfile = onEditProfile,
                    onShare = onShare,
                    onScanProfile = onScanProfile,
                    onMenuClick = if (mode == XhsProfileMode.Self) onOpenSideMenu else null,
                )

                if (!showTabPlaceholder && showStickyTabs) {
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
                            tabs = profileTabsFor(mode),
                            profile = profile,
                            selectedTab = selectedTab,
                            onTabSelected = { selectTab(it) },
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
    onOrders: () -> Unit,
    onBindAlipay: () -> Unit,
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
            onOrders = onOrders,
            onBindAlipay = onBindAlipay,
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

@Composable
private fun LazyStaggeredGridItemScope.ProfileTabPlaceholderItem(
    tabLocked: Boolean,
    isInitialLoading: Boolean,
    emptyMessage: String,
) {
    val minHeight = (LocalConfiguration.current.screenHeightDp * 0.42f).dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isInitialLoading -> CircularProgressIndicator(color = XhsRed)
            tabLocked -> XhsProfileLockedHint(isSelf = false)
            else -> Text(
                text = emptyMessage,
                color = XhsTextSecondary,
                fontSize = 14.sp,
            )
        }
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
                authorLabel = post.author?.displayName() ?: authorLabel,
                avatarUrl = post.author?.avatarUrl ?: avatarUrl,
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

private fun androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope.renderLibraryTab(
    items: List<ProfileLibraryFeedItem>,
    onPostClick: (Int) -> Unit,
    onActivityClick: (Int) -> Unit,
) {
    items(items, key = { it.stableKey() }) { item ->
        when (item) {
            is ProfileLibraryFeedItem.PostItem -> {
                Box(modifier = Modifier.clickable { onPostClick(item.post.id) }) {
                    XhsProfileFeedCard(
                        post = item.post,
                        authorLabel = item.post.author?.displayName()
                            ?: "同学${item.post.authorId}",
                        avatarUrl = item.post.author?.avatarUrl,
                    )
                }
            }
            is ProfileLibraryFeedItem.ActivityItem -> {
                XhsProfileActivityCard(
                    activity = item.activity,
                    authorLabel = item.activity.author?.displayName()
                        ?: "同学${item.activity.authorId}",
                    avatarUrl = item.activity.author?.avatarUrl,
                    modifier = Modifier.clickable { onActivityClick(item.activity.id) },
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope.renderCommentsTab(
    comments: List<ProfileCommentDto>,
    onPostClick: (Int) -> Unit,
) {
    items(comments, key = { it.id }) { comment ->
        val post = comment.post
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPostClick(comment.postId) },
            shape = RoundedCornerShape(10.dp),
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

@OptIn(ExperimentalFoundationApi::class)
private fun androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope.renderActivitiesTab(
    activities: List<ActivityDto>,
    authorLabel: String,
    avatarUrl: String? = null,
    isSelf: Boolean,
    onActivityClick: (Int) -> Unit,
    onEditActivity: ((ActivityDto) -> Unit)?,
    onDeleteActivity: ((Int) -> Unit)?,
) {
    items(activities, key = { it.id }) { activity ->
        var showMenu by remember(activity.id) { mutableStateOf(false) }

        Box(
            modifier = if (isSelf && onEditActivity != null && onDeleteActivity != null) {
                Modifier.combinedClickable(
                    onClick = { onActivityClick(activity.id) },
                    onLongClick = { showMenu = true },
                )
            } else {
                Modifier.clickable { onActivityClick(activity.id) }
            },
        ) {
            XhsProfileActivityCard(
                activity = activity,
                authorLabel = activity.author?.displayName() ?: authorLabel,
                avatarUrl = activity.author?.avatarUrl ?: avatarUrl,
            )
            if (isSelf && onEditActivity != null && onDeleteActivity != null) {
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        onClick = {
                            showMenu = false
                            onEditActivity(activity)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("删除") },
                        onClick = {
                            showMenu = false
                            onDeleteActivity(activity.id)
                        },
                    )
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
    onShare: (() -> Unit)?,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    showInlineTabs: Boolean,
    onShowMyQr: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val coverUrl = profile.coverUrl ?: profile.posts.firstNotNullOfOrNull { it.images?.firstOrNull() }
    val coverHeight = if (mode == XhsProfileMode.Other) 300.dp else 248.dp
    val sheetCorner = if (mode == XhsProfileMode.Other) 18.dp else 14.dp
    val coverOverlay = Brush.verticalGradient(
        colors = listOf(
            Color.Black.copy(alpha = 0.08f),
            Color.Black.copy(alpha = 0.28f),
            Color.Black.copy(alpha = 0.72f),
        ),
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(coverHeight),
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
                        size = if (mode == XhsProfileMode.Other) 72 else 68,
                        avatarUrl = profile.avatarUrl,
                    )
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text(
                            text = profile.displayName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (mode == XhsProfileMode.Other) 22.sp else 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "莲峰号：${profile.lfcNo}",
                                color = Color.White.copy(alpha = 0.78f),
                                fontSize = 12.sp,
                            )
                            if (mode == XhsProfileMode.Other) {
                                IconButton(
                                    onClick = { copyLfcNo(context, profile.lfcNo) },
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        Icons.Outlined.ContentCopy,
                                        contentDescription = "复制莲峰号",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(13.dp),
                                    )
                                }
                            } else if (onShowMyQr != null) {
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
                    Spacer(modifier = Modifier.height(14.dp))
                    XhsOtherProfileActionRow(
                        isFollowing = profile.isFollowing,
                        onFollowToggle = onFollowToggle,
                        onMessage = onMessage,
                        onShare = onShare,
                    )
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

        if (mode == XhsProfileMode.Self) {
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
                XhsProfileSummaryCard(
                    title = "报名",
                    value = "${profile.participationCount}",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (mode == XhsProfileMode.Other) {
            Spacer(modifier = Modifier.height(12.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = if (mode == XhsProfileMode.Other) 0.dp else (-10).dp)
                .clip(RoundedCornerShape(topStart = sheetCorner, topEnd = sheetCorner))
                .background(Color.White),
        ) {
            if (showInlineTabs) {
                XhsProfileTabBar(
                    tabs = profileTabsFor(mode),
                    profile = profile,
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                )
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun XhsOtherProfileActionRow(
    isFollowing: Boolean,
    onFollowToggle: () -> Unit,
    onMessage: (() -> Unit)?,
    onShare: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onFollowToggle,
            shape = RoundedCornerShape(20.dp),
            color = if (isFollowing) Color.White.copy(alpha = 0.2f) else XhsRed,
            modifier = Modifier
                .weight(1f)
                .height(38.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (isFollowing) "已关注" else "关注",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
        }

        if (onMessage != null) {
            Surface(
                onClick = onMessage,
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.18f),
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "发私信",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                    )
                }
            }
        }

        Surface(
            onClick = { onShare?.invoke() },
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.18f),
            modifier = Modifier.size(38.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.PersonAddAlt1,
                    contentDescription = "分享主页",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

private fun copyLfcNo(context: Context, lfcNo: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("莲峰号", lfcNo))
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
    var showMoreMenu by remember { mutableStateOf(false) }

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
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            } else if (onMenuClick != null) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "菜单",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
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
                IconButton(onClick = { onShare?.invoke() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "分享", tint = Color.White, modifier = Modifier.size(19.dp))
                }
            } else {
                Box {
                    IconButton(onClick = { showMoreMenu = true }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.MoreHoriz,
                            contentDescription = "更多",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("分享主页") },
                            onClick = {
                                showMoreMenu = false
                                onShare?.invoke()
                            },
                        )
                        if (onMessage != null) {
                            DropdownMenuItem(
                                text = { Text("发私信") },
                                onClick = {
                                    showMoreMenu = false
                                    onMessage()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun XhsProfileTabBar(
    tabs: List<ProfileTabItem>,
    profile: XhsProfileData,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val selected = selectedTab == tab.contentIndex
                val locked = profile.isTabLocked(tab.contentIndex)
                Column(
                    modifier = Modifier
                        .clickable { onTabSelected(tab.contentIndex) }
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
                            text = tab.label,
                            fontSize = 15.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) XhsTextPrimary else XhsTextSecondary,
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .width(if (selected) 24.dp else 0.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(if (selected) XhsRed else Color.Transparent),
                    )
                }
            }
        }
        HorizontalDivider(color = Color(0xFFEFEFEF), thickness = 0.5.dp)
    }
}

@Composable
private fun XhsProfileTabEmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = XhsTextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun XhsProfileTabLoadingHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.CircularProgressIndicator(color = XhsRed)
    }
}

@Composable
private fun XhsProfileLockedHint(isSelf: Boolean = true) {
    Box(
        modifier = Modifier.fillMaxWidth(),
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
