package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.data.model.FulfillmentStepDto
import com.lfc.consumer.data.model.OrderFulfillmentGuaranteeDto
import com.lfc.consumer.data.model.PaymentOrderListItemDto
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

@Composable
fun OrderCenterFulfillmentBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFF8F0),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                tint = Color(0xFF1677FF),
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "平台履约保障",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = XhsTextPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "闲置商品：支付后平台托管，确认收货后分账给卖家；活动报名：锁定名额，活动结束后分账给发起人。未履约均可申请退款。",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = XhsTextSecondary,
                )
            }
        }
    }
}

@Composable
fun OrderFulfillmentGuaranteeCard(
    fulfillment: OrderFulfillmentGuaranteeDto?,
    modifier: Modifier = Modifier,
) {
    if (fulfillment == null) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF7FAFF),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color(0xFF1677FF),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = fulfillment.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF1565C0),
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = fulfillment.summary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Color(0xFF445566),
            )
            if (fulfillment.steps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                OrderFulfillmentStepRow(steps = fulfillment.steps)
            }
        }
    }
}

@Composable
private fun OrderFulfillmentStepRow(steps: List<FulfillmentStepDto>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        steps.forEach { step ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                step.done -> Color(0xFF1B9B55)
                                step.active -> XhsRed
                                else -> Color(0xFFD8DEE8)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (step.done) {
                        Text("✓", color = Color.White, fontSize = 10.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = step.label,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    color = when {
                        step.active -> XhsRed
                        step.done -> Color(0xFF1B9B55)
                        else -> XhsTextSecondary
                    },
                    fontWeight = if (step.active) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
fun OrderBizMetaLines(
    order: PaymentOrderListItemDto,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (order.bizType == "ACTIVITY_JOIN") {
            order.bizStartTime?.let { start ->
                order.bizEndTime?.let { end ->
                    Text(
                        text = "活动时间 ${formatOrderDateTime(start)} ~ ${formatOrderDateTime(end)}",
                        fontSize = 12.sp,
                        color = XhsTextSecondary,
                        lineHeight = 18.sp,
                    )
                }
            }
            order.bizLocation?.takeIf { it.isNotBlank() }?.let { location ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "活动地点 $location",
                    fontSize = 12.sp,
                    color = XhsTextSecondary,
                    maxLines = 1,
                )
            }
        }
        order.autoConfirmAt?.takeIf { order.status == "PAID" }?.let { deadline ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (order.bizType == "ACTIVITY_JOIN") {
                    "预计 ${formatOrderDateTime(deadline)} 后分账"
                } else {
                    "请在 ${formatOrderDateTime(deadline)} 前确认收货"
                },
                fontSize = 12.sp,
                color = Color(0xFFB86A00),
            )
        }
    }
}

private fun formatOrderDateTime(value: String): String {
    return value.replace("T", " ").take(16)
}
