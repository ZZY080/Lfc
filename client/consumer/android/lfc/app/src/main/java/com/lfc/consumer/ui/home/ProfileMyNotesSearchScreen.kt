package com.lfc.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lfc.consumer.data.model.displayName
import com.lfc.consumer.data.model.ProfileSearchUiState
import com.lfc.consumer.ui.theme.XhsBackground
import com.lfc.consumer.ui.theme.XhsRed
import com.lfc.consumer.ui.theme.XhsTextPrimary
import com.lfc.consumer.ui.theme.XhsTextSecondary

@Composable
fun ProfileMyNotesSearchScreen(
    state: ProfileSearchUiState,
    onBack: () -> Unit,
    onKeywordChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClearKeyword: () -> Unit,
    onPostClick: (Int) -> Unit,
) {
    LaunchedEffect(Unit) {
        if (!state.hasSearched && state.keyword.isBlank()) {
            onClearKeyword()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(XhsBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            RowHeader(onBack = onBack)
            OutlinedTextField(
                value = state.keyword,
                onValueChange = onKeywordChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                placeholder = { Text("搜索我的笔记标题或正文") },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = XhsTextSecondary)
                },
                trailingIcon = {
                    if (state.keyword.isNotEmpty()) {
                        IconButton(onClick = onClearKeyword) {
                            Icon(Icons.Default.Close, contentDescription = "清空")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearch(state.keyword.trim()) },
                ),
                shape = RoundedCornerShape(12.dp),
            )
            Button(
                onClick = { onSearch(state.keyword.trim()) },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = XhsRed),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("搜索", fontWeight = FontWeight.Bold)
            }
        }

        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = XhsRed)
                }
            }
            !state.hasSearched -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "输入关键词后点击搜索",
                        color = XhsTextSecondary,
                        fontSize = 14.sp,
                    )
                }
            }
            state.results.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "没有找到相关笔记",
                        color = XhsTextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            else -> {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.results, key = { it.id }) { post ->
                        Box(modifier = Modifier.clickable { onPostClick(post.id) }) {
                            XhsProfileFeedCard(
                                post = post,
                                authorLabel = post.author?.displayName() ?: "同学${post.authorId}",
                                avatarUrl = post.author?.avatarUrl,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowHeader(onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
        }
        Text(
            text = "搜索我的笔记",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = XhsTextPrimary,
        )
    }
}
