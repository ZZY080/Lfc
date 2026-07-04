import SwiftUI

struct DiscoverFeedView: View {
    let feedState: FeedUiState
    var userLat: Double?
    var userLng: Double?
    let onRefresh: () async -> Void
    let onLoadMore: () async -> Void
    let onCategoryTabSelected: (String) -> Void
    let onToggleChannelPanel: () -> Void
    let onEnterChannelEditMode: () -> Void
    let onPostTap: (Int) -> Void

    private static let channelAnimation = XhsFeedLayout.channelPanelAnimation
    private static let categoryRowHeight = XhsFeedLayout.categoryRowHeight

    private var isFollowingTab: Bool {
        feedState.primaryTab == "关注"
    }

    private var showChannelPanel: Bool {
        feedState.isChannelPanelExpanded && !isFollowingTab
    }

    var body: some View {
        VStack(spacing: 0) {
            if !isFollowingTab {
                categoryHeader
            }

            if !showChannelPanel {
                feedContent
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    /// Category tabs collapse in place (XHS-style) before the panel fades in below.
    private var categoryHeader: some View {
        XhsFeedCategoryTabRow(
            myChannels: feedState.effectiveMyChannels,
            selectedTab: feedState.selectedTab,
            isPanelExpanded: feedState.isChannelPanelExpanded,
            onTabSelected: onCategoryTabSelected,
            onExpandPanel: onToggleChannelPanel,
            onChannelLongPress: onEnterChannelEditMode
        )
        .frame(height: showChannelPanel ? 0 : Self.categoryRowHeight)
        .opacity(showChannelPanel ? 0 : 1)
        .clipped()
        .allowsHitTesting(!showChannelPanel)
        .animation(Self.channelAnimation, value: showChannelPanel)
    }

    @ViewBuilder
    private var feedContent: some View {
        if feedState.posts.isEmpty, feedState.isInitialLoading || feedState.isRefreshing {
            ScrollView {
                FeedGridSkeletonStatic()
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if feedState.posts.isEmpty, let error = feedState.loadError {
            ContentUnavailableView {
                Label("加载失败", systemImage: "wifi.exclamationmark")
            } description: {
                Text(error)
            } actions: {
                Button("重试") {
                    Task { await onRefresh() }
                }
                .buttonStyle(.borderedProminent)
                .tint(XhsTheme.red)
            }
            .refreshable { await onRefresh() }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if feedState.posts.isEmpty {
            ContentUnavailableView("暂无笔记", systemImage: "doc.text", description: Text("下拉刷新试试"))
                .refreshable { await onRefresh() }
        } else {
            ScrollView {
                WaterfallFeedGrid(posts: feedState.posts, spacing: 8) { post in
                    XhsProfileFeedCard(
                        post: post,
                        userLat: userLat,
                        userLng: userLng
                    ) {
                        onPostTap(post.id)
                    }
                    .onAppear {
                        if post.id == feedState.posts.last?.id {
                            Task { await onLoadMore() }
                        }
                    }
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 8)

                if feedState.isLoadingMore {
                    SkeletonLoadMoreFooter()
                } else if !feedState.hasMore {
                    FeedListEndFooter()
                }
            }
            .refreshable { await onRefresh() }
        }
    }
}

/// Discover tab shell: primary header stays fixed above channel overlay.
struct DiscoverFeedScreen: View {
    @Bindable var store: HomeStore
    var onNavigate: (HomeRoute) -> Void

    private var showFeedChannelOverlay: Bool {
        store.feedState.isChannelPanelExpanded && store.feedState.primaryTab != "关注"
    }

    var body: some View {
        VStack(spacing: 0) {
            DiscoverFeedPrimaryHeader(
                selectedTab: store.feedState.primaryTab,
                cityLabel: store.feedCityLabel,
                onTabSelected: { store.selectFeedPrimaryTab($0) },
                onMessageTap: { store.selectedTab = .messages },
                onSearchTap: { onNavigate(.search) }
            )

            ZStack(alignment: .top) {
                DiscoverFeedView(
                    feedState: store.feedState,
                    userLat: store.userLatitude,
                    userLng: store.userLongitude,
                    onRefresh: { await store.loadFeed(refresh: true) },
                    onLoadMore: { await store.loadMoreFeed() },
                    onCategoryTabSelected: { store.selectFeedTab($0) },
                    onToggleChannelPanel: { store.toggleFeedChannelPanel() },
                    onEnterChannelEditMode: { store.enterFeedChannelEditMode() },
                    onPostTap: { onNavigate(.postDetail(id: $0)) }
                )

                if showFeedChannelOverlay {
                    FeedChannelPanelOverlay(
                        feedState: store.feedState,
                        recommendedChannels: store.recommendedFeedChannels,
                        onToggleChannelEditMode: { store.toggleFeedChannelEditMode() },
                        onEnterChannelEditMode: { store.enterFeedChannelEditMode() },
                        onCollapse: { store.collapseFeedChannelPanel() },
                        onChannelClick: { store.selectFeedTab($0) },
                        onAddChannel: { store.addFeedChannel($0) },
                        onRemoveChannel: { store.removeFeedChannel($0) }
                    )
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                    .ignoresSafeArea(edges: .bottom)
                    .transition(.opacity)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .animation(XhsFeedLayout.channelPanelAnimation, value: showFeedChannelOverlay)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
