package com.lfc.consumer.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
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
import kotlinx.coroutines.flow.first
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
import com.lfc.consumer.ui.findActivity
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
                is ProfileLibraryFeedItem.PostItem -> item.post.savedAt ?: item.post.createdAt
                is ProfileLibraryFeedItem.ActivityItem -> item.activity.savedAt ?: item.activity.createdAt
            }
        }
}

private fun ProfileLibraryFeedItem.stableKey(): String = when (this) {
    is ProfileLibraryFeedItem.PostItem -> "post-${post.id}"
    is ProfileLibraryFeedItem.ActivityItem -> "activity-${activity.id}"
}

private data class ProfileTabItem(
    val label: String,
    val contentIndex: Int,
    val count: Int = 0,
)

private data class ProfileTabCounts(
    val notes: Int = 0,
    val activities: Int = 0,
    val comments: Int = 0,
    val favorites: Int = 0,
    val likes: Int = 0,
)

private fun resolveProfileTabCounts(
    profile: XhsProfileData,
    profileTabs: ProfileTabsUiState,
    profileNotes: List<PostDto>,
    profileActivities: List<ActivityDto>,
    profileComments: List<ProfileCommentDto>,
    profileFavoritePosts: List<PostDto>,
    profileFavoriteActivities: List<ActivityDto>,
    profileLikedPosts: List<PostDto>,
    profileLikedActivities: List<ActivityDto>,
): ProfileTabCounts {
    fun tabTotal(index: Int, listSize: Int, profileFallback: Int = 0): Int {
        val fromTab = profileTabs.tabs.getOrNull(index)?.totalCount
        return when {
            fromTab != null -> fromTab
            profileFallback > 0 -> profileFallback
            listSize > 0 -> listSize
            else -> 0
        }
    }
    val favoritesTotal = when {
        profileTabs.favoritesPostsTotal != null || profileTabs.favoritesActivitiesTotal != null ->
            (profileTabs.favoritesPostsTotal ?: 0) + (profileTabs.favoritesActivitiesTotal ?: 0)
        profileFavoritePosts.isNotEmpty() || profileFavoriteActivities.isNotEmpty() ->
            profileFavoritePosts.size + profileFavoriteActivities.size
        else -> profileTabs.tabs.getOrElse(PROFILE_FAVORITES_TAB) { ProfileTabUiState() }.totalCount ?: 0
    }
    val likesTotal = when {
        profileTabs.likedPostsTotal != null || profileTabs.likedActivitiesTotal != null ->
            (profileTabs.likedPostsTotal ?: 0) + (profileTabs.likedActivitiesTotal ?: 0)
        profileLikedPosts.isNotEmpty() || profileLikedActivities.isNotEmpty() ->
            profileLikedPosts.size + profileLikedActivities.size
        else -> profileTabs.tabs.getOrElse(PROFILE_LIKES_TAB) { ProfileTabUiState() }.totalCount ?: 0
    }
    return ProfileTabCounts(
        notes = tabTotal(0, profileNotes.size, profile.postCount),
        activities = tabTotal(1, profileActivities.size, profile.activities.size),
        comments = tabTotal(2, profileComments.size),
        favorites = favoritesTotal,
        likes = likesTotal,
    )
}

private fun buildProfileTabs(mode: XhsProfileMode, counts: ProfileTabCounts): List<ProfileTabItem> =
    when (mode) {
        XhsProfileMode.Self -> listOf(
            ProfileTabItem("笔记", 0, counts.notes),
            ProfileTabItem("活动", 1, counts.activities),
            ProfileTabItem("评论", 2, counts.comments),
            ProfileTabItem("收藏", PROFILE_FAVORITES_TAB, counts.favorites),
            ProfileTabItem("赞", PROFILE_LIKES_TAB, counts.likes),
        )
        XhsProfileMode.Other -> listOf(
            ProfileTabItem("笔记", 0, counts.notes),
            ProfileTabItem("活动", 1, counts.activities),
            ProfileTabItem("收藏", PROFILE_FAVORITES_TAB, counts.favorites),
            ProfileTabItem("赞", PROFILE_LIKES_TAB, counts.likes),
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
private val selfProfileCoverHeight = 300.dp
private val otherProfileCoverHeight = 300.dp
/** 独立主页：封面 + Tab 栏等，用于计算内容区填满剩余空间 */
private val standaloneProfileHeaderEstimate = 372.dp

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
    profileTabsState: ProfileTabsUiState = ProfileTabsUiState(),
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
    onOffShelfPost: ((Int) -> Unit)? = null,
    onOnShelfPost: ((Int) -> Unit)? = null,
    onEditActivity: ((ActivityDto) -> Unit)? = null,
    onDeleteActivity: ((Int) -> Unit)? = null,
    onOffShelfActivity: ((Int) -> Unit)? = null,
    onOnShelfActivity: ((Int) -> Unit)? = null,
    onOpenSideMenu: (() -> Unit)? = null,
    selectedContentTab: Int? = null,
    onContentTabChange: ((Int) -> Unit)? = null,
    /** NavHost 独立主页：固定屏幕高度，避免 LazyVerticalStaggeredGrid 收到无限高度约束 */
    standalonePage: Boolean = false,
    /** 主 Tab「我」沉浸式：内容延伸到底栏下方，需额外 bottom padding */
    immersiveBottomPadding: Dp = 0.dp,
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
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    var scrollReady by remember { mutableStateOf(!standalonePage) }
    LaunchedEffect(standalonePage, profile?.userId, isLoading) {
        if (!standalonePage) {
            scrollReady = true
            return@LaunchedEffect
        }
        scrollReady = false
        if (!isLoading) {
            kotlinx.coroutines.yield()
            scrollReady = true
        }
    }

    DisposableEffect(mode) {
        val activity = view.context.findActivity()
        if (!view.isInEditMode && activity != null) {
            val window = activity.window
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        onDispose { }
    }

    @Composable
    fun ProfileScrollContent(
        scrollModifier: Modifier,
        tabPlaceholderMinHeight: Dp? = null,
    ) {
        Box(modifier = scrollModifier) {
        when {
            isLoading -> ProfilePageSkeleton(Modifier.fillMaxSize())
            profile == null -> XhsDetailEmpty("用户不存在", Modifier.fillMaxSize())
            !scrollReady -> ProfilePageSkeleton(Modifier.fillMaxSize())
            else -> {
                val tabCounts = resolveProfileTabCounts(
                    profile = profile,
                    profileTabs = profileTabsState,
                    profileNotes = profileNotes,
                    profileActivities = profileActivities,
                    profileComments = profileComments,
                    profileFavoritePosts = profileFavoritePosts,
                    profileFavoriteActivities = profileFavoriteActivities,
                    profileLikedPosts = profileLikedPosts,
                    profileLikedActivities = profileLikedActivities,
                )
                val profileTabs = buildProfileTabs(mode, tabCounts)
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
                val gridState = remember(selectedTab) { LazyStaggeredGridState() }
                var pinStickyTabsAfterSwitch by remember { mutableStateOf(false) }
                val isCollapsed by remember(gridState) {
                    derivedStateOf {
                        gridState.firstVisibleItemIndex > 0 ||
                            (gridState.firstVisibleItemIndex == 0 &&
                                gridState.firstVisibleItemScrollOffset > 120)
                    }
                }
                val showStickyTabs by remember(gridState) {
                    derivedStateOf { gridState.firstVisibleItemIndex >= 1 }
                }
                val effectiveStickyTabs = showStickyTabs ||
                    (pinStickyTabsAfterSwitch && showTabPlaceholder)
                val showInlineTabs = !effectiveStickyTabs

                fun handleTabSelected(tab: Int) {
                    if (tab == selectedTab) return
                    pinStickyTabsAfterSwitch = gridState.firstVisibleItemIndex >= 1
                    selectTab(tab)
                }

                LaunchedEffect(selectedTab, showTabPlaceholder, pinStickyTabsAfterSwitch) {
                    if (!pinStickyTabsAfterSwitch || showTabPlaceholder) return@LaunchedEffect
                    snapshotFlow { gridState.layoutInfo.totalItemsCount }
                        .first { it > 1 }
                    gridState.scrollToItem(1)
                    pinStickyTabsAfterSwitch = false
                }

                val showCollapsedHeader = effectiveStickyTabs ||
                    (!showTabPlaceholder && isCollapsed)

                ProfileImmersiveSystemBars(
                    mode = mode,
                    isCollapsed = showCollapsedHeader,
                    view = view,
                )

                LaunchedEffect(gridState, tabUiState.hasMore, tabUiState.isLoadingMore, selectedTab, tabLocked) {
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
                    modifier = scrollModifier,
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
                            bottom = 16.dp + immersiveBottomPadding,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(profileGridItemSpacing),
                        verticalItemSpacing = profileGridItemSpacing,
                        modifier = scrollModifier,
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
                                    tabs = profileTabs,
                                    onMessage = onMessage,
                                    onFollowToggle = onFollowToggle,
                                    onShare = onShare,
                                    selectedTab = selectedTab,
                                    onTabSelected = { handleTabSelected(it) },
                                    showInlineTabs = showInlineTabs,
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
                                    contentMinHeight = tabPlaceholderMinHeight,
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
                                onOffShelfPost = onOffShelfPost,
                                onOnShelfPost = onOnShelfPost,
                            )
                            1 -> renderActivitiesTab(
                                activities = profileActivities,
                                authorLabel = profile.displayName,
                                avatarUrl = profile.avatarUrl,
                                isSelf = mode == XhsProfileMode.Self,
                                onActivityClick = onActivityClick,
                                onEditActivity = onEditActivity,
                                onDeleteActivity = onDeleteActivity,
                                onOffShelfActivity = onOffShelfActivity,
                                onOnShelfActivity = onOnShelfActivity,
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
                                SkeletonLoadMoreFooter()
                            }
                        }
                    }
                }

                XhsProfileTopOverlay(
                    profile = profile,
                    mode = mode,
                    isCollapsed = showCollapsedHeader,
                    onBack = onBack,
                    onMessage = onMessage,
                    onEditProfile = onEditProfile,
                    onShare = onShare,
                    onScanProfile = onScanProfile,
                    onMenuClick = if (mode == XhsProfileMode.Self) onOpenSideMenu else null,
                )

                if (effectiveStickyTabs) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                    ) {
                        Spacer(
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .height(topBarHeight),
                        )
                        XhsProfileTabBar(
                            tabs = profileTabs,
                            profile = profile,
                            selectedTab = selectedTab,
                            onTabSelected = { handleTabSelected(it) },
                            modifier = Modifier.background(Color.White),
                        )
                    }
                }
            }
        }
        }
    }

    val background = Color(0xFFF5F5F5)
    if (standalonePage) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(background),
        ) {
            val pageHeight = if (constraints.hasBoundedHeight) maxHeight else screenHeight
            val scrollModifier = Modifier
                .fillMaxWidth()
                .height(pageHeight)
            val tabPlaceholderMinHeight = (pageHeight - standaloneProfileHeaderEstimate)
                .coerceAtLeast(200.dp)
            ProfileScrollContent(
                scrollModifier = scrollModifier,
                tabPlaceholderMinHeight = tabPlaceholderMinHeight,
            )
        }
    } else {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(background),
        ) {
            val boundedHeight = if (constraints.hasBoundedHeight && maxHeight < screenHeight * 2) {
                maxHeight
            } else {
                screenHeight
            }
            ProfileScrollContent(
                Modifier
                    .fillMaxWidth()
                    .requiredHeight(boundedHeight),
            )
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
    contentMinHeight: Dp? = null,
) {
    val defaultMin = (LocalConfiguration.current.screenHeightDp * 0.42f).dp
    val heightModifier = if (contentMinHeight != null) {
        Modifier.height(contentMinHeight)
    } else {
        Modifier.heightIn(min = defaultMin)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(heightModifier)
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isInitialLoading -> FeedGridSkeletonStatic(
                modifier = Modifier.fillMaxWidth(),
                itemCount = 4,
            )
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
    onOffShelfPost: ((Int) -> Unit)? = null,
    onOnShelfPost: ((Int) -> Unit)? = null,
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
                    if (!post.isVisible && onOnShelfPost != null) {
                        DropdownMenuItem(
                            text = { Text("上架") },
                            onClick = {
                                showMenu = false
                                onOnShelfPost(post.id)
                            },
                        )
                    } else if (post.isVisible && onOffShelfPost != null) {
                        DropdownMenuItem(
                            text = { Text("下架") },
                            onClick = {
                                showMenu = false
                                onOffShelfPost(post.id)
                            },
                        )
                    }
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
    items(
        items = comments,
        key = { it.id },
        span = { StaggeredGridItemSpan.FullLine },
    ) { comment ->
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
    onOffShelfActivity: ((Int) -> Unit)? = null,
    onOnShelfActivity: ((Int) -> Unit)? = null,
) {
    items(activities, key = { it.id }) { activity ->
        var showMenu by remember(activity.id) { mutableStateOf(false) }
        val status = activity.status.uppercase()

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
                    if (status == "OFF_SHELF" && onOnShelfActivity != null) {
                        DropdownMenuItem(
                            text = { Text("上架") },
                            onClick = {
                                showMenu = false
                                onOnShelfActivity(activity.id)
                            },
                        )
                    } else if (status == "APPROVED" && onOffShelfActivity != null) {
                        DropdownMenuItem(
                            text = { Text("下架") },
                            onClick = {
                                showMenu = false
                                onOffShelfActivity(activity.id)
                            },
                        )
                    }
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
    tabs: List<ProfileTabItem>,
    onMessage: (() -> Unit)?,
    onFollowToggle: (() -> Unit)?,
    onShare: (() -> Unit)?,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    showInlineTabs: Boolean,
    onShowMyQr: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val coverUrl = profile.coverUrl
        ?: profile.posts.orEmpty().firstNotNullOfOrNull { it.images?.firstOrNull() }
    val sheetCorner = if (mode == XhsProfileMode.Other) 18.dp else 14.dp
    val coverOverlay = Brush.verticalGradient(
        colors = listOf(
            Color.Black.copy(alpha = 0.08f),
            Color.Black.copy(alpha = 0.28f),
            Color.Black.copy(alpha = 0.72f),
        ),
    )
    val coverFallbackGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF3D5360),
            Color(0xFF243038),
            profileCoverDark,
        ),
    )

    @Composable
    fun ProfileCoverBackground(modifier: Modifier = Modifier) {
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier,
            )
        } else {
            Box(modifier = modifier.background(coverFallbackGradient))
        }
    }

    @Composable
    fun ProfileIdentityBlock(avatarSize: Dp, nameFontSize: androidx.compose.ui.unit.TextUnit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            XhsProfileAvatar(
                label = profile.displayName,
                size = avatarSize.value.toInt(),
                avatarUrl = profile.avatarUrl,
            )
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(
                    text = profile.displayName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = nameFontSize,
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
    }

    @Composable
    fun ProfileMetaBlock() {
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
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (mode == XhsProfileMode.Self) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(selfProfileCoverHeight),
            ) {
                ProfileCoverBackground(Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(coverOverlay))
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(start = 16.dp, end = 16.dp, bottom = 72.dp),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    ProfileIdentityBlock(avatarSize = 68.dp, nameFontSize = 20.sp)
                    ProfileMetaBlock()
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(otherProfileCoverHeight),
            ) {
                ProfileCoverBackground(Modifier.fillMaxSize())
                Box(modifier = Modifier.fillMaxSize().background(coverOverlay))
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = topBarHeight + 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    ProfileIdentityBlock(avatarSize = 72.dp, nameFontSize = 22.sp)
                    ProfileMetaBlock()
                    if (onFollowToggle != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        XhsOtherProfileActionRow(
                            isFollowing = profile.isFollowing,
                            onFollowToggle = onFollowToggle,
                            onMessage = onMessage,
                            onShare = onShare,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = if (mode == XhsProfileMode.Other) 0.dp else (-14).dp)
                .clip(RoundedCornerShape(topStart = sheetCorner, topEnd = sheetCorner))
                .background(Color.White),
        ) {
            if (showInlineTabs) {
                XhsProfileTabBar(
                    tabs = tabs,
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
private fun ProfileImmersiveSystemBars(
    mode: XhsProfileMode,
    isCollapsed: Boolean,
    view: android.view.View,
) {
    DisposableEffect(mode, isCollapsed) {
        if (view.isInEditMode) {
            onDispose { }
        } else {
            val activity = view.context.findActivity()
            if (activity == null) {
                onDispose { }
            } else {
                val controller = WindowCompat.getInsetsController(activity.window, view)
                val previousStatus = controller.isAppearanceLightStatusBars
                val previousNav = controller.isAppearanceLightNavigationBars
                val lightIcons = mode == XhsProfileMode.Self && isCollapsed
                controller.isAppearanceLightStatusBars = lightIcons
                controller.isAppearanceLightNavigationBars = lightIcons
                onDispose {
                    controller.isAppearanceLightStatusBars = previousStatus
                    controller.isAppearanceLightNavigationBars = previousNav
                }
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
    var showMoreMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        if (!isCollapsed) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(topBarHeight + 72.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.52f),
                                Color.Black.copy(alpha = 0.22f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isCollapsed) profileCoverDark else Color.Transparent)
                .windowInsetsPadding(WindowInsets.statusBars),
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
                            text = "编辑资料",
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
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val selected = selectedTab == tab.contentIndex
                val locked = profile.isTabLocked(tab.contentIndex)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(tab.contentIndex) }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (locked) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = XhsTextSecondary.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(9.dp)
                                .padding(end = 1.dp),
                        )
                    }
                    Text(
                        text = tab.label,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) XhsTextPrimary else XhsTextSecondary,
                        maxLines = 1,
                    )
                    Text(
                        text = formatProfileStatCount(tab.count),
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) XhsTextPrimary else XhsTextSecondary.copy(alpha = 0.85f),
                        modifier = Modifier.padding(start = 2.dp),
                        maxLines = 1,
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
    FeedGridSkeletonStatic(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White),
        itemCount = 4,
    )
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
