package com.lfc.consumer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lfc.consumer.data.ApiClient
import com.lfc.consumer.data.local.SearchHistoryStore
import com.lfc.consumer.data.local.TokenManager
import com.lfc.consumer.data.local.UserSession
import com.lfc.consumer.data.model.ActivityDto
import com.lfc.consumer.data.model.ActivityParticipantDto
import com.lfc.consumer.data.model.CreateActivityRequest
import com.lfc.consumer.data.model.CreatePostRequest
import com.lfc.consumer.data.model.FeedUiState
import com.lfc.consumer.data.model.SearchUiState
import com.lfc.consumer.data.model.MessageDto
import com.lfc.consumer.data.model.PostDto
import com.lfc.consumer.data.model.UpdateActivityRequest
import com.lfc.consumer.data.model.UpdatePostRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import com.google.gson.JsonParser

data class HomeUiState(
    val feed: FeedUiState = FeedUiState(),
    val posts: List<PostDto> = emptyList(),
    val activities: List<ActivityDto> = emptyList(),
    val myPosts: List<PostDto> = emptyList(),
    val myActivities: List<ActivityDto> = emptyList(),
    val myParticipations: List<ActivityParticipantDto> = emptyList(),
    val messages: List<MessageDto> = emptyList(),
    val unreadCount: Int = 0,
    val selectedMessage: MessageDto? = null,
    val isMessageLoading: Boolean = false,
    val selectedPost: PostDto? = null,
    val isPostLoading: Boolean = false,
    val selectedActivity: ActivityDto? = null,
    val isActivityLoading: Boolean = false,
    val isJoiningActivity: Boolean = false,
    val search: SearchUiState = SearchUiState(),
    val searchInput: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

class HomeViewModel(
    private val tokenManager: TokenManager,
    private val searchHistoryStore: SearchHistoryStore,
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
                val activities = api.getApprovedActivities()
                val myPosts = api.getMyPosts()
                val myActivities = api.getMyActivities()
                val myParticipations = api.getMyParticipations()
                val messages = api.getMessages()
                val unreadCount = api.getUnreadCount().count
                _uiState.value = _uiState.value.copy(
                    posts = myPosts,
                    activities = activities,
                    myPosts = myPosts,
                    myActivities = myActivities,
                    myParticipations = myParticipations,
                    messages = messages,
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

    fun createPost(title: String, content: String, onSuccess: () -> Unit = {}, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                api.createPost(CreatePostRequest(title, content))
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

    fun updatePost(id: Int, title: String, content: String, onSuccess: () -> Unit = {}, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                api.updatePost(id, UpdatePostRequest(title, content))
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
        onSuccess: () -> Unit = {},
        onComplete: () -> Unit = {},
    ) {
        viewModelScope.launch {
            try {
                api.createActivity(
                    CreateActivityRequest(
                        title = title,
                        description = description,
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
        onSuccess: () -> Unit = {},
        onComplete: () -> Unit = {},
    ) {
        viewModelScope.launch {
            try {
                api.updateActivity(
                    id,
                    UpdateActivityRequest(
                        title = title,
                        description = description,
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
            _uiState.value = _uiState.value.copy(isPostLoading = true, selectedPost = null)
            try {
                val post = api.getPost(id)
                _uiState.value = _uiState.value.copy(selectedPost = post, isPostLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isPostLoading = false,
                    error = parseError(e, "加载笔记失败"),
                )
            }
        }
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
        _uiState.value = _uiState.value.copy(selectedPost = null, isPostLoading = false)
    }

    fun clearSelectedActivity() {
        _uiState.value = _uiState.value.copy(selectedActivity = null, isActivityLoading = false)
    }

    fun loadMessageDetail(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMessageLoading = true, selectedMessage = null)
            try {
                val message = api.getMessage(id)
                val unreadCount = api.getUnreadCount().count
                val messages = _uiState.value.messages.map {
                    if (it.id == id) message else it
                }
                _uiState.value = _uiState.value.copy(
                    selectedMessage = message,
                    messages = messages,
                    unreadCount = unreadCount,
                    isMessageLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isMessageLoading = false,
                    error = parseError(e, "加载消息失败"),
                )
            }
        }
    }

    fun clearSelectedMessage() {
        _uiState.value = _uiState.value.copy(selectedMessage = null, isMessageLoading = false)
    }

    fun markAllMessagesRead() {
        viewModelScope.launch {
            try {
                api.markAllMessagesRead()
                refreshAll()
                _uiState.value = _uiState.value.copy(message = "已全部标记为已读")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "操作失败"))
            }
        }
    }

    fun refreshMessages() {
        viewModelScope.launch {
            try {
                val messages = api.getMessages()
                val unreadCount = api.getUnreadCount().count
                _uiState.value = _uiState.value.copy(
                    messages = messages,
                    unreadCount = unreadCount,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = parseError(e, "加载消息失败"))
            }
        }
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
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(tokenManager, searchHistoryStore) as T
    }
}
