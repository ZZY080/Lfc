import SwiftUI

// MARK: - Formatting

func formatSocialCount(_ count: Int) -> String {
    if count >= 10_000 {
        return String(format: "%.1f万", Double(count) / 10_000)
    }
    return "\(count)"
}

func formatPriceYuan(_ price: String) -> String {
    let trimmed = price.trimmingCharacters(in: .whitespacesAndNewlines)
    if trimmed.hasPrefix("¥") { return trimmed }
    return "¥\(trimmed)"
}

func productConditionLabel(_ condition: String) -> String {
    switch condition.uppercased() {
    case "NEW": return "全新"
    case "LIKE_NEW": return "几乎全新"
    case "GOOD": return "成色良好"
    case "FAIR": return "有使用痕迹"
    default: return condition
    }
}

func productDeliveryLabel(_ method: String) -> String {
    switch method.uppercased() {
    case "FACE_TO_FACE": return "面交"
    case "CAMPUS_DELIVERY": return "校内配送"
    case "EXPRESS": return "快递"
    default: return method
    }
}

func formatDateTimeLabel(_ iso: String) -> String {
    iso
        .prefix(16)
        .replacingOccurrences(of: "T", with: " ")
}

// MARK: - Remote image

struct DetailRemoteImage: View {
    let urlString: String?
    var cornerRadius: CGFloat = 0

    var body: some View {
        Group {
            if let urlString, let url = URL(string: urlString) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().scaledToFill()
                    default:
                        imagePlaceholder
                    }
                }
            } else {
                imagePlaceholder
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
    }

    private var imagePlaceholder: some View {
        Rectangle()
            .fill(Color(red: 0.94, green: 0.94, blue: 0.94))
            .overlay {
                Image(systemName: "photo")
                    .foregroundStyle(XhsTheme.textSecondary)
            }
    }
}

// MARK: - Image carousel

struct DetailImageCarousel: View {
    let images: [String]
    var height: CGFloat = 360

    @State private var selectedIndex = 0

    var body: some View {
        ZStack(alignment: .bottom) {
            if images.isEmpty {
                DetailRemoteImage(urlString: nil)
                    .frame(height: height)
            } else {
                TabView(selection: $selectedIndex) {
                    ForEach(Array(images.enumerated()), id: \.offset) { index, url in
                        DetailRemoteImage(urlString: url, cornerRadius: 0)
                            .frame(height: height)
                            .clipped()
                            .tag(index)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .frame(height: height)

                if images.count > 1 {
                    Text("\(selectedIndex + 1)/\(images.count)")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(.black.opacity(0.45))
                        .clipShape(Capsule())
                        .padding(.bottom, 12)
                }
            }
        }
    }
}

// MARK: - Author header

struct DetailAuthorHeader<Trailing: View>: View {
    let authorLabel: String
    let avatarUrl: String?
    let onBack: () -> Void
    let onAuthorTap: () -> Void
    @ViewBuilder let trailing: () -> Trailing

    var body: some View {
        HStack(spacing: 10) {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(XhsTheme.textPrimary)
                    .frame(width: 36, height: 36)
            }

            Button(action: onAuthorTap) {
                HStack(spacing: 8) {
                    avatar
                    Text(authorLabel)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(XhsTheme.textPrimary)
                        .lineLimit(1)
                }
            }
            .buttonStyle(.plain)

            Spacer()
            trailing()
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 6)
        .background(Color.white)
        .overlay(alignment: .bottom) {
            Divider().overlay(XhsTheme.divider)
        }
    }

    private var avatar: some View {
        DetailRemoteImage(urlString: avatarUrl, cornerRadius: 16)
            .frame(width: 32, height: 32)
            .clipShape(Circle())
    }
}

struct DetailFollowButton: View {
    let isFollowing: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(isFollowing ? "已关注" : "关注")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(isFollowing ? XhsTheme.textSecondary : .white)
                .padding(.horizontal, 14)
                .padding(.vertical, 6)
                .background(isFollowing ? Color(red: 0.95, green: 0.95, blue: 0.95) : XhsTheme.red)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Social bar

struct DetailSocialBar: View {
    let isLiked: Bool
    let likeCount: Int
    let isFavorited: Bool
    let favoriteCount: Int
    let commentCount: Int
    let isSubmitting: Bool
    let onLike: () -> Void
    let onFavorite: () -> Void
    let onComment: () -> Void

    var body: some View {
        HStack(spacing: 20) {
            socialButton(
                icon: isLiked ? "heart.fill" : "heart",
                label: formatSocialCount(likeCount),
                tint: isLiked ? XhsTheme.red : XhsTheme.textPrimary,
                action: onLike
            )
            socialButton(
                icon: isFavorited ? "star.fill" : "star",
                label: formatSocialCount(favoriteCount),
                tint: isFavorited ? .orange : XhsTheme.textPrimary,
                action: onFavorite
            )
            socialButton(
                icon: "bubble.right",
                label: commentCount > 0 ? formatSocialCount(commentCount) : "评论",
                tint: XhsTheme.textPrimary,
                action: onComment
            )
            Spacer()
        }
        .disabled(isSubmitting)
        .opacity(isSubmitting ? 0.6 : 1)
    }

    private func socialButton(icon: String, label: String, tint: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.system(size: 18))
                Text(label)
                    .font(.system(size: 12))
            }
            .foregroundStyle(tint)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Product link card

struct DetailProductLinkCard: View {
    let post: PostDto
    let product: PostProductDto
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                DetailRemoteImage(urlString: post.images?.first, cornerRadius: 10)
                    .frame(width: 56, height: 56)
                VStack(alignment: .leading, spacing: 6) {
                    Text(post.productDisplayTitle())
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(XhsTheme.textPrimary)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                    Text(formatPriceYuan(product.price))
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(XhsTheme.red)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 14))
                    .foregroundStyle(XhsTheme.textSecondary)
            }
            .padding(12)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Owner menu

struct DetailOwnerMenu: View {
    let contentLabel: String
    let isOffShelf: Bool
    let onEdit: () -> Void
    let onDelete: () -> Void
    let onOffShelf: () -> Void
    let onOnShelf: () -> Void

    @State private var showMenu = false
    @State private var showDeleteConfirm = false

    var body: some View {
        Menu {
            Button("编辑\(contentLabel)", action: onEdit)
            if isOffShelf {
                Button("重新上架", action: onOnShelf)
            } else {
                Button("下架\(contentLabel)", action: onOffShelf)
            }
            Button("删除\(contentLabel)", role: .destructive) {
                showDeleteConfirm = true
            }
        } label: {
            Image(systemName: "ellipsis")
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(XhsTheme.textPrimary)
                .frame(width: 36, height: 36)
        }
        .confirmationDialog("确认删除？", isPresented: $showDeleteConfirm, titleVisibility: .visible) {
            Button("删除", role: .destructive, action: onDelete)
            Button("取消", role: .cancel) {}
        } message: {
            Text("删除后无法恢复")
        }
    }
}

// MARK: - Comment row

struct DetailCommentRow: View {
    let comment: PostCommentDto
    let postAuthorId: Int
    let onReply: () -> Void
    let onLike: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top, spacing: 10) {
                DetailRemoteImage(urlString: comment.author?.avatarUrl, cornerRadius: 14)
                    .frame(width: 28, height: 28)
                    .clipShape(Circle())

                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 6) {
                        Text(comment.author?.displayName ?? "同学\(comment.userId)")
                            .font(.system(size: 13, weight: .medium))
                            .foregroundStyle(XhsTheme.textSecondary)
                        if comment.userId == postAuthorId {
                            Text("作者")
                                .font(.system(size: 10, weight: .medium))
                                .foregroundStyle(XhsTheme.red)
                                .padding(.horizontal, 4)
                                .padding(.vertical, 1)
                                .background(XhsTheme.red.opacity(0.1))
                                .clipShape(RoundedRectangle(cornerRadius: 3))
                        }
                    }
                    Text(comment.content)
                        .font(.system(size: 14))
                        .foregroundStyle(XhsTheme.textPrimary)
                        .fixedSize(horizontal: false, vertical: true)

                    HStack(spacing: 16) {
                        Text(formatDateTimeLabel(comment.createdAt))
                            .font(.system(size: 11))
                            .foregroundStyle(XhsTheme.textSecondary)
                        Button("回复", action: onReply)
                            .font(.system(size: 12))
                            .foregroundStyle(XhsTheme.textSecondary)
                        Button(action: onLike) {
                            HStack(spacing: 2) {
                                Image(systemName: comment.isLiked ? "heart.fill" : "heart")
                                if comment.likeCount > 0 {
                                    Text("\(comment.likeCount)")
                                }
                            }
                            .font(.system(size: 12))
                            .foregroundStyle(comment.isLiked ? XhsTheme.red : XhsTheme.textSecondary)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }

            if let replies = comment.previewReplies, !replies.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    ForEach(replies) { reply in
                        DetailCommentRow(
                            comment: reply,
                            postAuthorId: postAuthorId,
                            onReply: onReply,
                            onLike: onLike
                        )
                        .padding(.leading, 38)
                    }
                }
            }
        }
        .padding(.vertical, 6)
    }
}

// MARK: - Profile grid card

struct DetailProfilePostCard: View {
    let post: PostDto
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 0) {
                DetailRemoteImage(urlString: post.images?.first, cornerRadius: 8)
                    .frame(height: 140)
                    .clipped()
                Text(post.title)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(XhsTheme.textPrimary)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                    .padding(8)
            }
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

struct DetailProfileActivityRow: View {
    let activity: ActivityDto
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                DetailRemoteImage(urlString: activity.images?.first, cornerRadius: 8)
                    .frame(width: 72, height: 72)
                VStack(alignment: .leading, spacing: 4) {
                    Text(activity.title)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(XhsTheme.textPrimary)
                        .lineLimit(2)
                    Text(formatDateTimeLabel(activity.startTime))
                        .font(.system(size: 11))
                        .foregroundStyle(XhsTheme.textSecondary)
                    Text(activity.location)
                        .font(.system(size: 11))
                        .foregroundStyle(XhsTheme.textSecondary)
                        .lineLimit(1)
                }
                Spacer()
            }
            .padding(10)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

struct DetailPrimaryButtonStyle: ButtonStyle {
    let filled: Bool

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 16, weight: .semibold))
            .foregroundStyle(filled ? .white : XhsTheme.textPrimary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(filled ? XhsTheme.red : Color(red: 0.95, green: 0.95, blue: 0.95))
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .opacity(configuration.isPressed ? 0.85 : 1)
    }
}
