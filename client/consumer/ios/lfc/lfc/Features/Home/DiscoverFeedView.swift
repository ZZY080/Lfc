import SwiftUI

struct DiscoverFeedView: View {
    let feedState: FeedUiState
    let cityLabel: String
    let recommendedChannels: [String]
    var userLat: Double?
    var userLng: Double?
    let onRefresh: () async -> Void
    let onLoadMore: () async -> Void
    let onPrimaryTabSelected: (String) -> Void
    let onCategoryTabSelected: (String) -> Void
    let onSearchTap: () -> Void
    let onMessageTap: () -> Void
    let onToggleChannelPanel: () -> Void
    let onCollapseChannelPanel: () -> Void
    let onToggleChannelEditMode: () -> Void
    let onEnterChannelEditMode: () -> Void
    let onAddChannel: (String) -> Void
    let onRemoveChannel: (String) -> Void
    let onPostTap: (Int) -> Void

    private var isFollowingTab: Bool {
        feedState.primaryTab == "关注"
    }

    private var showChannelPanel: Bool {
        feedState.isChannelPanelExpanded && !isFollowingTab
    }

    var body: some View {
        VStack(spacing: 0) {
            header
            if showChannelPanel {
                channelPanel
            } else {
                feedContent
            }
        }
        .background(XhsTheme.background)
    }

    private var header: some View {
        VStack(spacing: 0) {
            XhsFeedPrimaryTabRow(
                selectedTab: feedState.primaryTab,
                cityLabel: cityLabel,
                onTabSelected: onPrimaryTabSelected,
                onMessageTap: onMessageTap,
                onSearchTap: onSearchTap
            )
            .padding(.top, 2)
            .padding(.bottom, 2)

            if !isFollowingTab {
                XhsFeedCategoryTabRow(
                    myChannels: feedState.effectiveMyChannels,
                    selectedTab: feedState.selectedTab,
                    isPanelExpanded: feedState.isChannelPanelExpanded,
                    onTabSelected: onCategoryTabSelected,
                    onExpandPanel: onToggleChannelPanel,
                    onChannelLongPress: onEnterChannelEditMode
                )
            }
        }
        .background(Color.white)
    }

    private var channelPanel: some View {
        ScrollView {
            XhsFeedChannelPanel(
                myChannels: feedState.effectiveMyChannels,
                recommendedChannels: recommendedChannels,
                isEditMode: feedState.isChannelEditMode,
                onToggleEditMode: onToggleChannelEditMode,
                onEnterEditMode: onEnterChannelEditMode,
                onCollapse: onCollapseChannelPanel,
                onChannelClick: onCategoryTabSelected,
                onAddChannel: onAddChannel,
                onRemoveChannel: onRemoveChannel
            )
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
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
