import SwiftUI

struct ChatView: View {
    @Bindable var store: HomeStore
    let currentUserId: Int?
    let onBack: () -> Void
    let onPostTap: (Int) -> Void
    let onActivityTap: (Int) -> Void
    let onProductTap: (Int) -> Void

    @State private var messageInput = ""
    @FocusState private var inputFocused: Bool

    private var conversation: ConversationDto? { store.selectedConversation }
    private var messages: [ChatMessageDto] { store.chatMessages }
    private var isLoading: Bool { store.isChatLoading }

    var body: some View {
        VStack(spacing: 0) {
            chatHeader

            if isLoading, messages.isEmpty {
                ChatMessageListSkeleton()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if messages.isEmpty {
                ContentUnavailableView("暂无消息", systemImage: "message", description: Text("发送第一条消息吧"))
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                messageList
            }

            composer
        }
        .background(XhsTheme.background)
    }

    private var chatHeader: some View {
        HStack(spacing: 12) {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(XhsTheme.textPrimary)
                    .frame(width: 36, height: 36)
            }
            DetailRemoteImage(urlString: conversation?.peerAvatarUrl, cornerRadius: 16)
                .frame(width: 32, height: 32)
                .clipShape(Circle())
            Text(conversation?.peerDisplayName ?? "聊天")
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(XhsTheme.textPrimary)
            Spacer()
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 6)
        .background(Color.white)
        .overlay(alignment: .bottom) { Divider().overlay(XhsTheme.divider) }
    }

    private var messageList: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 10) {
                    ForEach(messages) { message in
                        ChatBubbleRow(
                            message: message,
                            isMine: message.senderId == currentUserId,
                            onPostTap: onPostTap,
                            onActivityTap: onActivityTap,
                            onProductTap: onProductTap
                        )
                        .id(message.id)
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
            }
            .onChange(of: messages.count) { _, _ in
                if let last = messages.last {
                    withAnimation { proxy.scrollTo(last.id, anchor: .bottom) }
                }
            }
            .onAppear {
                if let last = messages.last {
                    proxy.scrollTo(last.id, anchor: .bottom)
                }
            }
        }
    }

    private var composer: some View {
        HStack(spacing: 10) {
            TextField("输入消息…", text: $messageInput, axis: .vertical)
                .lineLimit(1...4)
                .textFieldStyle(.plain)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                .focused($inputFocused)

            Button(store.isChatSending ? "发送中" : "发送") {
                sendMessage()
            }
            .font(.system(size: 14, weight: .semibold))
            .foregroundStyle(XhsTheme.red)
            .disabled(
                messageInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || store.isChatSending
            )
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(Color(red: 0.96, green: 0.96, blue: 0.96))
    }

    private func sendMessage() {
        guard let conversationId = conversation?.id else { return }
        let content = messageInput
        messageInput = ""
        inputFocused = false
        Task { await store.sendChatMessage(conversationId: conversationId, content: content) }
    }
}

private struct ChatBubbleRow: View {
    let message: ChatMessageDto
    let isMine: Bool
    let onPostTap: (Int) -> Void
    let onActivityTap: (Int) -> Void
    let onProductTap: (Int) -> Void

    var body: some View {
        HStack(alignment: .bottom, spacing: 8) {
            if isMine { Spacer(minLength: 48) }

            if !isMine {
                DetailRemoteImage(urlString: message.sender?.avatarUrl, cornerRadius: 14)
                    .frame(width: 28, height: 28)
                    .clipShape(Circle())
            }

            VStack(alignment: isMine ? .trailing : .leading, spacing: 4) {
                if !isMine {
                    Text(message.sender?.displayName ?? "用户")
                        .font(.system(size: 11))
                        .foregroundStyle(XhsTheme.textSecondary)
                }
                bubbleContent
                Text(formatDateTimeLabel(message.createdAt))
                    .font(.system(size: 10))
                    .foregroundStyle(XhsTheme.textSecondary)
            }

            if !isMine { Spacer(minLength: 48) }
        }
    }

    @ViewBuilder
    private var bubbleContent: some View {
        let type = message.messageType.uppercased()
        if type == "PRODUCT", let payload = decodeProductPayload(message.content) {
            Button { onProductTap(payload.postId) } label: {
                HStack(spacing: 10) {
                    DetailRemoteImage(urlString: payload.coverUrl, cornerRadius: 8)
                        .frame(width: 52, height: 52)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(payload.title)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(XhsTheme.textPrimary)
                            .lineLimit(2)
                        Text(formatPriceYuan(payload.price))
                            .font(.system(size: 14, weight: .bold))
                            .foregroundStyle(XhsTheme.red)
                    }
                    Spacer()
                }
                .padding(10)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            .buttonStyle(.plain)
        } else if type == "IMAGE" {
            DetailRemoteImage(urlString: message.content, cornerRadius: 12)
                .frame(maxWidth: 200, maxHeight: 200)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        } else {
            Text(message.content)
                .font(.system(size: 15))
                .foregroundStyle(isMine ? .white : XhsTheme.textPrimary)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(isMine ? XhsTheme.red : Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
    }

    private func decodeProductPayload(_ content: String) -> ChatProductPayload? {
        guard let data = content.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(ChatProductPayload.self, from: data)
    }
}
