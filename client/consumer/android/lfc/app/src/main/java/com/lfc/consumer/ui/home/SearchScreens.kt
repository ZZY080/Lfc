package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.data.model.SearchUiState
import com.lfc.consumer.data.model.displayName
import com.lfc.consumer.ui.theme.XhsBackground
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary
import kotlinx.coroutines.flow.distinctUntilChanged

val XHS_SEARCH_TABS = listOf("综合", "笔记", "活动")

private sealed interface SearchFeedItem {
    data class PostItem(val post: PostDto) : SearchFeedItem
    data class ActivityItem(val activity: ActivityDto) : SearchFeedItem
}

private fun buildMixedSearchFeed(
    posts: List<PostDto>,
    activities: List<ActivityDto>,
): List<SearchFeedItem> {
    return (posts.map { SearchFeedItem.PostItem(it) } + activities.map { SearchFeedItem.ActivityItem(it) })
        .sortedByDescending { item ->
            when (item) {
                is SearchFeedItem.PostItem -> item.post.createdAt
                is SearchFeedItem.ActivityItem -> item.activity.createdAt
            }
        }
}

private fun SearchFeedItem.stableKey(): String = when (this) {
    is SearchFeedItem.PostItem -> "post-${post.id}"
    is SearchFeedItem.ActivityItem -> "activity-${activity.id}"
}

val XHS_HOT_SEARCHES = listOf(
    "校园活动", "食堂推荐", "考研资料", "社团招新",
    "二手闲置", "学习笔记", "图书馆", "篮球赛",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchPageScreen(
    searchInput: String,
    onSearchInputChange: (String) -> Unit,
    searchHistory: List<String>,
    onSearch: (String) -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        SearchTopBar(
            value = searchInput,
            onValueChange = onSearchInputChange,
            placeholder = "搜索校园笔记、活动",
            onBack = onBack,
            onSearch = { onSearch(searchInput.trim()) },
            focusRequester = focusRequester,
            showClear = searchInput.isNotEmpty(),
            onClear = { onSearchInputChange("") },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (searchHistory.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "历史搜索",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = XhsTextPrimary,
                        )
                        IconButton(onClick = onClearHistory, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "清空历史",
                                tint = XhsTextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        searchHistory.forEach { keyword ->
                            SearchChip(text = keyword, onClick = { onSearch(keyword) })
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            item {
                Text(
                    "猜你想搜",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = XhsTextPrimary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    XHS_HOT_SEARCHES.forEach { keyword ->
                        SearchChip(text = keyword, onClick = { onSearch(keyword) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultScreen(
    searchState: SearchUiState,
    searchInput: String,
    onSearchInputChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onTabSelected: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onPostClick: (Int) -> Unit,
    onActivityClick: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyStaggeredGridState()
    val selectedIndex = XHS_SEARCH_TABS.indexOf(searchState.selectedTab).coerceAtLeast(0)
    val isEmpty = when (searchState.selectedTab) {
        "笔记" -> searchState.posts.isEmpty()
        "活动" -> searchState.activities.isEmpty()
        else -> searchState.posts.isEmpty() && searchState.activities.isEmpty()
    }
    val mixedFeedItems = remember(searchState.posts, searchState.activities) {
        buildMixedSearchFeed(searchState.posts, searchState.activities)
    }
    val supportsLoadMore = searchState.selectedTab != "活动"

    LaunchedEffect(listState, searchState.hasMore, searchState.isLoadingMore, searchState.selectedTab) {
        if (!supportsLoadMore) return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to info.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (lastVisible, total) ->
                if (total > 0 && lastVisible >= total - 3 && searchState.hasMore && !searchState.isLoadingMore) {
                    onLoadMore()
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XhsBackground),
    ) {
        Column(modifier = Modifier.background(Color.White)) {
            SearchTopBar(
                value = searchInput,
                onValueChange = onSearchInputChange,
                placeholder = "搜索校园笔记、活动",
                onBack = onBack,
                onSearch = { onSearch(searchInput.trim()) },
                showClear = searchInput.isNotEmpty(),
                onClear = { onSearchInputChange("") },
            )
            ScrollableTabRow(
                selectedTabIndex = selectedIndex,
                containerColor = Color.White,
                contentColor = XhsRed,
                edgePadding = 16.dp,
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
                XHS_SEARCH_TABS.forEachIndexed { index, title ->
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
        }

        PullToRefreshBox(
            isRefreshing = searchState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                searchState.isLoading -> {
                    FeedGridSkeleton()
                }
                isEmpty -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "没有找到「${searchState.keyword}」相关内容",
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
                        when (searchState.selectedTab) {
                            "笔记" -> {
                                items(searchState.posts, key = { it.id }) { post ->
                                    SearchProfilePostCard(
                                        post = post,
                                        onClick = { onPostClick(post.id) },
                                    )
                                }
                            }
                            "活动" -> {
                                items(searchState.activities, key = { it.id }) { activity ->
                                    XhsProfileActivityCard(
                                        activity = activity,
                                        authorLabel = activity.author?.displayName()
                                            ?: "同学${activity.authorId}",
                                        avatarUrl = activity.author?.avatarUrl,
                                        modifier = Modifier.clickable { onActivityClick(activity.id) },
                                    )
                                }
                            }
                            else -> {
                                items(mixedFeedItems, key = { it.stableKey() }) { item ->
                                    when (item) {
                                        is SearchFeedItem.PostItem -> {
                                            SearchProfilePostCard(
                                                post = item.post,
                                                onClick = { onPostClick(item.post.id) },
                                            )
                                        }
                                        is SearchFeedItem.ActivityItem -> {
                                            XhsProfileActivityCard(
                                                activity = item.activity,
                                                authorLabel = item.activity.author?.displayName()
                                                    ?: "同学${item.activity.authorId}",
                                                avatarUrl = item.activity.author?.avatarUrl,
                                                modifier = Modifier.clickable {
                                                    onActivityClick(item.activity.id)
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (supportsLoadMore && searchState.isLoadingMore) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                SearchLoadingFooter()
                            }
                        } else if ((!supportsLoadMore || !searchState.hasMore) && !isEmpty) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                SearchEndFooter()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchProfilePostCard(
    post: PostDto,
    onClick: () -> Unit,
) {
    val authorLabel = post.author?.displayName() ?: "同学${post.authorId}"
    Box(modifier = Modifier.clickable(onClick = onClick)) {
        XhsProfileFeedCard(
            post = post,
            authorLabel = authorLabel,
            avatarUrl = post.author?.avatarUrl,
        )
    }
}

@Composable
private fun SearchLoadingFooter() {
    SkeletonLoadMoreFooter()
}

@Composable
private fun SearchEndFooter() {
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

@Composable
private fun SearchChip(
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF5F5F5),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 13.sp,
            color = XhsTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SearchTopBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    focusRequester: FocusRequester? = null,
    showClear: Boolean = false,
    onClear: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFF5F5F5))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Search, null, tint = XhsTextSecondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, color = XhsTextPrimary),
                    cursorBrush = SolidColor(XhsRed),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    modifier = Modifier
                        .weight(1f)
                        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty()) {
                                Text(placeholder, color = XhsTextSecondary, fontSize = 14.sp)
                            }
                            inner()
                        }
                    },
                )
                if (showClear) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "清空",
                        tint = XhsTextSecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable(onClick = onClear),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "搜索",
            color = XhsRed,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onSearch)
                .padding(horizontal = 4.dp, vertical = 8.dp),
        )
    }
}
