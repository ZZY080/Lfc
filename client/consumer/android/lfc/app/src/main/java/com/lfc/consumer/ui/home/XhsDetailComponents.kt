package com.lfc.consumer.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

fun formatXhsTime(iso: String): String = iso.replace("T", " ").take(19)

@Composable
fun XhsDetailImageCarousel(
    images: List<String>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 1f,
) {
    if (images.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { images.size })

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            AsyncImage(
                model = images[page],
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (images.size > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                        ),
                    ),
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                repeat(images.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (pagerState.currentPage == index) 6.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index) Color.White
                                else Color.White.copy(alpha = 0.45f),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
fun XhsDetailAuthorHeader(
    authorLabel: String,
    authorId: Int,
    onBack: () -> Unit,
    onAuthorClick: (Int) -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = XhsTextPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onAuthorClick(authorId) }
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                XhsProfileAvatar(label = authorLabel, size = 32)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = authorLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = XhsTextPrimary,
                    maxLines = 1,
                )
            }

            actions()
        }
        HorizontalDivider(color = Color(0xFFF0F0F0))
    }
}

@Composable
fun XhsDetailFollowButton(
    isFollowing: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isFollowing) Color.Transparent else XhsRed,
        border = if (isFollowing) BorderStroke(1.dp, Color(0xFFDDDDDD)) else null,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = if (isFollowing) "已关注" else "关注",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
            color = if (isFollowing) XhsTextSecondary else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun XhsDetailBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onBack,
        modifier = modifier
            .statusBarsPadding()
            .padding(start = 8.dp, top = 4.dp)
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.85f)),
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "返回",
            tint = XhsTextPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun XhsDetailLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = XhsRed)
    }
}

@Composable
fun XhsDetailEmpty(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, color = XhsTextSecondary)
    }
}

@Composable
fun XhsDetailAuthorRow(
    authorLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        XhsProfileAvatar(label = authorLabel, size = 40)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = authorLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = XhsTextPrimary,
            )
            Text(
                text = "校园用户",
                fontSize = 12.sp,
                color = XhsTextSecondary,
            )
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = XhsRed,
        ) {
            Text(
                text = "关注",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun XhsActivityDetailBottomBar(
    participantCount: Int,
    maxParticipants: Int,
    isJoining: Boolean,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = Color.White,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "$participantCount 人已报名",
                    fontWeight = FontWeight.Bold,
                    color = XhsRed,
                    fontSize = 15.sp,
                )
                if (maxParticipants > 0) {
                    Text(
                        text = "限额 $maxParticipants 人",
                        fontSize = 12.sp,
                        color = XhsTextSecondary,
                    )
                }
            }
            Button(
                onClick = onJoin,
                enabled = !isJoining,
                colors = ButtonDefaults.buttonColors(containerColor = XhsRed),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(40.dp),
            ) {
                if (isJoining) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("立即报名", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun XhsParticipantAvatars(
    labels: List<String>,
    modifier: Modifier = Modifier,
) {
    if (labels.isEmpty()) return
    Row(modifier = modifier) {
        labels.take(5).forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .offset(x = (-8 * index).dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(coverGradientForId(label.hashCode())),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
        }
        if (labels.size > 5) {
            Text(
                text = "+${labels.size - 5}",
                modifier = Modifier
                    .offset(x = (-8 * 5 + 8).dp)
                    .padding(start = 4.dp)
                    .align(Alignment.CenterVertically),
                color = XhsTextSecondary,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
fun XhsDetailInfoRow(
    icon: @Composable () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Text(
            text = text,
            modifier = Modifier.padding(start = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = XhsTextSecondary,
        )
    }
}
