package com.lfc.consumer.ui.home

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.data.model.PostProductRequest
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

private data class ProductOption(val value: String, val label: String)

private val productCategories = listOf(
    ProductOption("SECOND_HAND", "二手闲置"),
    ProductOption("DIGITAL", "数码"),
    ProductOption("BOOK", "书籍"),
    ProductOption("DAILY", "日用"),
    ProductOption("OTHER", "其他"),
)

private val productConditions = listOf(
    ProductOption("BRAND_NEW", "全新"),
    ProductOption("LIKE_NEW", "几乎全新"),
    ProductOption("GOOD", "良好"),
    ProductOption("FAIR", "一般"),
)

private val deliveryMethods = listOf(
    ProductOption("PICKUP", "面交"),
    ProductOption("EXPRESS", "快递"),
    ProductOption("BOTH", "均可"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PublishPostScreen(
    initial: PostDto? = null,
    isSubmitting: Boolean = false,
    platformFeeRateLabel: String? = null,
    alipayBound: Boolean = false,
    alipayLoginIdMasked: String? = null,
    onBack: () -> Unit,
    onBindAlipay: () -> Unit = {},
    onSubmit: (
        title: String,
        content: String,
        imageUris: List<Uri>,
        product: PostProductRequest?,
    ) -> Unit,
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var content by remember { mutableStateOf(initial?.content ?: "") }
    var attachProduct by remember { mutableStateOf(initial?.product != null) }
    var price by remember { mutableStateOf(initial?.product?.price ?: "") }
    var originalPrice by remember { mutableStateOf(initial?.product?.originalPrice ?: "") }
    var category by remember { mutableStateOf(initial?.product?.category ?: "SECOND_HAND") }
    var condition by remember { mutableStateOf(initial?.product?.condition ?: "GOOD") }
    var deliveryMethod by remember { mutableStateOf(initial?.product?.deliveryMethod ?: "PICKUP") }
    val selectedImages = rememberPublishImageSelection()
    val existingImages = initial?.images.orEmpty()
    val canSubmit = content.isNotBlank() || selectedImages.isNotEmpty() || existingImages.isNotEmpty()
    val productPrice = price.toDoubleOrNull() ?: 0.0
    val needsAlipay = attachProduct && productPrice > 0 && !alipayBound

    XhsPublishScreenContainer(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            XhsPublishTopBar(
                title = if (initial == null) "发布笔记" else "编辑笔记",
                actionLabel = if (initial == null) "发布" else "保存",
                onBack = onBack,
                onAction = {
                    val product = if (attachProduct) {
                        PostProductRequest(
                            price = price.toDoubleOrNull() ?: 0.0,
                            originalPrice = originalPrice.toDoubleOrNull(),
                            category = category,
                            condition = condition,
                            deliveryMethod = deliveryMethod,
                        )
                    } else {
                        null
                    }
                    onSubmit(title.trim(), content.trim(), selectedImages.toList(), product)
                },
                actionEnabled = canSubmit &&
                    (!attachProduct || productPrice > 0) &&
                    !needsAlipay,
                isSubmitting = isSubmitting,
            )
            HorizontalDivider(color = Color(0xFFEEEEEE))

            if (needsAlipay) {
                AlipaySetupBanner(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    title = "绑定支付宝后才能发布付费商品",
                    description = "买家通过支付宝付款，确认收货后款项会分账到你的支付宝。请先绑定收款账号。",
                    onBindClick = onBindAlipay,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                PublishImagePicker(
                    existingImageUrls = existingImages,
                    selectedImages = selectedImages,
                )

                Spacer(modifier = Modifier.height(16.dp))

                XhsPublishTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "添加标题（可选）",
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = XhsTextPrimary,
                    ),
                )
                Spacer(modifier = Modifier.height(16.dp))
                XhsPublishTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = "分享你的校园生活、学习心得，或描述你要卖的闲置…",
                    minLines = 5,
                    textStyle = TextStyle(fontSize = 15.sp, lineHeight = 24.sp, color = XhsTextPrimary),
                )

                Spacer(modifier = Modifier.height(12.dp))

                XhsPublishFieldCard {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            XhsPublishSectionTitle("挂载商品")
                            Text(
                                text = "开启后可设置价格，买家支付宝付款，确认收货后分账给你",
                                fontSize = 12.sp,
                                color = XhsTextSecondary,
                            )
                            if (alipayBound) {
                                Spacer(modifier = Modifier.height(6.dp))
                                AlipayBoundStatusChip(
                                    alipayBound = true,
                                    alipayLoginIdMasked = alipayLoginIdMasked,
                                )
                            }
                        }
                        Switch(checked = attachProduct, onCheckedChange = { attachProduct = it })
                    }

                    if (attachProduct) {
                        Spacer(modifier = Modifier.height(14.dp))
                        XhsPublishTextField(
                            value = price,
                            onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                            placeholder = "售价（元）",
                            singleLine = true,
                        )
                        formatPayeeReceiveHint(price, platformFeeRateLabel)?.let { hint ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = hint,
                                fontSize = 12.sp,
                                color = XhsTextSecondary,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        XhsPublishTextField(
                            value = originalPrice,
                            onValueChange = { originalPrice = it.filter { c -> c.isDigit() || c == '.' } },
                            placeholder = "原价（可选）",
                            singleLine = true,
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("分类", fontSize = 13.sp, color = XhsTextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            productCategories.forEach { option ->
                                FilterChip(
                                    selected = category == option.value,
                                    onClick = { category = option.value },
                                    label = { Text(option.label, fontSize = 12.sp) },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("成色", fontSize = 13.sp, color = XhsTextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            productConditions.forEach { option ->
                                FilterChip(
                                    selected = condition == option.value,
                                    onClick = { condition = option.value },
                                    label = { Text(option.label, fontSize = 12.sp) },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("交易方式", fontSize = 13.sp, color = XhsTextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            deliveryMethods.forEach { option ->
                                FilterChip(
                                    selected = deliveryMethod == option.value,
                                    onClick = { deliveryMethod = option.value },
                                    label = { Text(option.label, fontSize = 12.sp) },
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "最多可选 $MAX_PUBLISH_IMAGES 张图片",
                    fontSize = 13.sp,
                    color = XhsTextSecondary,
                )
            }
        }
    }
}
