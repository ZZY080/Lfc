import SwiftUI

struct ConversationListView: View {
    let messagesState: MessagesUiState
    let unreadCount: Int
    let onRefresh: () async -> Void
    let onLoadMore: () async -> Void
    let onConversationTap: (Int) -> Void
    let onNotificationTap: (Int) -> Void
    let onViewAllNotifications: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Text("消息")
                .font(.system(size: 20, weight: .bold))
                .foregroundStyle(XhsTheme.red)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(Color.white)

            content
        }
        .background(XhsTheme.background)
        .task {
            if !messagesState.hasLoadedOnce {
                await onRefresh()
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        if messagesState.isInitialLoading, messagesState.conversations.isEmpty {
            ConversationListSkeleton()
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        } else {
            ScrollView {
                VStack(spacing: 0) {
                    notificationSection
                    conversationSection
                }
                .id(messagesState.listResetNonce)
            }
            .refreshable { await onRefresh() }
        }
    }

    private var notificationSection: some View {
        VStack(spacing: 0) {
            HStack {
                Text("通知")
                    .font(.system(size: 14, weight: .semibold))
                if messagesState.notificationUnreadCount > 0 {
                    Text("\(messagesState.notificationUnreadCount)条未读")
                        .font(.system(size: 11))
                        .foregroundStyle(XhsTheme.red)
                }
                Spacer()
                Button("查看全部", action: onViewAllNotifications)
                    .font(.system(size: 12))
                    .foregroundStyle(XhsTheme.textSecondary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)

            if messagesState.notificationPreview.isEmpty {
                Text("暂无通知")
                    .font(.system(size: 13))
                    .foregroundStyle(XhsTheme.textSecondary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
            } else {
                ForEach(messagesState.notificationPreview) { notification in
                    NotificationPreviewRow(notification: notification) {
                        onNotificationTap(notification.id)
                    }
                    Divider().padding(.leading, 16)
                }
            }
        }
        .background(Color.white)
        .padding(.bottom, 8)
    }

    private var conversationSection: some View {
        VStack(spacing: 0) {
            if messagesState.conversations.isEmpty {
                ContentUnavailableView("暂无会话", systemImage: "message", description: Text("和同学们聊聊吧"))
                    .padding(.top, 40)
            } else {
                ForEach(messagesState.conversations) { conversation in
                    ConversationRow(conversation: conversation) {
                        onConversationTap(conversation.id)
                    }
                    .onAppear {
                        if conversation.id == messagesState.conversations.last?.id {
                            Task { await onLoadMore() }
                        }
                    }
                    Divider().padding(.leading, 72)
                }
            }

            if messagesState.isLoadingMore {
                SkeletonLoadMoreFooter()
            }
        }
        .background(Color.white)
    }
}

private struct NotificationPreviewRow: View {
    let notification: NotificationDto
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                Circle()
                    .fill(notification.isRead ? Color.gray.opacity(0.2) : XhsTheme.red.opacity(0.15))
                    .frame(width: 40, height: 40)
                    .overlay {
                        Image(systemName: "bell.fill")
                            .foregroundStyle(notification.isRead ? XhsTheme.textSecondary : XhsTheme.red)
                    }
                VStack(alignment: .leading, spacing: 4) {
                    Text(notification.title)
                        .font(.system(size: 14, weight: notification.isRead ? .regular : .semibold))
                        .foregroundStyle(XhsTheme.textPrimary)
                        .lineLimit(1)
                    Text(notification.content)
                        .font(.system(size: 12))
                        .foregroundStyle(XhsTheme.textSecondary)
                        .lineLimit(1)
                }
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
        }
        .buttonStyle(.plain)
    }
}

private struct ConversationRow: View {
    let conversation: ConversationDto
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                avatar
                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Text(conversation.peerDisplayName)
                            .font(.system(size: 15, weight: .medium))
                            .foregroundStyle(XhsTheme.textPrimary)
                        Spacer()
                        if let time = conversation.lastMessageAt {
                            Text(String(time.prefix(16)).replacingOccurrences(of: "T", with: " "))
                                .font(.system(size: 11))
                                .foregroundStyle(XhsTheme.textSecondary)
                        }
                    }
                    HStack {
                        Text(conversation.lastMessageContent ?? "暂无消息")
                            .font(.system(size: 13))
                            .foregroundStyle(XhsTheme.textSecondary)
                            .lineLimit(1)
                        Spacer()
                        if conversation.unreadCount > 0 {
                            Text("\(conversation.unreadCount)")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundStyle(.white)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(XhsTheme.red)
                                .clipShape(Capsule())
                        }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private var avatar: some View {
        if let urlString = conversation.peerAvatarUrl, let url = URL(string: urlString) {
            AsyncImage(url: url) { image in
                image.resizable().scaledToFill()
            } placeholder: {
                avatarPlaceholder
            }
            .frame(width: 48, height: 48)
            .clipShape(Circle())
        } else {
            avatarPlaceholder
        }
    }

    private var avatarPlaceholder: some View {
        Circle()
            .fill(XhsTheme.red.opacity(0.12))
            .frame(width: 48, height: 48)
            .overlay {
                Text(String(conversation.peerDisplayName.prefix(1)))
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(XhsTheme.red)
            }
    }
}
