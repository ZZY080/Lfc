package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.data.model.FeedUiState
import com.lfc.consumer.data.model.displayName
import com.lfc.consumer.ui.theme.XhsBackground
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary
import kotlinx.coroutines.flow.distinctUntilChanged

val XHS_FEED_TABS = listOf("推荐", "最新", "二手闲置", "校园", "活动", "美食", "学习", "生活")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverFeedScreen(
    feedState: FeedUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onTabSelected: (String) -> Unit,
    onSearchClick: () -> Unit,
    onPostClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyStaggeredGridState()
    val selectedIndex = XHS_FEED_TABS.indexOf(feedState.selectedTab).coerceAtLeast(0)

    LaunchedEffect(feedState.selectedTab) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(listState, feedState.hasMore, feedState.isLoadingMore) {
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
                .background(Color.White),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFF5F5F5))
                        .clickable(onClick = onSearchClick)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = XhsTextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("搜索校园笔记、活动", color = XhsTextSecondary, fontSize = 14.sp)
                }
            }

            ScrollableTabRow(
                selectedTabIndex = selectedIndex,
                containerColor = Color.White,
                contentColor = XhsRed,
                edgePadding = 12.dp,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[selectedIndex])
                                .height(3.dp)
                                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)),
                            color = XhsRed,
                        )
                    }
                },
            ) {
                XHS_FEED_TABS.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedIndex == index,
                        onClick = { onTabSelected(title) },
                        text = {
                            Text(
                                title,
                                fontSize = 15.sp,
                                fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedIndex == index) XhsTextPrimary else XhsTextSecondary,
                            )
                        },
                    )
                }
            }

            Text(
                text = "推广内容已明确标注，优先展示不代表官方背书",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 6.dp),
                color = XhsTextSecondary,
                fontSize = 11.sp,
            )
        }

        PullToRefreshBox(
            isRefreshing = feedState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                feedState.isInitialLoading && feedState.posts.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = XhsRed)
                    }
                }
                feedState.posts.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "暂无校园信息，点击 + 发布第一条吧",
                            color = XhsTextSecondary,
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
