package com.lfc.consumer.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.data.model.LocationPick
import com.lfc.consumer.location.AmapLocationHelper
import com.lfc.consumer.location.hasValidCoordinate
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val locationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

@Composable
fun PublishActivityScreen(
    draft: PublishActivityFormDraft,
    onDraftChange: (PublishActivityFormDraft) -> Unit,
    onPatchDraft: ((PublishActivityFormDraft) -> PublishActivityFormDraft) -> Unit = { patch ->
        onDraftChange(patch(draft))
    },
    initial: ActivityDto? = null,
    isSubmitting: Boolean = false,
    platformFeeRateLabel: String? = null,
    alipayBound: Boolean = false,
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
        description: String,
        location: String,
        latitude: Double?,
        longitude: Double?,
        startTime: String,
        endTime: String,
        maxParticipants: Int,
        fee: Double,
        imageUris: List<Uri>,
        keptExistingImageUrls: List<String>,
    ) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var isLocating by remember { mutableStateOf(false) }
    var locationHint by remember { mutableStateOf<String?>(null) }
    var locateJob by remember { mutableStateOf<Job?>(null) }
    val selectedImages = remember { mutableStateListOf<Uri>() }
    val keptExistingImages = remember { mutableStateListOf<String>() }
    var imagesHydrated by remember { mutableStateOf(false) }

    LaunchedEffect(draft) {
        if (!imagesHydrated) {
            selectedImages.clear()
            selectedImages.addAll(draft.selectedImageUris.map(Uri::parse))
            keptExistingImages.clear()
            keptExistingImages.addAll(draft.existingImageUrls)
            imagesHydrated = true
        }
    }

    SideEffect {
        if (!imagesHydrated) return@SideEffect
        val uriStrings = selectedImages.map { it.toString() }
        val existing = keptExistingImages.toList()
        if (uriStrings != draft.selectedImageUris || existing != draft.existingImageUrls) {
            onDraftChange(
                draft.copy(
                    selectedImageUris = uriStrings,
                    existingImageUrls = existing,
                ),
            )
        }
    }

    val hasLocation = hasValidCoordinate(draft.latitude, draft.longitude)

    fun hasLocationPermission(): Boolean = locationPermissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun startLocate() {
        locateJob?.cancel()
        locationHint = null
        locateJob = scope.launch {
            isLocating = true
            try {
                AmapLocationHelper.getCurrentLocation(context)
                    .onSuccess { location ->
                        onPatchDraft {
                            it.copy(
                                locationLabel = location.address,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                autoLocateConsumed = true,
                            )
                        }
                    }
                    .onFailure { locationHint = it.message ?: "定位失败，请检查高德 Key 或定位权限" }
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
            locationHint = "需要定位权限才能记录活动位置"
        }
    }

    fun requestLocate() {
        if (hasLocationPermission()) {
            startLocate()
        } else {
            permissionLauncher.launch(locationPermissions)
        }
    }

    LaunchedEffect(pendingLocationPick) {
        pendingLocationPick?.let { pick ->
            locateJob?.cancel()
            locateJob = null
            isLocating = false
            onPatchDraft {
                it.copy(
                    locationLabel = pick.label,
                    latitude = pick.latitude,
                    longitude = pick.longitude,
                    autoLocateConsumed = true,
                )
            }
            locationHint = null
            onConsumeLocationPick()
        }
    }

    LaunchedEffect(Unit) {
        if (pendingLocationPick != null) return@LaunchedEffect
        if (draft.autoLocateConsumed || hasValidCoordinate(draft.latitude, draft.longitude)) {
            return@LaunchedEffect
        }
        onPatchDraft { it.copy(autoLocateConsumed = true) }
        requestLocate()
    }

    val isValid = hasLocation &&
        draft.startTime.isNotBlank() &&
        draft.endTime.isNotBlank() &&
        (draft.description.isNotBlank() || selectedImages.isNotEmpty() || keptExistingImages.isNotEmpty())
    val activityFee = draft.fee.toDoubleOrNull() ?: 0.0
    val needsAlipay = activityFee > 0 && !alipayBound

    XhsPublishScreenContainer(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            XhsPublishTopBar(
                title = if (initial == null) "发布活动" else "编辑活动",
                actionLabel = if (initial == null) "提交" else "保存",
                onBack = onBack,
                onAction = {
                    onSubmit(
                        draft.title.trim(),
                        draft.description.trim(),
                        draft.locationLabel.trim(),
                        draft.latitude,
                        draft.longitude,
                        draft.startTime.trim(),
                        draft.endTime.trim(),
                        draft.maxParticipants.toIntOrNull() ?: 0,
                        draft.fee.toDoubleOrNull() ?: 0.0,
                        selectedImages.toList(),
                        keptExistingImages.toList(),
                    )
                },
                actionEnabled = isValid && !needsAlipay,
                isSubmitting = isSubmitting,
            )
            HorizontalDivider(color = Color(0xFFEEEEEE))

            if (needsAlipay) {
                AlipaySetupBanner(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    title = "绑定支付宝后才能收取活动费用",
                    description = "参与者通过支付宝报名付款，款项会分账到你的支付宝。请先绑定收款账号。",
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

                XhsPublishFieldCard {
                    XhsPublishSectionTitle("基本信息")
                    XhsPublishTextField(
                        value = draft.title,
                        onValueChange = { onDraftChange(draft.copy(title = it)) },
                        placeholder = "活动标题（可选，不填将自动生成）",
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    XhsPublishTextField(
                        value = draft.description,
                        onValueChange = { onDraftChange(draft.copy(description = it)) },
                        placeholder = "介绍活动内容、流程和注意事项…\n支持纯文字，也可配图发布",
                        minLines = 4,
                        textStyle = TextStyle(fontSize = 15.sp, lineHeight = 24.sp),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                XhsPublishFieldCard {
                    XhsPublishSectionTitle("时间地点")
                    Text(
                        text = "用于展示活动位置与距离，可搜索地点或定位当前位置",
                        fontSize = 12.sp,
                        color = XhsTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    XhsPublishLocationPicker(
                        value = draft.locationLabel,
                        placeholder = if (isLocating) "正在获取当前位置…" else "点击搜索地点",
                        isLoading = isLocating,
                        onOpenSearch = {
                            onOpenLocationSearch(draft.latitude, draft.longitude)
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
                            text = "请选择或定位活动位置后才能提交",
                            fontSize = 12.sp,
                            color = XhsRed,
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    XhsPublishDateTimeField(
                        label = "开始时间",
                        value = draft.startTime,
                        placeholder = "请选择开始时间",
                        onClick = { showStartPicker = true },
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    XhsPublishDateTimeField(
                        label = "结束时间",
                        value = draft.endTime,
                        placeholder = "请选择结束时间",
                        onClick = { showEndPicker = true },
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                XhsPublishFieldCard {
                    XhsPublishSectionTitle("报名设置")
                    XhsPublishTextField(
                        value = draft.maxParticipants,
                        onValueChange = {
                            onDraftChange(draft.copy(maxParticipants = it.filter { c -> c.isDigit() }))
                        },
                        placeholder = "人数上限，0 表示不限",
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    XhsPublishTextField(
                        value = draft.fee,
                        onValueChange = {
                            onDraftChange(draft.copy(fee = it.filter { c -> c.isDigit() || c == '.' }))
                        },
                        placeholder = "向参与者收取费用（元），留空或 0 表示免费",
                        singleLine = true,
                    )
                    formatPayeeReceiveHint(draft.fee, platformFeeRateLabel)?.let { hint ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = hint,
                            fontSize = 12.sp,
                            color = XhsTextSecondary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "最多可选 $MAX_PUBLISH_IMAGES 张图片 · 提交后将由管理员审核",
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = XhsTextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "审核结果会通过消息通知你",
                    fontSize = 13.sp,
                    color = XhsRed,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }

    ActivityDateTimePickerSheet(
        visible = showStartPicker,
        title = "选择开始时间",
        initialValue = draft.startTime,
        onDismiss = { showStartPicker = false },
        onConfirm = { value ->
            showStartPicker = false
            val autoEndTime = if (draft.endTime.isBlank()) {
                parseActivityDateTime(value)?.let { startCalendar ->
                    val endCalendar = (startCalendar.clone() as java.util.Calendar).apply {
                        add(java.util.Calendar.HOUR_OF_DAY, 2)
                    }
                    formatActivityDateTimeForApi(endCalendar)
                }.orEmpty()
            } else {
                draft.endTime
            }
            onDraftChange(
                draft.copy(
                    startTime = value,
                    endTime = autoEndTime.ifBlank { draft.endTime },
                ),
            )
        },
    )

    ActivityDateTimePickerSheet(
        visible = showEndPicker,
        title = "选择结束时间",
        initialValue = draft.endTime.ifBlank { draft.startTime },
        onDismiss = { showEndPicker = false },
        onConfirm = { value ->
            onDraftChange(draft.copy(endTime = value))
            showEndPicker = false
        },
    )
}
