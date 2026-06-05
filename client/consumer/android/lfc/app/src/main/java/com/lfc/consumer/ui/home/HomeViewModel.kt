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
import com.lfc.consumer.data.model.PostProductRequest
import com.lfc.consumer.data.model.PostCommentDto
import com.lfc.consumer.data.model.FeedUiState
import com.lfc.consumer.data.model.ActivityFeedUiState
import com.lfc.consumer.data.model.SearchUiState
import com.lfc.consumer.data.model.ChatMessageDto
import com.lfc.consumer.data.model.ConversationDto
import com.lfc.consumer.data.model.CreateConversationRequest
import com.lfc.consumer.data.model.NotificationDto
import com.lfc.consumer.data.model.ChatProductPayload
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.data.model.toChatProductPayload
import com.lfc.consumer.data.model.toJson
import com.lfc.consumer.data.model.isPaidActivity
import com.lfc.consumer.data.model.UpdateActivityRequest
import com.lfc.consumer.data.model.UpdatePostRequest
import com.lfc.consumer.data.model.ActivitySocialStateDto
import com.lfc.consumer.data.model.PostSocialStateDto
import com.lfc.consumer.data.model.ProfileCommentDto
import com.lfc.consumer.data.model.ProfileTabUiState
import com.lfc.consumer.data.model.ProfileTabsUiState
import com.lfc.consumer.data.model.UpdateProfileRequest
import com.lfc.consumer.data.model.PaymentConfigDto
import com.lfc.consumer.data.model.PaymentOrderDetailDto
import com.lfc.consumer.data.model.PaymentOrderListItemDto
import com.lfc.consumer.data.model.OrderCenterUiState
import com.lfc.consumer.data.model.PaymentTransactionLedgerUiState
import com.lfc.consumer.data.model.ProfileSearchUiState
import com.lfc.consumer.data.model.CreateOrderReviewRequest
import com.lfc.consumer.data.model.ApplyAfterSalesRequest
import com.lfc.consumer.data.model.BindAlipayAccountRequest
import com.lfc.consumer.data.model.BindAlipayOAuthRequest
import com.lfc.consumer.data.model.SendChatMessageRequest
import com.lfc.consumer.data.model.UserProfileDto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
import com.lfc.consumer.payment.AlipayAuthResult
import com.lfc.consumer.payment.AlipayHelper
import com.lfc.consumer.payment.AlipayPayResult
import com.google.gson.JsonParser

data class HomeUiState(
    val feed: FeedUiState = FeedUiState(),
    val activityFeed: ActivityFeedUiState = ActivityFeedUiState(),
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
    val isActivitySocialSubmitting: Boolean = false,
    val isPurchasingProduct: Boolean = false,
    /** 全局支付进行中（含调起支付宝与轮询确认），用于禁用重复点击 */
    val isPaymentProcessing: Boolean = false,
    val payingActivityId: Int? = null,
    val payingPostId: Int? = null,
    val productPurchaseOrder: PaymentOrderDetailDto? = null,
    val isConfirmingReceipt: Boolean = false,
    val search: SearchUiState = SearchUiState(),
    val searchInput: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val selectedUserProfile: UserProfileDto? = null,
    val isUserProfileLoading: Boolean = false,
    val myProfile: UserProfileDto? = null,
    val profileNotes: List<PostDto> = emptyList(),
    val profileActivities: List<ActivityDto> = emptyList(),
    val profileFavoritePosts: List<PostDto> = emptyList(),
    val profileFavoriteActivities: List<ActivityDto> = emptyList(),
    val profileLikedPosts: List<PostDto> = emptyList(),
    val profileLikedActivities: List<ActivityDto> = emptyList(),
    val profileComments: List<ProfileCommentDto> = emptyList(),
    val profileTabs: ProfileTabsUiState = ProfileTabsUiState(),
    val profileSearch: ProfileSearchUiState = ProfileSearchUiState(),
    val isProfileUpdating: Boolean = false,
    val isPrivacyUpdating: Boolean = false,
    val detailAuthorFollowing: Boolean? = null,
    val paymentConfig: PaymentConfigDto? = null,
    val orderCenter: OrderCenterUiState = OrderCenterUiState(),
    val paymentTransactionLedger: PaymentTransactionLedgerUiState = PaymentTransactionLedgerUiState(),
)

class HomeViewModel(
    private val tokenManager: TokenManager,
    private val searchHistoryStore: SearchHistoryStore,
    private val appContext: Context,
) : ViewModel() {
    private val api = ApiClient.createApiService(tokenManager)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var alipayPayHandler: (suspend (String) -> AlipayPayResult)? = null
    private var alipayAuthHandler: (suspend (String) -> AlipayAuthResult)? = null

    private val paymentGate = Any()
    private var paymentGateKey: String? = null

    private fun tryAcquirePaymentGate(key: String): Boolean = synchronized(paymentGate) {
        if (paymentGateKey != null) {
            false
        } else {
            paymentGateKey = key
            true
        }
    }

    private fun releasePaymentGate(key: String) = synchronized(paymentGate) {
        if (paymentGateKey == key) {
            paymentGateKey = null
        }
    }

    private fun rejectDuplicatePayment() {
        _uiState.value = _uiState.value.copy(
            message = "支付处理中，请勿重复操作",
        )
    }

    private fun beginPaymentUi(
        activityId: Int? = null,
        postId: Int? = null,
        isJoining: Boolean = false,
        isPurchasing: Boolean = false,
    ) {
        _uiState.value = _uiState.value.copy(
            isPaymentProcessing = true,
            payingActivityId = activityId,
            payingPostId = postId,
            isJoiningActivity = isJoining,
            isPurchasingProduct = isPurchasing,
        )
    }

    private fun endPaymentUi(
        activityId: Int? = null,
        postId: Int? = null,
    ) {
        _uiState.value = _uiState.value.copy(
            isPaymentProcessing = false,
            payingActivityId = null,
            payingPostId = null,
            isJoiningActivity = false,
            isPurchasingProduct = false,
        )
    }

    fun setAlipayPayHandler(handler: (suspend (String) -> AlipayPayResult)?) {
        alipayPayHandler = handler
    }

    fun setAlipayAuthHandler(handler: (suspend (String) -> AlipayAuthResult)?) {
        alipayAuthHandler = handler
    }

    val userSession: StateFlow<UserSession?> = tokenManager.userSessionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val searchHistory: StateFlow<List<String>> = searchHistoryStore.historyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        loadPaymentConfig()
        loadFeed(refresh = true)
        loadActivityFeed(refresh = true)
        refreshProfileData()
    }

    fun refreshAll() {
        loadPaymentConfig()
        loadFeed(refresh = true)
        loadActivityFeed(refresh = true)
        refreshProfileData()
    }

    private fun loadPaymentConfig() {
        viewModelScope.launch {
            runCatching { api.getPaymentConfig() }
                .onSuccess { config ->
                    _uiState.value = _uiState.value.copy(paymentConfig = config)
                }
        }
    }

    private fun refreshProfileData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val profile = api.getMyProfile()
                val myParticipations = api.getMyParticipations()
                val notifications = api.getNotifications()
                val unreadCount = loadTotalUnreadCount()
                val myId = profile.id
                _uiState.value = _uiState.value.copy(
                    posts = profile.posts,
                    myProfile = profile,
                    myPosts = profile.posts,
                    myActivities = profile.activities,
                    myParticipations = myParticipations,
                    notifications = notifications,
                    unreadCount = unreadCount,
                    isLoading = false,
                    profileNotes = emptyList(),
                    profileActivities = emptyList(),
                    profileComments = emptyList(),
                    profileFavoritePosts = emptyList(),
                    profileFavoriteActivities = emptyList(),
                    profileLikedPosts = emptyList(),
                    profileLikedActivities = emptyList(),
                    profileTabs = ProfileTabsUiState(targetUserId = myId),
                )
                loadProfileTab(tab = 0, userId = myId)
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

    fun loadActivityFeed(refresh: Boolean = false) {
        val feed = _uiState.value.activityFeed
        if (refresh && feed.isRefreshing) return
        if (!refresh && feed.isLoadingMore) return

        val page = if (refresh) 1 else feed.page

        _uiState.value = _uiState.value.copy(
            activityFeed = feed.copy(
                isRefreshing = refresh,
                isInitialLoading = refresh && feed.activities.isEmpty(),
                isLoadingMore = !refresh && feed.hasMore,
                page = page,
            ),
        )

        viewModelScope.launch {
            try {
                val response = api.getActivityFeed(page = page, limit = ACTIVITY_FEED_PAGE_SIZE)
                val current = _uiState.value.activityFeed
                val joinedIds = _uiState.value.myParticipations.map { it.activityId }.toSet()
                val merged = if (refresh) {
                    response.items
                } else {
                    current.activities + response.items.filter { new ->
                        current.activities.none { it.id == new.id }
                    }
                }.map { activity ->
                    if (activity.isJoined || activity.id in joinedIds) {
                        activity.copy(isJoined = true)
                    } else {
                        activity
                    }
                }
                _uiState.value = _uiState.value.copy(
                    activityFeed = current.copy(
                        activities = merged,
                        page = page + 1,
                        hasMore = response.hasMore,
                        isRefreshing = false,
                        isInitialLoading = false,
                        isLoadingMore = false,
                    ),
                    activities = merged,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    activityFeed = _uiState.value.activityFeed.copy(
                        isRefreshing = false,
                        isInitialLoading = false,
                        isLoadingMore = false,
                    ),
                    error = parseError(e, "加载活动失败"),
                )
            }
        }
    }

    fun loadMoreActivityFeed() {
        val feed = _uiState.value.activityFeed
        if (!feed.hasMore || feed.isLoadingMore || feed.isRefreshing) return
        loadActivityFeed(refresh = false)
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
        product: PostProductRequest? = null,
        onSuccess: () -> Unit = {},
        onComplete: () -> Unit = {},
    ) {
        viewModelScope.launch {
            try {
                val price = product?.price ?: 0.0
                if (price > 0 && _uiState.value.myProfile?.alipayBound != true) {
                    _uiState.value = _uiState.value.copy(error = ALIPAY_BIND_REQUIRED_MESSAGE)
                    onComplete()
                    return@launch
                }
                val imageUrls = imageUris.map { uri -> uploadImage(uri) }
                api.createPost(
                    CreatePostRequest(
                        title = title.ifBlank { null },
                        content = content.ifBlank { null },
                        images = imageUrls.ifEmpty { null },
                        product = product,
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

    private suspend fun uploadVideo(uri: Uri, scope: String = "chat"): String {
        val uploadFile = MediaUploadHelper.uriToUploadFile(appContext, uri, scope)
        val requestBody = uploadFile.file.asRequestBody(uploadFile.mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", uploadFile.fileName, requestBody)
        return api.uploadVideo(part, scope).url
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
        fee: Double = 0.0,
        imageUris: List<Uri> = emptyList(),
        onSuccess: () -> Unit = {},
        onComplete: () -> Unit = {},
    ) {
        viewModelScope.launch {
            try {
                if (fee > 0 && _uiState.value.myProfile?.alipayBound != true) {
                    _uiState.value = _uiState.value.copy(error = ALIPAY_BIND_REQUIRED_MESSAGE)
                    onComplete()
                    return@launch
                }
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
                        fee = if (fee > 0) fee else null,
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
        val gateKey = "activity:$id"
        if (!tryAcquirePaymentGate(gateKey)) {
            rejectDuplicatePayment()
            return
        }
        viewModelScope.launch {
            beginPaymentUi(activityId = id, isJoining = true)
            try {
                val activity = _uiState.value.selectedActivity?.takeIf { it.id == id }
                    ?: api.getActivity(id)
                if (activity.isPaidActivity()) {
                    val order = api.createActivityPaymentOrder(id)
                    if (order.status.equals("PAID", ignoreCase = true)) {
                        refreshAll()
                        if (_uiState.value.selectedActivity?.id == id) {
                            _uiState.value = _uiState.value.copy(
                                selectedActivity = api.getActivity(id),
                            )
                        }
                        _uiState.value = _uiState.value.copy(message = "支付成功，已完成报名")
                        return@launch
                    }
                    val paid = payWithAlipay(order.outTradeNo, order.alipay.orderStr)
                    if (!paid) return@launch
                    refreshAll()
                    if (_uiState.value.selectedActivity?.id == id) {
                        _uiState.value = _uiState.value.copy(
                            selectedActivity = api.getActivity(id),
                        )
                    }
                    _uiState.value = _uiState.value.copy(message = "已向发起人支付，报名成功")
                } else {
                    api.joinActivity(id)
                    refreshAll()
                    if (_uiState.value.selectedActivity?.id == id) {
                        _uiState.value = _uiState.value.copy(
                            selectedActivity = api.getActivity(id),
                        )
                    }
                    _uiState.value = _uiState.value.copy(message = "报名成功")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "报名失败"))
            } finally {
                endPaymentUi(activityId = id)
                releasePaymentGate(gateKey)
            }
        }
    }

    fun purchasePostProduct(postId: Int) {
        val gateKey = "product:$postId"
        if (!tryAcquirePaymentGate(gateKey)) {
            rejectDuplicatePayment()
            return
        }
        viewModelScope.launch {
            beginPaymentUi(postId = postId, isPurchasing = true)
            try {
                val order = api.createPostProductOrder(postId)
                if (order.status.equals("PAID", ignoreCase = true)) {
                    loadProductDetail(postId)
                    refreshAll()
                    _uiState.value = _uiState.value.copy(
                        message = "支付成功，商品已标记为已售",
                    )
                    return@launch
                }
                val paid = payWithAlipay(order.outTradeNo, order.alipay.orderStr)
                if (!paid) return@launch
                loadProductDetail(postId)
                refreshAll()
                _uiState.value = _uiState.value.copy(
                    message = "支付成功，请面交/收货后确认，卖家才会收到款项",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "购买失败"))
            } finally {
                endPaymentUi(postId = postId)
                releasePaymentGate(gateKey)
            }
        }
    }

    private suspend fun payWithAlipay(outTradeNo: String, orderStr: String): Boolean {
        if (orderStr.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "支付宝下单失败，请稍后重试")
            return false
        }
        val handler = alipayPayHandler
        if (handler == null) {
            _uiState.value = _uiState.value.copy(error = "无法调起支付宝，请重试")
            return false
        }
        val result = try {
            handler(orderStr)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = e.message?.takeIf { it.isNotBlank() } ?: "支付宝调起失败",
            )
            return false
        }
        val shouldPoll = result.resultStatus == "9000" || result.resultStatus == "8000"
        val paid = shouldPoll && waitForPayment(outTradeNo)
        if (!paid) {
            val hint = AlipayHelper.resultMessage(result).ifBlank { "支付未完成，可稍后重试" }
            _uiState.value = _uiState.value.copy(message = hint)
        }
        return paid
    }

    private suspend fun waitForPayment(outTradeNo: String): Boolean {
        repeat(20) {
            val order = api.getPaymentOrder(outTradeNo)
            if (isPaymentConfirmedStatus(order.status)) {
                return true
            }
            delay(1500)
        }
        return false
    }

    private fun isPaymentConfirmedStatus(status: String): Boolean {
        return status.equals("PAID", ignoreCase = true) ||
            status.equals("CONFIRMED", ignoreCase = true) ||
            status.equals("SETTLED", ignoreCase = true)
    }

    fun loadProductDetail(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isPostLoading = true,
                selectedPost = null,
                productPurchaseOrder = null,
            )
            try {
                val post = api.getPost(id)
                val purchaseOrder = runCatching { api.getPostProductOrder(id) }.getOrNull()
                _uiState.value = _uiState.value.copy(
                    selectedPost = post,
                    productPurchaseOrder = purchaseOrder,
                    isPostLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isPostLoading = false,
                    error = parseError(e, "加载商品失败"),
                )
            }
        }
    }

    fun confirmProductReceipt(outTradeNo: String, postId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConfirmingReceipt = true)
            try {
                val order = api.confirmPaymentReceipt(outTradeNo)
                loadProductDetail(postId)
                refreshAll()
                _uiState.value = _uiState.value.copy(
                    message = if (order.status.equals("SETTLED", ignoreCase = true)) {
                        "已确认收货，款项已分账给卖家"
                    } else {
                        "已确认收货，分账处理中"
                    },
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "确认收货失败"))
            } finally {
                _uiState.value = _uiState.value.copy(isConfirmingReceipt = false)
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
                detailAuthorFollowing = null,
            )
            try {
                val post = api.getPost(id)
                val comments = api.getPostComments(id)
                val authorFollowing = loadAuthorFollowState(post.authorId)
                _uiState.value = _uiState.value.copy(
                    selectedPost = post,
                    postComments = comments,
                    detailAuthorFollowing = authorFollowing,
                    isPostLoading = false,
                    isPostCommentsLoading = false,
                )
                updatePostViewCount(post.id, post.viewCount)
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

    private fun updatePostViewCount(postId: Int, viewCount: Int) {
        fun mapPost(item: PostDto) = if (item.id == postId) item.copy(viewCount = viewCount) else item
        val current = _uiState.value
        _uiState.value = current.copy(
            feed = current.feed.copy(posts = current.feed.posts.map(::mapPost)),
            search = current.search.copy(posts = current.search.posts.map(::mapPost)),
            profileNotes = current.profileNotes.map(::mapPost),
            myPosts = current.myPosts.map(::mapPost),
            profileFavoritePosts = current.profileFavoritePosts.map(::mapPost),
            profileLikedPosts = current.profileLikedPosts.map(::mapPost),
        )
    }

    private fun updatePostSocialState(postId: Int, state: PostSocialStateDto) {
        val current = _uiState.value
        val post = current.selectedPost?.takeIf { it.id == postId }
            ?: current.feed.posts.find { it.id == postId }
            ?: current.search.posts.find { it.id == postId }
            ?: current.profileNotes.find { it.id == postId }
            ?: current.profileFavoritePosts.find { it.id == postId }
            ?: current.profileLikedPosts.find { it.id == postId }
            ?: current.myPosts.find { it.id == postId }

        if (current.selectedPost?.id == postId) {
            _uiState.value = _uiState.value.copy(
                selectedPost = current.selectedPost.copy(
                    likeCount = state.likeCount,
                    favoriteCount = state.favoriteCount,
                    commentCount = state.commentCount,
                    isLiked = state.isLiked,
                    isFavorited = state.isFavorited,
                ),
            )
        }

        fun PostDto.withSocialState() = copy(
            likeCount = state.likeCount,
            favoriteCount = state.favoriteCount,
            commentCount = state.commentCount,
            isLiked = state.isLiked,
            isFavorited = state.isFavorited,
        )

        fun mapPost(item: PostDto) = if (item.id == postId) item.withSocialState() else item

        val updatedPost = post?.withSocialState()
        _uiState.value = _uiState.value.copy(
            feed = current.feed.copy(posts = current.feed.posts.map(::mapPost)),
            search = current.search.copy(posts = current.search.posts.map(::mapPost)),
            profileNotes = current.profileNotes.map(::mapPost),
            myPosts = current.myPosts.map(::mapPost),
            profileFavoritePosts = if (updatedPost != null) {
                syncPostLibraryList(current.profileFavoritePosts, updatedPost, state.isFavorited)
            } else {
                current.profileFavoritePosts.map(::mapPost)
            },
            profileLikedPosts = if (updatedPost != null) {
                syncPostLibraryList(current.profileLikedPosts, updatedPost, state.isLiked)
            } else {
                current.profileLikedPosts.map(::mapPost)
            },
        )
    }

    private fun librarySortKey(savedAt: String?, createdAt: String): String = savedAt ?: createdAt

    private fun syncPostLibraryList(
        list: List<PostDto>,
        post: PostDto,
        include: Boolean,
    ): List<PostDto> = when {
        include -> {
            val next = post.copy(savedAt = post.savedAt ?: java.time.Instant.now().toString())
            val merged = if (list.any { it.id == post.id }) {
                list.map { if (it.id == post.id) next else it }
            } else {
                listOf(next) + list
            }
            merged.sortedByDescending { librarySortKey(it.savedAt, it.createdAt) }
        }
        else -> list.filter { it.id != post.id }
    }

    fun loadActivityDetail(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isActivityLoading = true,
                selectedActivity = null,
                detailAuthorFollowing = null,
            )
            try {
                val activity = api.getActivity(id)
                val authorFollowing = loadAuthorFollowState(activity.authorId)
                _uiState.value = _uiState.value.copy(
                    selectedActivity = activity,
                    detailAuthorFollowing = authorFollowing,
                    isActivityLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isActivityLoading = false,
                    error = parseError(e, "加载活动失败"),
                )
            }
        }
    }

    fun toggleActivityLike(activityId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActivitySocialSubmitting = true)
            try {
                val state = api.toggleActivityLike(activityId)
                updateActivitySocialState(activityId, state)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "操作失败"))
            } finally {
                _uiState.value = _uiState.value.copy(isActivitySocialSubmitting = false)
            }
        }
    }

    fun toggleActivityFavorite(activityId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActivitySocialSubmitting = true)
            try {
                val state = api.toggleActivityFavorite(activityId)
                updateActivitySocialState(activityId, state)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "操作失败"))
            } finally {
                _uiState.value = _uiState.value.copy(isActivitySocialSubmitting = false)
            }
        }
    }

    private fun updateActivitySocialState(activityId: Int, state: ActivitySocialStateDto) {
        val current = _uiState.value
        val activity = current.selectedActivity?.takeIf { it.id == activityId }
            ?: current.activities.find { it.id == activityId }
            ?: current.activityFeed.activities.find { it.id == activityId }
            ?: current.search.activities.find { it.id == activityId }
            ?: current.profileActivities.find { it.id == activityId }
            ?: current.profileFavoriteActivities.find { it.id == activityId }
            ?: current.profileLikedActivities.find { it.id == activityId }
            ?: current.myActivities.find { it.id == activityId }

        if (current.selectedActivity?.id == activityId) {
            _uiState.value = _uiState.value.copy(
                selectedActivity = current.selectedActivity.copy(
                    likeCount = state.likeCount,
                    favoriteCount = state.favoriteCount,
                    isLiked = state.isLiked,
                    isFavorited = state.isFavorited,
                ),
            )
        }

        fun ActivityDto.withSocialState() = copy(
            likeCount = state.likeCount,
            favoriteCount = state.favoriteCount,
            isLiked = state.isLiked,
            isFavorited = state.isFavorited,
        )

        fun mapActivity(item: ActivityDto) = if (item.id == activityId) item.withSocialState() else item

        val updatedActivity = activity?.withSocialState()
        _uiState.value = _uiState.value.copy(
            activities = current.activities.map(::mapActivity),
            myActivities = current.myActivities.map(::mapActivity),
            activityFeed = current.activityFeed.copy(
                activities = current.activityFeed.activities.map(::mapActivity),
            ),
            search = current.search.copy(
                activities = current.search.activities.map(::mapActivity),
            ),
            profileActivities = current.profileActivities.map(::mapActivity),
            profileFavoriteActivities = if (updatedActivity != null) {
                syncActivityLibraryList(current.profileFavoriteActivities, updatedActivity, state.isFavorited)
            } else {
                current.profileFavoriteActivities.map(::mapActivity)
            },
            profileLikedActivities = if (updatedActivity != null) {
                syncActivityLibraryList(current.profileLikedActivities, updatedActivity, state.isLiked)
            } else {
                current.profileLikedActivities.map(::mapActivity)
            },
        )
    }

    private fun syncActivityLibraryList(
        list: List<ActivityDto>,
        activity: ActivityDto,
        include: Boolean,
    ): List<ActivityDto> = when {
        include -> {
            val next = activity.copy(savedAt = activity.savedAt ?: java.time.Instant.now().toString())
            val merged = if (list.any { it.id == activity.id }) {
                list.map { if (it.id == activity.id) next else it }
            } else {
                listOf(next) + list
            }
            merged.sortedByDescending { librarySortKey(it.savedAt, it.createdAt) }
        }
        else -> list.filter { it.id != activity.id }
    }

    fun clearSelectedPost() {
        _uiState.value = _uiState.value.copy(
            selectedPost = null,
            postComments = emptyList(),
            detailAuthorFollowing = null,
            isPostLoading = false,
            isPostCommentsLoading = false,
        )
    }

    fun clearSelectedActivity() {
        _uiState.value = _uiState.value.copy(
            selectedActivity = null,
            detailAuthorFollowing = null,
            isActivityLoading = false,
        )
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
            sendChatPayloadInternal(conversationId, content.trim(), "TEXT")
        }
    }

    fun sendChatMedia(conversationId: Int, uri: Uri, messageType: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChatSending = true)
            try {
                val type = messageType.uppercase()
                val url = when (type) {
                    "VIDEO" -> uploadVideo(uri, "chat")
                    else -> uploadImage(uri, "chat")
                }
                sendChatPayloadInternal(conversationId, url, type)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isChatSending = false,
                    error = parseError(e, "发送失败"),
                )
            }
        }
    }

    private suspend fun sendChatPayloadInternal(
        conversationId: Int,
        content: String,
        messageType: String,
    ) {
        if (content.isBlank()) return
        _uiState.value = _uiState.value.copy(isChatSending = true)
        try {
            val message = api.sendChatMessage(
                conversationId,
                SendChatMessageRequest(content = content, messageType = messageType),
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

    fun startConversationWithProduct(
        peerUserId: Int,
        product: ChatProductPayload,
        onSuccess: (Int) -> Unit = {},
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChatSending = true)
            try {
                val conversation = api.createConversation(CreateConversationRequest(peerUserId))
                sendChatPayloadInternal(conversation.id, product.toJson(), "PRODUCT")
                refreshMessages()
                onSuccess(conversation.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isChatSending = false,
                    error = parseError(e, "发起私信失败"),
                )
            }
        }
    }

    fun startConversationWithProductFromPost(
        post: PostDto,
        onSuccess: (Int) -> Unit = {},
    ) {
        val payload = post.toChatProductPayload() ?: return
        startConversationWithProduct(post.authorId, payload, onSuccess)
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
            _uiState.value = _uiState.value.copy(
                isUserProfileLoading = true,
                selectedUserProfile = null,
                profileNotes = emptyList(),
                profileActivities = emptyList(),
                profileComments = emptyList(),
                profileFavoritePosts = emptyList(),
                profileFavoriteActivities = emptyList(),
                profileLikedPosts = emptyList(),
                profileLikedActivities = emptyList(),
                profileTabs = ProfileTabsUiState(targetUserId = userId),
            )
            try {
                val profile = api.getUserProfile(userId)
                _uiState.value = _uiState.value.copy(
                    selectedUserProfile = profile,
                    isUserProfileLoading = false,
                )
                loadProfileTab(tab = 0, userId = userId)
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
            profileNotes = emptyList(),
            profileActivities = emptyList(),
            profileComments = emptyList(),
            profileFavoritePosts = emptyList(),
            profileFavoriteActivities = emptyList(),
            profileLikedPosts = emptyList(),
            profileLikedActivities = emptyList(),
            profileTabs = ProfileTabsUiState(),
        )
    }

    fun loadProfileTab(
        tab: Int,
        userId: Int? = null,
        refresh: Boolean = false,
        loadMore: Boolean = false,
    ) {
        if (tab !in 0..PROFILE_LIKES_TAB) return
        if (tab == PROFILE_FAVORITES_TAB || tab == PROFILE_LIKES_TAB) {
            loadProfileLibraryTab(tab, userId, refresh, loadMore)
            return
        }

        val myId = _uiState.value.myProfile?.id
        val targetId = userId ?: myId ?: return
        val isSelf = targetId == myId

        if (!isSelf && tab == 2) {
            val profile = _uiState.value.selectedUserProfile
            if (profile?.id != targetId) return
            if (!profile.showCommentsPublic) return
        }

        val state = _uiState.value
        val tabsState = if (state.profileTabs.targetUserId == targetId) {
            state.profileTabs
        } else {
            ProfileTabsUiState(targetUserId = targetId)
        }
        val tabState = tabsState.tabs[tab]

        if (loadMore) {
            if (!tabState.hasMore || tabState.isLoadingMore || tabState.isRefreshing) return
        } else if (!refresh) {
            val hasData = when (tab) {
                0 -> state.profileNotes.isNotEmpty()
                1 -> state.profileActivities.isNotEmpty()
                2 -> state.profileComments.isNotEmpty()
                else -> false
            }
            if (hasData || tabState.isInitialLoading || tabState.isRefreshing) return
        }

        val page = when {
            refresh -> 1
            loadMore -> tabState.page
            else -> 1
        }
        _uiState.value = state.copy(
            profileTabs = tabsState.withTab(
                tab,
                tabState.copy(
                    isRefreshing = refresh,
                    isInitialLoading = !refresh && !loadMore,
                    isLoadingMore = loadMore,
                ),
            ),
        )

        viewModelScope.launch {
            try {
                val replace = !loadMore
                when (tab) {
                    0 -> {
                        val response = api.getUserPosts(targetId, page, PROFILE_TAB_PAGE_SIZE)
                        applyProfileTabResult(tab, page, response.hasMore, replace) { current ->
                            current.copy(
                                profileNotes = mergePosts(current.profileNotes, response.items, replace),
                            )
                        }
                    }
                    1 -> {
                        val response = api.getUserActivities(targetId, page, PROFILE_TAB_PAGE_SIZE)
                        applyProfileTabResult(tab, page, response.hasMore, replace) { current ->
                            current.copy(
                                profileActivities = mergeActivities(
                                    current.profileActivities,
                                    response.items,
                                    replace,
                                ),
                            )
                        }
                    }
                    2 -> {
                        val response = if (isSelf) {
                            api.getMyProfileComments(page, PROFILE_TAB_PAGE_SIZE)
                        } else {
                            api.getUserProfileComments(targetId, page, PROFILE_TAB_PAGE_SIZE)
                        }
                        applyProfileTabResult(tab, page, response.hasMore, replace) { current ->
                            current.copy(
                                profileComments = mergeComments(
                                    current.profileComments,
                                    response.items,
                                    replace,
                                ),
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                val current = _uiState.value
                val currentTabState = current.profileTabs.tabs[tab]
                _uiState.value = current.copy(
                    profileTabs = current.profileTabs.withTab(
                        tab,
                        currentTabState.copy(
                            isRefreshing = false,
                            isInitialLoading = false,
                            isLoadingMore = false,
                        ),
                    ),
                    error = parseError(e, "加载内容失败"),
                )
            }
        }
    }

    private enum class ProfileLibrarySource {
        FAVORITE_POSTS,
        FAVORITE_ACTIVITIES,
        LIKED_POSTS,
        LIKED_ACTIVITIES,
    }

    private fun loadProfileLibraryTab(
        tab: Int,
        userId: Int? = null,
        refresh: Boolean = false,
        loadMore: Boolean = false,
    ) {
        val myId = _uiState.value.myProfile?.id
        val targetId = userId ?: myId ?: return
        val isSelf = targetId == myId
        val profile = if (isSelf) _uiState.value.myProfile else _uiState.value.selectedUserProfile
        val isFavoritesTab = tab == PROFILE_FAVORITES_TAB
        val canLoad = when {
            isSelf -> true
            isFavoritesTab -> profile?.showFavoritesPublic == true
            else -> profile?.showLikesPublic == true
        }
        if (!canLoad) return

        val state = _uiState.value
        val tabsState = if (state.profileTabs.targetUserId == targetId) {
            state.profileTabs
        } else {
            ProfileTabsUiState(targetUserId = targetId)
        }
        val postsState = tabsState.tabs[tab]
        val activitiesState = if (isFavoritesTab) {
            tabsState.favoriteActivities
        } else {
            tabsState.likedActivities
        }

        if (loadMore) {
            val loading = postsState.isLoadingMore || activitiesState.isLoadingMore ||
                postsState.isRefreshing || activitiesState.isRefreshing
            if (loading) return

            viewModelScope.launch {
                try {
                    when {
                        postsState.hasMore -> loadProfileLibrarySource(
                            targetId = targetId,
                            isSelf = isSelf,
                            source = if (isFavoritesTab) {
                                ProfileLibrarySource.FAVORITE_POSTS
                            } else {
                                ProfileLibrarySource.LIKED_POSTS
                            },
                            page = postsState.page,
                            replace = false,
                        )
                        activitiesState.hasMore -> loadProfileLibrarySource(
                            targetId = targetId,
                            isSelf = isSelf,
                            source = if (isFavoritesTab) {
                                ProfileLibrarySource.FAVORITE_ACTIVITIES
                            } else {
                                ProfileLibrarySource.LIKED_ACTIVITIES
                            },
                            page = activitiesState.page,
                            replace = false,
                        )
                    }
                } catch (e: Exception) {
                    resetProfileLibraryLoading(tab)
                    _uiState.value = _uiState.value.copy(
                        error = parseError(e, "加载内容失败"),
                    )
                }
            }
            return
        }

        if (!refresh && !loadMore) {
            val busy = postsState.isInitialLoading || postsState.isRefreshing ||
                postsState.isLoadingMore || activitiesState.isInitialLoading ||
                activitiesState.isRefreshing || activitiesState.isLoadingMore
            if (busy) return
            if (postsState.hasLoadedOnce && activitiesState.hasLoadedOnce) return
        }

        _uiState.value = state.copy(
            profileTabs = tabsState
                .withTab(
                    tab,
                    postsState.copy(
                        isRefreshing = refresh,
                        isInitialLoading = !refresh,
                        isLoadingMore = false,
                    ),
                )
                .let { updated ->
                    if (isFavoritesTab) {
                        updated.withFavoriteActivities(
                            activitiesState.copy(
                                isRefreshing = refresh,
                                isInitialLoading = !refresh,
                                isLoadingMore = false,
                            ),
                        )
                    } else {
                        updated.withLikedActivities(
                            activitiesState.copy(
                                isRefreshing = refresh,
                                isInitialLoading = !refresh,
                                isLoadingMore = false,
                            ),
                        )
                    }
                },
        )

        viewModelScope.launch {
            try {
                coroutineScope {
                    val loadPosts = refresh || !postsState.hasLoadedOnce
                    val loadActivities = refresh || !activitiesState.hasLoadedOnce
                    awaitAll(
                        *(buildList {
                            if (loadPosts) {
                                add(
                                    async {
                                        loadProfileLibrarySource(
                                            targetId = targetId,
                                            isSelf = isSelf,
                                            source = if (isFavoritesTab) {
                                                ProfileLibrarySource.FAVORITE_POSTS
                                            } else {
                                                ProfileLibrarySource.LIKED_POSTS
                                            },
                                            page = 1,
                                            replace = true,
                                        )
                                    },
                                )
                            }
                            if (loadActivities) {
                                add(
                                    async {
                                        loadProfileLibrarySource(
                                            targetId = targetId,
                                            isSelf = isSelf,
                                            source = if (isFavoritesTab) {
                                                ProfileLibrarySource.FAVORITE_ACTIVITIES
                                            } else {
                                                ProfileLibrarySource.LIKED_ACTIVITIES
                                            },
                                            page = 1,
                                            replace = true,
                                        )
                                    },
                                )
                            }
                        }.toTypedArray()),
                    )
                }
            } catch (e: Exception) {
                resetProfileLibraryLoading(tab)
                _uiState.value = _uiState.value.copy(
                    error = parseError(e, "加载内容失败"),
                )
            }
        }
    }

    private fun resetProfileLibraryLoading(tab: Int) {
        val current = _uiState.value
        val cleared = ProfileTabUiState(
            page = current.profileTabs.tabs[tab].page,
            hasMore = current.profileTabs.tabs[tab].hasMore,
        )
        val clearedActivities = ProfileTabUiState(
            page = if (tab == PROFILE_FAVORITES_TAB) {
                current.profileTabs.favoriteActivities.page
            } else {
                current.profileTabs.likedActivities.page
            },
            hasMore = if (tab == PROFILE_FAVORITES_TAB) {
                current.profileTabs.favoriteActivities.hasMore
            } else {
                current.profileTabs.likedActivities.hasMore
            },
        )
        _uiState.value = current.copy(
            profileTabs = current.profileTabs
                .withTab(tab, cleared)
                .let { updated ->
                    if (tab == PROFILE_FAVORITES_TAB) {
                        updated.withFavoriteActivities(clearedActivities)
                    } else {
                        updated.withLikedActivities(clearedActivities)
                    }
                },
        )
    }

    private suspend fun loadProfileLibrarySource(
        targetId: Int,
        isSelf: Boolean,
        source: ProfileLibrarySource,
        page: Int,
        replace: Boolean,
    ) {
        val state = _uiState.value
        val tabsState = state.profileTabs
        if (!replace) {
            _uiState.value = when (source) {
                ProfileLibrarySource.FAVORITE_POSTS -> state.copy(
                    profileTabs = tabsState.withTab(
                        PROFILE_FAVORITES_TAB,
                        tabsState.tabs[PROFILE_FAVORITES_TAB].copy(isLoadingMore = true),
                    ),
                )
                ProfileLibrarySource.LIKED_POSTS -> state.copy(
                    profileTabs = tabsState.withTab(
                        PROFILE_LIKES_TAB,
                        tabsState.tabs[PROFILE_LIKES_TAB].copy(isLoadingMore = true),
                    ),
                )
                ProfileLibrarySource.FAVORITE_ACTIVITIES -> state.copy(
                    profileTabs = tabsState.withFavoriteActivities(
                        tabsState.favoriteActivities.copy(isLoadingMore = true),
                    ),
                )
                ProfileLibrarySource.LIKED_ACTIVITIES -> state.copy(
                    profileTabs = tabsState.withLikedActivities(
                        tabsState.likedActivities.copy(isLoadingMore = true),
                    ),
                )
            }
        }

        when (source) {
            ProfileLibrarySource.FAVORITE_POSTS -> {
                val response = if (isSelf) {
                    api.getMyFavoritePosts(page, PROFILE_TAB_PAGE_SIZE)
                } else {
                    api.getUserFavoritePosts(targetId, page, PROFILE_TAB_PAGE_SIZE)
                }
                applyProfileLibraryResult(
                    postsTab = PROFILE_FAVORITES_TAB,
                    page = page,
                    hasMore = response.hasMore,
                    replace = replace,
                ) { current ->
                    current.copy(
                        profileFavoritePosts = mergePosts(
                            current.profileFavoritePosts,
                            response.items,
                            replace,
                        ),
                    )
                }
            }
            ProfileLibrarySource.LIKED_POSTS -> {
                val response = if (isSelf) {
                    api.getMyLikedPosts(page, PROFILE_TAB_PAGE_SIZE)
                } else {
                    api.getUserLikedPosts(targetId, page, PROFILE_TAB_PAGE_SIZE)
                }
                applyProfileLibraryResult(
                    postsTab = PROFILE_LIKES_TAB,
                    page = page,
                    hasMore = response.hasMore,
                    replace = replace,
                ) { current ->
                    current.copy(
                        profileLikedPosts = mergePosts(
                            current.profileLikedPosts,
                            response.items,
                            replace,
                        ),
                    )
                }
            }
            ProfileLibrarySource.FAVORITE_ACTIVITIES -> {
                val response = if (isSelf) {
                    api.getMyFavoriteActivities(page, PROFILE_TAB_PAGE_SIZE)
                } else {
                    api.getUserFavoriteActivities(targetId, page, PROFILE_TAB_PAGE_SIZE)
                }
                applyProfileLibraryActivitiesResult(
                    isFavorites = true,
                    page = page,
                    hasMore = response.hasMore,
                    replace = replace,
                ) { current ->
                    current.copy(
                        profileFavoriteActivities = mergeActivities(
                            current.profileFavoriteActivities,
                            response.items,
                            replace,
                        ),
                    )
                }
            }
            ProfileLibrarySource.LIKED_ACTIVITIES -> {
                val response = if (isSelf) {
                    api.getMyLikedActivities(page, PROFILE_TAB_PAGE_SIZE)
                } else {
                    api.getUserLikedActivities(targetId, page, PROFILE_TAB_PAGE_SIZE)
                }
                applyProfileLibraryActivitiesResult(
                    isFavorites = false,
                    page = page,
                    hasMore = response.hasMore,
                    replace = replace,
                ) { current ->
                    current.copy(
                        profileLikedActivities = mergeActivities(
                            current.profileLikedActivities,
                            response.items,
                            replace,
                        ),
                    )
                }
            }
        }
    }

    private fun applyProfileLibraryResult(
        postsTab: Int,
        page: Int,
        hasMore: Boolean,
        @Suppress("UNUSED_PARAMETER") replace: Boolean,
        update: (HomeUiState) -> HomeUiState,
    ) {
        val current = _uiState.value
        val currentTabState = current.profileTabs.tabs[postsTab]
        _uiState.value = update(current).copy(
            profileTabs = current.profileTabs.withTab(
                postsTab,
                currentTabState.copy(
                    page = page + 1,
                    hasMore = hasMore,
                    isRefreshing = false,
                    isInitialLoading = false,
                    isLoadingMore = false,
                    hasLoadedOnce = true,
                ),
            ),
        )
    }

    private fun applyProfileLibraryActivitiesResult(
        isFavorites: Boolean,
        page: Int,
        hasMore: Boolean,
        @Suppress("UNUSED_PARAMETER") replace: Boolean,
        update: (HomeUiState) -> HomeUiState,
    ) {
        val current = _uiState.value
        val currentTabState = if (isFavorites) {
            current.profileTabs.favoriteActivities
        } else {
            current.profileTabs.likedActivities
        }
        val nextState = currentTabState.copy(
            page = page + 1,
            hasMore = hasMore,
            isRefreshing = false,
            isInitialLoading = false,
            isLoadingMore = false,
            hasLoadedOnce = true,
        )
        _uiState.value = update(current).copy(
            profileTabs = if (isFavorites) {
                current.profileTabs.withFavoriteActivities(nextState)
            } else {
                current.profileTabs.withLikedActivities(nextState)
            },
        )
    }

    fun refreshProfileTab(tab: Int, userId: Int? = null) {
        loadProfileTab(tab, userId, refresh = true)
    }

    fun loadMoreProfileTab(tab: Int, userId: Int? = null) {
        loadProfileTab(tab, userId, loadMore = true)
    }

    private fun applyProfileTabResult(
        tab: Int,
        page: Int,
        hasMore: Boolean,
        @Suppress("UNUSED_PARAMETER") replace: Boolean,
        update: (HomeUiState) -> HomeUiState,
    ) {
        val current = _uiState.value
        val currentTabState = current.profileTabs.tabs[tab]
        _uiState.value = update(current).copy(
            profileTabs = current.profileTabs.withTab(
                tab,
                currentTabState.copy(
                    page = page + 1,
                    hasMore = hasMore,
                    isRefreshing = false,
                    isInitialLoading = false,
                    isLoadingMore = false,
                    hasLoadedOnce = true,
                ),
            ),
        )
    }

    private fun mergePosts(
        existing: List<PostDto>,
        incoming: List<PostDto>,
        replace: Boolean,
    ): List<PostDto> {
        if (replace) return incoming
        return existing + incoming.filter { new -> existing.none { it.id == new.id } }
    }

    private fun mergeActivities(
        existing: List<ActivityDto>,
        incoming: List<ActivityDto>,
        replace: Boolean,
    ): List<ActivityDto> {
        if (replace) return incoming
        return existing + incoming.filter { new -> existing.none { it.id == new.id } }
    }

    private fun mergeComments(
        existing: List<ProfileCommentDto>,
        incoming: List<ProfileCommentDto>,
        replace: Boolean,
    ): List<ProfileCommentDto> {
        if (replace) return incoming
        return existing + incoming.filter { new -> existing.none { it.id == new.id } }
    }

    private fun ProfileTabsUiState.withTab(tab: Int, tabState: ProfileTabUiState): ProfileTabsUiState {
        val updated = tabs.toMutableList()
        updated[tab] = tabState
        return copy(tabs = updated)
    }

    private fun ProfileTabsUiState.withFavoriteActivities(
        tabState: ProfileTabUiState,
    ): ProfileTabsUiState = copy(favoriteActivities = tabState)

    private fun ProfileTabsUiState.withLikedActivities(
        tabState: ProfileTabUiState,
    ): ProfileTabsUiState = copy(likedActivities = tabState)

    fun refreshOrderCenter() {
        loadOrderTabCounts()
        loadOrders(refresh = true)
    }

    fun refreshPaymentTransactionLedger() {
        loadPaymentTransactions(refresh = true)
    }

    fun loadMorePaymentTransactions() {
        val ledger = _uiState.value.paymentTransactionLedger
        if (!ledger.hasMore || ledger.isLoadingMore || ledger.isRefreshing) return
        loadPaymentTransactions(refresh = false)
    }

    private fun loadPaymentTransactions(refresh: Boolean = false) {
        val ledger = _uiState.value.paymentTransactionLedger
        if (refresh && ledger.isRefreshing) return
        if (!refresh && ledger.isLoadingMore) return

        val page = if (refresh) 1 else ledger.page
        _uiState.value = _uiState.value.copy(
            paymentTransactionLedger = ledger.copy(
                isRefreshing = refresh,
                isInitialLoading = refresh && ledger.items.isEmpty(),
                isLoadingMore = !refresh && ledger.hasMore,
                page = page,
            ),
        )

        viewModelScope.launch {
            try {
                val response = api.getPaymentTransactions(
                    page = page,
                    limit = TRANSACTION_PAGE_SIZE,
                )
                val current = _uiState.value.paymentTransactionLedger
                val merged = if (refresh) {
                    response.items
                } else {
                    current.items + response.items.filter { new ->
                        current.items.none { it.txKey == new.txKey }
                    }
                }
                _uiState.value = _uiState.value.copy(
                    paymentTransactionLedger = current.copy(
                        items = merged,
                        page = page + 1,
                        hasMore = response.hasMore,
                        isRefreshing = false,
                        isInitialLoading = false,
                        isLoadingMore = false,
                    ),
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    paymentTransactionLedger = _uiState.value.paymentTransactionLedger.copy(
                        isRefreshing = false,
                        isInitialLoading = false,
                        isLoadingMore = false,
                    ),
                    error = parseError(e, "加载流水失败"),
                )
            }
        }
    }

    fun selectOrderTab(tab: String) {
        if (_uiState.value.orderCenter.selectedTab == tab) return
        _uiState.value = _uiState.value.copy(
            orderCenter = OrderCenterUiState(selectedTab = tab),
        )
        loadOrders(refresh = true)
    }

    private fun loadOrderTabCounts() {
        viewModelScope.launch {
            runCatching { api.getPaymentOrderTabCounts() }
                .onSuccess { counts ->
                    _uiState.value = _uiState.value.copy(
                        orderCenter = _uiState.value.orderCenter.copy(tabCounts = counts),
                    )
                }
        }
    }

    fun loadOrders(refresh: Boolean = false) {
        val center = _uiState.value.orderCenter
        if (refresh && center.isRefreshing) return
        if (!refresh && center.isLoadingMore) return

        val page = if (refresh) 1 else center.page
        _uiState.value = _uiState.value.copy(
            orderCenter = center.copy(
                isRefreshing = refresh,
                isInitialLoading = refresh && center.orders.isEmpty(),
                isLoadingMore = !refresh && center.hasMore,
                page = page,
            ),
        )

        viewModelScope.launch {
            try {
                if (refresh) {
                    loadOrderTabCounts()
                }
                val response = api.getPaymentOrders(
                    tab = _uiState.value.orderCenter.selectedTab,
                    page = page,
                    limit = ORDER_PAGE_SIZE,
                )
                val current = _uiState.value.orderCenter
                val merged = if (refresh) {
                    response.items
                } else {
                    current.orders + response.items.filter { new ->
                        current.orders.none { it.outTradeNo == new.outTradeNo }
                    }
                }
                _uiState.value = _uiState.value.copy(
                    orderCenter = current.copy(
                        orders = merged,
                        page = page + 1,
                        hasMore = response.hasMore,
                        isRefreshing = false,
                        isInitialLoading = false,
                        isLoadingMore = false,
                    ),
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    orderCenter = _uiState.value.orderCenter.copy(
                        isRefreshing = false,
                        isInitialLoading = false,
                        isLoadingMore = false,
                    ),
                    error = parseError(e, "加载订单失败"),
                )
            }
        }
    }

    fun loadMoreOrders() {
        val center = _uiState.value.orderCenter
        if (!center.hasMore || center.isLoadingMore || center.isRefreshing) return
        loadOrders(refresh = false)
    }

    fun payOrderFromList(order: PaymentOrderListItemDto) {
        val gateKey = "order:${order.outTradeNo}"
        if (!tryAcquirePaymentGate(gateKey)) {
            rejectDuplicatePayment()
            return
        }
        viewModelScope.launch {
            beginPaymentUi()
            _uiState.value = _uiState.value.copy(
                orderCenter = _uiState.value.orderCenter.copy(actingOutTradeNo = order.outTradeNo),
            )
            try {
                val payment = api.repayPaymentOrder(order.outTradeNo)
                if (payment.status.equals("PAID", ignoreCase = true) ||
                    isPaymentConfirmedStatus(payment.status)
                ) {
                    refreshOrderCenter()
                    refreshAll()
                    _uiState.value = _uiState.value.copy(message = "支付成功")
                    return@launch
                }
                val paid = payWithAlipay(payment.outTradeNo, payment.alipay.orderStr)
                if (paid) {
                    refreshOrderCenter()
                    refreshAll()
                    _uiState.value = _uiState.value.copy(message = "支付成功")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "支付失败"))
            } finally {
                endPaymentUi()
                releasePaymentGate(gateKey)
                _uiState.value = _uiState.value.copy(
                    orderCenter = _uiState.value.orderCenter.copy(actingOutTradeNo = null),
                )
            }
        }
    }

    fun cancelOrderFromList(order: PaymentOrderListItemDto) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                orderCenter = _uiState.value.orderCenter.copy(actingOutTradeNo = order.outTradeNo),
            )
            try {
                api.cancelPaymentOrder(order.outTradeNo)
                refreshOrderCenter()
                _uiState.value = _uiState.value.copy(message = "订单已取消")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "取消失败"))
            } finally {
                _uiState.value = _uiState.value.copy(
                    orderCenter = _uiState.value.orderCenter.copy(actingOutTradeNo = null),
                )
            }
        }
    }

    fun confirmReceiptFromList(order: PaymentOrderListItemDto) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                orderCenter = _uiState.value.orderCenter.copy(actingOutTradeNo = order.outTradeNo),
            )
            try {
                api.confirmPaymentReceipt(order.outTradeNo)
                refreshOrderCenter()
                refreshAll()
                _uiState.value = _uiState.value.copy(message = "已确认收货")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "确认收货失败"))
            } finally {
                _uiState.value = _uiState.value.copy(
                    orderCenter = _uiState.value.orderCenter.copy(actingOutTradeNo = null),
                )
            }
        }
    }

    fun submitOrderReview(order: PaymentOrderListItemDto, rating: Int, content: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                orderCenter = _uiState.value.orderCenter.copy(actingOutTradeNo = order.outTradeNo),
            )
            try {
                api.createPaymentOrderReview(
                    order.outTradeNo,
                    CreateOrderReviewRequest(rating = rating, content = content),
                )
                refreshOrderCenter()
                _uiState.value = _uiState.value.copy(message = "评价成功")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "评价失败"))
            } finally {
                _uiState.value = _uiState.value.copy(
                    orderCenter = _uiState.value.orderCenter.copy(actingOutTradeNo = null),
                )
            }
        }
    }

    fun applyOrderAfterSales(order: PaymentOrderListItemDto, reason: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                orderCenter = _uiState.value.orderCenter.copy(actingOutTradeNo = order.outTradeNo),
            )
            try {
                api.applyPaymentAfterSales(
                    order.outTradeNo,
                    ApplyAfterSalesRequest(reason = reason),
                )
                refreshOrderCenter()
                refreshAll()
                _uiState.value = _uiState.value.copy(message = "售后申请已提交")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "申请售后失败"))
            } finally {
                _uiState.value = _uiState.value.copy(
                    orderCenter = _uiState.value.orderCenter.copy(actingOutTradeNo = null),
                )
            }
        }
    }

    companion object {
        private const val PROFILE_TAB_PAGE_SIZE = 10
        private const val PROFILE_FAVORITES_TAB = 3
        private const val PROFILE_LIKES_TAB = 4
        private const val ACTIVITY_FEED_PAGE_SIZE = 10
        private const val ORDER_PAGE_SIZE = 10
        private const val TRANSACTION_PAGE_SIZE = 15
    }

    fun openProfileSearch() {
        _uiState.value = _uiState.value.copy(
            profileSearch = ProfileSearchUiState(),
        )
    }

    fun updateProfileSearchKeyword(keyword: String) {
        _uiState.value = _uiState.value.copy(
            profileSearch = _uiState.value.profileSearch.copy(keyword = keyword),
        )
    }

    fun clearProfileSearchKeyword() {
        _uiState.value = _uiState.value.copy(
            profileSearch = ProfileSearchUiState(),
        )
    }

    fun searchProfileNotes(keyword: String) {
        viewModelScope.launch {
            val trimmed = keyword.trim()
            _uiState.value = _uiState.value.copy(
                profileSearch = _uiState.value.profileSearch.copy(
                    keyword = trimmed,
                    isLoading = true,
                ),
            )
            try {
                val posts = api.getMyPosts(trimmed.takeIf { it.isNotBlank() })
                _uiState.value = _uiState.value.copy(
                    profileSearch = _uiState.value.profileSearch.copy(
                        results = posts,
                        isLoading = false,
                        hasSearched = true,
                    ),
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    profileSearch = _uiState.value.profileSearch.copy(isLoading = false),
                    error = parseError(e, "搜索失败"),
                )
            }
        }
    }

    fun searchMyPosts(keyword: String) {
        searchProfileNotes(keyword)
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

    fun authorizeAlipayAccount() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPrivacyUpdating = true)
            try {
                val authInfo = api.getAlipayOAuthAuthInfo().authInfo
                if (authInfo.isBlank()) {
                    _uiState.value = _uiState.value.copy(error = "获取支付宝授权信息失败")
                    return@launch
                }
                val handler = alipayAuthHandler
                if (handler == null) {
                    _uiState.value = _uiState.value.copy(error = "无法调起支付宝，请重试")
                    return@launch
                }
                val result = try {
                    handler(authInfo)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = e.message?.takeIf { it.isNotBlank() } ?: "支付宝调起失败",
                    )
                    return@launch
                }
                if (!result.success || result.authCode.isNullOrBlank()) {
                    val hint = AlipayHelper.authMessage(result).ifBlank { "授权未完成" }
                    _uiState.value = _uiState.value.copy(message = hint)
                    return@launch
                }
                api.bindMyAlipayByOAuth(BindAlipayOAuthRequest(authCode = result.authCode))
                val updated = api.getMyProfile()
                _uiState.value = _uiState.value.copy(
                    myProfile = updated,
                    message = "支付宝授权绑定成功",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "支付宝授权失败"))
            } finally {
                _uiState.value = _uiState.value.copy(isPrivacyUpdating = false)
            }
        }
    }

    fun bindAlipayAccount(alipayLoginId: String, alipayRealName: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPrivacyUpdating = true)
            try {
                api.bindMyAlipayAccount(
                    BindAlipayAccountRequest(
                        alipayLoginId = alipayLoginId.trim(),
                        alipayRealName = alipayRealName?.trim()?.takeIf { it.isNotBlank() },
                    ),
                )
                val updated = api.getMyProfile()
                _uiState.value = _uiState.value.copy(
                    myProfile = updated,
                    message = "支付宝收款账号已绑定",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "绑定失败"))
            } finally {
                _uiState.value = _uiState.value.copy(isPrivacyUpdating = false)
            }
        }
    }

    fun unbindAlipayAccount() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPrivacyUpdating = true)
            try {
                api.unbindMyAlipayAccount()
                val updated = api.getMyProfile()
                _uiState.value = _uiState.value.copy(
                    myProfile = updated,
                    message = "已解绑支付宝收款账号",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "解绑失败"))
            } finally {
                _uiState.value = _uiState.value.copy(isPrivacyUpdating = false)
            }
        }
    }

    fun toggleFollow(userId: Int) {
        viewModelScope.launch {
            try {
                val result = api.toggleFollow(userId)
                val profile = api.getUserProfile(userId)
                _uiState.value = _uiState.value.copy(
                    selectedUserProfile = profile,
                    detailAuthorFollowing = if (isDetailAuthor(userId)) result.isFollowing else _uiState.value.detailAuthorFollowing,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "操作失败"))
            }
        }
    }

    fun toggleDetailAuthorFollow(authorId: Int) {
        viewModelScope.launch {
            try {
                val result = api.toggleFollow(authorId)
                val current = _uiState.value
                _uiState.value = current.copy(
                    detailAuthorFollowing = result.isFollowing,
                    selectedUserProfile = current.selectedUserProfile?.takeIf { it.id == authorId }
                        ?.copy(isFollowing = result.isFollowing)
                        ?: current.selectedUserProfile,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "操作失败"))
            }
        }
    }

    private fun isDetailAuthor(authorId: Int): Boolean {
        val state = _uiState.value
        return state.selectedPost?.authorId == authorId || state.selectedActivity?.authorId == authorId
    }

    private suspend fun loadAuthorFollowState(authorId: Int): Boolean? {
        val myId = _uiState.value.myProfile?.id ?: return null
        if (authorId == myId) return null
        return api.getUserProfile(authorId).isFollowing
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
                    val messageNode = JsonParser.parseString(body).asJsonObject["message"]
                    when {
                        messageNode == null || messageNode.isJsonNull -> null
                        messageNode.isJsonPrimitive -> messageNode.asString
                        messageNode.isJsonArray -> messageNode.asJsonArray.firstOrNull()?.asString
                        else -> messageNode.toString()
                    }
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
