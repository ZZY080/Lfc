import SwiftUI

// MARK: - Search landing

struct SearchPageView: View {
    @Bindable var store: HomeStore
    @ObservedObject private var historyStore = SearchHistoryStore.shared
    let onBack: () -> Void
    let onSearchSubmitted: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            XhsSearchTopBar(
                text: $store.searchInput,
                placeholder: "搜索校园笔记、活动",
                onBack: onBack,
                onSearch: submitSearch,
                requestFocus: true
            )

            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    if !historyStore.history.isEmpty {
                        historySection
                        Spacer().frame(height: 24)
                    }

                    hotSearchSection
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            }
        }
        .background(Color.white)
    }

    private var historySection: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("历史搜索")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(XhsTheme.textPrimary)
                Spacer()
                Button(action: store.clearSearchHistory) {
                    Image(systemName: "trash")
                        .font(.system(size: 16))
                        .foregroundStyle(XhsTheme.textSecondary)
                        .frame(width: 32, height: 32)
                }
                .buttonStyle(.plain)
            }

            XhsFlowLayout {
                ForEach(historyStore.history, id: \.self) { keyword in
                    XhsSearchChip(text: keyword) {
                        store.searchInput = keyword
                        submitSearch()
                    }
                }
            }
        }
    }

    private var hotSearchSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("猜你想搜")
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(XhsTheme.textPrimary)

            XhsFlowLayout {
                ForEach(xhsHotSearches, id: \.self) { keyword in
                    XhsSearchChip(text: keyword) {
                        store.searchInput = keyword
                        submitSearch()
                    }
                }
            }
        }
    }

    private func submitSearch() {
        Task {
            await store.submitSearch(store.searchInput, onNavigate: onSearchSubmitted)
        }
    }
}

// MARK: - Search results

struct SearchResultView: View {
    @Bindable var store: HomeStore
    let onBack: () -> Void
    let onPostTap: (Int) -> Void
    let onActivityTap: (Int) -> Void

    private var searchState: SearchUiState { store.searchState }

    private var selectedTab: String { searchState.selectedTab }

    private var isEmpty: Bool {
        switch selectedTab {
        case "笔记": searchState.posts.isEmpty
        case "活动": searchState.activities.isEmpty
        default: searchState.posts.isEmpty && searchState.activities.isEmpty
        }
    }

    private var showSkeleton: Bool {
        searchState.isInitialLoading || searchState.isTabLoading
    }

    private var hasMore: Bool {
        switch selectedTab {
        case "笔记": searchState.postsHasMore
        case "活动": searchState.activitiesHasMore
        default: searchState.postsHasMore || searchState.activitiesHasMore
        }
    }

    private var mixedItems: [SearchFeedItem] {
        buildMixedSearchFeed(posts: searchState.posts, activities: searchState.activities)
    }

    var body: some View {
        VStack(spacing: 0) {
            VStack(spacing: 0) {
                XhsSearchTopBar(
                    text: $store.searchInput,
                    placeholder: "搜索校园笔记、活动",
                    onBack: {
                        store.clearSearch()
                        onBack()
                    },
                    onSearch: {
                        Task {
                            await store.submitSearch(store.searchInput, onNavigate: {})
                        }
                    }
                )
                XhsTextTabRow(
                    tabs: xhsSearchTabs,
                    selectedTab: selectedTab,
                    onTabSelected: store.selectSearchTab
                )
            }

            content
        }
        .background(XhsTheme.background)
        .task {
            guard !searchState.keyword.isEmpty,
                  !searchState.postsLoaded,
                  !searchState.activitiesLoaded,
                  !searchState.isInitialLoading,
                  !searchState.isTabLoading else { return }
            await store.loadSearchResults(refresh: true)
        }
    }

    @ViewBuilder
    private var content: some View {
        if showSkeleton {
            skeletonView
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        } else if isEmpty {
            emptyView
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            ScrollView {
                resultGrid
                    .id(searchState.listResetNonce)

                if searchState.isLoadingMore {
                    SkeletonLoadMoreFooter()
                } else if !hasMore {
                    FeedListEndFooter()
                }
            }
            .refreshable { await store.loadSearchResults(refresh: true) }
        }
    }

    @ViewBuilder
    private var skeletonView: some View {
        if selectedTab == "活动" {
            ActivityFeedSkeleton(itemCount: 3)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
        } else {
            FeedGridSkeletonStatic(itemCount: 6)
        }
    }

    private var emptyView: some View {
        Text("没有找到「\(searchState.keyword)」相关内容")
            .font(.system(size: 14))
            .foregroundStyle(XhsTheme.textSecondary)
    }

    @ViewBuilder
    private var resultGrid: some View {
        switch selectedTab {
        case "笔记":
            WaterfallFeedGrid(posts: searchState.posts, spacing: 8) { post in
                XhsProfileFeedCard(post: post, onTap: { onPostTap(post.id) })
                    .onAppear { triggerLoadMoreIfNeeded(for: post.id, in: searchState.posts.map(\.id)) }
            }
            .padding(8)
        case "活动":
            activityWaterfall(activities: searchState.activities)
        default:
            SearchResultWaterfallGrid(
                items: mixedItems,
                spacing: 8,
                onPostTap: onPostTap,
                onActivityTap: onActivityTap,
                onItemAppear: { item in
                    guard item.id == mixedItems.last?.id else { return }
                    Task { await store.loadMoreSearchResults() }
                }
            )
            .padding(8)
        }
    }

    private func activityWaterfall(activities: [ActivityDto]) -> some View {
        HStack(alignment: .top, spacing: 8) {
            LazyVStack(spacing: 8) {
                ForEach(leftActivities(activities)) { activity in
                    XhsProfileActivityCard(
                        activity: activity,
                        authorLabel: activity.author?.displayName ?? "同学\(activity.authorId)",
                        onTap: { onActivityTap(activity.id) }
                    )
                    .frame(maxWidth: .infinity)
                    .onAppear { triggerLoadMoreIfNeeded(for: activity.id, in: activities.map(\.id)) }
                }
            }
            .frame(maxWidth: .infinity, alignment: .top)

            LazyVStack(spacing: 8) {
                ForEach(rightActivities(activities)) { activity in
                    XhsProfileActivityCard(
                        activity: activity,
                        authorLabel: activity.author?.displayName ?? "同学\(activity.authorId)",
                        onTap: { onActivityTap(activity.id) }
                    )
                    .frame(maxWidth: .infinity)
                    .onAppear { triggerLoadMoreIfNeeded(for: activity.id, in: activities.map(\.id)) }
                }
            }
            .frame(maxWidth: .infinity, alignment: .top)
        }
        .padding(8)
    }

    private func leftActivities(_ activities: [ActivityDto]) -> [ActivityDto] {
        activities.enumerated().compactMap { index, item in
            index.isMultiple(of: 2) ? item : nil
        }
    }

    private func rightActivities(_ activities: [ActivityDto]) -> [ActivityDto] {
        activities.enumerated().compactMap { index, item in
            index.isMultiple(of: 2) ? nil : item
        }
    }

    private func triggerLoadMoreIfNeeded(for id: Int, in ids: [Int]) {
        guard id == ids.last else { return }
        Task { await store.loadMoreSearchResults() }
    }
}
