package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun XhsFeedChannelPanel(
    myChannels: List<String>,
    recommendedChannels: List<String>,
    isEditMode: Boolean,
    onToggleEditMode: () -> Unit,
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
                    text = if (isEditMode) "点击删除频道" else "点击进入频道",
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
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleEditMode,
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
                FeedChannelChip(
                    label = channel,
                    selected = true,
                    showRemove = isEditMode && channel != FeedChannels.RECOMMEND,
                    onClick = {
                        if (isEditMode && channel != FeedChannels.RECOMMEND) {
                            onRemoveChannel(channel)
                        } else if (!isEditMode) {
                            onChannelClick(channel)
                        }
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
                    showRemove = false,
                    enabled = isEditMode,
                    onClick = {
                        if (isEditMode) {
                            onAddChannel(channel)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun FeedChannelChip(
    label: String,
    selected: Boolean,
    showRemove: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    val textColor = if (enabled) XhsTextPrimary else XhsTextSecondary.copy(alpha = 0.55f)
    Box(
        modifier = Modifier
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
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showRemove) {
                Text(
                    text = "− ",
                    color = XhsTextSecondary,
                    fontSize = 14.sp,
                )
            }
            Text(
                text = label,
                fontSize = 14.sp,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
