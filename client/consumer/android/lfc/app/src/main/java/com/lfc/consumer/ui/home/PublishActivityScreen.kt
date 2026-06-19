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
import androidx.compose.runtime.getValue
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
import com.lfc.consumer.location.AmapLocationHelper
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextSecondary
import kotlinx.coroutines.launch

private val locationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

@Composable
fun PublishActivityScreen(
    initial: ActivityDto? = null,
    isSubmitting: Boolean = false,
    platformFeeRateLabel: String? = null,
    alipayBound: Boolean = false,
    onBack: () -> Unit,
    onBindAlipay: () -> Unit = {},
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
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var latitude by remember { mutableStateOf(initial?.latitude) }
    var longitude by remember { mutableStateOf(initial?.longitude) }
    var startTime by remember { mutableStateOf(initial?.startTime?.take(16)?.replace(" ", "T") ?: "") }
    var endTime by remember { mutableStateOf(initial?.endTime?.take(16)?.replace(" ", "T") ?: "") }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var maxParticipants by remember { mutableStateOf((initial?.maxParticipants ?: 0).toString()) }
    var fee by remember { mutableStateOf(initial?.fee?.toDoubleOrNull()?.let { if (it > 0) it.toString() else "" } ?: "") }
    var isLocating by remember { mutableStateOf(false) }
    var locationHint by remember { mutableStateOf<String?>(null) }
    val selectedImages = rememberPublishImageSelection()
    val keptExistingImages = rememberPublishExistingImages(initial?.images.orEmpty(), initial?.id)

    fun hasLocationPermission(): Boolean = locationPermissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun startLocate() {
        locationHint = null
        scope.launch {
            isLocating = true
            AmapLocationHelper.getCurrentLocation(context)
                .onSuccess {
                    location = it.address
                    latitude = it.latitude
                    longitude = it.longitude
                }
                .onFailure { locationHint = it.message ?: "定位失败，请检查高德 Key 或定位权限" }
            isLocating = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results.values.all { it }) {
            startLocate()
        } else {
            locationHint = "需要定位权限才能使用高德定位"
        }
    }

    fun requestLocate() {
        if (hasLocationPermission()) {
            startLocate()
        } else {
            permissionLauncher.launch(locationPermissions)
        }
    }

    val isValid = location.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank() &&
        (description.isNotBlank() || selectedImages.isNotEmpty() || keptExistingImages.isNotEmpty())
    val activityFee = fee.toDoubleOrNull() ?: 0.0
    val needsAlipay = activityFee > 0 && !alipayBound

    XhsPublishScreenContainer(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            XhsPublishTopBar(
                title = if (initial == null) "发布活动" else "编辑活动",
                actionLabel = if (initial == null) "提交" else "保存",
                onBack = onBack,
                onAction = {
                    onSubmit(
                        title.trim(),
                        description.trim(),
                        location.trim(),
                        latitude,
                        longitude,
                        startTime.trim(),
                        endTime.trim(),
                        maxParticipants.toIntOrNull() ?: 0,
                        fee.toDoubleOrNull() ?: 0.0,
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
                        value = title,
                        onValueChange = { title = it },
                        placeholder = "活动标题（可选，不填将自动生成）",
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    XhsPublishTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = "介绍活动内容、流程和注意事项…\n支持纯文字，也可配图发布",
                        minLines = 4,
                        textStyle = TextStyle(fontSize = 15.sp, lineHeight = 24.sp),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                XhsPublishFieldCard {
                    XhsPublishSectionTitle("时间地点")
                    XhsPublishLocationField(
                        value = location,
                        onValueChange = { newValue ->
                            location = newValue
                            latitude = null
                            longitude = null
                        },
                        placeholder = "点击右侧按钮获取当前位置，也可手动输入",
                        isLoading = isLocating,
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
                    Spacer(modifier = Modifier.height(14.dp))
                    XhsPublishDateTimeField(
                        label = "开始时间",
                        value = startTime,
                        placeholder = "请选择开始时间",
                        onClick = { showStartPicker = true },
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    XhsPublishDateTimeField(
                        label = "结束时间",
                        value = endTime,
                        placeholder = "请选择结束时间",
                        onClick = { showEndPicker = true },
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                XhsPublishFieldCard {
                    XhsPublishSectionTitle("报名设置")
                    XhsPublishTextField(
                        value = maxParticipants,
                        onValueChange = { maxParticipants = it.filter { c -> c.isDigit() } },
                        placeholder = "人数上限，0 表示不限",
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    XhsPublishTextField(
                        value = fee,
                        onValueChange = { fee = it.filter { c -> c.isDigit() || c == '.' } },
                        placeholder = "向参与者收取费用（元），留空或 0 表示免费",
                        singleLine = true,
                    )
                    formatPayeeReceiveHint(fee, platformFeeRateLabel)?.let { hint ->
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
        initialValue = startTime,
        onDismiss = { showStartPicker = false },
        onConfirm = { value ->
            startTime = value
            showStartPicker = false
            if (endTime.isBlank()) {
                parseActivityDateTime(value)?.let { startCalendar ->
                    val endCalendar = (startCalendar.clone() as java.util.Calendar).apply {
                        add(java.util.Calendar.HOUR_OF_DAY, 2)
                    }
                    endTime = formatActivityDateTimeForApi(endCalendar)
                }
            }
        },
    )

    ActivityDateTimePickerSheet(
        visible = showEndPicker,
        title = "选择结束时间",
        initialValue = endTime.ifBlank { startTime },
        onDismiss = { showEndPicker = false },
        onConfirm = { value ->
            endTime = value
            showEndPicker = false
        },
    )
}
