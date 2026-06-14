package com.lfc.consumer.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    favoriteActivities: List<ActivityDto>,
    likedPosts: List<PostDto>,
    likedActivities: List<ActivityDto>,
    comments: List<ProfileCommentDto>,
    tabUiState: ProfileTabUiState,
    selectedContentTab: Int,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onEditPost: (PostDto) -> Unit,
    onViewPost: (Int) -> Unit,
    onDeletePost: (Int) -> Unit,
    onOffShelfPost: (Int) -> Unit = {},
    onOnShelfPost: (Int) -> Unit = {},
    onEditActivity: (ActivityDto) -> Unit,
    onViewActivity: (Int) -> Unit,
    onDeleteActivity: (Int) -> Unit,
    onOffShelfActivity: (Int) -> Unit = {},
    onOnShelfActivity: (Int) -> Unit = {},
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    onShare: () -> Unit,
    onShowQr: () -> Unit,
    onScanProfile: () -> Unit,
    onSettings: () -> Unit,
    onBindAlipay: () -> Unit,
    onGoToMessages: () -> Unit,
    onOpenSideMenu: () -> Unit,
    unreadCount: Int = 0,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    immersiveBottomPadding: Dp = 0.dp,
) {
    XhsProfileScreen(
        profile = profile?.toXhsProfileData(),
        mode = XhsProfileMode.Self,
        isLoading = profile == null,
        immersiveBottomPadding = immersiveBottomPadding,
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
        onPostClick = onViewPost,
        onActivityClick = onViewActivity,
        onLogout = onLogout,
        onEditProfile = onEditProfile,
        onShare = onShare,
        onShowQr = onShowQr,
        onScanProfile = onScanProfile,
        onSettings = onSettings,
        onBindAlipay = onBindAlipay,
        onGoToMessages = onGoToMessages,
        onOpenSideMenu = onOpenSideMenu,
        unreadCount = unreadCount,
        onTabSelected = onTabSelected,
        onEditPost = onEditPost,
        onDeletePost = onDeletePost,
        onOffShelfPost = onOffShelfPost,
        onOnShelfPost = onOnShelfPost,
        onEditActivity = onEditActivity,
        onDeleteActivity = onDeleteActivity,
        onOffShelfActivity = onOffShelfActivity,
        onOnShelfActivity = onOnShelfActivity,
        modifier = modifier,
    )
}
