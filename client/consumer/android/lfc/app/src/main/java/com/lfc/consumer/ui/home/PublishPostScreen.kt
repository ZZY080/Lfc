package com.lfc.consumer.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.lfc.consumer.data.model.DEFAULT_POST_CATEGORY
import com.lfc.consumer.data.model.DEFAULT_POST_PRODUCT_CATEGORY
import com.lfc.consumer.data.model.LocationPick
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.data.model.PostProductRequest
import com.lfc.consumer.location.AmapLocationHelper
import com.lfc.consumer.location.hasValidCoordinate
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val postLocationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PublishPostScreen(
    initial: PostDto? = null,
    publishCategories: List<String> = emptyList(),
    isSubmitting: Boolean = false,
    platformFeeRateLabel: String? = null,
    alipayBound: Boolean = false,
    alipayLoginIdMasked: String? = null,
    onBack: () -> Unit,
    onBindAlipay: () -> Unit = {},
    pendingLocationPick: LocationPick? = null,
    onConsumeLocationPick: () -> Unit = {},
    onOpenLocationSearch: (
        latitude: Double?,
        longitude: Double?,
    ) -> Unit = { _, _ -> },
    onSubmit: (
        title: String,
        content: String,
        imageUris: List<Uri>,
        keptExistingImageUrls: List<String>,
        category: String,
        product: PostProductRequest?,
        latitude: Double?,
        longitude: Double?,
        location: String?,
    ) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val formSaveKey = if (initial == null) {
        "publish_post_create"
    } else {
        "publish_post_edit_${initial.id}"
    }
    var title by rememberSaveable(formSaveKey) { mutableStateOf(initial?.title ?: "") }
    var content by rememberSaveable(formSaveKey) { mutableStateOf(initial?.content ?: "") }
    var noteCategory by rememberSaveable(formSaveKey) {
        mutableStateOf(initial?.category ?: DEFAULT_POST_CATEGORY)
    }
    var locationLabel by rememberSaveable(formSaveKey) {
        mutableStateOf(initial?.location.orEmpty())
    }
    var latitude by rememberSaveable(formSaveKey) { mutableStateOf(initial?.latitude) }
    var longitude by rememberSaveable(formSaveKey) { mutableStateOf(initial?.longitude) }
    var isLocating by remember { mutableStateOf(false) }
    var locationHint by remember { mutableStateOf<String?>(null) }
    var locateJob by remember { mutableStateOf<Job?>(null) }
    var autoLocateConsumed by rememberSaveable(formSaveKey) {
        mutableStateOf(hasValidCoordinate(initial?.latitude, initial?.longitude))
    }
    var attachProduct by rememberSaveable(formSaveKey) {
        mutableStateOf(initial?.product != null)
    }
    var price by rememberSaveable(formSaveKey) {
        mutableStateOf(initial?.product?.price ?: "")
    }
    var originalPrice by rememberSaveable(formSaveKey) {
        mutableStateOf(initial?.product?.originalPrice ?: "")
    }
    var category by rememberSaveable(formSaveKey) {
        mutableStateOf(initial?.product?.category ?: DEFAULT_POST_PRODUCT_CATEGORY)
    }
    var condition by rememberSaveable(formSaveKey) {
        mutableStateOf(initial?.product?.condition ?: "GOOD")
    }
    var deliveryMethod by rememberSaveable(formSaveKey) {
        mutableStateOf(initial?.product?.deliveryMethod ?: "PICKUP")
    }
    val selectedImages = rememberPublishImageSelection(saveKey = "${formSaveKey}_images")
    val keptExistingImages = rememberPublishExistingImages(
        initial = initial?.images.orEmpty(),
        key = initial?.id,
        saveKey = "${formSaveKey}_existing_images",
    )
    val hasLocation = hasValidCoordinate(latitude, longitude)
    val canSubmit = (content.isNotBlank() || selectedImages.isNotEmpty() || keptExistingImages.isNotEmpty()) &&
        hasLocation
    val productPrice = price.toDoubleOrNull() ?: 0.0
    val needsAlipay = attachProduct && productPrice > 0 && !alipayBound

    fun hasLocationPermission(): Boolean = postLocationPermissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun startLocate() {
        locateJob?.cancel()
        locationHint = null
        locateJob = scope.launch {
            isLocating = true
            try {
                AmapLocationHelper.getCurrentLocation(context)
                    .onSuccess {
                        locationLabel = it.address
                        latitude = it.latitude
                        longitude = it.longitude
                    }
                    .onFailure { locationHint = it.message ?: "定位失败，请检查定位权限或高德 Key" }
            } finally {
                isLocating = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results.values.all { it }) {
            startLocate()
        } else {
            locationHint = "需要定位权限才能记录发布位置"
        }
    }

    fun requestLocate() {
        if (hasLocationPermission()) {
            startLocate()
        } else {
            permissionLauncher.launch(postLocationPermissions)
        }
    }

    LaunchedEffect(pendingLocationPick) {
        pendingLocationPick?.let { pick ->
            locateJob?.cancel()
            locateJob = null
            isLocating = false
            locationLabel = pick.label
            latitude = pick.latitude
            longitude = pick.longitude
            locationHint = null
            autoLocateConsumed = true
            onConsumeLocationPick()
        }
    }

    LaunchedEffect(Unit) {
        if (pendingLocationPick != null) return@LaunchedEffect
        if (autoLocateConsumed || hasValidCoordinate(latitude, longitude)) return@LaunchedEffect
        autoLocateConsumed = true
        requestLocate()
    }

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
                    onSubmit(
                        title.trim(),
                        content.trim(),
                        selectedImages.toList(),
                        keptExistingImages.toList(),
                        noteCategory,
                        product,
                        latitude,
                        longitude,
                        locationLabel.trim().ifBlank { null },
                    )
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
                    existingImageUrls = keptExistingImages,
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
                    placeholder = "分享你的校园生活、学习心得，或描述你要出售的商品…",
                    minLines = 5,
                    textStyle = TextStyle(fontSize = 15.sp, lineHeight = 24.sp, color = XhsTextPrimary),
                )

                Spacer(modifier = Modifier.height(12.dp))

                XhsPublishFieldCard {
                    XhsPublishSectionTitle("笔记类型")
                    Text(
                        text = "选择后笔记会出现在发现页对应频道",
                        fontSize = 12.sp,
                        color = XhsTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        publishCategories.forEach { option ->
                            FilterChip(
                                selected = noteCategory == option,
                                onClick = { noteCategory = option },
                                label = { Text(option, fontSize = 12.sp) },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                XhsPublishFieldCard {
                    XhsPublishSectionTitle("发布位置")
                    Text(
                        text = "用于展示笔记与你的距离，可搜索楼栋（如3号楼）或定位当前位置",
                        fontSize = 12.sp,
                        color = XhsTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    XhsPublishLocationPicker(
                        value = locationLabel,
                        placeholder = if (isLocating) "正在获取当前位置…" else "点击搜索楼栋或地点",
                        isLoading = isLocating,
                        onOpenSearch = {
                            onOpenLocationSearch(latitude, longitude)
                        },
                        onLocate = ::requestLocate,
                    )
                    locationHint?.let { hint ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = hint,
                            fontSize = 12.sp,
                            color = XhsRed,
                        )
                    }
                    if (!hasLocation && !isLocating) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "获取位置后才能发布，以便他人看到距离",
                            fontSize = 12.sp,
                            color = XhsRed,
                        )
                    }
                }

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
                                text = "可挂载任意类型商品，设置价格后买家支付宝付款，确认收货后分账给你",
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
                            POST_PRODUCT_CATEGORIES.forEach { option ->
                                FilterChip(
                                    selected = category == option.value,
                                    onClick = { category = option.value },
                                    label = { Text(option.label, fontSize = 12.sp) },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("商品状态", fontSize = 13.sp, color = XhsTextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            POST_PRODUCT_CONDITIONS.forEach { option ->
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
                            POST_PRODUCT_DELIVERY_METHODS.forEach { option ->
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
