package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

val XHS_FEED_PRIMARY_TABS = listOf("关注", "发现")

@Composable
fun XhsFeedPrimaryTabRow(
    selectedTab: String,
    cityLabel: String,
    onTabSelected: (String) -> Unit,
    onMessageClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = XHS_FEED_PRIMARY_TABS + cityLabel
    val selectedIndex = tabs.indexOf(selectedTab).takeIf { it >= 0 } ?: 1

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.ChatBubbleOutline,
            contentDescription = "消息",
            tint = XhsTextPrimary,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onMessageClick)
                .padding(8.dp),
        )
        BoxWithConstraints(
            modifier = Modifier.weight(1f),
        ) {
            val tabWidth = maxWidth / tabs.size
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, title ->
                    val selected = selectedIndex == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onTabSelected(title) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) XhsTextPrimary else XhsTextSecondary,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = tabWidth * selectedIndex + tabWidth / 2 - 12.dp)
                    .width(24.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(XhsRed),
            )
        }
        Icon(
            Icons.Default.Search,
            contentDescription = "搜索",
            tint = XhsTextPrimary,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onSearchClick)
                .padding(8.dp),
        )
    }
}

@Composable
fun XhsFeedCategoryTabRow(
    myChannels: List<String>,
    selectedTab: String,
    isPanelExpanded: Boolean,
    onTabSelected: (String) -> Unit,
    onExpandPanel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 14.dp,
                end = 4.dp,
            ),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(18.dp),
        ) {
            items(myChannels, key = { it }) { title ->
                val selected = selectedTab == title
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) XhsRed else XhsTextSecondary,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onTabSelected(title) }
                        .padding(vertical = 8.dp),
                )
            }
        }
        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = if (isPanelExpanded) "收起频道" else "展开频道",
            tint = XhsTextPrimary,
            modifier = Modifier
                .size(40.dp)
                .clickable(onClick = onExpandPanel)
                .padding(8.dp),
        )
    }
}
