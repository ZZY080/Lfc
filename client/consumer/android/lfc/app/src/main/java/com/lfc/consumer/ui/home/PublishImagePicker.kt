package com.lfc.consumer.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lfc.consumer.ui.theme.XhsTextSecondary

const val MAX_PUBLISH_IMAGES = 20

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PublishImagePicker(
    existingImageUrls: List<String> = emptyList(),
    selectedImages: androidx.compose.runtime.snapshots.SnapshotStateList<Uri>,
    modifier: Modifier = Modifier,
) {
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_PUBLISH_IMAGES),
    ) { uris ->
        val merged = (selectedImages + uris).distinct().take(MAX_PUBLISH_IMAGES - existingImageUrls.size)
        selectedImages.clear()
        selectedImages.addAll(merged)
    }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        existingImageUrls.forEach { url ->
            PublishExistingImageItem(url = url)
        }
        selectedImages.forEachIndexed { index, uri ->
            PublishSelectedImageItem(
                uri = uri,
                onRemove = { selectedImages.removeAt(index) },
            )
        }
        if (existingImageUrls.size + selectedImages.size < MAX_PUBLISH_IMAGES) {
            PublishAddImageTile(
                onClick = {
                    pickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            )
        }
    }
}

@Composable
fun rememberPublishImageSelection(): androidx.compose.runtime.snapshots.SnapshotStateList<Uri> {
    return remember { mutableStateListOf<Uri>() }
}

@Composable
fun rememberSingleImagePicker(onSelected: (Uri) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(onSelected) }
    return {
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

@Composable
private fun PublishAddImageTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F5))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Add, contentDescription = "添加图片", tint = XhsTextSecondary)
            Text("添加", fontSize = 12.sp, color = XhsTextSecondary)
        }
    }
}

@Composable
private fun PublishSelectedImageItem(uri: Uri, onRemove: () -> Unit) {
    Box(modifier = Modifier.size(96.dp)) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Close, contentDescription = "移除", tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun PublishExistingImageItem(url: String) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(12.dp)),
    )
}
