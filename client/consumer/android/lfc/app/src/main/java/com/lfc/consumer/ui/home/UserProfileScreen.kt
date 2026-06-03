package com.lfc.consumer.ui.home

import androidx.compose.runtime.Composable
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.data.model.ProfileCommentDto
import com.lfc.consumer.data.model.UserProfileDto

@Composable
fun UserProfileScreen(
    profile: UserProfileDto?,
    isLoading: Boolean,
    isSelf: Boolean,
    favoritePosts: List<PostDto>,
    likedPosts: List<PostDto>,
    comments: List<ProfileCommentDto>,
    isLibraryLoading: Boolean,
    onBack: () -> Unit,
    onPostClick: (Int) -> Unit,
    onMessage: () -> Unit,
    onFollowToggle: () -> Unit,
    onShare: () -> Unit,
    onTabSelected: (Int) -> Unit,
) {
    XhsProfileScreen(
        profile = profile?.toXhsProfileData(),
        mode = if (isSelf) XhsProfileMode.Self else XhsProfileMode.Other,
        isLoading = isLoading,
        profileFavoritePosts = favoritePosts,
        profileLikedPosts = likedPosts,
        profileComments = comments,
        isLibraryLoading = isLibraryLoading,
        onBack = if (isSelf) null else onBack,
        onPostClick = onPostClick,
        onMessage = if (isSelf) null else onMessage,
        onFollowToggle = if (isSelf) null else onFollowToggle,
        onShare = onShare,
        onTabSelected = onTabSelected,
    )
}
