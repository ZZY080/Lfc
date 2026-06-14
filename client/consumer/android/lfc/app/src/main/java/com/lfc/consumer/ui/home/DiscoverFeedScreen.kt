package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.data.local.FeedChannels
import com.lfc.consumer.data.model.FeedUiState
import com.lfc.consumer.data.model.displayName
import com.lfc.consumer.ui.theme.XhsBackground
import com.lfc.consumer.ui.theme.XhsTextSecondary
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverFeedScreen(
    feedState: FeedUiState,
    cityLabel: String,
    recommendedChannels: List<String>,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onPrimaryTabSelected: (String) -> Unit,
    onTabSelected: (String) -> Unit,
    onMessageClick: () -> Unit,
    onSearchClick: () -> Unit,
    onToggleChannelPanel: () -> Unit,
    onCollapseChannelPanel: () -> Unit,
    onToggleChannelEditMode: () -> Unit,
    onAddChannel: (String) -> Unit,
    onRemoveChannel: (String) -> Unit,
    onPostClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyStaggeredGridState()
    val isFollowingTab = feedState.primaryTab == "关注"
    val myChannels = feedState.myChannels.ifEmpty { FeedChannels.defaultMyChannels }
    val showChannelPanel = feedState.isChannelPanelExpanded && !isFollowingTab

    LaunchedEffect(feedState.selectedTab, feedState.primaryTab, showChannelPanel) {
        if (!showChannelPanel) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(listState, feedState.hasMore, feedState.isLoadingMore, showChannelPanel) {
        if (showChannelPanel) return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to info.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (lastVisible, total) ->
                if (total > 0 && lastVisible >= total - 3 && feedState.hasMore && !feedState.isLoadingMore) {
                    onLoadMore()
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XhsBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding(),
        ) {
            XhsFeedPrimaryTabRow(
                selectedTab = feedState.primaryTab,
                cityLabel = cityLabel,
                onTabSelected = onPrimaryTabSelected,
                onMessageClick = onMessageClick,
                onSearchClick = onSearchClick,
                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
            )

            if (!isFollowingTab) {
                XhsFeedCategoryTabRow(
                    myChannels = myChannels,
                    selectedTab = feedState.selectedTab,
                    isPanelExpanded = feedState.isChannelPanelExpanded,
                    onTabSelected = onTabSelected,
                    onExpandPanel = onToggleChannelPanel,
                )
            }

            if (showChannelPanel) {
                XhsFeedChannelPanel(
                    myChannels = myChannels,
                    recommendedChannels = recommendedChannels,
                    isEditMode = feedState.isChannelEditMode,
                    onToggleEditMode = onToggleChannelEditMode,
                    onCollapse = onCollapseChannelPanel,
                    onChannelClick = onTabSelected,
                    onAddChannel = onAddChannel,
                    onRemoveChannel = onRemoveChannel,
                )
            }
        }

        if (showChannelPanel) {
            return@Column
        }

        PullToRefreshBox(
            isRefreshing = feedState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                feedState.isInitialLoading && feedState.posts.isEmpty() -> {
                    FeedGridSkeleton()
                }
                feedState.posts.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (isFollowingTab) {
                                "关注的人暂无动态\n去发现页看看热门内容吧"
                            } else {
                                "暂无笔记，点击 + 发布第一条吧"
                            },
                            color = XhsTextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = if (isFollowingTab) 22.sp else 20.sp,
                        )
                    }
                }
                else -> {
                    LazyVerticalStaggeredGrid(
                        state = listState,
                        columns = StaggeredGridCells.Fixed(2),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalItemSpacing = 8.dp,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(feedState.posts, key = { it.id }) { post ->
                            val authorLabel = post.author?.displayName() ?: "同学${post.authorId}"
                            Box(modifier = Modifier.clickable { onPostClick(post.id) }) {
                                XhsProfileFeedCard(
                                    post = post,
                                    authorLabel = authorLabel,
                                    avatarUrl = post.author?.avatarUrl,
                                )
                            }
                        }
                        if (feedState.isLoadingMore) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                SkeletonLoadMoreFooter()
                            }
                        } else if (!feedState.hasMore) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Text(
                                    "— 已经到底了 —",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    color = XhsTextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
