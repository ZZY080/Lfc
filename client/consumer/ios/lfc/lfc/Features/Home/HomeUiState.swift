import Foundation

// MARK: - Home tab list state (mirrors Android FeedUiState / HomeUiState buckets)

struct FeedUiState: Sendable {
    var posts: [PostDto] = []
    var primaryTab: String = "发现"
    var selectedTab: String = "推荐"
    var myChannels: [String] = []
    var allChannels: [String] = []
    var recommendedChannels: [String] = []
    var defaultMyChannels: [String] = []
    var isChannelPanelExpanded: Bool = false
    var isChannelEditMode: Bool = false
    var page: Int = 1
    var hasMore: Bool = true
    var isRefreshing: Bool = false
    var isLoadingMore: Bool = false
    var isInitialLoading: Bool = false
    var hasLoadedOnce: Bool = false
    var loadError: String?

    var effectiveMyChannels: [String] {
        if !myChannels.isEmpty { return myChannels }
        if !defaultMyChannels.isEmpty { return defaultMyChannels }
        return fallbackMyChannels
    }
}

struct ActivityFeedUiState: Sendable {
    var activities: [ActivityDto] = []
    var page: Int = 1
    var hasMore: Bool = true
    var isRefreshing: Bool = false
    var isLoadingMore: Bool = false
    var isInitialLoading: Bool = false
    var hasLoadedOnce: Bool = false
    var loadError: String?
    var listResetNonce: Int = 0
}

struct MessagesUiState: Sendable {
    var conversations: [ConversationDto] = []
    var notificationPreview: [NotificationDto] = []
    var notificationTotal: Int = 0
    var notificationUnreadCount: Int = 0
    var page: Int = 1
    var hasMore: Bool = true
    var isRefreshing: Bool = false
    var isLoadingMore: Bool = false
    var isInitialLoading: Bool = false
    var hasLoadedOnce: Bool = false
    var listResetNonce: Int = 0
}

struct NotificationFeedUiState: Sendable {
    var notifications: [NotificationDto] = []
    var page: Int = 1
    var hasMore: Bool = true
    var isRefreshing: Bool = false
    var isLoadingMore: Bool = false
    var isInitialLoading: Bool = false
    var hasLoadedOnce: Bool = false
}

struct SearchUiState: Sendable {
    var keyword: String = ""
    var selectedTab: String = "综合"
    var posts: [PostDto] = []
    var activities: [ActivityDto] = []
    var postsPage: Int = 1
    var activitiesPage: Int = 1
    var postsHasMore: Bool = true
    var activitiesHasMore: Bool = true
    var postsLoaded: Bool = false
    var activitiesLoaded: Bool = false
    var isRefreshing: Bool = false
    var isLoadingMore: Bool = false
    var isInitialLoading: Bool = false
    var isTabLoading: Bool = false
    var listResetNonce: Int = 0
}

struct ProfileTabUiState: Sendable {
    var page: Int = 1
    var hasMore: Bool = true
    var isRefreshing: Bool = false
    var isLoadingMore: Bool = false
    var isInitialLoading: Bool = false
    var hasLoadedOnce: Bool = false
    var totalCount: Int?
}

struct ProfileTabsUiState: Sendable {
    var targetUserId: Int?
    var tabs: [ProfileTabUiState] = Array(repeating: ProfileTabUiState(), count: 5)
    var favoriteActivities: ProfileTabUiState = ProfileTabUiState()
    var likedActivities: ProfileTabUiState = ProfileTabUiState()
    var favoritesPostsTotal: Int?
    var favoritesActivitiesTotal: Int?
    var likedPostsTotal: Int?
    var likedActivitiesTotal: Int?

    func withTab(_ tab: Int, _ tabState: ProfileTabUiState) -> ProfileTabsUiState {
        var updated = tabs
        while updated.count <= tab {
            updated.append(ProfileTabUiState())
        }
        updated[tab] = tabState
        return ProfileTabsUiState(
            targetUserId: targetUserId,
            tabs: updated,
            favoriteActivities: favoriteActivities,
            likedActivities: likedActivities,
            favoritesPostsTotal: favoritesPostsTotal,
            favoritesActivitiesTotal: favoritesActivitiesTotal,
            likedPostsTotal: likedPostsTotal,
            likedActivitiesTotal: likedActivitiesTotal
        )
    }

    func withFavoriteActivities(_ tabState: ProfileTabUiState) -> ProfileTabsUiState {
        ProfileTabsUiState(
            targetUserId: targetUserId,
            tabs: tabs,
            favoriteActivities: tabState,
            likedActivities: likedActivities,
            favoritesPostsTotal: favoritesPostsTotal,
            favoritesActivitiesTotal: favoritesActivitiesTotal,
            likedPostsTotal: likedPostsTotal,
            likedActivitiesTotal: likedActivitiesTotal
        )
    }

    func withLikedActivities(_ tabState: ProfileTabUiState) -> ProfileTabsUiState {
        ProfileTabsUiState(
            targetUserId: targetUserId,
            tabs: tabs,
            favoriteActivities: favoriteActivities,
            likedActivities: tabState,
            favoritesPostsTotal: favoritesPostsTotal,
            favoritesActivitiesTotal: favoritesActivitiesTotal,
            likedPostsTotal: likedPostsTotal,
            likedActivitiesTotal: likedActivitiesTotal
        )
    }
}

enum ProfileLibraryFeedItem: Identifiable, Sendable {
    case post(PostDto)
    case activity(ActivityDto)

    var id: String {
        switch self {
        case .post(let post): "post-\(post.id)"
        case .activity(let activity): "activity-\(activity.id)"
        }
    }

    var sortDate: String {
        switch self {
        case .post(let post): post.savedAt ?? post.createdAt
        case .activity(let activity): activity.savedAt ?? activity.createdAt
        }
    }
}

func combineProfileLibraryTabState(
    posts: ProfileTabUiState,
    activities: ProfileTabUiState
) -> ProfileTabUiState {
    ProfileTabUiState(
        page: max(posts.page, activities.page),
        hasMore: posts.hasMore || activities.hasMore,
        isRefreshing: posts.isRefreshing || activities.isRefreshing,
        isLoadingMore: posts.isLoadingMore || activities.isLoadingMore,
        isInitialLoading: posts.isInitialLoading || activities.isInitialLoading,
        hasLoadedOnce: posts.hasLoadedOnce && activities.hasLoadedOnce,
        totalCount: posts.totalCount
    )
}

func profileTabUiStateFor(selectedTab: Int, profileTabs: ProfileTabsUiState) -> ProfileTabUiState {
    switch selectedTab {
    case 3:
        return combineProfileLibraryTabState(
            posts: profileTabs.tabs[safe: 3] ?? ProfileTabUiState(),
            activities: profileTabs.favoriteActivities
        )
    case 4:
        return combineProfileLibraryTabState(
            posts: profileTabs.tabs[safe: 4] ?? ProfileTabUiState(),
            activities: profileTabs.likedActivities
        )
    default:
        return profileTabs.tabs[safe: selectedTab] ?? ProfileTabUiState()
    }
}

func buildProfileLibraryFeed(posts: [PostDto], activities: [ActivityDto]) -> [ProfileLibraryFeedItem] {
    let postItems = posts.map { ProfileLibraryFeedItem.post($0) }
    let activityItems = activities.map { ProfileLibraryFeedItem.activity($0) }
    return (postItems + activityItems).sorted { $0.sortDate > $1.sortDate }
}

extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}

struct ProfileUiState: Sendable {
    var myProfile: UserProfileDto?
    var selectedUserProfile: UserProfileDto?
    var profileContentTab: Int = 0
    var isLoading: Bool = false
    var loadFailed: Bool = false
    var isUserProfileLoading: Bool = false
    var profileNotes: [PostDto] = []
    var profileActivities: [ActivityDto] = []
    var profileFavoritePosts: [PostDto] = []
    var profileFavoriteActivities: [ActivityDto] = []
    var profileLikedPosts: [PostDto] = []
    var profileLikedActivities: [ActivityDto] = []
    var profileComments: [ProfileCommentDto] = []
    var profileTabs: ProfileTabsUiState = ProfileTabsUiState()
    var visitorProfileNotes: [PostDto] = []
    var visitorProfileActivities: [ActivityDto] = []
    var visitorProfileFavoritePosts: [PostDto] = []
    var visitorProfileFavoriteActivities: [ActivityDto] = []
    var visitorProfileLikedPosts: [PostDto] = []
    var visitorProfileLikedActivities: [ActivityDto] = []
    var visitorProfileComments: [ProfileCommentDto] = []
    var visitorProfileTabs: ProfileTabsUiState = ProfileTabsUiState()
    var visitorProfileContentTab: Int = 0
}

struct OrderCenterUiState: Sendable {
    var selectedTab: String = "all"
    var orders: [PaymentOrderListItemDto] = []
    var tabCounts: PaymentOrderTabCountsDto?
    var page: Int = 1
    var hasMore: Bool = true
    var isRefreshing: Bool = false
    var isLoadingMore: Bool = false
    var isInitialLoading: Bool = false
    var actingOutTradeNo: String?
}

struct PaymentTransactionLedgerUiState: Sendable {
    var items: [PaymentTransactionItemDto] = []
    var page: Int = 1
    var hasMore: Bool = true
    var isRefreshing: Bool = false
    var isLoadingMore: Bool = false
    var isInitialLoading: Bool = false
}

struct ProfileSearchUiState: Sendable {
    var keyword: String = ""
    var results: [PostDto] = []
    var isLoading: Bool = false
    var hasSearched: Bool = false
}

// MARK: - Detail screens

struct PostCommentsUiState: Sendable {
    var comments: [PostCommentDto] = []
    var page: Int = 1
    var hasMore: Bool = true
    var sort: String = "default"
    var isInitialLoading: Bool = false
    var isLoadingMore: Bool = false
    var loadingReplyRoots: Set<Int> = []
    var replyHasMore: [Int: Bool] = [:]
}
