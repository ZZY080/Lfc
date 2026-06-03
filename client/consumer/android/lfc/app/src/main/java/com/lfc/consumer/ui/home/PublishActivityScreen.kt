package com.lfc.consumer.ui.home

import android.net.Uri
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextSecondary

@Composable
fun PublishActivityScreen(
    initial: ActivityDto? = null,
    isSubmitting: Boolean = false,
    onBack: () -> Unit,
    onSubmit: (
        title: String,
        description: String,
        location: String,
        startTime: String,
        endTime: String,
        maxParticipants: Int,
        imageUris: List<Uri>,
    ) -> Unit,
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var startTime by remember { mutableStateOf(initial?.startTime?.take(16)?.replace(" ", "T") ?: "") }
    var endTime by remember { mutableStateOf(initial?.endTime?.take(16)?.replace(" ", "T") ?: "") }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var maxParticipants by remember { mutableStateOf((initial?.maxParticipants ?: 0).toString()) }
    val selectedImages = rememberPublishImageSelection()
    val existingImages = initial?.images.orEmpty()

    val isValid = location.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank() &&
        (description.isNotBlank() || selectedImages.isNotEmpty() || existingImages.isNotEmpty())

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
                        startTime.trim(),
                        endTime.trim(),
                        maxParticipants.toIntOrNull() ?: 0,
                        selectedImages.toList(),
                    )
                },
                actionEnabled = isValid,
                isSubmitting = isSubmitting,
            )
            HorizontalDivider(color = Color(0xFFEEEEEE))

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
                    XhsPublishTextField(
                        value = location,
                        onValueChange = { location = it },
                        placeholder = "活动地点，例如：体育馆",
                        singleLine = true,
                    )
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
