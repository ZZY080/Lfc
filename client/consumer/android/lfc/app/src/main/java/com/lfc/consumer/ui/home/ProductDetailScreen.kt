package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.data.model.PostProductDto
import com.lfc.consumer.data.model.PaymentOrderDetailDto
import com.lfc.consumer.data.model.displayName
import com.lfc.consumer.data.model.isOnSale
import com.lfc.consumer.data.model.isSold
import com.lfc.consumer.data.model.productDisplayTitle
import com.lfc.consumer.ui.theme.XhsBackground
import com.lfc.consumer.ui.theme.XhsDivider
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsRedContainer
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

private val sheetRadius = 16.dp
private val imageAspect = 1f

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
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 0.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(52.dp)
                    .background(XhsRed),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
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
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = XhsTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatPriceYuan(product.price),
                        color = XhsRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Text(
                        text = " · ${productCategoryLabel(product.category)}",
                        fontSize = 12.sp,
                        color = XhsTextSecondary,
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(18.dp),
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
            .background(XhsBackground),
    ) {
        when {
            isLoading -> XhsDetailLoading(Modifier.fillMaxSize())
            post == null || post.product == null ->
                XhsDetailEmpty("商品不存在或已下架", Modifier.fillMaxSize())
            else -> {
                val product = post.product!!
                val images = post.images.orEmpty()
                val isSelf = currentUserId != null && currentUserId == post.authorId
                val authorLabel = post.author?.displayName() ?: "同学${post.authorId}"
                val displayTitle = post.productDisplayTitle()
                val description = productDescriptionText(post)

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
                                onBack = onBack,
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = (-sheetRadius))
                                    .clip(RoundedCornerShape(topStart = sheetRadius, topEnd = sheetRadius))
                                    .background(Color.White)
                                    .padding(top = 18.dp, bottom = 24.dp),
                            ) {
                                ProductPriceSection(
                                    product = product,
                                    title = displayTitle,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                FlowRow(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    ProductFilledTag(productCategoryLabel(product.category))
                                    ProductFilledTag(productConditionLabel(product.condition))
                                    ProductFilledTag(productDeliveryLabel(product.deliveryMethod))
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                ProductC2CTradeNotice(
                                    productPrice = product.price,
                                    platformFeeRateLabel = platformFeeRateLabel,
                                    autoConfirmDays = autoConfirmDays,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )

                                Spacer(modifier = Modifier.height(18.dp))
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = XhsDivider,
                                    thickness = 0.5.dp,
                                )
                                Spacer(modifier = Modifier.height(18.dp))

                                ProductSellerCard(
                                    authorLabel = authorLabel,
                                    avatarUrl = post.author?.avatarUrl,
                                    deliveryLabel = productDeliveryLabel(product.deliveryMethod),
                                    onClick = { onAuthorClick(post.authorId) },
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )

                                if (description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(18.dp))
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = XhsDivider,
                                        thickness = 0.5.dp,
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        Text(
                                            text = "商品描述",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = XhsTextPrimary,
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = description,
                                            fontSize = 15.sp,
                                            color = Color(0xFF555555),
                                            lineHeight = 24.sp,
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                ProductLinkedNoteCard(
                                    coverUrl = images.firstOrNull(),
                                    postId = post.id,
                                    onClick = onViewNote,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }
                    }

                    ProductDetailBottomBar(
                        product = product,
                        isSelf = isSelf,
                        isPurchasing = isPurchasing,
                        isConfirmingReceipt = isConfirmingReceipt,
                        purchaseOrder = purchaseOrder,
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
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(imageAspect),
    ) {
        if (images.isNotEmpty()) {
            XhsDetailImageCarousel(
                images = images,
                contentDescription = displayTitle,
                aspectRatio = imageAspect,
                indicatorStyle = CarouselIndicatorStyle.Counter,
                indicatorBottomPadding = 28.dp,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(coverGradientForId(postId)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.22f), Color.Transparent),
                    ),
                ),
        )

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 12.dp, top = 6.dp)
                .size(34.dp)
                .shadow(4.dp, CircleShape, clip = false, ambientColor = Color.Black.copy(0.12f))
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.94f)),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = XhsTextPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ProductPriceSection(
    product: PostProductDto,
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "¥",
                    color = XhsRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
                Text(
                    text = formatPriceNumber(product.price),
                    color = XhsRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    letterSpacing = (-0.5).sp,
                )
            }
            ProductStatusPill(product = product)
        }

        val original = product.originalPrice?.toDoubleOrNull()
        val current = product.price.toDoubleOrNull()
        if (original != null && current != null && original > current) {
            Text(
                text = "原价 ${formatPriceYuan(product.originalPrice)}",
                color = XhsTextSecondary,
                fontSize = 12.sp,
                textDecoration = TextDecoration.LineThrough,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            color = XhsTextPrimary,
            lineHeight = 25.sp,
        )
    }
}

@Composable
private fun ProductStatusPill(product: PostProductDto) {
    val (text, bg, fg) = when {
        product.isSold() -> Triple("已售出", Color(0xFFF3F3F3), XhsTextSecondary)
        product.isOnSale() -> Triple("在售", XhsRedContainer, XhsRed)
        else -> Triple("已下架", Color(0xFFF3F3F3), XhsTextSecondary)
    }
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun ProductC2CTradeNotice(
    productPrice: String,
    platformFeeRateLabel: String?,
    autoConfirmDays: Int,
    modifier: Modifier = Modifier,
) {
    val receiveHint = formatPayeeReceiveHint(productPrice, platformFeeRateLabel)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFFFF8F0)),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(76.dp)
                .background(XhsRed),
        )
        Text(
            text = buildString {
                append("同学间闲置交易 · 支付宝付款 · 平台托管")
                if (!receiveHint.isNullOrBlank()) {
                    append("\n")
                    append(receiveHint)
                }
                append("\n")
                append("确认收货后分账给卖家 · ${autoConfirmDays}天未确认将自动完成")
            },
            fontSize = 12.sp,
            color = Color(0xFF996633),
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun ProductFilledTag(label: String) {
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(XhsBackground)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        fontSize = 12.sp,
        color = XhsTextSecondary,
    )
}

@Composable
private fun ProductSellerCard(
    authorLabel: String,
    avatarUrl: String?,
    deliveryLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(0.5.dp, XhsDivider, RoundedCornerShape(10.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        XhsProfileAvatar(label = authorLabel, size = 44, avatarUrl = avatarUrl)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = authorLabel,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = XhsTextPrimary,
            )
            Text(
                text = "卖家 · 校园闲置 · $deliveryLabel",
                fontSize = 12.sp,
                color = XhsTextSecondary,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = XhsBackground,
        ) {
            Text(
                text = "进店",
                fontSize = 12.sp,
                color = XhsTextPrimary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun ProductLinkedNoteCard(
    coverUrl: String?,
    postId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, XhsDivider),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
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
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "查看关联笔记",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = XhsTextPrimary,
                )
                Text(
                    text = "了解发布背景和更多图片",
                    fontSize = 12.sp,
                    color = XhsTextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ProductDetailBottomBar(
    product: PostProductDto,
    isSelf: Boolean,
    isPurchasing: Boolean,
    isConfirmingReceipt: Boolean,
    purchaseOrder: PaymentOrderDetailDto?,
    onContactSeller: () -> Unit,
    onPurchase: () -> Unit,
    onConfirmReceipt: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = Color.White,
    ) {
        when {
            purchaseOrder?.canConfirmReceipt == true && !isSelf -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = "已付款，面交/收货后请确认，卖家才会收到款项",
                        fontSize = 12.sp,
                        color = XhsTextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Button(
                        onClick = onConfirmReceipt,
                        enabled = !isConfirmingReceipt,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = XhsRed),
                        shape = RoundedCornerShape(22.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    ) {
                        if (isConfirmingReceipt) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = "确认收货",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            )
                        }
                    }
                }
            }
            product.isOnSale() && !isSelf -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        onClick = onContactSeller,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8E8E8)),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = XhsTextPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "我想要",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = XhsTextPrimary,
                            )
                        }
                    }

                    Button(
                        onClick = onPurchase,
                        enabled = !isPurchasing,
                        modifier = Modifier
                            .weight(1.15f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = XhsRed),
                        shape = RoundedCornerShape(22.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    ) {
                        if (isPurchasing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = "立即支付 ${formatPriceYuan(product.price)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        }
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
                            !isSelf -> "交易已完成，款项已分账给卖家"
                        purchaseOrder?.status.equals("CONFIRMED", ignoreCase = true) &&
                            !isSelf -> "已确认收货，分账处理中"
                        isSelf && product.isOnSale() -> "已发布，等待同学咨询或支付"
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
