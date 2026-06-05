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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import com.lfc.consumer.data.model.ORDER_CENTER_TABS
import com.lfc.consumer.data.model.OrderCenterUiState
import com.lfc.consumer.data.model.PaymentOrderListItemDto
import com.lfc.consumer.ui.theme.XhsBackground
import com.lfc.consumer.ui.theme.XhsDivider
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderCenterScreen(
    state: OrderCenterUiState,
    isPaymentProcessing: Boolean = false,
    onBack: () -> Unit,
    onOpenTransactions: () -> Unit = {},
    onTabSelected: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onOrderClick: (PaymentOrderListItemDto) -> Unit,
    onPayOrder: (PaymentOrderListItemDto) -> Unit,
    onCancelOrder: (PaymentOrderListItemDto) -> Unit,
    onConfirmReceipt: (PaymentOrderListItemDto) -> Unit,
    onReviewOrder: (PaymentOrderListItemDto, Int, String?) -> Unit,
    onApplyAfterSales: (PaymentOrderListItemDto, String) -> Unit,
) {
    val listState = rememberLazyListState()
    val pullRefreshState = rememberPullToRefreshState()
    val selectedIndex = ORDER_CENTER_TABS.indexOfFirst { it.first == state.selectedTab }.coerceAtLeast(0)
    val latestState by rememberUpdatedState(state)

    var reviewTarget by remember { mutableStateOf<PaymentOrderListItemDto?>(null) }
    var reviewRating by remember { mutableIntStateOf(5) }
    var reviewContent by remember { mutableStateOf("") }
    var afterSalesTarget by remember { mutableStateOf<PaymentOrderListItemDto?>(null) }
    var afterSalesReason by remember { mutableStateOf("") }

    LaunchedEffect(state.selectedTab) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(listState, state.orders.size) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        }
            .distinctUntilChanged()
            .collect { lastVisible ->
                val current = latestState
                val count = current.orders.size
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
            .background(XhsBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text(
                text = "我的订单",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = XhsTextPrimary,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpenTransactions) {
                Text(
                    text = "收支流水",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = XhsRed,
                )
            }
        }
        HorizontalDivider(color = XhsDivider)

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
            ORDER_CENTER_TABS.forEachIndexed { index, (tabKey, title) ->
                val count = when (tabKey) {
                    "all" -> state.tabCounts?.all
                    "pending_payment" -> state.tabCounts?.pendingPayment
                    "awaiting_receipt" -> state.tabCounts?.awaitingReceipt
                    "review" -> state.tabCounts?.review
                    "after_sales" -> state.tabCounts?.afterSales
                    else -> null
                }
                Tab(
                    selected = selectedIndex == index,
                    onClick = { onTabSelected(tabKey) },
                    text = {
                        Text(
                            text = if (count != null && count > 0 && tabKey != "all") "$title($count)" else title,
                            fontSize = 14.sp,
                            fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedIndex == index) XhsTextPrimary else XhsTextSecondary,
                        )
                    },
                )
            }
        }

        PullToRefreshBox(
            state = pullRefreshState,
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    isRefreshing = state.isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    color = XhsRed,
                )
            },
        ) {
            when {
                state.isInitialLoading && state.orders.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = XhsRed)
                    }
                }
                state.orders.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无相关订单", color = XhsTextSecondary, fontSize = 14.sp)
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.orders, key = { it.outTradeNo }) { order ->
                            OrderListItemCard(
                                order = order,
                                isActing = state.actingOutTradeNo == order.outTradeNo,
                                isPaymentBlocked = isPaymentProcessing &&
                                    state.actingOutTradeNo != order.outTradeNo,
                                onClick = { onOrderClick(order) },
                                onPay = { onPayOrder(order) },
                                onCancel = { onCancelOrder(order) },
                                onConfirmReceipt = { onConfirmReceipt(order) },
                                onReview = {
                                    reviewTarget = order
                                    reviewRating = 5
                                    reviewContent = ""
                                },
                                onAfterSales = {
                                    afterSalesTarget = order
                                    afterSalesReason = ""
                                },
                            )
                        }
                        if (state.isLoadingMore) {
                            item(key = "loading-more") {
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

    reviewTarget?.let { order ->
        AlertDialog(
            onDismissRequest = { reviewTarget = null },
            title = { Text("评价订单") },
            text = {
                Column {
                    Text(order.bizTitle, fontSize = 14.sp, color = XhsTextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..5).forEach { star ->
                            Text(
                                text = if (star <= reviewRating) "★" else "☆",
                                color = XhsRed,
                                fontSize = 28.sp,
                                modifier = Modifier.clickable { reviewRating = star },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reviewContent,
                        onValueChange = { reviewContent = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("说说你的体验（选填）") },
                        maxLines = 4,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReviewOrder(order, reviewRating, reviewContent.takeIf { it.isNotBlank() })
                        reviewTarget = null
                    },
                ) { Text("提交", color = XhsRed) }
            },
            dismissButton = {
                TextButton(onClick = { reviewTarget = null }) { Text("取消") }
            },
        )
    }

    afterSalesTarget?.let { order ->
        AlertDialog(
            onDismissRequest = { afterSalesTarget = null },
            title = { Text("申请售后/退款") },
            text = {
                Column {
                    Text(
                        text = if (order.bizType == "ACTIVITY_JOIN") {
                            "活动报名退款将取消你的报名资格"
                        } else {
                            "未确认收货前可申请退款，商品将重新上架"
                        },
                        fontSize = 13.sp,
                        color = XhsTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = afterSalesReason,
                        onValueChange = { afterSalesReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("退款原因") },
                        maxLines = 3,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (afterSalesReason.isNotBlank()) {
                            onApplyAfterSales(order, afterSalesReason.trim())
                            afterSalesTarget = null
                        }
                    },
                ) { Text("提交申请", color = XhsRed) }
            },
            dismissButton = {
                TextButton(onClick = { afterSalesTarget = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun OrderListItemCard(
    order: PaymentOrderListItemDto,
    isActing: Boolean,
    isPaymentBlocked: Boolean,
    onClick: () -> Unit,
    onPay: () -> Unit,
    onCancel: () -> Unit,
    onConfirmReceipt: () -> Unit,
    onReview: () -> Unit,
    onAfterSales: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                    text = if (order.bizType == "ACTIVITY_JOIN") "校园活动" else "闲置商品",
                    fontSize = 12.sp,
                    color = XhsTextSecondary,
                )
                Text(
                    text = order.statusLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = XhsRed,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(coverGradientForId(order.bizId)),
                ) {
                    order.coverImage?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = order.bizTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.bizTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = XhsTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "卖家 ${order.payeeName}",
                        fontSize = 12.sp,
                        color = XhsTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatPriceYuan(order.amount),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = XhsRed,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF3F3F3))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isActing) {
                    CircularProgressIndicator(
                        color = XhsRed,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    return@Row
                }

                if (order.canPay) {
                    OutlinedButton(
                        onClick = onCancel,
                        enabled = !isPaymentBlocked,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                    ) {
                        Text("取消", fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onPay,
                        enabled = !isPaymentBlocked,
                        colors = ButtonDefaults.buttonColors(containerColor = XhsRed),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                    ) {
                        Text("去支付", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (order.canConfirmReceipt) {
                    Button(
                        onClick = onConfirmReceipt,
                        colors = ButtonDefaults.buttonColors(containerColor = XhsRed),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                    ) {
                        Text("确认收货", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (order.canReview) {
                    Button(
                        onClick = onReview,
                        colors = ButtonDefaults.buttonColors(containerColor = XhsRed),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                    ) {
                        Text("去评价", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (order.canApplyAfterSales) {
                    OutlinedButton(
                        onClick = onAfterSales,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                    ) {
                        Text("申请售后", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
