package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsRedContainer
import com.lfc.consumer.ui.theme.XhsTextSecondary

@Composable
fun ActivityFeedScreen(
    activities: List<ActivityDto>,
    onJoin: (Int) -> Unit,
    onActivityClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        XhsPageTitle("校园活动")
        ActivityFeedContent(
            activities = activities,
            onJoin = onJoin,
            onActivityClick = onActivityClick,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ActivityFeedContent(
    activities: List<ActivityDto>,
    onJoin: (Int) -> Unit,
    onActivityClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (activities.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无活动，点击 + 发布第一个活动吧", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(activities, key = { it.id }) { activity ->
            ActivityCard(
                activity = activity,
                onClick = { onActivityClick(activity.id) },
                onJoin = { onJoin(activity.id) },
            )
        }
    }
}

@Composable
private fun ActivityCard(
    activity: ActivityDto,
    onClick: () -> Unit,
    onJoin: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Column {
            val coverUrl = activity.images?.firstOrNull()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(coverGradientForId(activity.id)),
                contentAlignment = Alignment.BottomStart,
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = activity.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Text(
                    text = activity.title,
                    modifier = Modifier.padding(16.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = XhsTextSecondary)
                    Text(
                        text = activity.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = XhsTextSecondary,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = XhsTextSecondary)
                    Text(
                        text = formatActivityTime(activity.startTime, activity.endTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = XhsTextSecondary,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val count = activity.participants?.size ?: 0
                    Text(
                        text = "$count 人已报名${if (activity.maxParticipants > 0) " / ${activity.maxParticipants}" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = XhsRed,
                    )
                    Button(
                        onClick = onJoin,
                        colors = ButtonDefaults.buttonColors(containerColor = XhsRed),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text("立即报名")
                    }
                }
            }
        }
    }
}

private fun formatActivityTime(start: String, end: String): String {
    val startShort = start.replace("T", " ").take(16)
    val endShort = end.replace("T", " ").take(16)
    return "$startShort ~ $endShort"
}
