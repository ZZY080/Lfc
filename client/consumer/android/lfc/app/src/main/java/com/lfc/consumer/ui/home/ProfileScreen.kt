package com.lfc.consumer.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.data.model.ProfileCommentDto
import com.lfc.consumer.data.model.ProfileTabUiState
import com.lfc.consumer.data.model.UserProfileDto

@Composable
fun ProfileScreen(
    profile: UserProfileDto?,
    profileNotes: List<PostDto>,
    profileActivities: List<ActivityDto>,
    favoritePosts: List<PostDto>,
    likedPosts: List<PostDto>,
    comments: List<ProfileCommentDto>,
    tabUiState: ProfileTabUiState,
    selectedContentTab: Int,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onEditPost: (PostDto) -> Unit,
    onViewPost: (Int) -> Unit,
    onDeletePost: (Int) -> Unit,
    onEditActivity: (ActivityDto) -> Unit,
    onViewActivity: (Int) -> Unit,
    onDeleteActivity: (Int) -> Unit,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    onShare: () -> Unit,
    onShowQr: () -> Unit,
    onScanProfile: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onGoToMessages: () -> Unit,
    onOpenSideMenu: () -> Unit,
    unreadCount: Int = 0,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    XhsProfileScreen(
        profile = profile?.toXhsProfileData(),
        mode = XhsProfileMode.Self,
        isLoading = profile == null,
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
        onPostClick = onViewPost,
        onActivityClick = onViewActivity,
        onLogout = onLogout,
        onEditProfile = onEditProfile,
        onShare = onShare,
        onShowQr = onShowQr,
        onScanProfile = onScanProfile,
        onSearch = onSearch,
        onSettings = onSettings,
        onGoToMessages = onGoToMessages,
        onOpenSideMenu = onOpenSideMenu,
        unreadCount = unreadCount,
        onTabSelected = onTabSelected,
        onEditPost = onEditPost,
        onDeletePost = onDeletePost,
        onEditActivity = onEditActivity,
        onDeleteActivity = onDeleteActivity,
        modifier = modifier,
    )
}
