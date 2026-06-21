package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.data.local.LocationSearchHistoryStore
import com.lfc.consumer.data.model.LocationPick
import com.lfc.consumer.data.model.PlaceSuggestionDto
import com.lfc.consumer.location.GeocodeAddressHelper
import com.lfc.consumer.ui.theme.XhsBackground
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PublishLocationSearchScreen(
    onBack: () -> Unit,
    onSelect: (LocationPick) -> Unit,
    biasLatitude: Double? = null,
    biasLongitude: Double? = null,
    biasCity: String? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val historyStore = remember { LocationSearchHistoryStore(context) }
    val searchHistory by historyStore.historyFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    var keyword by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<PlaceSuggestionDto>>(emptyList()) }
    var page by remember { mutableIntStateOf(1) }
    var hasMore by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    fun recordSearchHistory(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) return
        scope.launch {
            historyStore.add(trimmed)
        }
    }

    fun performSearch(reset: Boolean) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) {
            items = emptyList()
            hasMore = false
            page = 1
            return
        }
        searchJob?.cancel()
        searchJob = scope.launch {
            val nextPage = if (reset) 1 else page
            if (reset) {
                isSearching = true
            } else {
                isLoadingMore = true
            }
            val (results, more) = GeocodeAddressHelper.searchPlaces(
                keyword = trimmed,
                page = nextPage,
                limit = 20,
                latitude = biasLatitude,
                longitude = biasLongitude,
                city = biasCity,
            )
            if (reset) {
                items = results
                page = 2
            } else {
                items = items + results.filter { new ->
                    items.none { it.id == new.id }
                }
                page = nextPage + 1
            }
            hasMore = more
            isSearching = false
            isLoadingMore = false
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(keyword) {
        searchJob?.cancel()
        if (keyword.trim().isEmpty()) {
            items = emptyList()
            hasMore = false
            page = 1
            isSearching = false
            return@LaunchedEffect
        }
        searchJob = scope.launch {
            delay(300)
            performSearch(reset = true)
        }
    }

    LaunchedEffect(listState, hasMore, isLoadingMore, isSearching) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to info.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (lastVisible, total) ->
                if (
                    total > 0 &&
                    lastVisible >= total - 3 &&
                    hasMore &&
                    !isLoadingMore &&
                    !isSearching
                ) {
                    performSearch(reset = false)
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XhsBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
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
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = XhsTextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp, color = XhsTextPrimary),
                        cursorBrush = SolidColor(XhsRed),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                recordSearchHistory(keyword)
                                performSearch(reset = true)
                            },
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (keyword.isEmpty()) {
                                    Text("搜索位置", color = XhsTextSecondary, fontSize = 14.sp)
                                }
                                inner()
                            }
                        },
                    )
                    if (keyword.isNotEmpty()) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "清空",
                            tint = XhsTextSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { keyword = "" },
                        )
                    }
                }
            }
        }

        when {
            isSearching && items.isEmpty() -> {
                ListRowSkeleton(modifier = Modifier.fillMaxSize())
            }
            keyword.trim().isEmpty() -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (searchHistory.isNotEmpty()) {
                        item(key = "history-header") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "历史搜索",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = XhsTextPrimary,
                                )
                                IconButton(
                                    onClick = { scope.launch { historyStore.clear() } },
                                    modifier = Modifier.size(32.dp),
                                ) {
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
                                searchHistory.forEach { historyKeyword ->
                                    PublishLocationHistoryChip(
                                        text = historyKeyword,
                                        onClick = { keyword = historyKeyword },
                                    )
                                }
                            }
                        }
                    } else {
                        item(key = "history-empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "输入楼栋或地点名称开始搜索",
                                    color = XhsTextSecondary,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    }
                }
            }
            items.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("未找到相关地点", color = XhsTextSecondary, fontSize = 14.sp)
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(items, key = { it.id }) { place ->
                        PublishLocationSearchItem(
                            place = place,
                            onClick = {
                                recordSearchHistory(keyword)
                                onSelect(
                                    LocationPick(
                                        label = place.displayLabel(),
                                        latitude = place.latitude,
                                        longitude = place.longitude,
                                    ),
                                )
                            },
                        )
                    }
                    if (isLoadingMore) {
                        item(key = "loading-more") {
                            SkeletonLoadMoreFooter()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PublishLocationSearchItem(
    place: PlaceSuggestionDto,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            tint = XhsRed,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = place.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = XhsTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (place.address.isNotBlank() && place.address != place.name) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = place.address,
                    fontSize = 12.sp,
                    color = XhsTextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (place.district.isNotBlank() && !place.address.contains(place.district)) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = place.district,
                    fontSize = 11.sp,
                    color = XhsTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    HorizontalDivider(color = Color(0xFFF0F0F0))
}

@Composable
private fun PublishLocationHistoryChip(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF5F5F5))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = XhsTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun PlaceSuggestionDto.displayLabel(): String {
    return when {
        address.isNotBlank() -> address
        district.isNotBlank() -> "$name · $district"
        else -> name
    }
}
