package com.lfc.consumer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.net.Uri
import com.lfc.consumer.data.ApiClient
import com.lfc.consumer.data.MediaUploadHelper
import com.lfc.consumer.data.local.SearchHistoryStore
import com.lfc.consumer.data.local.TokenManager
import com.lfc.consumer.data.local.UserSession
import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.data.model.ActivityParticipantDto
import com.lfc.consumer.data.model.CreateActivityRequest
import com.lfc.consumer.data.model.CreatePostCommentRequest
import com.lfc.consumer.data.model.CreatePostRequest
import com.lfc.consumer.data.model.PostCommentDto
import com.lfc.consumer.data.model.FeedUiState
import com.lfc.consumer.data.model.SearchUiState
import com.lfc.consumer.data.model.ChatMessageDto
import com.lfc.consumer.data.model.ConversationDto
import com.lfc.consumer.data.model.CreateConversationRequest
import com.lfc.consumer.data.model.NotificationDto
import com.lfc.consumer.data.model.SendChatMessageRequest
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.data.model.UpdateActivityRequest
import com.lfc.consumer.data.model.UpdatePostRequest
import com.lfc.consumer.data.model.PostSocialStateDto
import com.lfc.consumer.data.model.ProfileCommentDto
import com.lfc.consumer.data.model.UpdateProfileRequest
import com.lfc.consumer.data.model.UserProfileDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import com.google.gson.JsonParser

data class HomeUiState(
    val feed: FeedUiState = FeedUiState(),
    val posts: List<PostDto> = emptyList(),
    val activities: List<ActivityDto> = emptyList(),
    val myPosts: List<PostDto> = emptyList(),
    val myActivities: List<ActivityDto> = emptyList(),
    val myParticipations: List<ActivityParticipantDto> = emptyList(),
    val conversations: List<ConversationDto> = emptyList(),
    val notifications: List<NotificationDto> = emptyList(),
    val unreadCount: Int = 0,
    val selectedNotification: NotificationDto? = null,
    val isNotificationLoading: Boolean = false,
    val selectedConversation: ConversationDto? = null,
    val chatMessages: List<ChatMessageDto> = emptyList(),
    val isChatLoading: Boolean = false,
    val isChatSending: Boolean = false,
    val selectedPost: PostDto? = null,
    val postComments: List<PostCommentDto> = emptyList(),
    val isPostLoading: Boolean = false,
    val isPostCommentsLoading: Boolean = false,
    val isPostSocialSubmitting: Boolean = false,
    val selectedActivity: ActivityDto? = null,
    val isActivityLoading: Boolean = false,
    val isJoiningActivity: Boolean = false,
    val search: SearchUiState = SearchUiState(),
    val searchInput: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val selectedUserProfile: UserProfileDto? = null,
    val isUserProfileLoading: Boolean = false,
    val myProfile: UserProfileDto? = null,
    val profileFavoritePosts: List<PostDto> = emptyList(),
    val profileLikedPosts: List<PostDto> = emptyList(),
    val profileComments: List<ProfileCommentDto> = emptyList(),
    val profileSearchKeyword: String = "",
    val isProfileLibraryLoading: Boolean = false,
    val isProfileUpdating: Boolean = false,
    val isPrivacyUpdating: Boolean = false,
)

class HomeViewModel(
    private val tokenManager: TokenManager,
    private val searchHistoryStore: SearchHistoryStore,
    private val appContext: Context,
) : ViewModel() {
    private val api = ApiClient.createApiService(tokenManager)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val userSession: StateFlow<UserSession?> = tokenManager.userSessionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val searchHistory: StateFlow<List<String>> = searchHistoryStore.historyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        loadFeed(refresh = true)
        refreshProfileData()
    }

    fun refreshAll() {
        loadFeed(refresh = true)
        refreshProfileData()
    }

    private fun refreshProfileData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val profile = api.getMyProfile()
                val activities = api.getApprovedActivities()
                val myParticipations = api.getMyParticipations()
                val notifications = api.getNotifications()
                val unreadCount = loadTotalUnreadCount()
                _uiState.value = _uiState.value.copy(
                    posts = profile.posts,
                    activities = activities,
                    myProfile = profile,
                    myPosts = profile.posts,
                    myActivities = profile.activities,
                    myParticipations = myParticipations,
                    notifications = notifications,
                    unreadCount = unreadCount,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = parseError(e, "加载失败"),
                )
            }
        }
    }

    fun loadFeed(refresh: Boolean = false) {
        viewModelScope.launch {
            val feed = _uiState.value.feed
            val page = if (refresh) 1 else feed.page
            _uiState.value = _uiState.value.copy(
                feed = feed.copy(
                    isRefreshing = refresh,
                    isInitialLoading = refresh && feed.posts.isEmpty(),
                    isLoadingMore = !refresh && feed.hasMore,
                    page = page,
                ),
            )
            try {
                val sort = when (feed.selectedTab) {
                    "最新" -> "latest"
                    else -> "recommend"
                }
                val tabParam = when {
                    feed.selectedTab in listOf("推荐", "最新") -> null
                    else -> feed.selectedTab
                }
                val response = api.getPostFeed(
                    page = page,
                    limit = 10,
                    sort = sort,
                    keyword = null,
                    tab = tabParam,
                )
                val merged = if (refresh) {
                    response.items
                } else {
                    feed.posts + response.items.filter { new ->
                        feed.posts.none { it.id == new.id }
                    }
                }
                _uiState.value = _uiState.value.copy(
                    feed = feed.copy(
                        posts = merged,
                        page = page + 1,
                        hasMore = response.hasMore,
                        isRefreshing = false,
                        isInitialLoading = false,
                        isLoadingMore = false,
                    ),
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    feed = _uiState.value.feed.copy(
                        isRefreshing = false,
                        isInitialLoading = false,
                        isLoadingMore = false,
                    ),
                    error = parseError(e, "加载失败"),
                )
            }
        }
    }

    fun loadMoreFeed() {
        val feed = _uiState.value.feed
        if (!feed.hasMore || feed.isLoadingMore || feed.isRefreshing) return
        loadFeed(refresh = false)
    }

    fun selectFeedTab(tab: String) {
        if (_uiState.value.feed.selectedTab == tab) return
        _uiState.value = _uiState.value.copy(
            feed = FeedUiState(selectedTab = tab),
        )
        loadFeed(refresh = true)
    }

    fun updateSearchInput(input: String) {
        _uiState.value = _uiState.value.copy(searchInput = input)
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            searchHistoryStore.clear()
        }
    }

    fun submitSearch(keyword: String, onNavigate: () -> Unit) {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            searchHistoryStore.add(trimmed)
            _uiState.value = _uiState.value.copy(
                searchInput = trimmed,
                search = SearchUiState(keyword = trimmed, isLoading = true),
            )
            onNavigate()
            loadSearchResults(refresh = true)
        }
    }

    fun loadSearchResults(refresh: Boolean = false) {
        viewModelScope.launch {
            val search = _uiState.value.search
            if (search.keyword.isBlank()) return@launch
            val page = if (refresh) 1 else search.page
            _uiState.value = _uiState.value.copy(
                search = search.copy(
                    isLoading = refresh && search.posts.isEmpty() && search.activities.isEmpty(),
                    isRefreshing = refresh,
                    isLoadingMore = !refresh && search.hasMore,
                    page = page,
                ),
            )
            try {
                if (_uiState.value.activities.isEmpty()) {
                    val activities = api.getApprovedActivities()
                    _uiState.value = _uiState.value.copy(activities = activities)
                }
                val response = api.getPostFeed(
                    page = page,
                    limit = 10,
                    sort = "recommend",
                    keyword = search.keyword,
                    tab = null,
                )
                val filteredActivities = filterActivities(_uiState.value.activities, search.keyword)
                val mergedPosts = if (refresh) {
                    response.items
                } else {
                    search.posts + response.items.filter { new ->
                        search.posts.none { it.id == new.id }
                    }
                }
                _uiState.value = _uiState.value.copy(
                    search = search.copy(
                        posts = mergedPosts,
                        activities = filteredActivities,
                        page = page + 1,
                        hasMore = response.hasMore,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                    ),
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    search = _uiState.value.search.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                    ),
                    error = parseError(e, "搜索失败"),
                )
            }
        }
    }

    fun loadMoreSearchResults() {
        val search = _uiState.value.search
        if (!search.hasMore || search.isLoadingMore || search.isRefreshing || search.selectedTab == "活动") return
        loadSearchResults(refresh = false)
    }

    fun selectSearchTab(tab: String) {
        if (_uiState.value.search.selectedTab == tab) return
        _uiState.value = _uiState.value.copy(
            search = _uiState.value.search.copy(selectedTab = tab),
        )
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            search = SearchUiState(),
            searchInput = "",
        )
    }

    private fun filterActivities(activities: List<ActivityDto>, keyword: String): List<ActivityDto> {
        val lower = keyword.lowercase()
        return activities.filter { activity ->
            activity.title.lowercase().contains(lower) ||
                activity.description.lowercase().contains(lower) ||
                activity.location.lowercase().contains(lower)
        }
    }

    fun createPost(
        title: String,
        content: String,
        imageUris: List<Uri> = emptyList(),
        onSuccess: () -> Unit = {},
        onComplete: () -> Unit = {},
    ) {
        viewModelScope.launch {
            try {
                val imageUrls = imageUris.map { uri -> uploadImage(uri) }
                api.createPost(
                    CreatePostRequest(
                        title = title.ifBlank { null },
                        content = content.ifBlank { null },
                        images = imageUrls.ifEmpty { null },
                    ),
                )
                _uiState.value = _uiState.value.copy(message = "信息发布成功")
                onSuccess()
                refreshAll()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "发布失败"))
            } finally {
                onComplete()
            }
        }
    }

    fun updatePost(
        id: Int,
        title: String,
        content: String,
        imageUris: List<Uri> = emptyList(),
        existingImages: List<String> = emptyList(),
        onSuccess: () -> Unit = {},
        onComplete: () -> Unit = {},
    ) {
        viewModelScope.launch {
            try {
                val newUrls = imageUris.map { uri -> uploadImage(uri) }
                val images = existingImages + newUrls
                api.updatePost(
                    id,
                    UpdatePostRequest(
                        title = title.ifBlank { null },
                        content = content.ifBlank { null },
                        images = images.ifEmpty { null },
                    ),
                )
                _uiState.value = _uiState.value.copy(message = "信息更新成功")
                onSuccess()
                refreshAll()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "更新失败"))
            } finally {
                onComplete()
            }
        }
    }

    private suspend fun uploadImage(uri: Uri, scope: String = "post"): String {
        val uploadFile = MediaUploadHelper.uriToUploadFile(appContext, uri, scope)
        val requestBody = uploadFile.file.asRequestBody(uploadFile.mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", uploadFile.fileName, requestBody)
        return api.uploadImage(part, scope).url
    }

    fun deletePost(id: Int) {
        viewModelScope.launch {
            try {
                api.deletePost(id)
                refreshAll()
                _uiState.value = _uiState.value.copy(message = "信息已删除")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "删除失败"))
            }
        }
    }

    fun createActivity(
        title: String,
        description: String,
        location: String,
        startTime: String,
        endTime: String,
        maxParticipants: Int,
        imageUris: List<Uri> = emptyList(),
        onSuccess: () -> Unit = {},
        onComplete: () -> Unit = {},
    ) {
        viewModelScope.launch {
            try {
                val imageUrls = imageUris.map { uri -> uploadImage(uri, "activity") }
                api.createActivity(
                    CreateActivityRequest(
                        title = title.ifBlank { null },
                        description = description.ifBlank { null },
                        images = imageUrls.ifEmpty { null },
                        location = location,
                        startTime = normalizeDateTime(startTime),
                        endTime = normalizeDateTime(endTime),
                        maxParticipants = maxParticipants,
                    ),
                )
                _uiState.value = _uiState.value.copy(message = "活动已提交，等待审核")
                onSuccess()
                refreshAll()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "发布失败"))
            } finally {
                onComplete()
            }
        }
    }

    fun updateActivity(
        id: Int,
        title: String,
        description: String,
        location: String,
        startTime: String,
        endTime: String,
        maxParticipants: Int,
        imageUris: List<Uri> = emptyList(),
        existingImages: List<String> = emptyList(),
        onSuccess: () -> Unit = {},
        onComplete: () -> Unit = {},
    ) {
        viewModelScope.launch {
            try {
                val newUrls = imageUris.map { uri -> uploadImage(uri, "activity") }
                val images = existingImages + newUrls
                api.updateActivity(
                    id,
                    UpdateActivityRequest(
                        title = title.ifBlank { null },
                        description = description.ifBlank { null },
                        images = images.ifEmpty { null },
                        location = location,
                        startTime = normalizeDateTime(startTime),
                        endTime = normalizeDateTime(endTime),
                        maxParticipants = maxParticipants,
                    ),
                )
                _uiState.value = _uiState.value.copy(message = "活动已更新，等待重新审核")
                onSuccess()
                refreshAll()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "更新失败"))
            } finally {
                onComplete()
            }
        }
    }

    fun deleteActivity(id: Int) {
        viewModelScope.launch {
            try {
                api.deleteActivity(id)
                refreshAll()
                _uiState.value = _uiState.value.copy(message = "活动已删除")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "删除失败"))
            }
        }
    }

    fun joinActivity(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isJoiningActivity = true)
            try {
                api.joinActivity(id)
                refreshAll()
                if (_uiState.value.selectedActivity?.id == id) {
                    val activity = api.getActivity(id)
                    _uiState.value = _uiState.value.copy(selectedActivity = activity)
                }
                _uiState.value = _uiState.value.copy(message = "报名成功")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "报名失败"))
            } finally {
                _uiState.value = _uiState.value.copy(isJoiningActivity = false)
            }
        }
    }

    fun loadPostDetail(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isPostLoading = true,
                isPostCommentsLoading = true,
                selectedPost = null,
                postComments = emptyList(),
            )
            try {
                val post = api.getPost(id)
                val comments = api.getPostComments(id)
                _uiState.value = _uiState.value.copy(
                    selectedPost = post,
                    postComments = comments,
                    isPostLoading = false,
                    isPostCommentsLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isPostLoading = false,
                    isPostCommentsLoading = false,
                    error = parseError(e, "加载笔记失败"),
                )
            }
        }
    }

    fun togglePostLike(postId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPostSocialSubmitting = true)
            try {
                val state = api.togglePostLike(postId)
                updatePostSocialState(postId, state)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "操作失败"))
            } finally {
                _uiState.value = _uiState.value.copy(isPostSocialSubmitting = false)
            }
        }
    }

    fun togglePostFavorite(postId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPostSocialSubmitting = true)
            try {
                val state = api.togglePostFavorite(postId)
                updatePostSocialState(postId, state)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "操作失败"))
            } finally {
                _uiState.value = _uiState.value.copy(isPostSocialSubmitting = false)
            }
        }
    }

    fun submitPostComment(postId: Int, content: String, parentId: Int? = null) {
        if (content.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPostSocialSubmitting = true)
            try {
                val comment = api.createPostComment(
                    postId,
                    CreatePostCommentRequest(content = content.trim(), parentId = parentId),
                )
                val post = _uiState.value.selectedPost
                if (post?.id == postId) {
                    _uiState.value = _uiState.value.copy(
                        postComments = _uiState.value.postComments + comment,
                        selectedPost = post.copy(commentCount = post.commentCount + 1),
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "评论失败"))
            } finally {
                _uiState.value = _uiState.value.copy(isPostSocialSubmitting = false)
            }
        }
    }

    private fun updatePostSocialState(postId: Int, state: PostSocialStateDto) {
        val post = _uiState.value.selectedPost
        if (post?.id == postId) {
            _uiState.value = _uiState.value.copy(
                selectedPost = post.copy(
                    likeCount = state.likeCount,
                    favoriteCount = state.favoriteCount,
                    commentCount = state.commentCount,
                    isLiked = state.isLiked,
                    isFavorited = state.isFavorited,
                ),
            )
        }
        val feed = _uiState.value.feed
        _uiState.value = _uiState.value.copy(
            feed = feed.copy(
                posts = feed.posts.map { item ->
                    if (item.id == postId) {
                        item.copy(
                            likeCount = state.likeCount,
                            favoriteCount = state.favoriteCount,
                            commentCount = state.commentCount,
                            isLiked = state.isLiked,
                            isFavorited = state.isFavorited,
                        )
                    } else {
                        item
                    }
                },
            ),
        )
    }

    fun loadActivityDetail(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActivityLoading = true, selectedActivity = null)
            try {
                val activity = api.getActivity(id)
                _uiState.value = _uiState.value.copy(selectedActivity = activity, isActivityLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isActivityLoading = false,
                    error = parseError(e, "加载活动失败"),
                )
            }
        }
    }

    fun clearSelectedPost() {
        _uiState.value = _uiState.value.copy(
            selectedPost = null,
            postComments = emptyList(),
            isPostLoading = false,
            isPostCommentsLoading = false,
        )
    }

    fun clearSelectedActivity() {
        _uiState.value = _uiState.value.copy(selectedActivity = null, isActivityLoading = false)
    }

    fun loadNotificationDetail(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isNotificationLoading = true, selectedNotification = null)
            try {
                val notification = api.getNotification(id)
                val unreadCount = loadTotalUnreadCount()
                val notifications = _uiState.value.notifications.map {
                    if (it.id == id) notification else it
                }
                _uiState.value = _uiState.value.copy(
                    selectedNotification = notification,
                    notifications = notifications,
                    unreadCount = unreadCount,
                    isNotificationLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isNotificationLoading = false,
                    error = parseError(e, "加载通知失败"),
                )
            }
        }
    }

    fun clearSelectedNotification() {
        _uiState.value = _uiState.value.copy(selectedNotification = null, isNotificationLoading = false)
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            try {
                api.markAllNotificationsRead()
                refreshMessages()
                _uiState.value = _uiState.value.copy(message = "已全部标记为已读")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "操作失败"))
            }
        }
    }

    fun refreshMessages() {
        viewModelScope.launch {
            try {
                val conversations = api.getConversations()
                val notifications = api.getNotifications()
                val unreadCount = loadTotalUnreadCount()
                _uiState.value = _uiState.value.copy(
                    conversations = conversations,
                    notifications = notifications,
                    unreadCount = unreadCount,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "加载消息失败"))
            }
        }
    }

    fun loadChat(conversationId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChatLoading = true, chatMessages = emptyList())
            try {
                val messages = api.getChatMessages(conversationId)
                val conversations = api.getConversations()
                val unreadCount = loadTotalUnreadCount()
                _uiState.value = _uiState.value.copy(
                    chatMessages = messages,
                    conversations = conversations,
                    selectedConversation = conversations.find { it.id == conversationId },
                    unreadCount = unreadCount,
                    isChatLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isChatLoading = false,
                    error = parseError(e, "加载私信失败"),
                )
            }
        }
    }

    fun sendChatMessage(conversationId: Int, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChatSending = true)
            try {
                val message = api.sendChatMessage(
                    conversationId,
                    SendChatMessageRequest(content = content.trim()),
                )
                val conversations = api.getConversations()
                _uiState.value = _uiState.value.copy(
                    chatMessages = _uiState.value.chatMessages + message,
                    conversations = conversations,
                    selectedConversation = conversations.find { it.id == conversationId },
                    isChatSending = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isChatSending = false,
                    error = parseError(e, "发送失败"),
                )
            }
        }
    }

    fun startConversation(peerUserId: Int, onSuccess: (Int) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val conversation = api.createConversation(CreateConversationRequest(peerUserId))
                refreshMessages()
                onSuccess(conversation.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "发起私信失败"))
            }
        }
    }

    fun clearChat() {
        _uiState.value = _uiState.value.copy(
            selectedConversation = null,
            chatMessages = emptyList(),
            isChatLoading = false,
            isChatSending = false,
        )
    }

    fun loadUserProfile(userId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUserProfileLoading = true, selectedUserProfile = null)
            try {
                val profile = api.getUserProfile(userId)
                _uiState.value = _uiState.value.copy(
                    selectedUserProfile = profile,
                    isUserProfileLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUserProfileLoading = false,
                    error = parseError(e, "加载用户主页失败"),
                )
            }
        }
    }

    suspend fun resolveUserIdByLfcNo(lfcNo: String): Int? {
        return try {
            api.getUserProfileByLfcNo(lfcNo).id
        } catch (_: Exception) {
            null
        }
    }

    fun clearUserProfile() {
        _uiState.value = _uiState.value.copy(
            selectedUserProfile = null,
            isUserProfileLoading = false,
            profileComments = emptyList(),
            profileFavoritePosts = emptyList(),
            profileLikedPosts = emptyList(),
        )
    }

    fun loadProfileLibrary(tab: Int, userId: Int? = null) {
        if (tab !in 2..4) return
        val myId = _uiState.value.myProfile?.id
        val targetId = userId ?: myId ?: return
        val isSelf = targetId == myId

        if (!isSelf) {
            val profile = _uiState.value.selectedUserProfile
            if (profile?.id != targetId) return
            val allowed = when (tab) {
                2 -> profile.showCommentsPublic
                3 -> profile.showFavoritesPublic
                4 -> profile.showLikesPublic
                else -> true
            }
            if (!allowed) return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProfileLibraryLoading = true)
            try {
                when (tab) {
                    2 -> {
                        val comments = if (isSelf) {
                            api.getMyProfileComments()
                        } else {
                            api.getUserProfileComments(targetId)
                        }
                        _uiState.value = _uiState.value.copy(profileComments = comments)
                    }
                    3 -> {
                        val favorites = if (isSelf) {
                            api.getMyFavoritePosts()
                        } else {
                            api.getUserFavoritePosts(targetId)
                        }
                        _uiState.value = _uiState.value.copy(profileFavoritePosts = favorites)
                    }
                    4 -> {
                        val likes = if (isSelf) {
                            api.getMyLikedPosts()
                        } else {
                            api.getUserLikedPosts(targetId)
                        }
                        _uiState.value = _uiState.value.copy(profileLikedPosts = likes)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "加载内容失败"))
            } finally {
                _uiState.value = _uiState.value.copy(isProfileLibraryLoading = false)
            }
        }
    }

    fun searchMyPosts(keyword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                profileSearchKeyword = keyword,
                isProfileLibraryLoading = true,
            )
            try {
                val posts = api.getMyPosts(keyword.takeIf { it.isNotBlank() })
                val profile = _uiState.value.myProfile
                _uiState.value = _uiState.value.copy(
                    myPosts = posts,
                    myProfile = profile?.copy(posts = posts, postCount = posts.size),
                    isProfileLibraryLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProfileLibraryLoading = false,
                    error = parseError(e, "搜索失败"),
                )
            }
        }
    }

    fun updateProfile(
        nickname: String,
        bio: String,
        avatarUri: Uri?,
        coverUri: Uri?,
        onSuccess: () -> Unit,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProfileUpdating = true)
            try {
                val avatarUrl = avatarUri?.let { uploadImage(it, "profile") }
                val coverUrl = coverUri?.let { uploadImage(it, "profile") }
                val current = _uiState.value.myProfile
                val updated = api.updateMyProfile(
                    UpdateProfileRequest(
                        nickname = nickname.trim().ifBlank { null },
                        bio = bio.trim().ifBlank { null },
                        avatarUrl = avatarUrl ?: current?.avatarUrl,
                        coverUrl = coverUrl ?: current?.coverUrl,
                    ),
                )
                _uiState.value = _uiState.value.copy(
                    myProfile = updated,
                    myPosts = updated.posts,
                    myActivities = updated.activities,
                    message = "主页已更新",
                )
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "更新主页失败"))
            } finally {
                _uiState.value = _uiState.value.copy(isProfileUpdating = false)
                onComplete()
            }
        }
    }

    fun updatePrivacySettings(
        showCommentsPublic: Boolean,
        showFavoritesPublic: Boolean,
        showLikesPublic: Boolean,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPrivacyUpdating = true)
            try {
                val updated = api.updateMyProfile(
                    UpdateProfileRequest(
                        showCommentsPublic = showCommentsPublic,
                        showFavoritesPublic = showFavoritesPublic,
                        showLikesPublic = showLikesPublic,
                    ),
                )
                _uiState.value = _uiState.value.copy(
                    myProfile = updated,
                    message = "隐私设置已保存",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "保存隐私设置失败"))
            } finally {
                _uiState.value = _uiState.value.copy(isPrivacyUpdating = false)
            }
        }
    }

    fun toggleFollow(userId: Int) {
        viewModelScope.launch {
            try {
                api.toggleFollow(userId)
                val profile = api.getUserProfile(userId)
                _uiState.value = _uiState.value.copy(selectedUserProfile = profile)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "操作失败"))
            }
        }
    }

    private suspend fun loadTotalUnreadCount(): Int {
        val notificationUnread = api.getNotificationUnreadCount().count
        val chatUnread = api.getConversationUnreadCount().count
        return notificationUnread + chatUnread
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    private fun parseError(e: Exception, fallback: String): String {
        if (e is HttpException) {
            val body = e.response()?.errorBody()?.string()
            if (!body.isNullOrBlank()) {
                runCatching {
                    JsonParser.parseString(body).asJsonObject["message"]?.asString
                }.getOrNull()?.let { return it }
            }
        }
        return e.message ?: fallback
    }

    private fun normalizeDateTime(value: String): String {
        val trimmed = value.trim()
        if (trimmed.contains("T")) {
            return trimmed
        }
        return trimmed.replace(" ", "T")
    }
}

class HomeViewModelFactory(
    private val tokenManager: TokenManager,
    private val searchHistoryStore: SearchHistoryStore,
    private val appContext: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(tokenManager, searchHistoryStore, appContext) as T
    }
}
