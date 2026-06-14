package com.lfc.consumer.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.data.model.ProfileCommentDto
import com.lfc.consumer.data.model.ProfileTabUiState
import com.lfc.consumer.data.model.UserProfileDto

@Composable
fun UserProfileScreen(
    modifier: Modifier = Modifier,
    profile: UserProfileDto?,
    isLoading: Boolean,
    isSelf: Boolean,
    profileNotes: List<PostDto>,
    profileActivities: List<ActivityDto>,
    favoritePosts: List<PostDto>,
    favoriteActivities: List<ActivityDto>,
    likedPosts: List<PostDto>,
    likedActivities: List<ActivityDto>,
    comments: List<ProfileCommentDto>,
    tabUiState: ProfileTabUiState,
    selectedContentTab: Int = 0,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onBack: () -> Unit,
    onPostClick: (Int) -> Unit,
    onActivityClick: (Int) -> Unit = {},
    onMessage: () -> Unit,
    onFollowToggle: () -> Unit,
    onShare: () -> Unit,
    onTabSelected: (Int) -> Unit,
) {
    XhsProfileScreen(
        modifier = modifier.fillMaxSize(),
        profile = profile?.toXhsProfileData(),
        standalonePage = true,
        // 独立主页：统一访客页布局（顶栏返回），本人不展示关注/私信
        mode = XhsProfileMode.Other,
        isLoading = isLoading,
        profileNotes = profileNotes,
        profileActivities = profileActivities,
        profileFavoritePosts = favoritePosts,
        profileFavoriteActivities = favoriteActivities,
        profileLikedPosts = likedPosts,
        profileLikedActivities = likedActivities,
        profileComments = comments,
        tabUiState = tabUiState,
        onRefresh = onRefresh,
        onLoadMore = onLoadMore,
        selectedContentTab = selectedContentTab,
        onContentTabChange = onTabSelected,
        onBack = onBack,
        onPostClick = onPostClick,
        onActivityClick = onActivityClick,
        onMessage = if (isSelf) null else onMessage,
        onFollowToggle = if (isSelf) null else onFollowToggle,
        onShare = onShare,
        onTabSelected = onTabSelected,
    )
}
