package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lfc.consumer.data.model.PaymentTransactionItemDto
import com.lfc.consumer.data.model.PaymentTransactionLedgerUiState
import com.lfc.consumer.ui.theme.XhsBackground
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentTransactionLedgerScreen(
    state: PaymentTransactionLedgerUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val listState = rememberLazyListState()
    val pullRefreshState = rememberPullToRefreshState()
    val latestState by rememberUpdatedState(state)

    LaunchedEffect(listState, state.items.size) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        }
            .distinctUntilChanged()
            .collect { lastVisible ->
                val current = latestState
                val count = current.items.size
                if (
                    count > 0 &&
                    lastVisible >= count - 1 &&
                    lastVisible < count &&
                    current.hasMore &&
                    !current.isLoadingMore &&
                    !current.isRefreshing &&
                    !current.isInitialLoading
                ) {
                    onLoadMore()
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(XhsBackground)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = XhsTextPrimary,
                )
            }
            Text(
                text = "收支流水",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = XhsTextPrimary,
            )
        }

        PullToRefreshBox(
            state = pullRefreshState,
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                LfcPullToRefreshIndicator(
                    isRefreshing = state.isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            },
        ) {
            when {
                state.isInitialLoading && state.items.isEmpty() -> {
                    PaymentLedgerSkeleton()
                }
                state.items.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无支付或退款记录", color = XhsTextSecondary, fontSize = 14.sp)
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.items, key = { it.txKey }) { item ->
                            PaymentTransactionItemCard(item = item)
                        }
                        if (state.isLoadingMore) {
                            item(key = "loading-more") {
                                SkeletonLoadMoreFooter()
                            }
                        } else if (!state.hasMore) {
                            item(key = "end") {
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

@Composable
private fun PaymentTransactionItemCard(item: PaymentTransactionItemDto) {
    val isRefund = item.type.equals("REFUND", ignoreCase = true)
    val amountPrefix = if (item.direction.equals("IN", ignoreCase = true)) "+" else "-"
    val amountColor = if (isRefund) Color(0xFF1F8A4C) else XhsRed
    val bizLabel = paymentBizTypeLabel(item.bizType)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.typeLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isRefund) Color(0xFF1F8A4C) else XhsRed,
                )
                Text(
                    text = "$amountPrefix${formatPriceYuan(item.amount)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(coverGradientForId(item.bizId)),
                ) {
                    item.coverImage?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = item.bizTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.bizTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = XhsTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$bizLabel · ${item.counterpartyName}",
                        fontSize = 12.sp,
                        color = XhsTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatTransactionTime(item.occurredAt),
                        fontSize = 12.sp,
                        color = XhsTextSecondary,
                    )
                }
            }

            item.tradeNo?.takeIf { it.isNotBlank() }?.let { tradeNo ->
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFF3F3F3))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "支付宝单号 ${tradeNo.takeLast(8)}",
                    fontSize = 11.sp,
                    color = XhsTextSecondary,
                )
            }
        }
    }
}

private fun formatTransactionTime(raw: String): String {
    return raw.replace("T", " ").take(16)
}
