import SwiftUI

// MARK: - Shared navigation chrome

struct HomeStubNavigationBar: View {
    let title: String
    let onBack: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(XhsTheme.textPrimary)
                    .frame(width: 36, height: 36)
            }
            Text(title)
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(XhsTheme.textPrimary)
            Spacer()
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 6)
        .background(Color.white)
        .overlay(alignment: .bottom) {
            Divider().overlay(XhsTheme.divider)
        }
    }
}

private struct StubScreen<Content: View>: View {
    let title: String
    let onBack: () -> Void
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(spacing: 0) {
            HomeStubNavigationBar(title: title, onBack: onBack)
            content()
        }
        .background(Color.white)
    }
}

// MARK: - Notifications & chat

struct NotificationListView: View {
    @Bindable var store: HomeStore
    let onBack: () -> Void
    let onNotificationTap: (Int) -> Void

    var body: some View {
        StubScreen(title: "通知", onBack: onBack) {
            if store.notificationFeedState.isInitialLoading,
               store.notificationFeedState.notifications.isEmpty {
                NotificationListSkeleton()
            } else if store.notificationFeedState.notifications.isEmpty {
                ContentUnavailableView("暂无通知", systemImage: "bell")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List {
                    ForEach(store.notificationFeedState.notifications) { item in
                        Button(item.title) { onNotificationTap(item.id) }
                    }
                    if store.notificationFeedState.isLoadingMore {
                        SkeletonLoadMoreFooter()
                            .listRowSeparator(.hidden)
                    }
                }
                .refreshable { await store.loadNotificationFeed(refresh: true) }
            }
        }
        .task { await store.loadNotificationFeed(refresh: true) }
    }
}

struct NotificationDetailView: View {
    let notification: NotificationDto?
    let isLoading: Bool
    let onBack: () -> Void
    let onActivityTap: (Int) -> Void

    var body: some View {
        StubScreen(title: "通知详情", onBack: onBack) {
            if isLoading {
                MessageDetailSkeleton()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let notification {
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(notification.title)
                            .font(.headline)
                        Text(notification.content)
                            .foregroundStyle(XhsTheme.textSecondary)
                        if notification.relatedType == "ACTIVITY", let id = notification.relatedId {
                            Button("查看活动") { onActivityTap(id) }
                                .foregroundStyle(XhsTheme.red)
                        }
                    }
                    .padding()
                }
            } else {
                ContentUnavailableView("通知不存在", systemImage: "bell.slash")
            }
        }
    }
}

