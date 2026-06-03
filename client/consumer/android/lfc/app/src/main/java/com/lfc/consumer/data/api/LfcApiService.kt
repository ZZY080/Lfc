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
import com.lfc.consumer.data.model.PostSocialStateDto
import com.lfc.consumer.data.model.PostFeedResponse
import com.lfc.consumer.data.model.UnreadCountDto
import com.lfc.consumer.data.model.UploadImageResponse
import com.lfc.consumer.data.model.UpdateProfileRequest
import com.lfc.consumer.data.model.FollowStateDto
import com.lfc.consumer.data.model.ProfileCommentDto
import com.lfc.consumer.data.model.UserProfileDto
import retrofit2.http.Query
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

    @GET("consumer/user/me/favorites")
    suspend fun getMyFavoritePosts(): List<PostDto>

    @GET("consumer/user/me/likes")
    suspend fun getMyLikedPosts(): List<PostDto>

    @GET("consumer/user/me/comments")
    suspend fun getMyProfileComments(): List<ProfileCommentDto>

    @GET("consumer/user/{id}/favorites")
    suspend fun getUserFavoritePosts(@Path("id") id: Int): List<PostDto>

    @GET("consumer/user/{id}/likes")
    suspend fun getUserLikedPosts(@Path("id") id: Int): List<PostDto>

    @GET("consumer/user/{id}/comments")
    suspend fun getUserProfileComments(@Path("id") id: Int): List<ProfileCommentDto>

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

    @PATCH("consumer/post/{id}")
    suspend fun updatePost(
        @Path("id") id: Int,
        @Body request: UpdatePostRequest,
    ): PostDto

    @DELETE("consumer/post/{id}")
    suspend fun deletePost(@Path("id") id: Int)

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

    @DELETE("consumer/activity/{id}/join")
    suspend fun leaveActivity(@Path("id") id: Int)

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
