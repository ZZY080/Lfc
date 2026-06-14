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
    var cancelTarget by remember { mutableStateOf<PaymentOrderListItemDto?>(null) }
    var confirmReceiptTarget by remember { mutableStateOf<PaymentOrderListItemDto?>(null) }

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
                    OrderListSkeleton()
                }
                state.orders.isEmpty() -> {
                    OrderCenterEmptyState(
                        tabLabel = ORDER_CENTER_TABS.getOrNull(selectedIndex)?.second ?: "订单",
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item(key = "fulfillment-banner") {
                            OrderCenterFulfillmentBanner()
                        }
                        items(state.orders, key = { it.outTradeNo }) { order ->
                            OrderListItemCard(
                                order = order,
                                isActing = state.actingOutTradeNo == order.outTradeNo,
                                isPaymentBlocked = isPaymentProcessing &&
                                    state.actingOutTradeNo != order.outTradeNo,
                                onClick = { onOrderClick(order) },
                                onPay = { onPayOrder(order) },
                                onCancel = { cancelTarget = order },
                                onConfirmReceipt = { confirmReceiptTarget = order },
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
        val ruleHint = when (order.bizType) {
            "ACTIVITY_JOIN" -> buildString {
                append("· 活动未开始前可申请全额退款\n")
                append("· 退款成功后自动取消报名资格\n")
                append("· 退款将原路返回至支付宝，通常 1–7 个工作日到账\n")
                append("· 活动开始后原则上不再受理退款")
            }
            else -> buildString {
                append("· 未确认收货前可申请全额退款\n")
                append("· 退款成功后商品将重新上架\n")
                append("· 退款将原路返回至支付宝，通常 1–7 个工作日到账\n")
                append("· 确认收货后如有争议请先与卖家协商")
            }
        }
        AlertDialog(
            onDismissRequest = { afterSalesTarget = null },
            title = { Text("申请售后/退款") },
            text = {
                Column {
                    Text(
                        text = ruleHint,
                        fontSize = 13.sp,
                        color = XhsTextSecondary,
                        lineHeight = 20.sp,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = afterSalesReason,
                        onValueChange = { afterSalesReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("请填写退款原因") },
                        placeholder = { Text("如：商品与描述不符、无法面交等") },
                        maxLines = 3,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "详细规则见「设置 - 法律与隐私 - C2C 退货售后规则」",
                        fontSize = 11.sp,
                        color = XhsTextSecondary,
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

    cancelTarget?.let { order ->
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = { Text("确认取消订单？") },
            text = {
                Column {
                    Text(
                        text = "取消后需重新下单，此操作不可撤销。",
                        fontSize = 14.sp,
                        color = XhsTextSecondary,
                        lineHeight = 20.sp,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF7F7F7),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = order.bizTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = XhsTextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatPriceYuan(order.amount),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = XhsRed,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCancelOrder(order)
                        cancelTarget = null
                    },
                ) {
                    Text("确认取消", color = XhsRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelTarget = null }) {
                    Text("再想想", color = XhsTextPrimary)
                }
            },
        )
    }

    confirmReceiptTarget?.let { order ->
        ConfirmReceiptAlertDialog(
            title = order.bizTitle,
            amount = order.amount,
            bizType = order.bizType,
            onDismiss = { confirmReceiptTarget = null },
            onConfirm = {
                onConfirmReceipt(order)
                confirmReceiptTarget = null
            },
        )
    }
}

@Composable
fun ConfirmReceiptAlertDialog(
    title: String,
    amount: String?,
    bizType: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val hint = when (bizType) {
        "ACTIVITY_JOIN" -> "确认已参加活动？确认后款项将分账给发起人，请谨慎操作。"
        else -> "确认已收到商品？确认后款项将分账给卖家，请谨慎操作。"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认收货？") },
        text = {
            Column {
                Text(
                    text = hint,
                    fontSize = 14.sp,
                    color = XhsTextSecondary,
                    lineHeight = 20.sp,
                )
                if (amount != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF7F7F7),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = XhsTextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatPriceYuan(amount),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = XhsRed,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确认收货", color = XhsRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("再想想", color = XhsTextPrimary)
            }
        },
    )
}

@Composable
private fun OrderCenterEmptyState(tabLabel: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "暂无$tabLabel",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = XhsTextPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "去首页逛逛，发现更多校园好物与活动",
                fontSize = 13.sp,
                color = XhsTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
        }
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
    val hasActions = order.canPay || order.canConfirmReceipt ||
        order.canReview || order.canApplyAfterSales

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 1.dp,
    ) {
        Column {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OrderBizTypeChip(bizType = order.bizType)
                    OrderStatusChip(status = order.status, label = order.statusLabel)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
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
                            lineHeight = 22.sp,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${order.payeeRoleLabel} · ${order.payeeName}",
                            fontSize = 12.sp,
                            color = XhsTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        OrderBizMetaLines(
                            order = order,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = formatOrderCreatedAt(order.createdAt),
                        fontSize = 11.sp,
                        color = Color(0xFFB0B0B0),
                    )
                    Text(
                        text = formatPriceYuan(order.amount),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = XhsRed,
                    )
                }

                if (order.fulfillment != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OrderFulfillmentGuaranteeCard(fulfillment = order.fulfillment)
                }
            }

            if (hasActions) {
                HorizontalDivider(color = Color(0xFFF3F3F3))
                OrderActionBar(
                    order = order,
                    isActing = isActing,
                    isPaymentBlocked = isPaymentBlocked,
                    onPay = onPay,
                    onCancel = onCancel,
                    onConfirmReceipt = onConfirmReceipt,
                    onReview = onReview,
                    onAfterSales = onAfterSales,
                )
            }
        }
    }
}

@Composable
private fun OrderBizTypeChip(bizType: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFF5F5F5),
    ) {
        Text(
            text = paymentBizTypeLabel(bizType),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 11.sp,
            color = XhsTextSecondary,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun OrderStatusChip(status: String, label: String) {
    val (bg, fg) = orderStatusColors(status)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg,
        )
    }
}

private fun orderStatusColors(status: String): Pair<Color, Color> = when (status.uppercase()) {
    "PENDING" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
    "PAID" -> Color(0xFFE8F4FD) to Color(0xFF1565C0)
    "CONFIRMED" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
    "SETTLED" -> Color(0xFFF5F5F5) to XhsTextSecondary
    "REFUNDED" -> Color(0xFFFCE4EC) to Color(0xFFC62828)
    "CLOSED" -> Color(0xFFF5F5F5) to XhsTextSecondary
    else -> Color(0xFFF5F5F5) to XhsTextSecondary
}

private fun formatOrderCreatedAt(value: String): String {
    return value.replace("T", " ").take(16)
}

@Composable
private fun OrderActionBar(
    order: PaymentOrderListItemDto,
    isActing: Boolean,
    isPaymentBlocked: Boolean,
    onPay: () -> Unit,
    onCancel: () -> Unit,
    onConfirmReceipt: () -> Unit,
    onReview: () -> Unit,
    onAfterSales: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isActing) {
            CircularProgressIndicator(
                color = XhsRed,
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
            )
            return
        }

        if (order.canPay) {
            TextButton(
                onClick = onCancel,
                enabled = !isPaymentBlocked,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "取消订单",
                    fontSize = 13.sp,
                    color = if (isPaymentBlocked) Color(0xFFCCCCCC) else XhsTextSecondary,
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Button(
                onClick = onPay,
                enabled = !isPaymentBlocked,
                colors = ButtonDefaults.buttonColors(containerColor = XhsRed),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
            ) {
                Text("去支付", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        } else if (order.canConfirmReceipt) {
            Button(
                onClick = onConfirmReceipt,
                colors = ButtonDefaults.buttonColors(containerColor = XhsRed),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
            ) {
                Text("确认收货", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        } else if (order.canReview) {
            Button(
                onClick = onReview,
                colors = ButtonDefaults.buttonColors(containerColor = XhsRed),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
            ) {
                Text("去评价", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        } else if (order.canApplyAfterSales) {
            OutlinedButton(
                onClick = onAfterSales,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                Text("申请售后", fontSize = 13.sp)
            }
        }
    }
}
