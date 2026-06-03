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
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

@Composable
fun PublishPostScreen(
    initial: PostDto? = null,
    isSubmitting: Boolean = false,
    onBack: () -> Unit,
    onSubmit: (title: String, content: String, imageUris: List<Uri>) -> Unit,
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var content by remember { mutableStateOf(initial?.content ?: "") }
    val selectedImages = rememberPublishImageSelection()
    val existingImages = initial?.images.orEmpty()
    val canSubmit = content.isNotBlank() || selectedImages.isNotEmpty() || existingImages.isNotEmpty()

    XhsPublishScreenContainer(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            XhsPublishTopBar(
                title = if (initial == null) "发布笔记" else "编辑笔记",
                actionLabel = if (initial == null) "发布" else "保存",
                onBack = onBack,
                onAction = { onSubmit(title.trim(), content.trim(), selectedImages.toList()) },
                actionEnabled = canSubmit,
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
                    placeholder = "分享你的校园生活、学习经验…\n支持纯文字，也可配图发布",
                    minLines = 10,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 26.sp,
                        color = XhsTextPrimary,
                    ),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "最多可选 $MAX_PUBLISH_IMAGES 张图片 · 纯文字或图文均可发布",
                    fontSize = 12.sp,
                    color = XhsTextSecondary,
                )
            }
        }
    }
}
