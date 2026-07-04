import Foundation
import Observation

@MainActor
@Observable
final class HomeStore {
    // MARK: - Tab / list state

    var feedState = FeedUiState()
    var activityFeedState = ActivityFeedUiState()
    var messagesState = MessagesUiState()
    var profileState = ProfileUiState()
    var searchState = SearchUiState()

    // MARK: - Global UI state

    var selectedTab: HomeTab = .discover
    var searchInput: String = ""
    var unreadCount: Int = 0
    var toastMessage: String?
    var toastError: String?

    // MARK: - Detail selections

    var selectedPost: PostDto?
    var isPostLoading = false
    var postDetailTargetId: Int?
    var postDetailLoadFailed = false
    var postDetailRequestID = 0
    var visitorProfileTargetId: Int?
    var visitorProfileRequestID = 0
    var visitorProfileLoadFailed = false
    var selectedActivity: ActivityDto?
    var isActivityLoading = false
    var selectedNotification: NotificationDto?
    var isNotificationLoading = false
    var selectedConversation: ConversationDto?
    var chatMessages: [ChatMessageDto] = []
    var isChatLoading = false
    var isChatSending = false
    var notificationFeedState = NotificationFeedUiState()

    // Detail interaction state
    var postCommentsUi = PostCommentsUiState()
    var detailAuthorFollowing: Bool?
    var isPostSocialSubmitting = false
    var isActivitySocialSubmitting = false
    var isJoiningActivity = false
    var isPurchasingProduct = false
    var productPurchaseOrder: PaymentOrderDetailDto?

    // MARK: - Publish / orders / profile

    var pendingLocationPick: LocationPick?
    var publishCategories: [String] = []
    var isPublishSubmitting = false
    var orderCenterState = OrderCenterUiState()
    var paymentTransactionState = PaymentTransactionLedgerUiState()
    var profileSearchState = ProfileSearchUiState()
    var isProfileUpdating = false
    var isPrivacyUpdating = false
    var paymentConfig: PaymentConfigDto?

    var userLocationAddress: String?
    var userLatitude: Double?
    var userLongitude: Double?

    let api = LFCAPIService.shared
    private var messagesLoadGeneration = 0

    // MARK: - Bootstrap

    func bootstrap() async {
        if feedState.posts.isEmpty {
            feedState.isInitialLoading = true
        }
        async let feed: Void = loadFeed(refresh: true)
        async let profile: Void = loadProfile()
        async let channels: Void = loadPublishCategories()
        async let feedChannels: Void = loadFeedChannels()
        async let payment: Void = loadPaymentConfig()
        _ = await (feed, profile, channels, feedChannels, payment)
    }

    func clearToast() {
        toastMessage = nil
        toastError = nil
    }

    // MARK: - Feed

    func loadFeed(refresh: Bool = false) async {
        let page = refresh ? 1 : feedState.page
        if refresh, feedState.posts.isEmpty {
            feedState.isInitialLoading = true
        }
        feedState.isRefreshing = refresh
        feedState.isLoadingMore = !refresh && feedState.hasMore
        feedState.page = page

        do {
            let sort: String? = {
                if feedState.primaryTab == "关注" { return "latest" }
                if feedState.selectedTab == "最新" { return "latest" }
                return "recommend"
            }()
            let tab: String? = {
                if feedState.primaryTab == "关注" { return "关注" }
                if feedState.selectedTab == "推荐" || feedState.selectedTab == "最新" { return nil }
                return feedState.selectedTab
            }()

            let response = try await api.getPostFeed(
                page: page,
                limit: 10,
                sort: sort,
                tab: tab,
                city: feedCityQueryParamForCurrentTab()
            )

            let merged: [PostDto]
            if refresh {
                merged = response.items
            } else {
                let existingIDs = Set(feedState.posts.map(\.id))
                merged = feedState.posts + response.items.filter { !existingIDs.contains($0.id) }
            }

            feedState.posts = merged
            feedState.page = response.page + 1
            feedState.hasMore = response.hasMore
        } catch {
            toastError = parseError(error, fallback: "加载笔记失败")
        }

        feedState.isRefreshing = false
        feedState.isInitialLoading = false
        feedState.isLoadingMore = false
    }

    func loadMoreFeed() async {
        guard feedState.hasMore, !feedState.isLoadingMore, !feedState.isRefreshing else { return }
        await loadFeed(refresh: false)
    }

    func selectFeedPrimaryTab(_ tab: String) {
        let cityLabel = feedCityLabel
        let normalized: String = {
            if xhsFeedPrimaryTabs.contains(tab) { return tab }
            if tab == cityLabel { return cityLabel }
            return tab
        }()
        guard feedState.primaryTab != normalized else { return }
        feedState.primaryTab = normalized
        feedState.posts = []
        feedState.page = 1
        feedState.isInitialLoading = true
        feedState.isChannelPanelExpanded = false
        feedState.isChannelEditMode = false
        Task { await loadFeed(refresh: true) }
    }

    func selectFeedTab(_ tab: String) {
        if feedState.selectedTab == tab, !feedState.isChannelPanelExpanded { return }
        feedState.selectedTab = tab
        feedState.posts = []
        feedState.page = 1
        feedState.isInitialLoading = true
        feedState.isChannelPanelExpanded = false
        feedState.isChannelEditMode = false
        Task { await loadFeed(refresh: true) }
    }

    // MARK: - Activity feed

    func loadActivityFeed(refresh: Bool = false) async {
        if refresh, activityFeedState.isRefreshing { return }
        if !refresh, activityFeedState.isLoadingMore { return }

        let page = refresh ? 1 : activityFeedState.page
        activityFeedState.isRefreshing = refresh
        activityFeedState.isInitialLoading = !activityFeedState.hasLoadedOnce && activityFeedState.activities.isEmpty
        activityFeedState.isLoadingMore = !refresh && activityFeedState.hasMore
        activityFeedState.page = page
        if refresh {
            activityFeedState.listResetNonce += 1
        }

        do {
            let response = try await api.getActivityFeed(page: page, limit: 10)
            let merged: [ActivityDto]
            if refresh {
                merged = response.items
            } else {
                let existingIDs = Set(activityFeedState.activities.map(\.id))
                merged = activityFeedState.activities + response.items.filter { !existingIDs.contains($0.id) }
            }
            activityFeedState.activities = merged
            activityFeedState.page = response.page + 1
            activityFeedState.hasMore = response.hasMore
            activityFeedState.hasLoadedOnce = true
        } catch {
            toastError = parseError(error, fallback: "加载活动失败")
        }

        activityFeedState.isRefreshing = false
        activityFeedState.isInitialLoading = false
        activityFeedState.isLoadingMore = false
    }

    func loadMoreActivityFeed() async {
        guard activityFeedState.hasMore, !activityFeedState.isLoadingMore, !activityFeedState.isRefreshing else { return }
        await loadActivityFeed(refresh: false)
    }

    // MARK: - Messages

    func loadMessages(refresh: Bool = false) async {
        if !refresh, messagesState.isLoadingMore || !messagesState.hasMore || messagesState.isRefreshing {
            return
        }

        let page = refresh ? 1 : messagesState.page
        messagesLoadGeneration += 1
        let generation = messagesLoadGeneration

        messagesState.isRefreshing = refresh
        messagesState.isInitialLoading = !messagesState.hasLoadedOnce && messagesState.conversations.isEmpty
        messagesState.isLoadingMore = !refresh && messagesState.hasMore
        messagesState.page = page
        if refresh {
            messagesState.listResetNonce += 1
        }

        do {
            let conversationResponse = try await api.getConversations(page: page, limit: 20)
            let notificationResponse: PaginatedResponse<NotificationDto>?
            if refresh || page == 1 {
                notificationResponse = try await api.getNotifications(page: 1, limit: 5)
            } else {
                notificationResponse = nil
            }
            let notificationUnread = try await api.getNotificationUnreadCount().count
            let conversationUnread = try await api.getConversationUnreadCount().count

            guard generation == messagesLoadGeneration else { return }

            let merged: [ConversationDto]
            if refresh {
                merged = conversationResponse.items
            } else {
                let existingIDs = Set(messagesState.conversations.map(\.id))
                merged = messagesState.conversations + conversationResponse.items.filter { !existingIDs.contains($0.id) }
            }

            messagesState.conversations = merged
            if let notificationResponse {
                messagesState.notificationPreview = notificationResponse.items
                messagesState.notificationTotal = notificationResponse.total
            }
            messagesState.notificationUnreadCount = notificationUnread
            messagesState.page = conversationResponse.page + 1
            messagesState.hasMore = conversationResponse.hasMore
            messagesState.hasLoadedOnce = true
            unreadCount = conversationUnread + notificationUnread
        } catch {
            toastError = parseError(error, fallback: "加载消息失败")
        }

        messagesState.isRefreshing = false
        messagesState.isInitialLoading = false
        messagesState.isLoadingMore = false
    }

    func loadMoreMessages() async {
        guard messagesState.hasMore, !messagesState.isLoadingMore, !messagesState.isRefreshing else { return }
        await loadMessages(refresh: false)
    }

    func loadNotificationFeed(refresh: Bool = false) async {
        let page = refresh ? 1 : notificationFeedState.page
        notificationFeedState.isRefreshing = refresh
        notificationFeedState.isInitialLoading = !notificationFeedState.hasLoadedOnce && notificationFeedState.notifications.isEmpty
        notificationFeedState.isLoadingMore = !refresh && notificationFeedState.hasMore
        notificationFeedState.page = page

        do {
            let response = try await api.getNotifications(page: page, limit: 20)
            let merged: [NotificationDto]
            if refresh {
                merged = response.items
            } else {
                let existingIDs = Set(notificationFeedState.notifications.map(\.id))
                merged = notificationFeedState.notifications + response.items.filter { !existingIDs.contains($0.id) }
            }
            notificationFeedState.notifications = merged
            notificationFeedState.page = response.page + 1
            notificationFeedState.hasMore = response.hasMore
            notificationFeedState.hasLoadedOnce = true
        } catch {
            toastError = parseError(error, fallback: "加载通知失败")
        }

        notificationFeedState.isRefreshing = false
        notificationFeedState.isInitialLoading = false
        notificationFeedState.isLoadingMore = false
    }

    func loadNotificationDetail(_ id: Int) async {
        isNotificationLoading = true
        selectedNotification = nil
        do {
            selectedNotification = try await api.getNotification(id: id)
        } catch {
            toastError = parseError(error, fallback: "加载通知详情失败")
        }
        isNotificationLoading = false
    }

    func clearSelectedNotification() {
        selectedNotification = nil
    }

    // MARK: - Profile

    func loadProfile() async {
        profileState.isLoading = true
        do {
            let profile = try await api.getMyProfile()
            let myId = profile.id
            profileState.myProfile = profile
            profileState.profileNotes = []
            profileState.profileActivities = []
            profileState.profileComments = []
            profileState.profileFavoritePosts = []
            profileState.profileFavoriteActivities = []
            profileState.profileLikedPosts = []
            profileState.profileLikedActivities = []
            profileState.profileTabs = seedSelfProfileTabs(userId: myId, profile: profile)
            await loadSelfProfileTab(tab: profileState.profileContentTab, userId: myId)
            await preloadSelfProfileTabTotals(userId: myId)
        } catch {
            toastError = parseError(error, fallback: "加载个人资料失败")
        }
        profileState.isLoading = false
    }

    func ensureMyProfileReady() async {
        guard let myId = profileState.myProfile?.id else {
            await loadProfile()
            return
        }
        prepareSelfProfileTabs(userId: myId)
        if profileState.profileTabs.targetUserId != myId
            || profileState.profileNotes.isEmpty && profileState.profileContentTab == 0 {
            await loadSelfProfileTab(tab: profileState.profileContentTab, userId: myId)
        }
        await preloadSelfProfileTabTotals(userId: myId)
    }

    func ensureMyProfileLoaded() async {
        guard profileState.myProfile == nil else { return }
        await loadProfile()
    }

    // Visitor profile — see HomeStore+VisitorProfile.swift

    // MARK: - Search

    func updateSearchInput(_ value: String) {
        searchInput = value
    }

    func submitSearch(_ keyword: String, onNavigate: () -> Void) async {
        let trimmed = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }

        SearchHistoryStore.shared.add(trimmed)
        searchInput = trimmed
        searchState = SearchUiState(keyword: trimmed, isInitialLoading: true)
        onNavigate()
        await loadSearchResults(refresh: true)
    }

    func clearSearchHistory() {
        SearchHistoryStore.shared.clear()
    }

    func selectSearchTab(_ tab: String) {
        guard searchState.selectedTab != tab else { return }
        let needsPosts = tab != "活动" && !searchState.postsLoaded
        let needsActivities = tab != "笔记" && !searchState.activitiesLoaded
        searchState.selectedTab = tab
        searchState.isTabLoading = needsPosts || needsActivities
        searchState.listResetNonce += 1

        switch tab {
        case "笔记":
            if needsPosts { Task { await loadSearchPosts(refresh: true) } }
        case "活动":
            if needsActivities { Task { await loadSearchActivities(refresh: true) } }
        default:
            if needsPosts || needsActivities {
                Task { await loadSearchResults(refresh: true) }
            }
        }
    }

    func loadSearchResults(refresh: Bool = false) async {
        switch searchState.selectedTab {
        case "笔记":
            await loadSearchPosts(refresh: refresh)
        case "活动":
            await loadSearchActivities(refresh: refresh)
        default:
            async let posts: Void = loadSearchPosts(refresh: refresh)
            async let activities: Void = loadSearchActivities(refresh: refresh)
            _ = await (posts, activities)
        }
    }

    func loadMoreSearchResults() async {
        if searchState.isLoadingMore
            || searchState.isRefreshing
            || searchState.isInitialLoading
            || searchState.isTabLoading {
            return
        }

        switch searchState.selectedTab {
        case "笔记":
            guard searchState.postsHasMore else { return }
            await loadSearchPosts(refresh: false)
        case "活动":
            guard searchState.activitiesHasMore else { return }
            await loadSearchActivities(refresh: false)
        default:
            if searchState.postsHasMore {
                await loadSearchPosts(refresh: false)
            } else if searchState.activitiesHasMore {
                await loadSearchActivities(refresh: false)
            }
        }
    }

    func clearSearch() {
        searchState = SearchUiState()
        searchInput = ""
    }

    private func loadSearchPosts(refresh: Bool) async {
        guard !searchState.keyword.isEmpty else { return }
        let page = refresh ? 1 : searchState.postsPage

        if refresh {
            searchState.isInitialLoading = !searchState.postsLoaded
            searchState.isRefreshing = searchState.postsLoaded
            searchState.isLoadingMore = false
            searchState.listResetNonce += 1
        } else {
            guard searchState.postsHasMore else { return }
            searchState.isLoadingMore = true
        }

        do {
            let response = try await api.getPostFeed(page: page, limit: 10, keyword: searchState.keyword)
            if refresh {
                searchState.posts = response.items
            } else {
                let existingIDs = Set(searchState.posts.map(\.id))
                searchState.posts.append(contentsOf: response.items.filter { !existingIDs.contains($0.id) })
            }
            searchState.postsPage = response.page + 1
            searchState.postsHasMore = response.hasMore
            searchState.postsLoaded = true

            let stillWaitingActivities = searchState.selectedTab == "综合" && !searchState.activitiesLoaded
            searchState.isInitialLoading = stillWaitingActivities
            searchState.isTabLoading = stillWaitingActivities
        } catch {
            searchState.isInitialLoading = false
            searchState.isTabLoading = false
            toastError = parseError(error, fallback: "搜索笔记失败")
        }

        searchState.isRefreshing = false
        searchState.isLoadingMore = false
    }

    private func loadSearchActivities(refresh: Bool) async {
        guard !searchState.keyword.isEmpty else { return }
        let page = refresh ? 1 : searchState.activitiesPage

        if refresh {
            searchState.isInitialLoading = !searchState.activitiesLoaded
            searchState.isRefreshing = searchState.activitiesLoaded
            searchState.isLoadingMore = false
            if searchState.selectedTab != "综合" || searchState.postsLoaded {
                searchState.listResetNonce += 1
            }
        } else {
            guard searchState.activitiesHasMore else { return }
            searchState.isLoadingMore = true
        }

        do {
            let response = try await api.getActivityFeed(page: page, limit: 10, keyword: searchState.keyword)
            if refresh {
                searchState.activities = response.items
            } else {
                let existingIDs = Set(searchState.activities.map(\.id))
                searchState.activities.append(contentsOf: response.items.filter { !existingIDs.contains($0.id) })
            }
            searchState.activitiesPage = response.page + 1
            searchState.activitiesHasMore = response.hasMore
            searchState.activitiesLoaded = true

            let stillWaitingPosts = searchState.selectedTab == "综合" && !searchState.postsLoaded
            searchState.isInitialLoading = stillWaitingPosts
            searchState.isTabLoading = stillWaitingPosts
        } catch {
            searchState.isInitialLoading = false
            searchState.isTabLoading = false
            toastError = parseError(error, fallback: "搜索活动失败")
        }

        searchState.isRefreshing = false
        searchState.isLoadingMore = false
    }

    // MARK: - Helpers

    func parseError(_ error: Error, fallback: String) -> String {
        if let apiError = error as? ApiError {
            return apiError.message
        }
        if let clientError = error as? APIClientError {
            switch clientError {
            case .decodingFailed:
                return "数据解析失败"
            case .unauthorized:
                return "登录已过期，请重新登录"
            case .invalidURL, .invalidResponse:
                return fallback
            }
        }
        let message = error.localizedDescription
        return message.isEmpty ? fallback : message
    }
}
