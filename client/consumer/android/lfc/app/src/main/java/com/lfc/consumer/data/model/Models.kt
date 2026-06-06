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
    val viewCount: Int = 0,
    val isLiked: Boolean = false,
    val isFavorited: Boolean = false,
    val savedAt: String? = null,
    val author: UserDto? = null,
    val product: PostProductDto? = null,
)

data class PostProductDto(
    val id: Int,
    val postId: Int,
    val price: String,
    val originalPrice: String? = null,
    val category: String = "SECOND_HAND",
    val condition: String = "GOOD",
    val deliveryMethod: String = "PICKUP",
    val status: String = "ON_SALE",
    val buyerId: Int? = null,
    val soldAt: String? = null,
)

data class PostProductRequest(
    val price: Double,
    val originalPrice: Double? = null,
    val category: String? = null,
    val condition: String? = null,
    val deliveryMethod: String? = null,
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

data class ActivitySocialStateDto(
    val likeCount: Int,
    val favoriteCount: Int,
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
    val hasLoadedOnce: Boolean = false,
)

data class ProfileTabsUiState(
    val targetUserId: Int? = null,
    val tabs: List<ProfileTabUiState> = List(5) { ProfileTabUiState() },
    val favoriteActivities: ProfileTabUiState = ProfileTabUiState(),
    val likedActivities: ProfileTabUiState = ProfileTabUiState(),
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
    val product: PostProductRequest? = null,
)

data class UpdatePostRequest(
    val title: String? = null,
    val content: String? = null,
    val images: List<String>? = null,
    val product: PostProductRequest? = null,
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
    val latitude: Double? = null,
    val longitude: Double? = null,
    val startTime: String,
    val endTime: String,
    val maxParticipants: Int,
    val fee: String? = "0",
    val status: String,
    val authorId: Int,
    val createdAt: String,
    val updatedAt: String,
    val author: UserDto? = null,
    val participants: List<ActivityParticipantDto>? = null,
    val isJoined: Boolean = false,
    val likeCount: Int = 0,
    val favoriteCount: Int = 0,
    val isLiked: Boolean = false,
    val isFavorited: Boolean = false,
    val savedAt: String? = null,
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
    val latitude: Double? = null,
    val longitude: Double? = null,
    val startTime: String,
    val endTime: String,
    val maxParticipants: Int = 0,
    val fee: Double? = null,
)

data class UpdateActivityRequest(
    val title: String? = null,
    val description: String? = null,
    val images: List<String>? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val maxParticipants: Int? = null,
    val fee: Double? = null,
)

data class PaymentOrderResultDto(
    val outTradeNo: String,
    val channel: String,
    val amount: String,
    val platformFee: String,
    val payeeAmount: String,
    val platformFeeRateLabel: String,
    val subject: String,
    val status: String,
    val payeeId: Int,
    val alipay: AlipayPayPayloadDto,
)

data class PaymentConfigDto(
    val platformFeeRate: Double,
    val platformFeeRateLabel: String,
    val platformFeeMin: Double,
    val autoConfirmDays: Int = 7,
)

data class AlipayPayPayloadDto(
    val orderStr: String,
)

data class AlipayAuthInfoDto(
    val authInfo: String,
)

data class BindAlipayOAuthRequest(
    val authCode: String,
)

data class PaymentOrderDetailDto(
    val outTradeNo: String,
    val channel: String,
    val amount: String,
    val platformFee: String? = null,
    val payeeAmount: String? = null,
    val subject: String,
    val status: String,
    val bizType: String,
    val bizId: Int,
    val payeeId: Int,
    val tradeNo: String? = null,
    val paidAt: String? = null,
    val confirmedAt: String? = null,
    val settledAt: String? = null,
    val autoConfirmAt: String? = null,
    val canConfirmReceipt: Boolean = false,
    val createdAt: String,
)

data class FulfillmentStepDto(
    val label: String,
    val done: Boolean = false,
    val active: Boolean = false,
)

data class OrderFulfillmentGuaranteeDto(
    val title: String,
    val summary: String,
    val steps: List<FulfillmentStepDto> = emptyList(),
)

data class PaymentOrderListItemDto(
    val outTradeNo: String,
    val amount: String,
    val subject: String,
    val status: String,
    val statusLabel: String,
    val bizType: String,
    val bizId: Int,
    val bizTitle: String,
    val coverImage: String? = null,
    val payeeId: Int,
    val payeeName: String,
    val payeeRoleLabel: String = "卖家",
    val payeeAvatarUrl: String? = null,
    val platformFee: String? = null,
    val payeeAmount: String? = null,
    val paidAt: String? = null,
    val confirmedAt: String? = null,
    val settledAt: String? = null,
    val autoConfirmAt: String? = null,
    val bizStartTime: String? = null,
    val bizEndTime: String? = null,
    val bizLocation: String? = null,
    val fulfillment: OrderFulfillmentGuaranteeDto? = null,
    val createdAt: String,
    val canPay: Boolean = false,
    val canConfirmReceipt: Boolean = false,
    val canReview: Boolean = false,
    val canApplyAfterSales: Boolean = false,
    val hasReview: Boolean = false,
    val afterSalesStatus: String? = null,
)

data class PaymentOrderTabCountsDto(
    val all: Int = 0,
    val pendingPayment: Int = 0,
    val awaitingReceipt: Int = 0,
    val review: Int = 0,
    val afterSales: Int = 0,
)

data class CreateOrderReviewRequest(
    val rating: Int,
    val content: String? = null,
)

data class ApplyAfterSalesRequest(
    val reason: String,
)

data class OrderCenterUiState(
    val selectedTab: String = "all",
    val orders: List<PaymentOrderListItemDto> = emptyList(),
    val tabCounts: PaymentOrderTabCountsDto? = null,
    val page: Int = 1,
    val hasMore: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isInitialLoading: Boolean = false,
    val actingOutTradeNo: String? = null,
)

data class PaymentTransactionItemDto(
    val txKey: String,
    val type: String,
    val typeLabel: String,
    val direction: String,
    val amount: String,
    val outTradeNo: String,
    val tradeNo: String? = null,
    val subject: String,
    val bizType: String,
    val bizId: Int,
    val bizTitle: String,
    val coverImage: String? = null,
    val counterpartyName: String,
    val occurredAt: String,
)

data class PaymentTransactionLedgerUiState(
    val items: List<PaymentTransactionItemDto> = emptyList(),
    val page: Int = 1,
    val hasMore: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isInitialLoading: Boolean = false,
)

data class ProfileSearchUiState(
    val keyword: String = "",
    val results: List<PostDto> = emptyList(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
)

val ORDER_CENTER_TABS = listOf(
    "all" to "全部",
    "pending_payment" to "待付款",
    "awaiting_receipt" to "待履约",
    "review" to "评价",
    "after_sales" to "售后",
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
    val alipayBound: Boolean = false,
    val alipayLoginIdMasked: String? = null,
    val posts: List<PostDto> = emptyList(),
    val activities: List<ActivityDto> = emptyList(),
    val participationCount: Int = 0,
)

data class BindAlipayAccountRequest(
    val alipayLoginId: String,
    val alipayRealName: String? = null,
)

data class AlipayAccountBindingDto(
    val alipayBound: Boolean,
    val alipayLoginIdMasked: String? = null,
    val alipayRealName: String? = null,
    val alipayBoundAt: String? = null,
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

fun ActivityDto.feeAmount(): Double = fee?.toDoubleOrNull() ?: 0.0

fun ActivityDto.isPaidActivity(): Boolean = feeAmount() > 0

fun PostProductDto.isOnSale(): Boolean = status.equals("ON_SALE", ignoreCase = true)

fun PostProductDto.isSold(): Boolean = status.equals("SOLD", ignoreCase = true)

fun PostDto.hasOnSaleProduct(): Boolean = product?.isOnSale() == true

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
    val peerNickname: String? = null,
    val peerAvatarUrl: String? = null,
    val lastMessageContent: String? = null,
    val lastMessageAt: String? = null,
    val unreadCount: Int = 0,
    val createdAt: String,
    val updatedAt: String,
)

fun ConversationDto.peerDisplayName(): String =
    peerNickname?.takeIf { it.isNotBlank() } ?: peerStudentId

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
    val messageType: String? = null,
)

data class ChatProductPayload(
    val postId: Int,
    val title: String,
    val price: String,
    val coverUrl: String? = null,
    val status: String? = null,
    val category: String? = null,
)

fun PostDto.toChatProductPayload(): ChatProductPayload? {
    val product = product ?: return null
    return ChatProductPayload(
        postId = id,
        title = productDisplayTitle(),
        price = product.price,
        coverUrl = images?.firstOrNull(),
        status = product.status,
        category = product.category,
    )
}

fun PostDto.productDisplayTitle(): String {
    val trimmedTitle = title.trim()
    val trimmedContent = content.trim()
    val categoryLabel = when (product?.category?.uppercase()) {
        "SECOND_HAND" -> "二手闲置"
        "DIGITAL" -> "数码"
        "BOOK" -> "书籍"
        "DAILY" -> "日用"
        else -> "闲置"
    }
    val titleLooksWeak = trimmedTitle.isBlank() ||
        trimmedTitle in setOf("图片笔记", "校园笔记") ||
        trimmedTitle == trimmedContent.take(30) ||
        (trimmedTitle.length <= 4 && trimmedTitle.all { it.isDigit() || it.isLetter() })
    return when {
        !titleLooksWeak -> trimmedTitle
        trimmedContent.isNotBlank() -> trimmedContent.lineSequence().first().take(36)
        else -> "${categoryLabel}好物"
    }
}

data class UnreadCountDto(
    val count: Int,
)
