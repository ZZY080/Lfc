package com.lfc.consumer.ui.home

import androidx.compose.runtime.Composable
import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.data.model.ProfileCommentDto
import com.lfc.consumer.data.model.ProfileTabUiState
import com.lfc.consumer.data.model.UserProfileDto

@Composable
fun UserProfileScreen(
    profile: UserProfileDto?,
    isLoading: Boolean,
    isSelf: Boolean,
    profileNotes: List<PostDto>,
    profileActivities: List<ActivityDto>,
    favoritePosts: List<PostDto>,
    likedPosts: List<PostDto>,
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
        profile = profile?.toXhsProfileData(),
        mode = if (isSelf) XhsProfileMode.Self else XhsProfileMode.Other,
        isLoading = isLoading,
        profileNotes = profileNotes,
        profileActivities = profileActivities,
        profileFavoritePosts = favoritePosts,
        profileLikedPosts = likedPosts,
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
