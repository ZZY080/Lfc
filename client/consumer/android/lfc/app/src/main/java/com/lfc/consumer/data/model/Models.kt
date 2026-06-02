package com.lfc.consumer.data.model

data class UserDto(
    val id: Int,
    val email: String,
    val studentId: String,
    val role: String,
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
    val author: UserDto? = null,
)

data class PostFeedResponse(
    val items: List<PostDto>,
    val total: Int,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean,
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
    val title: String,
    val content: String,
)

data class UpdatePostRequest(
    val title: String? = null,
    val content: String? = null,
)

data class ActivityDto(
    val id: Int,
    val title: String,
    val description: String,
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
    val title: String,
    val description: String,
    val location: String,
    val startTime: String,
    val endTime: String,
    val maxParticipants: Int = 0,
)

data class UpdateActivityRequest(
    val title: String? = null,
    val description: String? = null,
    val location: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val maxParticipants: Int? = null,
)

data class LoginRequest(
    val email: String,
    val password: String,
)

data class MessageDto(
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

data class UnreadCountDto(
    val count: Int,
)
