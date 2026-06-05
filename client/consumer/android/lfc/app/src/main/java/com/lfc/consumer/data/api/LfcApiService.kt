package com.lfc.consumer.data.api

import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.data.model.ActivityParticipantDto
import com.lfc.consumer.data.model.AuthResponse
import com.lfc.consumer.data.model.CreateActivityRequest
import com.lfc.consumer.data.model.CreatePostRequest
import com.lfc.consumer.data.model.LoginRequest
import com.lfc.consumer.data.model.ChatMessageDto
import com.lfc.consumer.data.model.ConversationDto
import com.lfc.consumer.data.model.CreateConversationRequest
import com.lfc.consumer.data.model.NotificationDto
import com.lfc.consumer.data.model.SendChatMessageRequest
import com.lfc.consumer.data.model.CreatePostCommentRequest
import com.lfc.consumer.data.model.PostCommentDto
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.data.model.ActivitySocialStateDto
import com.lfc.consumer.data.model.PostSocialStateDto
import com.lfc.consumer.data.model.PostFeedResponse
import com.lfc.consumer.data.model.PaginatedResponse
import com.lfc.consumer.data.model.UnreadCountDto
import com.lfc.consumer.data.model.UploadImageResponse
import com.lfc.consumer.data.model.AlipayAccountBindingDto
import com.lfc.consumer.data.model.AlipayAuthInfoDto
import com.lfc.consumer.data.model.BindAlipayAccountRequest
import com.lfc.consumer.data.model.BindAlipayOAuthRequest
import com.lfc.consumer.data.model.UpdateProfileRequest
import com.lfc.consumer.data.model.FollowStateDto
import com.lfc.consumer.data.model.ProfileCommentDto
import com.lfc.consumer.data.model.UserProfileDto
import retrofit2.http.PUT
import retrofit2.http.Query
import com.lfc.consumer.data.model.PaymentConfigDto
import com.lfc.consumer.data.model.PaymentOrderDetailDto
import com.lfc.consumer.data.model.PaymentOrderListItemDto
import com.lfc.consumer.data.model.PaymentOrderResultDto
import com.lfc.consumer.data.model.PaymentOrderTabCountsDto
import com.lfc.consumer.data.model.PaymentTransactionItemDto
import com.lfc.consumer.data.model.CreateOrderReviewRequest
import com.lfc.consumer.data.model.ApplyAfterSalesRequest
import com.lfc.consumer.data.model.UpdateActivityRequest
import com.lfc.consumer.data.model.UpdatePostRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface LfcApiService {
    @POST("consumer/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @Multipart
    @POST("consumer/auth/register")
    suspend fun register(
        @Part("email") email: RequestBody,
        @Part("password") password: RequestBody,
        @Part("studentId") studentId: RequestBody,
        @Part("realName") realName: RequestBody,
        @Part studentCard: MultipartBody.Part,
    ): AuthResponse

    @GET("consumer/post")
    suspend fun getPosts(): List<PostDto>

    @GET("consumer/post/feed")
    suspend fun getPostFeed(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("sort") sort: String? = null,
        @Query("keyword") keyword: String? = null,
        @Query("tab") tab: String? = null,
    ): PostFeedResponse

    @GET("consumer/post/mine")
    suspend fun getMyPosts(@Query("keyword") keyword: String? = null): List<PostDto>

    @GET("consumer/user/me")
    suspend fun getMyProfile(): UserProfileDto

    @PATCH("consumer/user/me")
    suspend fun updateMyProfile(@Body request: UpdateProfileRequest): UserProfileDto

    @GET("consumer/user/me/alipay")
    suspend fun getMyAlipayAccount(): AlipayAccountBindingDto

    @GET("consumer/user/me/alipay/auth-info")
    suspend fun getAlipayOAuthAuthInfo(): AlipayAuthInfoDto

    @POST("consumer/user/me/alipay/oauth")
    suspend fun bindMyAlipayByOAuth(@Body request: BindAlipayOAuthRequest): AlipayAccountBindingDto

    @PUT("consumer/user/me/alipay")
    suspend fun bindMyAlipayAccount(@Body request: BindAlipayAccountRequest): AlipayAccountBindingDto

    @DELETE("consumer/user/me/alipay")
    suspend fun unbindMyAlipayAccount(): AlipayAccountBindingDto

    @GET("consumer/user/me/favorites")
    suspend fun getMyFavoritePosts(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
    ): PaginatedResponse<PostDto>

    @GET("consumer/user/me/likes")
    suspend fun getMyLikedPosts(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
    ): PaginatedResponse<PostDto>

    @GET("consumer/user/me/favorite-activities")
    suspend fun getMyFavoriteActivities(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
    ): PaginatedResponse<ActivityDto>

    @GET("consumer/user/me/liked-activities")
    suspend fun getMyLikedActivities(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
    ): PaginatedResponse<ActivityDto>

    @GET("consumer/user/me/comments")
    suspend fun getMyProfileComments(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
    ): PaginatedResponse<ProfileCommentDto>

    @GET("consumer/user/{id}/posts")
    suspend fun getUserPosts(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
    ): PaginatedResponse<PostDto>

    @GET("consumer/user/{id}/activities")
    suspend fun getUserActivities(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
    ): PaginatedResponse<ActivityDto>

    @GET("consumer/user/{id}/favorites")
    suspend fun getUserFavoritePosts(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
    ): PaginatedResponse<PostDto>

    @GET("consumer/user/{id}/likes")
    suspend fun getUserLikedPosts(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
    ): PaginatedResponse<PostDto>

    @GET("consumer/user/{id}/favorite-activities")
    suspend fun getUserFavoriteActivities(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
    ): PaginatedResponse<ActivityDto>

    @GET("consumer/user/{id}/liked-activities")
    suspend fun getUserLikedActivities(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
    ): PaginatedResponse<ActivityDto>

    @GET("consumer/user/{id}/comments")
    suspend fun getUserProfileComments(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
    ): PaginatedResponse<ProfileCommentDto>

    @POST("consumer/user/{id}/follow")
    suspend fun toggleFollow(@Path("id") id: Int): FollowStateDto

    @GET("consumer/user/lfc/{lfcNo}/profile")
    suspend fun getUserProfileByLfcNo(@Path("lfcNo") lfcNo: String): UserProfileDto

    @GET("consumer/user/{id}/profile")
    suspend fun getUserProfile(@Path("id") id: Int): UserProfileDto

    @GET("consumer/post/{id}")
    suspend fun getPost(@Path("id") id: Int): PostDto

    @POST("consumer/post/{id}/like")
    suspend fun togglePostLike(@Path("id") id: Int): PostSocialStateDto

    @POST("consumer/post/{id}/favorite")
    suspend fun togglePostFavorite(@Path("id") id: Int): PostSocialStateDto

    @GET("consumer/post/{id}/comments")
    suspend fun getPostComments(@Path("id") id: Int): List<PostCommentDto>

    @POST("consumer/post/{id}/comments")
    suspend fun createPostComment(
        @Path("id") id: Int,
        @Body request: CreatePostCommentRequest,
    ): PostCommentDto

    @DELETE("consumer/post/comments/{commentId}")
    suspend fun deletePostComment(@Path("commentId") commentId: Int)

    @POST("consumer/post")
    suspend fun createPost(@Body request: CreatePostRequest): PostDto

    @Multipart
    @POST("consumer/upload/image")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part,
        @Query("scope") scope: String? = null,
    ): UploadImageResponse

    @Multipart
    @POST("consumer/upload/video")
    suspend fun uploadVideo(
        @Part file: MultipartBody.Part,
        @Query("scope") scope: String? = null,
    ): UploadImageResponse

    @PATCH("consumer/post/{id}")
    suspend fun updatePost(
        @Path("id") id: Int,
        @Body request: UpdatePostRequest,
    ): PostDto

    @DELETE("consumer/post/{id}")
    suspend fun deletePost(@Path("id") id: Int)

    @GET("consumer/activity/feed")
    suspend fun getActivityFeed(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): PaginatedResponse<ActivityDto>

    @GET("consumer/activity")
    suspend fun getApprovedActivities(): List<ActivityDto>

    @GET("consumer/activity/mine")
    suspend fun getMyActivities(): List<ActivityDto>

    @GET("consumer/activity/{id}")
    suspend fun getActivity(@Path("id") id: Int): ActivityDto

    @GET("consumer/activity/participations/mine")
    suspend fun getMyParticipations(): List<ActivityParticipantDto>

    @POST("consumer/activity")
    suspend fun createActivity(@Body request: CreateActivityRequest): ActivityDto

    @PATCH("consumer/activity/{id}")
    suspend fun updateActivity(
        @Path("id") id: Int,
        @Body request: UpdateActivityRequest,
    ): ActivityDto

    @DELETE("consumer/activity/{id}")
    suspend fun deleteActivity(@Path("id") id: Int)

    @POST("consumer/activity/{id}/join")
    suspend fun joinActivity(@Path("id") id: Int): ActivityParticipantDto

    @POST("consumer/activity/{id}/like")
    suspend fun toggleActivityLike(@Path("id") id: Int): ActivitySocialStateDto

    @POST("consumer/activity/{id}/favorite")
    suspend fun toggleActivityFavorite(@Path("id") id: Int): ActivitySocialStateDto

    @DELETE("consumer/activity/{id}/join")
    suspend fun leaveActivity(@Path("id") id: Int)

    @GET("consumer/payment/config")
    suspend fun getPaymentConfig(): PaymentConfigDto

    @GET("consumer/payment/orders")
    suspend fun getPaymentOrders(
        @Query("tab") tab: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): PaginatedResponse<PaymentOrderListItemDto>

    @GET("consumer/payment/orders/counts")
    suspend fun getPaymentOrderTabCounts(): PaymentOrderTabCountsDto

    @GET("consumer/payment/transactions")
    suspend fun getPaymentTransactions(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): PaginatedResponse<PaymentTransactionItemDto>

    @POST("consumer/payment/orders/{outTradeNo}/cancel")
    suspend fun cancelPaymentOrder(@Path("outTradeNo") outTradeNo: String): PaymentOrderDetailDto

    @POST("consumer/payment/orders/{outTradeNo}/repay")
    suspend fun repayPaymentOrder(@Path("outTradeNo") outTradeNo: String): PaymentOrderResultDto

    @POST("consumer/payment/orders/{outTradeNo}/reviews")
    suspend fun createPaymentOrderReview(
        @Path("outTradeNo") outTradeNo: String,
        @Body request: CreateOrderReviewRequest,
    ): Map<String, Any?>

    @POST("consumer/payment/orders/{outTradeNo}/after-sales")
    suspend fun applyPaymentAfterSales(
        @Path("outTradeNo") outTradeNo: String,
        @Body request: ApplyAfterSalesRequest,
    ): Map<String, Any?>

    @DELETE("consumer/payment/orders/{outTradeNo}/after-sales")
    suspend fun cancelPaymentAfterSales(@Path("outTradeNo") outTradeNo: String): Map<String, Any?>

    @POST("consumer/payment/activity/{activityId}/order")
    suspend fun createActivityPaymentOrder(
        @Path("activityId") activityId: Int,
    ): PaymentOrderResultDto

    @POST("consumer/payment/post/{postId}/order")
    suspend fun createPostProductOrder(
        @Path("postId") postId: Int,
    ): PaymentOrderResultDto

    @GET("consumer/payment/post/{postId}/order")
    suspend fun getPostProductOrder(
        @Path("postId") postId: Int,
    ): PaymentOrderDetailDto?

    @GET("consumer/payment/orders/{outTradeNo}")
    suspend fun getPaymentOrder(@Path("outTradeNo") outTradeNo: String): PaymentOrderDetailDto

    @POST("consumer/payment/orders/{outTradeNo}/confirm-receipt")
    suspend fun confirmPaymentReceipt(
        @Path("outTradeNo") outTradeNo: String,
    ): PaymentOrderDetailDto

    @PATCH("consumer/post/{id}/product/off-shelf")
    suspend fun offShelfPostProduct(@Path("id") id: Int)

    @PATCH("consumer/post/{id}/product/on-shelf")
    suspend fun onShelfPostProduct(@Path("id") id: Int)

    @GET("consumer/notification")
    suspend fun getNotifications(): List<NotificationDto>

    @GET("consumer/notification/unread-count")
    suspend fun getNotificationUnreadCount(): UnreadCountDto

    @GET("consumer/notification/{id}")
    suspend fun getNotification(@Path("id") id: Int): NotificationDto

    @PATCH("consumer/notification/read-all")
    suspend fun markAllNotificationsRead()

    @DELETE("consumer/notification/{id}")
    suspend fun deleteNotification(@Path("id") id: Int)

    @GET("consumer/conversation")
    suspend fun getConversations(): List<ConversationDto>

    @GET("consumer/conversation/unread-count")
    suspend fun getConversationUnreadCount(): UnreadCountDto

    @POST("consumer/conversation")
    suspend fun createConversation(@Body request: CreateConversationRequest): ConversationDto

    @GET("consumer/conversation/{id}/messages")
    suspend fun getChatMessages(@Path("id") id: Int): List<ChatMessageDto>

    @POST("consumer/conversation/{id}/messages")
    suspend fun sendChatMessage(
        @Path("id") id: Int,
        @Body request: SendChatMessageRequest,
    ): ChatMessageDto
}
