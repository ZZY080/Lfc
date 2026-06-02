package com.lfc.consumer.data.api

import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.data.model.ActivityParticipantDto
import com.lfc.consumer.data.model.AuthResponse
import com.lfc.consumer.data.model.CreateActivityRequest
import com.lfc.consumer.data.model.CreatePostRequest
import com.lfc.consumer.data.model.LoginRequest
import com.lfc.consumer.data.model.MessageDto
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.data.model.PostFeedResponse
import com.lfc.consumer.data.model.UnreadCountDto
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
    suspend fun getMyPosts(): List<PostDto>

    @GET("consumer/post/{id}")
    suspend fun getPost(@Path("id") id: Int): PostDto

    @POST("consumer/post")
    suspend fun createPost(@Body request: CreatePostRequest): PostDto

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

    @GET("consumer/message")
    suspend fun getMessages(): List<MessageDto>

    @GET("consumer/message/unread-count")
    suspend fun getUnreadCount(): UnreadCountDto

    @GET("consumer/message/{id}")
    suspend fun getMessage(@Path("id") id: Int): MessageDto

    @PATCH("consumer/message/read-all")
    suspend fun markAllMessagesRead()

    @DELETE("consumer/message/{id}")
    suspend fun deleteMessage(@Path("id") id: Int)
}
