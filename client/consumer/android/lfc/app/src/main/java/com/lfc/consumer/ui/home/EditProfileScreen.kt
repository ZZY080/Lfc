package com.lfc.consumer.ui.home

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lfc.consumer.data.model.UserProfileDto
import com.lfc.consumer.data.model.displayName
import com.lfc.consumer.ui.theme.XhsBackground
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

private const val NICKNAME_MAX_LENGTH = 50
private const val BIO_MAX_LENGTH = 200
private const val DEFAULT_PROFILE_BIO = "莲峰校园 · 记录校园生活"
private val rowDividerColor = Color(0xFFF5F5F5)
private val coverFallbackDark = Color(0xFF243038)

private fun normalizeEditBio(raw: String?): String {
    val value = raw.orEmpty().trim()
    return if (value == DEFAULT_PROFILE_BIO) "" else value
}

@Composable
fun EditProfileScreen(
    profile: UserProfileDto?,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onSubmit: (nickname: String, bio: String, avatarUri: Uri?, coverUri: Uri?) -> Unit,
) {
    var nickname by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    var showBioEditor by remember { mutableStateOf(false) }

    LaunchedEffect(profile?.id) {
        profile ?: return@LaunchedEffect
        nickname = profile.nickname.orEmpty()
        bio = normalizeEditBio(profile.bio)
        avatarUri = null
        coverUri = null
    }

    val previewCoverUrl = coverUri?.toString() ?: profile?.coverUrl
    val previewAvatarUrl = avatarUri?.toString() ?: profile?.avatarUrl
    val previewName = nickname.trim().ifBlank { profile?.displayName() ?: "未设置昵称" }

    val originalNickname = profile?.nickname.orEmpty().trim()
    val originalBio = normalizeEditBio(profile?.bio)
    val hasChanges = nickname.trim() != originalNickname ||
        bio.trim() != originalBio ||
        avatarUri != null ||
        coverUri != null
    val canSave = profile != null &&
        hasChanges &&
        nickname.length <= NICKNAME_MAX_LENGTH &&
        bio.length <= BIO_MAX_LENGTH

    val pickCover = rememberSingleImagePicker { coverUri = it }
    val pickAvatar = rememberSingleImagePicker { avatarUri = it }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(XhsBackground),
        ) {
            EditProfileTopBar(
                onBack = onBack,
                onSave = { onSubmit(nickname, bio, avatarUri, coverUri) },
                canSave = canSave,
                isSubmitting = isSubmitting,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding(),
            ) {
                EditProfileCoverHeader(
                    coverUrl = previewCoverUrl,
                    avatarUrl = previewAvatarUrl,
                    displayName = previewName,
                    onPickCover = pickCover,
                    onPickAvatar = pickAvatar,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                ) {
                    Column {
                        EditProfileNicknameRow(
                            value = nickname,
                            onValueChange = { nickname = it.take(NICKNAME_MAX_LENGTH) },
                        )
                        HorizontalDivider(color = rowDividerColor, thickness = 0.5.dp)
                        EditProfileReadonlyRow(
                            label = "莲峰号",
                            value = profile?.lfcNo.orEmpty().ifBlank { "—" },
                        )
                        HorizontalDivider(color = rowDividerColor, thickness = 0.5.dp)
                        EditProfileBioEntryRow(
                            bio = bio,
                            onClick = { showBioEditor = true },
                        )
                    }
                }

                Text(
                    text = "莲峰号用于搜索与分享，暂不支持修改",
                    color = XhsTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp),
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (showBioEditor) {
            EditProfileBioEditor(
                initialBio = bio,
                onBack = { showBioEditor = false },
                onDone = { updated ->
                    bio = updated.take(BIO_MAX_LENGTH)
                    showBioEditor = false
                },
            )
        }
    }
}

@Composable
private fun EditProfileTopBar(
    onBack: () -> Unit,
    onSave: () -> Unit,
    canSave: Boolean,
    isSubmitting: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = XhsTextPrimary,
                )
            }
            Text(
                text = "编辑资料",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = XhsTextPrimary,
            )
            TextButton(
                onClick = onSave,
                enabled = canSave && !isSubmitting,
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = XhsRed,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = "保存",
                        color = if (canSave) XhsRed else XhsTextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }
            }
        }
        HorizontalDivider(color = rowDividerColor, thickness = 0.5.dp)
    }
}

@Composable
private fun EditProfileCoverHeader(
    coverUrl: String?,
    avatarUrl: String?,
    displayName: String,
    onPickCover: () -> Unit,
    onPickAvatar: () -> Unit,
) {
    val coverHeight = 160.dp
    val avatarSize = 80.dp
    val avatarOverlap = avatarSize / 2

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(coverHeight)
                .clickable(onClick = onPickCover),
        ) {
            if (!coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = "背景图",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF4A5D68), coverFallbackDark),
                            ),
                        ),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.12f)),
            )
            Text(
                text = "更换背景图",
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
                    .background(Color.Black.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp)
                .offset(y = (-avatarOverlap)),
        ) {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .border(3.dp, Color.White, CircleShape)
                    .clickable(onClick = onPickAvatar),
                contentAlignment = Alignment.Center,
            ) {
                XhsProfileAvatar(
                    label = displayName,
                    size = avatarSize.value.toInt(),
                    avatarUrl = avatarUrl,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xE6000000)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = "更换头像",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(avatarOverlap + 12.dp))
    }
}

@Composable
private fun EditProfileNicknameRow(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "名字",
            color = XhsTextPrimary,
            fontSize = 16.sp,
            modifier = Modifier.widthLabel(),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 15.sp,
                color = XhsTextPrimary,
                textAlign = TextAlign.End,
            ),
            cursorBrush = SolidColor(XhsRed),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (value.isEmpty()) {
                        Text(
                            text = "填写名字",
                            color = XhsTextSecondary,
                            fontSize = 15.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    inner()
                }
            },
        )
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = XhsTextSecondary.copy(alpha = 0.45f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun EditProfileReadonlyRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = XhsTextPrimary,
            fontSize = 16.sp,
            modifier = Modifier.widthLabel(),
        )
        Text(
            text = value,
            color = XhsTextSecondary,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EditProfileBioEntryRow(
    bio: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "简介",
            color = XhsTextPrimary,
            fontSize = 16.sp,
            modifier = Modifier.widthLabel(),
        )
        Text(
            text = bio.ifBlank { "填写简介" },
            color = if (bio.isBlank()) XhsTextSecondary else XhsTextPrimary,
            fontSize = 15.sp,
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = XhsTextSecondary.copy(alpha = 0.45f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun EditProfileBioEditor(
    initialBio: String,
    onBack: () -> Unit,
    onDone: (String) -> Unit,
) {
    var draft by remember { mutableStateOf(initialBio) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .imePadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text(
                text = "编辑简介",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = XhsTextPrimary,
            )
            TextButton(onClick = { onDone(draft.trim()) }) {
                Text(
                    text = "完成",
                    color = XhsRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        }
        HorizontalDivider(color = rowDividerColor, thickness = 0.5.dp)

        BasicTextField(
            value = draft,
            onValueChange = { draft = it.take(BIO_MAX_LENGTH) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = XhsTextPrimary,
                lineHeight = 24.sp,
            ),
            cursorBrush = SolidColor(XhsRed),
            decorationBox = { inner ->
                Box {
                    if (draft.isEmpty()) {
                        Text(
                            text = "写一句简介，让同学更了解你",
                            color = XhsTextSecondary,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                        )
                    }
                    inner()
                }
            },
        )

        Text(
            text = "${draft.length}/$BIO_MAX_LENGTH",
            color = if (draft.length >= BIO_MAX_LENGTH) XhsRed else XhsTextSecondary,
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 20.dp, bottom = 16.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun Modifier.widthLabel(): Modifier = this.then(
    Modifier
        .width(64.dp)
        .padding(end = 12.dp),
)
