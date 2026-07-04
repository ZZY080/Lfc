import PhotosUI
import SwiftUI
import UIKit

// MARK: - Content helpers

func postDisplayTitle(_ post: PostDto) -> String? {
    let title = post.title.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !title.isEmpty else { return nil }
    if ["图片笔记", "校园笔记"].contains(title) { return nil }
    let contentPrefix = post.content.trimmingCharacters(in: .whitespacesAndNewlines).prefix(30)
    if title == contentPrefix { return nil }
    return title
}

func postDisplayBody(_ post: PostDto) -> String {
    post.content.trimmingCharacters(in: .whitespacesAndNewlines)
}

func nextPostCommentSort(_ current: String) -> String {
    current == "newest" ? "default" : "newest"
}

func formatXhsTime(_ iso: String) -> String {
    iso.replacingOccurrences(of: "T", with: " ").prefix(19).description
}

func formatRelativeTime(_ iso: String) -> String {
    let formatter = ISO8601DateFormatter()
    formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    var date = formatter.date(from: iso)
    if date == nil {
        formatter.formatOptions = [.withInternetDateTime]
        date = formatter.date(from: iso)
    }
    if date == nil {
        let fallback = DateFormatter()
        fallback.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
        fallback.locale = Locale(identifier: "en_US_POSIX")
        date = fallback.date(from: String(iso.prefix(19)))
    }
    guard let date else { return formatXhsTime(iso).prefix(16).description }

    let minutes = Int(Date().timeIntervalSince(date) / 60)
    if minutes < 1 { return "刚刚" }
    if minutes < 60 { return "\(minutes)分钟前" }
    if minutes < 60 * 24 { return "\(minutes / 60)小时前" }
    if minutes < 60 * 24 * 7 { return "\(minutes / (60 * 24))天前" }

    let display = DateFormatter()
    display.dateFormat = "yyyy-MM-dd"
    return display.string(from: date)
}

// MARK: - Image carousel

struct XhsDetailImageCarousel: View {
    let images: [String]
    var aspectRatio: CGFloat = 1

    @State private var selectedIndex = 0
    @State private var previewIndex: Int?

    var body: some View {
        if images.isEmpty { EmptyView() } else {
            GeometryReader { geometry in
                let height = geometry.size.width / aspectRatio
                ZStack(alignment: .bottom) {
                    TabView(selection: $selectedIndex) {
                        ForEach(Array(images.enumerated()), id: \.offset) { index, url in
                            DetailRemoteImage(urlString: url, cornerRadius: 0)
                                .frame(width: geometry.size.width, height: height)
                                .clipped()
                                .tag(index)
                                .onTapGesture { previewIndex = index }
                        }
                    }
                    .tabViewStyle(.page(indexDisplayMode: .never))
                    .frame(width: geometry.size.width, height: height)

                    if images.count > 1 {
                        LinearGradient(
                            colors: [.clear, .black.opacity(0.35)],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                        .frame(height: 48)
                        .allowsHitTesting(false)

                        HStack(spacing: 5) {
                            ForEach(0 ..< images.count, id: \.self) { index in
                                Circle()
                                    .fill(index == selectedIndex ? Color.white : Color.white.opacity(0.45))
                                    .frame(width: index == selectedIndex ? 6 : 5, height: index == selectedIndex ? 6 : 5)
                            }
                        }
                        .padding(.bottom, 14)
                    }
                }
            }
            .aspectRatio(aspectRatio, contentMode: .fit)
            .fullScreenCover(item: Binding(
                get: { previewIndex.map { PreviewImageIndex(value: $0) } },
                set: { previewIndex = $0?.value }
            )) { item in
                XhsImagePreviewDialog(
                    images: images,
                    startIndex: item.value,
                    onDismiss: { previewIndex = nil }
                )
            }
        }
    }
}

private struct PreviewImageIndex: Identifiable {
    let value: Int
    var id: Int { value }
}

struct XhsImagePreviewDialog: View {
    let images: [String]
    let startIndex: Int
    let onDismiss: () -> Void

    @State private var selectedIndex: Int

    init(images: [String], startIndex: Int, onDismiss: @escaping () -> Void) {
        self.images = images
        self.startIndex = startIndex
        self.onDismiss = onDismiss
        _selectedIndex = State(initialValue: startIndex)
    }

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            TabView(selection: $selectedIndex) {
                ForEach(Array(images.enumerated()), id: \.offset) { index, url in
                    DetailRemoteImage(urlString: url, cornerRadius: 0)
                        .scaledToFit()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .tag(index)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))

            VStack {
                HStack {
                    Spacer()
                    Button(action: onDismiss) {
                        Image(systemName: "xmark")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundStyle(.white)
                            .frame(width: 40, height: 40)
                    }
                }
                .padding(.horizontal, 8)
                .padding(.top, 8)

                Spacer()

                if images.count > 1 {
                    Text("\(selectedIndex + 1)/\(images.count)")
                        .font(.system(size: 14))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(Color.black.opacity(0.45))
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                        .padding(.bottom, 32)
                }
            }
        }
    }
}

// MARK: - Author header

struct XhsDetailAuthorHeader<Actions: View>: View {
    let authorLabel: String
    let avatarUrl: String?
    let onBack: () -> Void
    let onAuthorTap: () -> Void
    @ViewBuilder let actions: () -> Actions

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundStyle(XhsTheme.textPrimary)
                        .frame(width: 40, height: 40)
                }
                .buttonStyle(.plain)

                Button(action: onAuthorTap) {
                    HStack(spacing: 8) {
                        XhsProfileAvatar(label: authorLabel, size: 32, avatarUrl: avatarUrl)
                        Text(authorLabel)
                            .font(.system(size: 15, weight: .bold))
                            .foregroundStyle(XhsTheme.textPrimary)
                            .lineLimit(1)
                    }
                    .padding(.horizontal, 4)
                    .padding(.vertical, 4)
                }
                .buttonStyle(.plain)

                Spacer(minLength: 8)
                actions()
            }
            .padding(.horizontal, 4)
            .padding(.vertical, 6)
            .background(Color.white)

            Divider().overlay(Color(red: 0.94, green: 0.94, blue: 0.94))
        }
    }
}

struct XhsDetailFollowButton: View {
    let isFollowing: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(isFollowing ? "已关注" : "关注")
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(isFollowing ? XhsTheme.textSecondary : .white)
                .padding(.horizontal, 14)
                .padding(.vertical, 5)
                .background(isFollowing ? Color.clear : XhsTheme.red)
                .overlay {
                    if isFollowing {
                        Capsule().stroke(Color(red: 0.87, green: 0.87, blue: 0.87), lineWidth: 1)
                    }
                }
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
        .padding(.trailing, 8)
    }
}

// MARK: - Content section

struct XhsPostDetailContentSection: View {
    let post: PostDto

    var body: some View {
        let title = postDisplayTitle(post)
        let body = postDisplayBody(post)
        let hasImages = !(post.images?.isEmpty ?? true)

        VStack(alignment: .leading, spacing: 0) {
            if let title {
                Text(title)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(XhsTheme.textPrimary)
                    .lineSpacing(6)
                Spacer().frame(height: 10)
            }

            if !body.isEmpty {
                Text(body)
                    .font(.system(size: 16))
                    .foregroundStyle(XhsTheme.textPrimary)
                    .lineSpacing(8)
                Spacer().frame(height: 14)
            }

            Text("发布于 \(formatXhsTime(post.createdAt))")
                .font(.system(size: 12))
                .foregroundStyle(XhsTheme.textSecondary)

            if let location = post.location?.trimmingCharacters(in: .whitespacesAndNewlines), !location.isEmpty {
                HStack(spacing: 4) {
                    Image(systemName: "location.fill")
                        .font(.system(size: 11))
                    Text(location)
                        .font(.system(size: 12))
                        .lineLimit(2)
                }
                .foregroundStyle(XhsTheme.textSecondary)
                .padding(.top, 6)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.top, hasImages ? 16 : 12)
        .padding(.bottom, 12)
    }
}

// MARK: - Product card

struct XhsPostProductLinkCard: View {
    let post: PostDto
    let product: PostProductDto
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 20) {
                DetailRemoteImage(urlString: post.images?.first, cornerRadius: 10)
                    .frame(width: 56, height: 56)
                VStack(alignment: .leading, spacing: 8) {
                    Text(post.productDisplayTitle())
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(XhsTheme.textPrimary)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                    Text(formatPriceYuan(product.price))
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(XhsTheme.red)
                }
                Spacer(minLength: 20)
                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color(red: 0.82, green: 0.82, blue: 0.82))
                    .padding(.leading, 8)
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 14)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .shadow(color: .black.opacity(0.04), radius: 4, y: 1)
        }
        .buttonStyle(.plain)
        .padding(.horizontal, 16)
        .padding(.top, 12)
        .padding(.bottom, 12)
    }
}

// MARK: - Comments

private enum XhsCommentLayout {
    static let rootAvatarSize: CGFloat = 36
    static let replyAvatarSize: CGFloat = 26
    static let avatarSpacing: CGFloat = 10
    static let horizontalPadding: CGFloat = 16
    /// 子回复头像左缘与主评论文字左缘对齐
    static let replyIndent: CGFloat = rootAvatarSize + avatarSpacing
}

struct XhsPostCommentsHeader: View {
    let commentCount: Int
    let isNewestSort: Bool
    let onSortToggle: () -> Void

    var body: some View {
        HStack {
            Text("共 \(commentCount) 条评论")
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(XhsTheme.textPrimary)
            Spacer()
            Button(action: onSortToggle) {
                HStack(spacing: 4) {
                    if isNewestSort {
                        Text("最新")
                            .font(.system(size: 12))
                            .foregroundStyle(XhsTheme.red)
                    }
                    Image(systemName: "arrow.up.arrow.down")
                        .font(.system(size: 14))
                        .foregroundStyle(isNewestSort ? XhsTheme.red : XhsTheme.textSecondary)
                }
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 16)
        .padding(.top, 16)
        .padding(.bottom, 14)
        .background(Color.white)
    }
}

struct XhsPostQuickCommentTrigger: View {
    let userLabel: String
    let userAvatarUrl: String?
    let onTap: () -> Void
    var onImageTap: (() -> Void)?

    var body: some View {
        HStack(spacing: 10) {
            XhsProfileAvatar(label: userLabel, size: 32, avatarUrl: userAvatarUrl)
            HStack {
                Button(action: onTap) {
                    Text("有话要说，快来评论")
                        .font(.system(size: 14))
                        .foregroundStyle(Color(red: 0.67, green: 0.67, blue: 0.67))
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .buttonStyle(.plain)

                if let onImageTap {
                    Button(action: onImageTap) {
                        Image(systemName: "photo")
                            .font(.system(size: 16))
                            .foregroundStyle(Color(red: 0.73, green: 0.73, blue: 0.73))
                    }
                    .buttonStyle(.plain)
                } else {
                    Image(systemName: "photo")
                        .font(.system(size: 16))
                        .foregroundStyle(Color(red: 0.73, green: 0.73, blue: 0.73))
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(Color(red: 0.96, green: 0.96, blue: 0.96))
            .clipShape(Capsule())
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(Color.white)
    }
}

struct XhsPostCommentsEmptyState: View {
    var body: some View {
        VStack(spacing: 10) {
            Image(systemName: "bubble.left")
                .font(.system(size: 32))
                .foregroundStyle(Color(red: 0.88, green: 0.88, blue: 0.88))
            Text("还没有评论")
                .font(.system(size: 14))
                .foregroundStyle(XhsTheme.textSecondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 32)
        .background(Color.white)
    }
}

struct XhsPostCommentThreadView: View {
    let root: PostCommentDto
    let postAuthorId: Int
    let isFirstComment: Bool
    let isLoadingReplies: Bool
    let hasMoreReplies: Bool
    let hiddenReplyCount: Int
    let onReply: (PostCommentDto) -> Void
    let onLike: (PostCommentDto) -> Void
    let onLoadMoreReplies: () -> Void

    private var commentIndex: [Int: PostCommentDto] {
        var index = [root.id: root]
        for reply in root.previewReplies ?? [] {
            index[reply.id] = reply
        }
        return index
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            XhsPostCommentRow(
                comment: root,
                postAuthorId: postAuthorId,
                parentComment: nil,
                avatarSize: XhsCommentLayout.rootAvatarSize,
                showFirstBadge: isFirstComment,
                onReply: { onReply(root) },
                onLike: { onLike(root) }
            )

            ForEach(root.previewReplies ?? []) { reply in
                XhsPostCommentRow(
                    comment: reply,
                    postAuthorId: postAuthorId,
                    parentComment: reply.parentId.flatMap { commentIndex[$0] } ?? root,
                    avatarSize: XhsCommentLayout.replyAvatarSize,
                    showFirstBadge: false,
                    onReply: { onReply(reply) },
                    onLike: { onLike(reply) }
                )
                .padding(.leading, XhsCommentLayout.replyIndent)
                .padding(.top, 10)
            }

            if isLoadingReplies {
                ProgressView()
                    .scaleEffect(0.8)
                    .padding(.leading, XhsCommentLayout.replyIndent)
                    .padding(.top, 10)
            } else if hasMoreReplies, hiddenReplyCount > 0 {
                Button("展开 \(hiddenReplyCount) 条回复", action: onLoadMoreReplies)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(Color(red: 0.36, green: 0.49, blue: 0.6))
                    .padding(.leading, XhsCommentLayout.replyIndent)
                    .padding(.top, 10)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, XhsCommentLayout.horizontalPadding)
        .padding(.vertical, 10)
        .background(Color.white)
    }
}

struct XhsPostCommentRow: View {
    let comment: PostCommentDto
    let postAuthorId: Int
    let parentComment: PostCommentDto?
    let avatarSize: CGFloat
    let showFirstBadge: Bool
    let onReply: () -> Void
    let onLike: () -> Void

    private var authorLabel: String {
        comment.author?.displayName ?? "同学\(comment.userId)"
    }

    private var isAuthor: Bool {
        comment.userId == postAuthorId
    }

    private var isReply: Bool {
        parentComment != nil
    }

    private var parsedContent: ParsedCommentContent {
        CommentContentHelper.parse(comment.content)
    }

    private var displayContent: String {
        parsedContent.text.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var commentImageUrl: String? {
        parsedContent.imageUrl
    }

    private var replyTargetName: String? {
        guard let parentComment else { return nil }
        return parentComment.author?.displayName ?? "同学\(parentComment.userId)"
    }

    var body: some View {
        HStack(alignment: .top, spacing: XhsCommentLayout.avatarSpacing) {
            XhsProfileAvatar(label: authorLabel, size: avatarSize, avatarUrl: comment.author?.avatarUrl)
                .frame(width: avatarSize, height: avatarSize)

            VStack(alignment: .leading, spacing: 6) {
                if !isReply {
                    HStack(spacing: 6) {
                        Text(authorLabel)
                            .font(.system(size: 13, weight: isAuthor ? .semibold : .regular))
                            .foregroundStyle(isAuthor ? XhsTheme.textPrimary : XhsTheme.textSecondary)
                            .lineLimit(1)
                        if isAuthor {
                            Text("作者")
                                .font(.system(size: 9, weight: .semibold))
                                .foregroundStyle(XhsTheme.red)
                                .padding(.horizontal, 4)
                                .padding(.vertical, 1)
                                .background(Color(red: 1, green: 0.93, blue: 0.93))
                                .clipShape(RoundedRectangle(cornerRadius: 3, style: .continuous))
                        }
                        Spacer(minLength: 0)
                    }
                }

                if !displayContent.isEmpty, displayContent != "[图片]" {
                    if isReply, let replyTargetName {
                        replyContentText(targetName: replyTargetName, content: displayContent)
                    } else {
                        Text(displayContent)
                            .font(.system(size: 15))
                            .foregroundStyle(XhsTheme.textPrimary)
                            .lineSpacing(4)
                            .fixedSize(horizontal: false, vertical: true)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }

                if let commentImageUrl {
                    DetailRemoteImage(urlString: commentImageUrl, cornerRadius: 8)
                        .frame(width: 120, height: 120)
                        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                }

                HStack(spacing: 12) {
                    Text(formatRelativeTime(comment.createdAt))
                        .font(.system(size: 11))
                        .foregroundStyle(Color(red: 0.6, green: 0.6, blue: 0.6))
                    Button("回复", action: onReply)
                        .font(.system(size: 12))
                        .foregroundStyle(Color(red: 0.6, green: 0.6, blue: 0.6))
                    if showFirstBadge {
                        Text("首评")
                            .font(.system(size: 10))
                            .foregroundStyle(XhsTheme.textSecondary)
                            .padding(.horizontal, 5)
                            .padding(.vertical, 1)
                            .background(Color(red: 0.94, green: 0.94, blue: 0.94))
                            .clipShape(RoundedRectangle(cornerRadius: 4, style: .continuous))
                    }
                    Spacer(minLength: 0)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            VStack(spacing: 2) {
                Button(action: onLike) {
                    Image(systemName: comment.isLiked ? "heart.fill" : "heart")
                        .font(.system(size: 16))
                        .foregroundStyle(comment.isLiked ? XhsTheme.red : Color(red: 0.8, green: 0.8, blue: 0.8))
                }
                .buttonStyle(.plain)
                if comment.likeCount > 0 {
                    Text(comment.likeCount > 99 ? "99+" : "\(comment.likeCount)")
                        .font(.system(size: 10))
                        .foregroundStyle(comment.isLiked ? XhsTheme.red : Color(red: 0.6, green: 0.6, blue: 0.6))
                }
            }
            .padding(.top, isReply ? 0 : 1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func replyContentText(targetName: String, content: String) -> some View {
        (
            Text("回复 ")
                .foregroundStyle(XhsTheme.textSecondary)
            + Text(targetName)
                .foregroundStyle(XhsTheme.textSecondary)
            + Text("：")
                .foregroundStyle(XhsTheme.textSecondary)
            + Text(content)
                .foregroundStyle(XhsTheme.textPrimary)
        )
        .font(.system(size: 15))
        .lineSpacing(4)
        .fixedSize(horizontal: false, vertical: true)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

// MARK: - Bottom bar & composer

struct XhsPostDetailInteractionBar: View {
    let likeCount: Int
    let favoriteCount: Int
    let commentCount: Int
    let isLiked: Bool
    let isFavorited: Bool
    let isSubmitting: Bool
    let onCommentTap: () -> Void
    let onLike: () -> Void
    let onFavorite: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Divider().overlay(Color(red: 0.94, green: 0.94, blue: 0.94))
            HStack(spacing: 4) {
                Button(action: onCommentTap) {
                    HStack(spacing: 8) {
                        Image(systemName: "pencil")
                            .font(.system(size: 14))
                            .foregroundStyle(Color(red: 0.69, green: 0.69, blue: 0.69))
                        Text("说点什么...")
                            .font(.system(size: 14))
                            .foregroundStyle(Color(red: 0.67, green: 0.67, blue: 0.67))
                        Spacer()
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(Color(red: 0.96, green: 0.96, blue: 0.96))
                    .clipShape(Capsule())
                }
                .buttonStyle(.plain)

                barAction(
                    icon: isLiked ? "heart.fill" : "heart",
                    caption: likeCount > 0 ? detailBarCount(likeCount) : "点赞",
                    tint: isLiked ? XhsTheme.red : XhsTheme.textPrimary,
                    action: onLike
                )
                barAction(
                    icon: isFavorited ? "star.fill" : "star",
                    caption: favoriteCount > 0 ? detailBarCount(favoriteCount) : "收藏",
                    tint: isFavorited ? Color(red: 1, green: 0.72, blue: 0) : XhsTheme.textPrimary,
                    action: onFavorite
                )
                barAction(
                    icon: "bubble.right",
                    caption: commentCount > 0 ? detailBarCount(commentCount) : nil,
                    tint: XhsTheme.textPrimary,
                    action: onCommentTap
                )
            }
            .padding(.horizontal, 12)
            .padding(.top, 8)
            .padding(.bottom, 8)
            .background(Color.white)
        }
        .disabled(isSubmitting)
        .opacity(isSubmitting ? 0.7 : 1)
    }

    private func barAction(icon: String, caption: String?, tint: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.system(size: 20))
                    .foregroundStyle(tint)
                if let caption {
                    Text(caption)
                        .font(.system(size: 12))
                        .foregroundStyle(XhsTheme.textPrimary)
                }
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
        }
        .buttonStyle(.plain)
    }

    private func detailBarCount(_ count: Int) -> String {
        count > 99 ? "99+" : "\(count)"
    }
}

struct XhsPostCommentScrim: View {
    let onDismiss: () -> Void

    var body: some View {
        Color.black.opacity(0.45)
            .ignoresSafeArea()
            .onTapGesture(perform: onDismiss)
    }
}

struct XhsPostCommentExpandedPanel: View {
    let replyLabel: String?
    @Binding var text: String
    @Binding var pendingImage: PendingCommentImage?
    @Binding var pendingImagePreview: UIImage?
    var requestPickImageOnOpen: Bool
    let onPickImageRequestHandled: () -> Void
    let isSubmitting: Bool
    let onSubmit: () -> Void

    @FocusState private var isFocused: Bool
    @State private var pickerItem: PhotosPickerItem?
    @State private var showImagePicker = false

    private let quickEmojis = ["😀", "😂", "🥰", "😭", "👍", "🙏", "❤️", "🔥"]

    private var canSend: Bool {
        (!text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || pendingImage != nil) && !isSubmitting
    }

    var body: some View {
        VStack(spacing: 0) {
            if let replyLabel {
                HStack {
                    Text("回复 \(replyLabel)")
                        .font(.system(size: 12))
                        .foregroundStyle(XhsTheme.textSecondary)
                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.top, 10)
            }

            VStack(alignment: .leading, spacing: 10) {
                TextField("友善评论，文明发言", text: $text, axis: .vertical)
                    .lineLimit(3...6)
                    .font(.system(size: 15))
                    .focused($isFocused)

                if let pendingImagePreview {
                    ZStack(alignment: .topTrailing) {
                        Image(uiImage: pendingImagePreview)
                            .resizable()
                            .scaledToFill()
                            .frame(width: 72, height: 72)
                            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))

                        Button {
                            clearPendingImage()
                        } label: {
                            Image(systemName: "xmark")
                                .font(.system(size: 10, weight: .bold))
                                .foregroundStyle(.white)
                                .padding(5)
                                .background(Color.black.opacity(0.45))
                                .clipShape(Circle())
                        }
                        .buttonStyle(.plain)
                        .padding(4)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)

            HStack(spacing: 16) {
                Button {
                    showImagePicker = true
                } label: {
                    Image(systemName: "photo")
                        .font(.system(size: 20))
                        .foregroundStyle(XhsTheme.textSecondary)
                }
                .buttonStyle(.plain)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(quickEmojis, id: \.self) { emoji in
                            Button(emoji) {
                                text += emoji
                            }
                            .font(.system(size: 24))
                        }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 10)

            HStack {
                Text("\(text.count)/500")
                    .font(.system(size: 12))
                    .foregroundStyle(XhsTheme.textSecondary)
                Spacer()
                Button("发送", action: onSubmit)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(canSend ? XhsTheme.red : XhsTheme.textSecondary)
                    .disabled(!canSend)
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 12)
        }
        .background(Color.white)
        .photosPicker(isPresented: $showImagePicker, selection: $pickerItem, matching: .images)
        .onChange(of: pickerItem) { _, item in
            guard let item else {
                clearPendingImage()
                return
            }
            Task { await loadPendingImage(from: item) }
        }
        .onAppear {
            DispatchQueue.main.async { isFocused = true }
            if requestPickImageOnOpen {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.12) {
                    showImagePicker = true
                    onPickImageRequestHandled()
                }
            }
        }
    }

    private func clearPendingImage() {
        pickerItem = nil
        pendingImage = nil
        pendingImagePreview = nil
    }

    private func loadPendingImage(from item: PhotosPickerItem) async {
        guard let data = try? await item.loadTransferable(type: Data.self) else { return }
        let filename = "comment_\(UUID().uuidString).jpg"
        pendingImage = PendingCommentImage(data: data, filename: filename)
        pendingImagePreview = UIImage(data: data)
    }
}
