package com.lfc.consumer.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.data.model.ProfileCommentDto
import com.lfc.consumer.data.model.UserProfileDto

@Composable
fun ProfileScreen(
    profile: UserProfileDto?,
    favoritePosts: List<PostDto>,
    likedPosts: List<PostDto>,
    comments: List<ProfileCommentDto>,
    isLibraryLoading: Boolean,
    selectedContentTab: Int,
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
        profileFavoritePosts = favoritePosts,
        profileLikedPosts = likedPosts,
        profileComments = comments,
        isLibraryLoading = isLibraryLoading,
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
