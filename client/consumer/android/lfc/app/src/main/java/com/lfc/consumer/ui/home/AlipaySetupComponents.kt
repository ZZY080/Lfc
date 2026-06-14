package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

@Composable
fun AlipaySetupBanner(
    modifier: Modifier = Modifier,
    title: String = "绑定支付宝，才能收到买家付款",
    description: String = "普通支付宝用户即可：在设置中跳转支付宝 App 授权，无需商户账号。买家确认收货后，款项会通过商家分账转给你。",
    actionLabel: String = "去支付宝授权",
    onBindClick: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFF8F0),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color(0xFF1677FF),
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = XhsTextPrimary,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = XhsTextSecondary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onBindClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = XhsRed),
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(actionLabel, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
fun AlipayBoundStatusChip(
    alipayBound: Boolean,
    alipayLoginIdMasked: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(
                color = if (alipayBound) Color(0xFFE8F7EE) else Color(0xFFFFF3E8),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (alipayBound) {
                "收款：${alipayLoginIdMasked ?: "已绑定支付宝"}"
            } else {
                "收款账号未绑定"
            },
            fontSize = 11.sp,
            color = if (alipayBound) Color(0xFF1F8A4C) else Color(0xFFB86A00),
        )
    }
}

const val ALIPAY_BIND_REQUIRED_MESSAGE =
    "发布付费内容前，请先在「我的 → 设置」跳转支付宝授权"

const val ALIPAY_BIND_HINT_FOR_SELLERS =
    "若要挂载商品或收取活动费，请在「我的 → 设置」跳转支付宝 App 完成授权"
