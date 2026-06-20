package com.lfc.consumer.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.data.local.FeedChannels
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun XhsFeedChannelPanel(
    myChannels: List<String>,
    recommendedChannels: List<String>,
    isEditMode: Boolean,
    onToggleEditMode: () -> Unit,
    onEnterEditMode: () -> Unit,
    onCollapse: () -> Unit,
    onChannelClick: (String) -> Unit,
    onAddChannel: (String) -> Unit,
    onRemoveChannel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(bottom = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "我的频道",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = XhsTextPrimary,
                )
                Text(
                    text = when {
                        isEditMode -> "点击 × 移除频道"
                        else -> "长按进入编辑，点击进入频道"
                    },
                    fontSize = 12.sp,
                    color = XhsTextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                text = if (isEditMode) "完成编辑" else "进入编辑",
                fontSize = 13.sp,
                color = XhsTextSecondary,
                modifier = Modifier
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleEditMode,
                        onLongClick = onEnterEditMode,
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = "收起",
                tint = XhsTextPrimary,
                modifier = Modifier
                    .clickable(onClick = onCollapse)
                    .padding(4.dp),
            )
        }

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            myChannels.forEach { channel ->
                val removable = isEditMode && channel != FeedChannels.RECOMMEND
                FeedChannelChip(
                    label = channel,
                    selected = true,
                    isEditMode = isEditMode,
                    showRemoveBadge = removable,
                    onClick = {
                        if (!isEditMode) {
                            onChannelClick(channel)
                        }
                    },
                    onLongClick = if (!isEditMode) {
                        { onEnterEditMode() }
                    } else {
                        null
                    },
                    onRemove = if (removable) {
                        { onRemoveChannel(channel) }
                    } else {
                        null
                    },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(top = 18.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "推荐频道",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = XhsTextPrimary,
            )
            Text(
                text = "点击添加频道",
                fontSize = 12.sp,
                color = XhsTextSecondary,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            recommendedChannels.forEach { channel ->
                FeedChannelChip(
                    label = "+$channel",
                    selected = false,
                    isEditMode = false,
                    showRemoveBadge = false,
                    onClick = { onAddChannel(channel) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeedChannelChip(
    label: String,
    selected: Boolean,
    isEditMode: Boolean,
    showRemoveBadge: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier.padding(top = if (isEditMode) 8.dp else 0.dp),
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 56.dp)
                .heightIn(min = 36.dp)
                .clip(shape)
                .then(
                    if (selected) {
                        Modifier.background(Color(0xFFF5F5F5))
                    } else {
                        Modifier
                            .background(Color.White)
                            .border(0.5.dp, Color(0xFFE8E8E8), shape)
                    },
                )
                .then(
                    if (onLongClick != null) {
                        Modifier.combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClick,
                            onLongClick = onLongClick,
                        )
                    } else {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClick,
                        )
                    },
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = XhsTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }

        if (showRemoveBadge && onRemove != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-6).dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFBDBDBD))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "移除频道",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}
