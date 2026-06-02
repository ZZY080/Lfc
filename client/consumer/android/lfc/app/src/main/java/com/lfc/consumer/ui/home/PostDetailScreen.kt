package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.ui.theme.XhsBackground
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

@Composable
fun PostDetailScreen(
    post: PostDto?,
    isLoading: Boolean,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(XhsBackground),
    ) {
        when {
            isLoading -> XhsDetailLoading(Modifier.fillMaxSize())
            post == null -> XhsDetailEmpty("笔记不存在或已删除", Modifier.fillMaxSize())
            else -> {
                val authorLabel = post.author?.studentId ?: "同学${post.authorId}"
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .background(coverGradientForId(post.id)),
                            contentAlignment = Alignment.BottomStart,
                        ) {
                            Text(
                                text = post.title,
                                modifier = Modifier.padding(20.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                lineHeight = 30.sp,
                            )
                        }
                        XhsDetailAuthorRow(authorLabel = authorLabel)
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = post.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = XhsTextPrimary,
                                lineHeight = 26.sp,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = post.content,
                                style = MaterialTheme.typography.bodyLarge,
                                color = XhsTextPrimary,
                                lineHeight = 24.sp,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = formatXhsTime(post.createdAt),
                                fontSize = 12.sp,
                                color = XhsTextSecondary,
                            )
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                    XhsPostDetailBottomBar()
                }
            }
        }
        XhsDetailBackButton(onBack = onBack)
    }
}
