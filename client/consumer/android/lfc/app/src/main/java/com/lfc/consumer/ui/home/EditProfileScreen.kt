package com.lfc.consumer.ui.home

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lfc.consumer.data.model.UserProfileDto
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

@Composable
fun EditProfileScreen(
    profile: UserProfileDto?,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onSubmit: (nickname: String, bio: String, avatarUri: Uri?, coverUri: Uri?) -> Unit,
) {
    var nickname by remember(profile?.id) { mutableStateOf(profile?.nickname.orEmpty()) }
    var bio by remember(profile?.id) { mutableStateOf(profile?.bio.orEmpty()) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var coverUri by remember { mutableStateOf<Uri?>(null) }

    XhsPublishScreenContainer(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            XhsPublishTopBar(
                title = "编辑主页",
                actionLabel = "保存",
                onBack = onBack,
                onAction = { onSubmit(nickname, bio, avatarUri, coverUri) },
                actionEnabled = profile != null,
                isSubmitting = isSubmitting,
            )
            HorizontalDivider(color = Color(0xFFEEEEEE))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Text("封面", fontWeight = FontWeight.Bold, color = XhsTextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                ProfileImagePicker(
                    label = "更换封面",
                    imageUrl = coverUri?.toString() ?: profile?.coverUrl,
                    height = 140.dp,
                    onImageSelected = { coverUri = it },
                )

                Spacer(modifier = Modifier.height(20.dp))
                Text("头像", fontWeight = FontWeight.Bold, color = XhsTextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                ProfileImagePicker(
                    label = "更换头像",
                    imageUrl = avatarUri?.toString() ?: profile?.avatarUrl,
                    height = 96.dp,
                    shape = CircleShape,
                    onImageSelected = { avatarUri = it },
                )

                Spacer(modifier = Modifier.height(20.dp))
                XhsPublishTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    placeholder = "昵称（可选）",
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(16.dp))
                XhsPublishTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    placeholder = "简介",
                    singleLine = false,
                    minLines = 3,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "莲峰号：${profile?.lfcNo.orEmpty()}",
                    color = XhsTextSecondary,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun ProfileImagePicker(
    label: String,
    imageUrl: String?,
    height: androidx.compose.ui.unit.Dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
    onImageSelected: (Uri) -> Unit,
) {
    val launcher = rememberSingleImagePicker(onImageSelected)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(Color(0xFFF3F3F3))
            .clickable(onClick = launcher),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.35f), shape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(label, color = Color.White, fontSize = 12.sp)
        }
    }
}
