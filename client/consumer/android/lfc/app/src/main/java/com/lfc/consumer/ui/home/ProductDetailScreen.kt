package com.lfc.consumer.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lfc.consumer.data.model.PaymentOrderDetailDto
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.data.model.PostProductDto
import com.lfc.consumer.data.model.displayName
import com.lfc.consumer.data.model.isOnSale
import com.lfc.consumer.data.model.isSold
import com.lfc.consumer.data.model.productDisplayTitle
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsRedContainer
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

private val pageBg = Color(0xFFF4F4F5)
private val cardRadius = 16.dp
private val cardOverlap = 18.dp

@Composable
fun PostProductLinkCard(
    post: PostDto,
    product: PostProductDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coverUrl = post.images?.firstOrNull()
    val title = post.productDisplayTitle()

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(cardRadius),
        color = Color.White,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(coverGradientForId(post.id)),
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = XhsTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatPriceYuan(product.price),
                    color = XhsRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFD0D0D0),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
    post: PostDto?,
    isLoading: Boolean,
    isPurchasing: Boolean,
    isPaymentProcessing: Boolean = false,
    isConfirmingReceipt: Boolean = false,
    platformFeeRateLabel: String? = null,
    autoConfirmDays: Int = 7,
    purchaseOrder: PaymentOrderDetailDto? = null,
    currentUserId: Int? = null,
    onBack: () -> Unit,
    onViewNote: () -> Unit,
    onAuthorClick: (Int) -> Unit,
    onContactSeller: () -> Unit,
    onPurchase: () -> Unit,
    onConfirmReceipt: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg),
    ) {
        when {
            isLoading -> XhsDetailLoading(Modifier.fillMaxSize(), style = DetailSkeletonStyle.Product)
            post == null || post.product == null ->
                XhsDetailEmpty("商品不存在或已下架", Modifier.fillMaxSize())
            else -> {
                val product = post.product!!
                val images = post.images.orEmpty()
                val isSelf = currentUserId != null && currentUserId == post.authorId
                val authorLabel = post.author?.displayName() ?: "同学${post.authorId}"
                val displayTitle = post.productDisplayTitle()
                val description = productDescriptionText(post)
                val deliveryLabel = productDeliveryLabel(product.deliveryMethod)
                val receiveHint = formatPayeeReceiveHint(product.price, platformFeeRateLabel)

                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                        ) {
                            ProductHeroGallery(
                                images = images,
                                postId = post.id,
                                displayTitle = displayTitle,
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = -cardOverlap)
                                    .padding(horizontal = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                ProductMainCard(
                                    product = product,
                                    title = displayTitle,
                                )

                                ProductTrustCard(
                                    deliveryLabel = deliveryLabel,
                                    autoConfirmDays = autoConfirmDays,
                                    receiveHint = receiveHint,
                                )

                                ProductSellerCard(
                                    authorLabel = authorLabel,
                                    avatarUrl = post.author?.avatarUrl,
                                    onClick = { onAuthorClick(post.authorId) },
                                )

                                if (description.isNotBlank()) {
                                    ProductSectionCard(title = "商品描述") {
                                        Text(
                                            text = description,
                                            fontSize = 15.sp,
                                            color = Color(0xFF444444),
                                            lineHeight = 26.sp,
                                        )
                                    }
                                }

                                ProductLinkedNoteCard(
                                    coverUrl = images.firstOrNull(),
                                    postId = post.id,
                                    onClick = onViewNote,
                                )

                                Spacer(modifier = Modifier.height(88.dp))
                            }
                        }

                        ProductDetailTopBar(
                            onBack = onBack,
                            authorLabel = authorLabel,
                            authorAvatarUrl = post.author?.avatarUrl,
                            isSelf = isSelf,
                            onVisitShop = { onAuthorClick(post.authorId) },
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                    }

                    ProductDetailBottomBar(
                        product = product,
                        isSelf = isSelf,
                        isPurchasing = isPurchasing,
                        isPaymentProcessing = isPaymentProcessing,
                        isConfirmingReceipt = isConfirmingReceipt,
                        purchaseOrder = purchaseOrder,
                        confirmTitle = post.title,
                        confirmAmount = purchaseOrder?.amount ?: product.price,
                        onContactSeller = onContactSeller,
                        onPurchase = onPurchase,
                        onConfirmReceipt = onConfirmReceipt,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductHeroGallery(
    images: List<String>,
    postId: Int,
    displayTitle: String,
) {
    if (images.isNotEmpty()) {
        XhsDetailImageCarousel(
            images = images,
            contentDescription = displayTitle,
            aspectRatio = 1f,
            indicatorStyle = CarouselIndicatorStyle.Counter,
            indicatorBottomPadding = 36.dp,
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(coverGradientForId(postId)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.ShoppingBag,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(52.dp),
            )
        }
    }
}

@Composable
private fun ProductDetailTopBar(
    onBack: () -> Unit,
    authorLabel: String,
    authorAvatarUrl: String?,
    isSelf: Boolean,
    onVisitShop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = XhsTextPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }

                if (!isSelf) {
                    Surface(
                        onClick = onVisitShop,
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            XhsProfileAvatar(
                                label = authorLabel,
                                size = 24,
                                avatarUrl = authorAvatarUrl,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "进店",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = XhsTextPrimary,
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = null,
                                tint = XhsTextSecondary,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 0.5.dp)
        }
    }
}

@Composable
private fun ProductStatusBadge(
    product: PostProductDto,
    modifier: Modifier = Modifier,
) {
    val (text, bg, fg) = when {
        product.isSold() -> Triple("已售出", Color(0xFFF3F4F6), Color(0xFF6B7280))
        product.isOnSale() -> Triple("在售", Color(0xFFECFDF3), Color(0xFF15803D))
        else -> Triple("已下架", Color(0xFFF3F4F6), Color(0xFF9CA3AF))
    }
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductMainCard(
    product: PostProductDto,
    title: String,
) {
    val original = product.originalPrice?.toDoubleOrNull()
    val current = product.price.toDoubleOrNull()
    val hasDiscount = original != null && current != null && original > current
    val discountPercent = if (hasDiscount) {
        ((1 - current / original) * 100).toInt().coerceIn(1, 99)
    } else {
        null
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cardRadius),
        color = Color.White,
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "¥",
                    color = XhsRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(bottom = 5.dp),
                )
                Text(
                    text = formatPriceNumber(product.price),
                    color = XhsRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    letterSpacing = (-1).sp,
                )
                if (hasDiscount) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = XhsRedContainer,
                    ) {
                        Text(
                            text = "${discountPercent}折",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = XhsRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            if (hasDiscount) {
                Text(
                    text = "原价 ${formatPriceYuan(product.originalPrice)}",
                    color = XhsTextSecondary,
                    fontSize = 13.sp,
                    textDecoration = TextDecoration.LineThrough,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = XhsTextPrimary,
                lineHeight = 28.sp,
            )

            Spacer(modifier = Modifier.height(14.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProductStatusBadge(product = product)
                ProductMetaChip(
                    label = productCategoryLabel(product.category),
                    background = Color(0xFFF3F4F6),
                    textColor = Color(0xFF6B7280),
                )
                ProductMetaChip(
                    label = productConditionLabel(product.condition),
                    background = Color(0xFFECFDF3),
                    textColor = Color(0xFF15803D),
                )
                ProductMetaChip(
                    label = productDeliveryLabel(product.deliveryMethod),
                    background = Color(0xFFEFF6FF),
                    textColor = Color(0xFF1D4ED8),
                )
            }
        }
    }
}

@Composable
private fun ProductMetaChip(
    label: String,
    background: Color,
    textColor: Color,
) {
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        fontSize = 12.sp,
        color = textColor,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun ProductTrustCard(
    deliveryLabel: String,
    autoConfirmDays: Int,
    receiveHint: String?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cardRadius),
        color = Color.White,
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ProductTrustItem(
                    icon = Icons.Default.Payments,
                    iconTint = Color(0xFF2563EB),
                    iconBg = Color(0xFFEFF6FF),
                    title = "支付宝",
                    subtitle = "安全付款",
                )
                ProductTrustDivider()
                ProductTrustItem(
                    icon = Icons.Default.Shield,
                    iconTint = Color(0xFF16A34A),
                    iconBg = Color(0xFFECFDF3),
                    title = "平台托管",
                    subtitle = "确认后分账",
                )
                ProductTrustDivider()
                ProductTrustItem(
                    icon = Icons.Default.LocalShipping,
                    iconTint = Color(0xFFEA580C),
                    iconBg = Color(0xFFFFF7ED),
                    title = deliveryLabel,
                    subtitle = "交付方式",
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = buildString {
                    append("${autoConfirmDays}天未确认自动完成")
                    if (!receiveHint.isNullOrBlank()) {
                        append(" · ")
                        append(receiveHint)
                    }
                    append(" · 未确认前可在订单中心申请退款")
                },
                fontSize = 12.sp,
                color = XhsTextSecondary,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun ProductTrustDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(Color(0xFFF0F0F0)),
    )
}

@Composable
private fun ProductTrustItem(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(96.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = XhsTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = XhsTextSecondary,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun ProductSellerCard(
    authorLabel: String,
    avatarUrl: String?,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cardRadius),
        color = Color.White,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            XhsProfileAvatar(
                label = authorLabel,
                size = 48,
                avatarUrl = avatarUrl,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = authorLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = XhsTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "校园卖家",
                        fontSize = 11.sp,
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFECFDF3))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "查看主页",
                        fontSize = 12.sp,
                        color = XhsTextSecondary,
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFD0D0D0),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ProductSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cardRadius),
        color = Color.White,
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = XhsTextPrimary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ProductLinkedNoteCard(
    coverUrl: String?,
    postId: Int,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cardRadius),
        color = Color.White,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(coverGradientForId(postId)),
            ) {
                coverUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Text("笔记", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "关联笔记",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = XhsTextPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "查看发布者的图文介绍与更多细节",
                    fontSize = 13.sp,
                    color = XhsTextSecondary,
                    lineHeight = 18.sp,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFD0D0D0),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ProductDetailBottomBar(
    product: PostProductDto,
    isSelf: Boolean,
    isPurchasing: Boolean,
    isPaymentProcessing: Boolean,
    isConfirmingReceipt: Boolean,
    purchaseOrder: PaymentOrderDetailDto?,
    confirmTitle: String,
    confirmAmount: String,
    onContactSeller: () -> Unit,
    onPurchase: () -> Unit,
    onConfirmReceipt: () -> Unit,
) {
    val purchaseBusy = isPurchasing || isPaymentProcessing
    var showConfirmReceipt by remember { mutableStateOf(false) }

    if (showConfirmReceipt) {
        ConfirmReceiptAlertDialog(
            title = confirmTitle,
            amount = confirmAmount,
            bizType = "POST_PRODUCT_PURCHASE",
            onDismiss = { showConfirmReceipt = false },
            onConfirm = {
                showConfirmReceipt = false
                onConfirmReceipt()
            },
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 12.dp,
        color = Color.White,
        tonalElevation = 0.dp,
    ) {
        Column {
            HorizontalDivider(color = Color(0xFFEBEBEB), thickness = 0.5.dp)
            when {
                purchaseOrder?.canConfirmReceipt == true && !isSelf -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = "已付款，收货后请确认，卖家才会收到款项",
                            fontSize = 12.sp,
                            color = XhsTextSecondary,
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                        Button(
                            onClick = { showConfirmReceipt = true },
                            enabled = !isConfirmingReceipt,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = XhsRed),
                            shape = RoundedCornerShape(24.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        ) {
                            if (isConfirmingReceipt) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("确认收货", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
                product.isOnSale() && !isSelf -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 18.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "¥",
                                    color = XhsRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(bottom = 2.dp),
                                )
                                Text(
                                    text = formatPriceNumber(product.price),
                                    color = XhsRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                )
                            }
                            Text(
                                text = "平台托管 · 安全交易",
                                fontSize = 11.sp,
                                color = XhsTextSecondary,
                            )
                        }

                        Surface(
                            onClick = onContactSeller,
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = Color(0xFFF5F5F5),
                            border = BorderStroke(0.5.dp, Color(0xFFE8E8E8)),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ChatBubbleOutline,
                                    contentDescription = "我想要",
                                    tint = XhsTextPrimary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = onPurchase,
                            enabled = !purchaseBusy,
                            modifier = Modifier
                                .height(48.dp)
                                .width(132.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = XhsRed),
                            shape = RoundedCornerShape(24.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        ) {
                            if (purchaseBusy) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("立即购买", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = when {
                                purchaseOrder?.status.equals("SETTLED", ignoreCase = true) &&
                                    !isSelf -> "交易已完成"
                                purchaseOrder?.status.equals("CONFIRMED", ignoreCase = true) &&
                                    !isSelf -> "已确认收货，分账处理中"
                                isSelf && product.isOnSale() -> "已发布，等待同学咨询或购买"
                                product.isSold() -> "已被同学拍下"
                                else -> "暂不可交易"
                            },
                            color = XhsTextSecondary,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun productDescriptionText(post: PostDto): String {
    val content = post.content.trim()
    val title = post.title.trim()
    if (content.isBlank()) return ""
    if (content == title || content == title.take(30)) return ""
    val displayTitle = post.productDisplayTitle()
    if (content.lineSequence().firstOrNull() == displayTitle) {
        return content.lineSequence().drop(1).joinToString("\n").trim()
    }
    return content
}

private fun formatPriceNumber(amount: String?): String {
    val value = amount?.toDoubleOrNull() ?: return "0"
    return if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)
}
