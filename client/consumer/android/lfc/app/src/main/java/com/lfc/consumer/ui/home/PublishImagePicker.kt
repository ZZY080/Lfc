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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.lfc.consumer.ui.theme.XhsTextSecondary

const val MAX_PUBLISH_IMAGES = 20

private sealed interface PublishPreviewModel {
    data class Remote(val url: String) : PublishPreviewModel
    data class Local(val uri: Uri) : PublishPreviewModel
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PublishImagePicker(
    existingImageUrls: SnapshotStateList<String>,
    selectedImages: SnapshotStateList<Uri>,
    modifier: Modifier = Modifier,
) {
    var previewState by remember { mutableStateOf<Pair<List<PublishPreviewModel>, Int>?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_PUBLISH_IMAGES),
    ) { uris ->
        val room = MAX_PUBLISH_IMAGES - existingImageUrls.size
        val merged = (selectedImages + uris).distinct().take(room)
        selectedImages.clear()
        selectedImages.addAll(merged)
    }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        existingImageUrls.forEachIndexed { index, url ->
            PublishImageTile(
                model = url,
                onClick = {
                    previewState = buildPublishPreviewModels(existingImageUrls, selectedImages) to index
                },
                onRemove = { existingImageUrls.remove(url) },
            )
        }
        selectedImages.forEachIndexed { index, uri ->
            val previewIndex = existingImageUrls.size + index
            PublishImageTile(
                model = uri,
                onClick = {
                    previewState = buildPublishPreviewModels(existingImageUrls, selectedImages) to previewIndex
                },
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

    previewState?.let { (items, startIndex) ->
        PublishImagePreviewDialog(
            items = items,
            startIndex = startIndex,
            onDismiss = { previewState = null },
        )
    }
}

@Composable
fun rememberPublishImageSelection(
    saveKey: String = "publish_selected_images",
): SnapshotStateList<Uri> {
    var savedUris by rememberSaveable(saveKey) { mutableStateOf(emptyList<String>()) }
    val list = remember(saveKey) {
        mutableStateListOf<Uri>().apply {
            addAll(savedUris.map(Uri::parse))
        }
    }
    SideEffect {
        val encoded = list.map { it.toString() }
        if (encoded != savedUris) {
            savedUris = encoded
        }
    }
    return list
}

@Composable
fun rememberPublishExistingImages(
    initial: List<String>,
    key: Any? = initial,
    saveKey: String = "publish_existing_images",
): SnapshotStateList<String> {
    var savedUrls by rememberSaveable(saveKey, key) { mutableStateOf(initial) }
    val list = remember(saveKey, key) {
        mutableStateListOf<String>().apply { addAll(savedUrls) }
    }
    SideEffect {
        val encoded = list.toList()
        if (encoded != savedUrls) {
            savedUrls = encoded
        }
    }
    return list
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

private fun buildPublishPreviewModels(
    existingImageUrls: List<String>,
    selectedImages: List<Uri>,
): List<PublishPreviewModel> {
    return existingImageUrls.map { PublishPreviewModel.Remote(it) } +
        selectedImages.map { PublishPreviewModel.Local(it) }
}

@Composable
private fun PublishImagePreviewDialog(
    items: List<PublishPreviewModel>,
    startIndex: Int,
    onDismiss: () -> Unit,
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, items.lastIndex),
        pageCount = { items.size },
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val model = when (val item = items[page]) {
                    is PublishPreviewModel.Remote -> item.url
                    is PublishPreviewModel.Local -> item.uri
                }
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }

            if (items.size > 1) {
                Text(
                    text = "${pagerState.currentPage + 1}/${items.size}",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
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
private fun PublishImageTile(
    model: Any,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Box(modifier = Modifier.size(96.dp)) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
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
            Icon(
                Icons.Default.Close,
                contentDescription = "移除",
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
