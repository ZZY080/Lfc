import SwiftUI

struct MainTabView: View {
    @Bindable var store: HomeStore
    var onNavigate: (HomeRoute) -> Void
    var onPublishTap: () -> Void
    var onOpenSideMenu: () -> Void
    var onLogout: () -> Void

    var body: some View {
        ZStack(alignment: .bottom) {
            Group {
                switch store.selectedTab {
                case .discover:
                    DiscoverFeedView(
                        feedState: store.feedState,
                        cityLabel: store.feedCityLabel,
                        recommendedChannels: store.recommendedFeedChannels,
                        userLat: store.userLatitude,
                        userLng: store.userLongitude,
                        onRefresh: { await store.loadFeed(refresh: true) },
                        onLoadMore: { await store.loadMoreFeed() },
                        onPrimaryTabSelected: { store.selectFeedPrimaryTab($0) },
                        onCategoryTabSelected: { store.selectFeedTab($0) },
                        onSearchTap: { onNavigate(.search) },
                        onMessageTap: { store.selectedTab = .messages },
                        onToggleChannelPanel: { store.toggleFeedChannelPanel() },
                        onCollapseChannelPanel: { store.collapseFeedChannelPanel() },
                        onToggleChannelEditMode: { store.toggleFeedChannelEditMode() },
                        onEnterChannelEditMode: { store.enterFeedChannelEditMode() },
                        onAddChannel: { store.addFeedChannel($0) },
                        onRemoveChannel: { store.removeFeedChannel($0) },
                        onPostTap: { onNavigate(.postDetail(id: $0)) }
                    )
                case .activity:
                    ActivityFeedView(
                        feedState: store.activityFeedState,
                        onRefresh: { await store.loadActivityFeed(refresh: true) },
                        onLoadMore: { await store.loadMoreActivityFeed() },
                        onActivityTap: { onNavigate(.activityDetail(id: $0)) }
                    )
                case .messages:
                    ConversationListView(
                        messagesState: store.messagesState,
                        unreadCount: store.unreadCount,
                        onRefresh: { await store.loadMessages(refresh: true) },
                        onLoadMore: { await store.loadMoreMessages() },
                        onConversationTap: { onNavigate(.chat(conversationId: $0)) },
                        onNotificationTap: { onNavigate(.notificationDetail(id: $0)) },
                        onViewAllNotifications: { onNavigate(.notifications) }
                    )
                case .profile:
                    ProfileTabView(
                        store: store,
                        onOpenSideMenu: onOpenSideMenu,
                        onEditProfile: { onNavigate(.editProfile) },
                        onShowQr: { onNavigate(.myQrcode) },
                        onScanProfile: { onNavigate(.profileQrScan) },
                        onPostTap: { onNavigate(.postDetail(id: $0)) },
                        onActivityTap: { onNavigate(.activityDetail(id: $0)) },
                        onBindAlipay: { onNavigate(.settings) }
                    )
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .safeAreaInset(edge: .bottom, spacing: 0) {
                Color.clear.frame(height: 56)
            }

            XhsBottomBar(
                selectedTab: store.selectedTab,
                unreadCount: store.unreadCount,
                onTabSelected: { store.selectedTab = $0 },
                onPublishTap: onPublishTap
            )
        }
        .background(XhsTheme.background)
        .onChange(of: store.selectedTab) { _, tab in
            if tab == .messages {
                Task { await store.loadMessages(refresh: true) }
            }
            if tab == .activity, !store.activityFeedState.hasLoadedOnce {
                Task { await store.loadActivityFeed(refresh: true) }
            }
            if tab == .profile {
                Task { await store.ensureMyProfileReady() }
            }
        }
    }
}

private struct XhsBottomBar: View {
    let selectedTab: HomeTab
    let unreadCount: Int
    let onTabSelected: (HomeTab) -> Void
    let onPublishTap: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Divider().overlay(XhsTheme.divider)
            HStack(alignment: .center, spacing: 0) {
                tabButton(.discover)
                tabButton(.activity)
                publishButton
                tabButton(.messages, badge: unreadCount)
                tabButton(.profile)
            }
            .padding(.top, 6)
            .padding(.bottom, 4)
            .background(
                selectedTab == .profile
                    ? Color.white.opacity(0.94)
                    : Color.white
            )
        }
    }

    private func tabButton(_ tab: HomeTab, badge: Int = 0) -> some View {
        Button {
            onTabSelected(tab)
        } label: {
            VStack(spacing: 2) {
                ZStack(alignment: .topTrailing) {
                    Image(systemName: tab.systemImage)
                        .font(.system(size: 22))
                    if badge > 0 {
                        Text(badge > 99 ? "99+" : "\(badge)")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 4)
                            .padding(.vertical, 1)
                            .background(XhsTheme.red)
                            .clipShape(Capsule())
                            .offset(x: 10, y: -6)
                    }
                }
                Text(tab.title)
                    .font(.system(size: 11))
            }
            .foregroundStyle(selectedTab == tab ? XhsTheme.textPrimary : XhsTheme.textSecondary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 4)
        }
        .buttonStyle(.plain)
    }

    private var publishButton: some View {
        Button(action: onPublishTap) {
            Image(systemName: "plus")
                .font(.system(size: 22, weight: .bold))
                .foregroundStyle(.white)
                .frame(width: 48, height: 48)
                .background(
                    LinearGradient(
                        colors: [XhsTheme.red, Color(red: 1, green: 0.45, blue: 0.38)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .shadow(color: XhsTheme.red.opacity(0.35), radius: 6, y: 3)
                .offset(y: -2)
        }
        .buttonStyle(.plain)
        .frame(maxWidth: .infinity)
        .frame(height: 52)
    }
}
