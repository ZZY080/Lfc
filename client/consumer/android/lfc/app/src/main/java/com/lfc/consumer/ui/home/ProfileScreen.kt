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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lfc.consumer.data.local.UserSession
import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsRedLight
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userSession: UserSession?,
    myPosts: List<PostDto>,
    myActivities: List<ActivityDto>,
    participationCount: Int,
    onEditPost: (PostDto) -> Unit,
    onViewPost: (Int) -> Unit,
    onDeletePost: (Int) -> Unit,
    onEditActivity: (ActivityDto) -> Unit,
    onViewActivity: (Int) -> Unit,
    onDeleteActivity: (Int) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var profileTab by remember { mutableIntStateOf(0) }
    val displayName = userSession?.studentId ?: "校园用户"
    val email = userSession?.email ?: ""

    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                XhsRedLight.copy(alpha = 0.55f),
                                XhsRedLight.copy(alpha = 0.2f),
                                Color.White,
                            ),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(bottom = 12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "退出登录",
                            tint = XhsTextSecondary,
                        )
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置", tint = XhsTextSecondary)
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        XhsProfileAvatar(label = displayName, size = 72)
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = XhsTextPrimary,
                            )
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodySmall,
                                color = XhsTextSecondary,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "莲峰校园 · 记录校园生活",
                                style = MaterialTheme.typography.labelSmall,
                                color = XhsTextSecondary,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        XhsStatItem(count = myPosts.size, label = "笔记")
                        XhsStatItem(count = myActivities.size, label = "活动")
                        XhsStatItem(count = participationCount, label = "报名")
                    }
                }
            }
        }

        PrimaryTabRow(
            selectedTabIndex = profileTab,
            containerColor = Color.Transparent,
            contentColor = XhsRed,
        ) {
            Tab(
                selected = profileTab == 0,
                onClick = { profileTab = 0 },
                text = { Text("笔记", fontWeight = if (profileTab == 0) FontWeight.Bold else FontWeight.Normal) },
            )
            Tab(
                selected = profileTab == 1,
                onClick = { profileTab = 1 },
                text = { Text("活动", fontWeight = if (profileTab == 1) FontWeight.Bold else FontWeight.Normal) },
            )
        }

        when (profileTab) {
            0 -> {
                if (myPosts.isEmpty()) {
                    EmptyProfileHint("还没有发布笔记")
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalItemSpacing = 8.dp,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(myPosts, key = { it.id }) { post ->
                            Box {
                                XhsFeedCard(
                                    title = post.title,
                                    subtitle = post.content,
                                    authorLabel = "我",
                                    id = post.id,
                                    onClick = { onViewPost(post.id) },
                                )
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp),
                                ) {
                                    IconButton(
                                        onClick = { onEditPost(post) },
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.9f)),
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "编辑", tint = XhsRed)
                                    }
                                    IconButton(
                                        onClick = { onDeletePost(post.id) },
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.9f)),
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = XhsRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                if (myActivities.isEmpty()) {
                    EmptyProfileHint("还没有发布活动")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(myActivities, key = { it.id }) { activity ->
                            MyActivityItem(
                                activity = activity,
                                onClick = { onViewActivity(activity.id) },
                                onEdit = { onEditActivity(activity) },
                                onDelete = { onDeleteActivity(activity.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyProfileHint(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = XhsTextSecondary)
    }
}

@Composable
private fun MyActivityItem(
    activity: ActivityDto,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .height(64.dp)
                    .width(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(coverGradientForId(activity.id)),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(activity.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = activityStatusLabel(activity.status),
                    style = MaterialTheme.typography.labelSmall,
                    color = XhsRed,
                )
                Text(
                    activity.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = XhsTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column {
                TextButton(onClick = onEdit) { Text("编辑", color = XhsRed) }
                TextButton(onClick = onDelete) { Text("删除", color = XhsTextSecondary) }
            }
        }
    }
}

private fun activityStatusLabel(status: String): String = when (status.uppercase()) {
    "PENDING" -> "待审核"
    "APPROVED" -> "已通过"
    "REJECTED" -> "已拒绝"
    else -> status
}
