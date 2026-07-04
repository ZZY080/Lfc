package com.lfc.consumer.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lfc.consumer.ui.theme.XhsBackground

private val SkeletonBase = Color(0xFFE8E8E8)
private val SkeletonHighlight = Color(0xFFF5F5F5)

fun Modifier.skeletonShimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skeletonTranslate",
    )
    background(
        brush = Brush.linearGradient(
            colors = listOf(SkeletonBase, SkeletonHighlight, SkeletonBase),
            start = Offset(translate - 400f, translate - 400f),
            end = Offset(translate, translate),
        ),
    )
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    Box(
        modifier = modifier
            .clip(shape)
            .skeletonShimmer(),
    )
}

@Composable
fun SkeletonCircle(size: Dp, modifier: Modifier = Modifier) {
    SkeletonBox(
        modifier = modifier.size(size),
        shape = CircleShape,
    )
}

@Composable
fun SkeletonLine(
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
    widthFraction: Float = 1f,
) {
    SkeletonBox(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height),
        shape = RoundedCornerShape(6.dp),
    )
}

@Composable
fun SkeletonLoadMoreFooter(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        SkeletonLine(
            modifier = Modifier.width(120.dp),
            height = 10.dp,
            widthFraction = 1f,
        )
    }
}

@Composable
private fun FeedCardSkeleton(
    aspectRatio: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White),
    ) {
        Box {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio),
                shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            )
            SkeletonBox(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 3.dp, vertical = 3.dp)
                    .height(16.dp),
                shape = RoundedCornerShape(10.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonLine(
            modifier = Modifier.padding(horizontal = 8.dp),
            height = 12.dp,
            widthFraction = 0.92f,
        )
        Spacer(modifier = Modifier.height(6.dp))
        SkeletonLine(
            modifier = Modifier.padding(horizontal = 8.dp),
            height = 10.dp,
            widthFraction = 0.65f,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonCircle(size = 16.dp)
            Spacer(modifier = Modifier.width(6.dp))
            SkeletonLine(modifier = Modifier.weight(1f), height = 9.dp, widthFraction = 0.4f)
            SkeletonBox(modifier = Modifier.size(11.dp), shape = CircleShape)
        }
    }
}

/** 静态双列骨架，可安全嵌套在 LazyVerticalStaggeredGrid 等滚动容器 item 内 */
@Composable
fun FeedGridSkeletonStatic(
    modifier: Modifier = Modifier,
    itemCount: Int = 4,
    contentPadding: PaddingValues = PaddingValues(8.dp),
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (rowStart in 0 until itemCount step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FeedCardSkeleton(
                    aspectRatio = 0.68f + (rowStart % 3) * 0.06f,
                    modifier = Modifier.weight(1f),
                )
                if (rowStart + 1 < itemCount) {
                    FeedCardSkeleton(
                        aspectRatio = 0.68f + ((rowStart + 1) % 3) * 0.06f,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun FeedGridSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 6,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = false,
    ) {
        items(itemCount) { index ->
            FeedCardSkeleton(aspectRatio = 0.68f + (index % 3) * 0.06f)
        }
    }
}

@Composable
private fun ActivityCardSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        SkeletonBox(
            modifier = Modifier.size(96.dp),
            shape = RoundedCornerShape(6.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            SkeletonLine(height = 16.dp, widthFraction = 0.95f)
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonLine(height = 16.dp, widthFraction = 0.7f)
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonLine(height = 13.dp, widthFraction = 0.8f)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                SkeletonBox(
                    modifier = Modifier.size(14.dp),
                    shape = RoundedCornerShape(3.dp),
                )
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    SkeletonLine(height = 13.dp, widthFraction = 0.9f)
                    Spacer(modifier = Modifier.height(4.dp))
                    SkeletonLine(height = 13.dp, widthFraction = 0.55f)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonLine(height = 13.dp, widthFraction = 0.45f)
                SkeletonBox(
                    modifier = Modifier.size(width = 76.dp, height = 28.dp),
                    shape = RoundedCornerShape(14.dp),
                )
            }
        }
    }
}

@Composable
fun ActivityFeedSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 3,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White),
    ) {
        repeat(itemCount) { index ->
            ActivityCardSkeleton()
            if (index < itemCount - 1) {
                HorizontalDivider(
                    color = Color(0xFFEEEEEE),
                    thickness = 0.5.dp,
                )
            }
        }
    }
}

@Composable
fun OrderListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 4,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
    ) {
        items(itemCount) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SkeletonLine(modifier = Modifier.weight(1f), height = 12.dp, widthFraction = 0.35f)
                    SkeletonLine(modifier = Modifier.width(56.dp), height = 12.dp, widthFraction = 1f)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SkeletonBox(
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(10.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        SkeletonLine(height = 14.dp, widthFraction = 0.9f)
                        Spacer(modifier = Modifier.height(8.dp))
                        SkeletonLine(height = 12.dp, widthFraction = 0.5f)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                SkeletonLine(height = 10.dp, widthFraction = 0.45f)
            }
        }
    }
}

@Composable
fun PaymentLedgerSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 5,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false,
    ) {
        items(itemCount) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonCircle(size = 36.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonLine(height = 13.dp, widthFraction = 0.7f)
                    Spacer(modifier = Modifier.height(6.dp))
                    SkeletonLine(height = 11.dp, widthFraction = 0.45f)
                }
                SkeletonLine(modifier = Modifier.width(52.dp), height = 14.dp, widthFraction = 1f)
            }
        }
    }
}

@Composable
fun CommentListSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        repeat(3) {
            Row(modifier = Modifier.fillMaxWidth()) {
                SkeletonCircle(size = 34.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonLine(height = 11.dp, widthFraction = 0.28f)
                    Spacer(modifier = Modifier.height(8.dp))
                    SkeletonLine(height = 12.dp, widthFraction = 0.95f)
                    Spacer(modifier = Modifier.height(6.dp))
                    SkeletonLine(height = 12.dp, widthFraction = 0.72f)
                }
            }
        }
    }
}

@Composable
fun PostDetailSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonCircle(size = 32.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                SkeletonLine(height = 12.dp, widthFraction = 0.35f)
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonLine(height = 10.dp, widthFraction = 0.22f)
            }
            SkeletonBox(modifier = Modifier.size(56.dp, 28.dp), shape = RoundedCornerShape(14.dp))
        }
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(0.dp),
        )
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            SkeletonLine(height = 18.dp, widthFraction = 0.88f)
            Spacer(modifier = Modifier.height(10.dp))
            SkeletonLine(height = 14.dp)
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonLine(height = 14.dp)
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonLine(height = 14.dp, widthFraction = 0.76f)
            Spacer(modifier = Modifier.height(16.dp))
            SkeletonLine(height = 11.dp, widthFraction = 0.3f)
        }
        CommentListSkeleton()
    }
}

@Composable
fun ActivityDetailSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XhsBackground),
    ) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            shape = RoundedCornerShape(0.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(Color.White)
                .padding(16.dp),
        ) {
            SkeletonLine(height = 20.dp, widthFraction = 0.82f)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBox(modifier = Modifier.size(72.dp, 28.dp), shape = RoundedCornerShape(14.dp))
                SkeletonBox(modifier = Modifier.size(56.dp, 28.dp), shape = RoundedCornerShape(14.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            repeat(2) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(XhsBackground)
                        .padding(14.dp),
                ) {
                    SkeletonLine(height = 12.dp, widthFraction = 0.25f)
                    Spacer(modifier = Modifier.height(10.dp))
                    SkeletonLine(height = 15.dp, widthFraction = 0.7f)
                    Spacer(modifier = Modifier.height(6.dp))
                    SkeletonLine(height = 13.dp, widthFraction = 0.55f)
                }
            }
        }
    }
}

@Composable
fun ProfilePageSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            SkeletonBox(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(0.dp),
            )
            SkeletonCircle(
                size = 72.dp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            SkeletonLine(height = 18.dp, widthFraction = 0.35f)
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonLine(height = 12.dp, widthFraction = 0.55f)
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                repeat(3) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SkeletonLine(modifier = Modifier.width(36.dp), height = 16.dp, widthFraction = 1f)
                        Spacer(modifier = Modifier.height(4.dp))
                        SkeletonLine(modifier = Modifier.width(28.dp), height = 10.dp, widthFraction = 1f)
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            repeat(4) {
                SkeletonLine(modifier = Modifier.width(36.dp), height = 12.dp, widthFraction = 1f)
            }
        }
        FeedGridSkeleton(modifier = Modifier.weight(1f), itemCount = 4)
    }
}

@Composable
fun ChatMessageListSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XhsBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            SkeletonCircle(size = 32.dp)
            Spacer(modifier = Modifier.width(8.dp))
            SkeletonBox(
                modifier = Modifier
                    .width(180.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            SkeletonBox(
                modifier = Modifier
                    .width(140.dp)
                    .height(40.dp),
                shape = RoundedCornerShape(12.dp),
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            SkeletonCircle(size = 32.dp)
            Spacer(modifier = Modifier.width(8.dp))
            SkeletonBox(
                modifier = Modifier
                    .width(220.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
            )
        }
    }
}

@Composable
fun MessageDetailSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XhsBackground)
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            shape = RoundedCornerShape(8.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        SkeletonLine(height = 18.dp, widthFraction = 0.75f)
        Spacer(modifier = Modifier.height(12.dp))
        repeat(4) {
            SkeletonLine(height = 14.dp)
            Spacer(modifier = Modifier.height(8.dp))
        }
        SkeletonLine(height = 14.dp, widthFraction = 0.6f)
    }
}

@Composable
fun NotificationListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 8,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        items(itemCount) {
            NotificationListItemSkeleton()
        }
    }
}

@Composable
private fun NotificationListItemSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonCircle(size = 40.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            SkeletonLine(height = 14.dp, widthFraction = 0.55f)
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonLine(height = 12.dp, widthFraction = 0.85f)
        }
    }
}

@Composable
fun ConversationListSkeleton(modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item(key = "notification-title") {
            SkeletonLine(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                height = 12.dp,
                widthFraction = 0.22f,
            )
        }
        items(3, key = { "notification-skeleton-$it" }) {
            NotificationListItemSkeleton()
        }
        item(key = "divider") {
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                shape = RoundedCornerShape(0.dp),
            )
        }
        item(key = "conversation-title") {
            SkeletonLine(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                height = 12.dp,
                widthFraction = 0.18f,
            )
        }
        items(5, key = { "conversation-skeleton-$it" }) {
            ConversationListItemSkeleton()
        }
    }
}

@Composable
private fun ConversationListItemSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonCircle(size = 48.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonLine(
                    modifier = Modifier.weight(1f),
                    height = 14.dp,
                    widthFraction = 0.5f,
                )
                Spacer(modifier = Modifier.width(8.dp))
                SkeletonLine(
                    modifier = Modifier.width(36.dp),
                    height = 10.dp,
                    widthFraction = 1f,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonLine(height = 12.dp, widthFraction = 0.75f)
        }
    }
}

@Composable
fun ProductDetailSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F4F5)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonBox(modifier = Modifier.size(36.dp), shape = CircleShape)
            SkeletonBox(
                modifier = Modifier.size(width = 88.dp, height = 32.dp),
                shape = RoundedCornerShape(18.dp),
            )
        }
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(0.dp),
        )
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp),
                shape = RoundedCornerShape(16.dp),
            )
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp),
                shape = RoundedCornerShape(16.dp),
            )
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(16.dp),
            )
        }
    }
}

@Composable
fun EditProfileSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XhsBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonBox(modifier = Modifier.size(36.dp), shape = CircleShape)
            SkeletonLine(
                modifier = Modifier.width(88.dp),
                height = 16.dp,
                widthFraction = 1f,
            )
            SkeletonLine(
                modifier = Modifier.width(44.dp),
                height = 16.dp,
                widthFraction = 1f,
            )
        }
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            shape = RoundedCornerShape(0.dp),
        )
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
            SkeletonCircle(size = 80.dp)
            Spacer(modifier = Modifier.height(28.dp))
            repeat(4) {
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                )
            }
        }
    }
}

@Composable
fun ListRowSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 6,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(itemCount) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonCircle(size = 36.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonLine(height = 14.dp, widthFraction = 0.7f)
                    Spacer(modifier = Modifier.height(6.dp))
                    SkeletonLine(height = 11.dp, widthFraction = 0.45f)
                }
            }
        }
    }
}

@Composable
fun SettingsSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        repeat(3) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonLine(height = 14.dp, widthFraction = 0.25f)
        Spacer(modifier = Modifier.height(12.dp))
        repeat(3) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(8.dp),
            )
        }
    }
}

enum class DetailSkeletonStyle {
    Post,
    Activity,
    Product,
}

@Composable
fun XhsDetailSkeleton(
    modifier: Modifier = Modifier,
    style: DetailSkeletonStyle = DetailSkeletonStyle.Post,
) {
    when (style) {
        DetailSkeletonStyle.Post -> PostDetailSkeleton(modifier)
        DetailSkeletonStyle.Activity -> ActivityDetailSkeleton(modifier)
        DetailSkeletonStyle.Product -> ProductDetailSkeleton(modifier)
    }
}
