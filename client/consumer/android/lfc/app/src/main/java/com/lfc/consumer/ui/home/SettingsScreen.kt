package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.data.model.UserProfileDto
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

@Composable
fun SettingsScreen(
    profile: UserProfileDto?,
    isUpdating: Boolean,
    platformFeeRateLabel: String? = null,
    onBack: () -> Unit,
    onOpenOrders: () -> Unit,
    onPrivacyChange: (
        showCommentsPublic: Boolean,
        showFavoritesPublic: Boolean,
        showLikesPublic: Boolean,
    ) -> Unit,
    onAuthorizeAlipay: () -> Unit,
    onBindAlipay: (loginId: String, realName: String?) -> Unit,
    onUnbindAlipay: () -> Unit,
) {
    var showCommentsPublic by remember(profile?.id) { mutableStateOf(profile?.showCommentsPublic ?: false) }
    var showFavoritesPublic by remember(profile?.id) { mutableStateOf(profile?.showFavoritesPublic ?: false) }
    var showLikesPublic by remember(profile?.id) { mutableStateOf(profile?.showLikesPublic ?: false) }
    var showManualAlipayEntry by remember { mutableStateOf(false) }
    var alipayLoginId by remember(profile?.id) { mutableStateOf("") }
    var alipayRealName by remember(profile?.id) { mutableStateOf("") }

    LaunchedEffect(
        profile?.showCommentsPublic,
        profile?.showFavoritesPublic,
        profile?.showLikesPublic,
    ) {
        profile?.let {
            showCommentsPublic = it.showCommentsPublic
            showFavoritesPublic = it.showFavoritesPublic
            showLikesPublic = it.showLikesPublic
        }
    }

    XhsPublishScreenContainer(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    text = "设置",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = XhsTextPrimary,
                )
            }
            HorizontalDivider(color = Color(0xFFEEEEEE))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenOrders),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = XhsRed,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = "我的订单",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = XhsTextPrimary,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "收款账号",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = XhsTextPrimary,
                )
                AlipayBoundStatusChip(
                    alipayBound = profile?.alipayBound == true,
                    alipayLoginIdMasked = profile?.alipayLoginIdMasked,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
                Text(
                    text = buildString {
                        append("发布闲置或收取活动费时需绑定。点击授权将跳转支付宝完成身份验证，")
                        append("买家支付后平台托管，确认收货后通过商家分账转给你（扣除")
                        append(platformFeeRateLabel?.takeIf { it.isNotBlank() } ?: "服务费")
                        append("）。")
                    },
                    fontSize = 12.sp,
                    color = XhsTextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (profile?.alipayBound == true) {
                            Text(
                                text = "已绑定 ${profile.alipayLoginIdMasked ?: "支付宝账号"}",
                                fontSize = 15.sp,
                                color = XhsTextPrimary,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(
                                onClick = onUnbindAlipay,
                                enabled = !isUpdating,
                            ) {
                                Text("解绑收款账号", color = XhsTextSecondary)
                            }
                        } else {
                            Button(
                                onClick = onAuthorizeAlipay,
                                enabled = !isUpdating,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = XhsRed),
                            ) {
                                Text("跳转支付宝授权绑定")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { showManualAlipayEntry = !showManualAlipayEntry },
                                enabled = !isUpdating,
                            ) {
                                Text(
                                    text = if (showManualAlipayEntry) "收起手动填写" else "无法授权？手动填写账号",
                                    color = XhsTextSecondary,
                                )
                            }
                            if (showManualAlipayEntry) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = alipayLoginId,
                                    onValueChange = { alipayLoginId = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("支付宝手机号或邮箱") },
                                    singleLine = true,
                                    enabled = !isUpdating,
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = alipayRealName,
                                    onValueChange = { alipayRealName = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("支付宝实名（建议填写）") },
                                    singleLine = true,
                                    enabled = !isUpdating,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { onBindAlipay(alipayLoginId, alipayRealName) },
                                    enabled = !isUpdating && alipayLoginId.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = XhsRed),
                                ) {
                                    Text("手动绑定收款账号")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "隐私设置",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = XhsTextPrimary,
                )
                Text(
                    text = "关闭后，他人访问你的主页时将无法查看对应内容",
                    fontSize = 12.sp,
                    color = XhsTextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                ) {
                    Column {
                        PrivacySwitchRow(
                            title = "公开我的评论",
                            subtitle = "他人可查看你发表过的评论",
                            checked = showCommentsPublic,
                            enabled = profile != null && !isUpdating,
                            onCheckedChange = {
                                showCommentsPublic = it
                                onPrivacyChange(it, showFavoritesPublic, showLikesPublic)
                            },
                        )
                        HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(start = 16.dp))
                        PrivacySwitchRow(
                            title = "公开我的收藏",
                            subtitle = "他人可查看你收藏的内容",
                            checked = showFavoritesPublic,
                            enabled = profile != null && !isUpdating,
                            onCheckedChange = {
                                showFavoritesPublic = it
                                onPrivacyChange(showCommentsPublic, it, showLikesPublic)
                            },
                        )
                        HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(start = 16.dp))
                        PrivacySwitchRow(
                            title = "公开我的赞过",
                            subtitle = "他人可查看你赞过的内容",
                            checked = showLikesPublic,
                            enabled = profile != null && !isUpdating,
                            onCheckedChange = {
                                showLikesPublic = it
                                onPrivacyChange(showCommentsPublic, showFavoritesPublic, it)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacySwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = XhsTextPrimary, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 12.sp, color = XhsTextSecondary, modifier = Modifier.padding(top = 2.dp))
        }
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedTrackColor = XhsRed,
                checkedThumbColor = Color.White,
            ),
        )
    }
}
