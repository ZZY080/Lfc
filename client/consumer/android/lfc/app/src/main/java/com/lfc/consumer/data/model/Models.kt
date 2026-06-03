package com.lfc.consumer.data.model

data class UserDto(
    val id: Int,
    val email: String,
    val studentId: String,
    val role: String,
    val nickname: String? = null,
    val avatarUrl: String? = null,
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDto,
)

data class PostDto(
    val id: Int,
    val title: String,
    val content: String,
    val authorId: Int,
    val createdAt: String,
    val updatedAt: String,
    val images: List<String>? = null,
    val likeCount: Int = 0,
    val favoriteCount: Int = 0,
    val commentCount: Int = 0,
    val isLiked: Boolean = false,
    val isFavorited: Boolean = false,
    val author: UserDto? = null,
)

data class PostCommentDto(
    val id: Int,
    val postId: Int,
    val userId: Int,
    val content: String,
    val parentId: Int? = null,
    val createdAt: String,
    val author: UserDto? = null,
)

data class CreatePostCommentRequest(
    val content: String,
    val parentId: Int? = null,
)

data class PostSocialStateDto(
    val likeCount: Int,
    val favoriteCount: Int,
    val commentCount: Int,
    val isLiked: Boolean,
    val isFavorited: Boolean,
)

data class PostFeedResponse(
    val items: List<PostDto>,
    val total: Int,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean,
)

data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Int,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean,
)

data class ProfileTabUiState(
    val page: Int = 1,
    val hasMore: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isInitialLoading: Boolean = false,
)

data class ProfileTabsUiState(
    val targetUserId: Int? = null,
    val tabs: List<ProfileTabUiState> = List(5) { ProfileTabUiState() },
)

data class FeedUiState(
    val posts: List<PostDto> = emptyList(),
    val selectedTab: String = "推荐",
    val page: Int = 1,
    val hasMore: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isInitialLoading: Boolean = false,
)

data class ActivityFeedUiState(
    val activities: List<ActivityDto> = emptyList(),
    val page: Int = 1,
    val hasMore: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isInitialLoading: Boolean = false,
)

data class SearchUiState(
    val keyword: String = "",
    val selectedTab: String = "综合",
    val posts: List<PostDto> = emptyList(),
    val activities: List<ActivityDto> = emptyList(),
    val page: Int = 1,
    val hasMore: Boolean = true,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
)

data class CreatePostRequest(
    val title: String? = null,
    val content: String? = null,
    val images: List<String>? = null,
)

data class UpdatePostRequest(
    val title: String? = null,
    val content: String? = null,
    val images: List<String>? = null,
)

data class UploadImageResponse(
    val url: String,
)

data class ActivityDto(
    val id: Int,
    val title: String,
    val description: String,
    val images: List<String>? = null,
    val location: String,
    val startTime: String,
    val endTime: String,
    val maxParticipants: Int,
    val status: String,
    val authorId: Int,
    val createdAt: String,
    val updatedAt: String,
    val author: UserDto? = null,
    val participants: List<ActivityParticipantDto>? = null,
)

data class ActivityParticipantDto(
    val id: Int,
    val activityId: Int,
    val userId: Int,
    val joinedAt: String,
    val user: UserDto? = null,
    val activity: ActivityDto? = null,
)

data class CreateActivityRequest(
    val title: String? = null,
    val description: String? = null,
    val images: List<String>? = null,
    val location: String,
    val startTime: String,
    val endTime: String,
    val maxParticipants: Int = 0,
)

data class UpdateActivityRequest(
    val title: String? = null,
    val description: String? = null,
    val images: List<String>? = null,
    val location: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val maxParticipants: Int? = null,
)

data class UserProfileDto(
    val id: Int,
    val lfcNo: String,
    val studentId: String,
    val nickname: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val coverUrl: String? = null,
    val postCount: Int,
    val followingCount: Int = 0,
    val followerCount: Int = 0,
    val likeAndFavoriteCount: Int = 0,
    val isFollowing: Boolean = false,
    val isSelf: Boolean = false,
    val showCommentsPublic: Boolean = false,
    val showFavoritesPublic: Boolean = false,
    val showLikesPublic: Boolean = false,
    val posts: List<PostDto> = emptyList(),
    val activities: List<ActivityDto> = emptyList(),
    val participationCount: Int = 0,
)

data class UpdateProfileRequest(
    val nickname: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val coverUrl: String? = null,
    val showCommentsPublic: Boolean? = null,
    val showFavoritesPublic: Boolean? = null,
    val showLikesPublic: Boolean? = null,
)

data class FollowStateDto(
    val isFollowing: Boolean,
)

data class ProfileCommentDto(
    val id: Int,
    val postId: Int,
    val userId: Int,
    val content: String,
    val parentId: Int? = null,
    val createdAt: String,
    val post: PostDto? = null,
)

fun UserDto.displayName(): String = nickname?.takeIf { it.isNotBlank() } ?: studentId

fun UserProfileDto.displayName(): String = nickname?.takeIf { it.isNotBlank() } ?: studentId

data class LoginRequest(
    val email: String,
    val password: String,
)

data class NotificationDto(
    val id: Int,
    val userId: Int,
    val title: String,
    val content: String,
    val type: String,
    val relatedType: String?,
    val relatedId: Int?,
    val isRead: Boolean,
    val createdAt: String,
)

data class ConversationDto(
    val id: Int,
    val peerUserId: Int,
    val peerStudentId: String,
    val lastMessageContent: String? = null,
    val lastMessageAt: String? = null,
    val unreadCount: Int = 0,
    val createdAt: String,
    val updatedAt: String,
)

data class ChatMessageDto(
    val id: Int,
    val conversationId: Int,
    val senderId: Int,
    val content: String,
    val messageType: String = "TEXT",
    val isRead: Boolean = false,
    val createdAt: String,
    val sender: UserDto? = null,
)

data class CreateConversationRequest(
    val peerUserId: Int,
)

data class SendChatMessageRequest(
    val content: String,
)

data class UnreadCountDto(
    val count: Int,
)
