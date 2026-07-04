import SwiftUI

struct FeedListEndFooter: View {
    var text: String = "— 已经到底了 —"

    var body: some View {
        Text(text)
            .font(.system(size: 12))
            .foregroundStyle(XhsTheme.textSecondary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
    }
}

// MARK: - Primary tabs (关注 / 发现 / 城市)

struct XhsFeedPrimaryTabRow: View {
    let selectedTab: String
    let cityLabel: String
    let onTabSelected: (String) -> Void
    let onMessageTap: () -> Void
    let onSearchTap: () -> Void

    private var tabs: [String] {
        xhsFeedPrimaryTabs + [cityLabel]
    }

    private var selectedIndex: Int {
        tabs.firstIndex(of: selectedTab) ?? 1
    }

    var body: some View {
        HStack(spacing: 0) {
            Button(action: onMessageTap) {
                Image(systemName: "bubble.left")
                    .font(.system(size: 20))
                    .foregroundStyle(XhsTheme.textPrimary)
                    .frame(width: 40, height: 40)
            }
            .buttonStyle(.plain)

            GeometryReader { geometry in
                let tabWidth = geometry.size.width / CGFloat(tabs.count)
                ZStack(alignment: .bottomLeading) {
                    HStack(spacing: 0) {
                        ForEach(Array(tabs.enumerated()), id: \.offset) { index, title in
                            Button {
                                onTabSelected(title)
                            } label: {
                                Text(title)
                                    .font(.system(size: 16, weight: index == selectedIndex ? .bold : .regular))
                                    .foregroundStyle(index == selectedIndex ? XhsTheme.textPrimary : XhsTheme.textSecondary)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 40)
                            }
                            .buttonStyle(.plain)
                        }
                    }

                    RoundedRectangle(cornerRadius: 1, style: .continuous)
                        .fill(XhsTheme.red)
                        .frame(width: 24, height: 2)
                        .offset(x: tabWidth * CGFloat(selectedIndex) + tabWidth / 2 - 12, y: 0)
                }
            }
            .frame(height: 40)

            Button(action: onSearchTap) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 20))
                    .foregroundStyle(XhsTheme.textPrimary)
                    .frame(width: 40, height: 40)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 4)
    }
}

// MARK: - Category tabs

struct XhsFeedCategoryTabRow: View {
    let myChannels: [String]
    let selectedTab: String
    let isPanelExpanded: Bool
    let onTabSelected: (String) -> Void
    let onExpandPanel: () -> Void
    let onChannelLongPress: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 18) {
                    ForEach(myChannels, id: \.self) { title in
                        let selected = selectedTab == title
                        Text(title)
                            .font(.system(size: 14, weight: selected ? .bold : .regular))
                            .foregroundStyle(selected ? XhsTheme.red : XhsTheme.textSecondary)
                            .padding(.vertical, 8)
                            .onTapGesture { onTabSelected(title) }
                            .onLongPressGesture {
                                if !isPanelExpanded { onExpandPanel() }
                                onChannelLongPress()
                            }
                    }
                }
                .padding(.leading, 14)
                .padding(.trailing, 4)
            }

            Button(action: onExpandPanel) {
                Image(systemName: "chevron.down")
                    .font(.system(size: 20, weight: .medium))
                    .foregroundStyle(XhsTheme.textPrimary)
                    .frame(width: 40, height: 40)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(isPanelExpanded ? "收起频道" : "展开频道")
        }
    }
}

// MARK: - Waterfall grid

struct WaterfallFeedGrid<Content: View>: View {
    let posts: [PostDto]
    let spacing: CGFloat
    let content: (PostDto) -> Content

    var body: some View {
        HStack(alignment: .top, spacing: spacing) {
            columnView(posts: leftColumn)
            columnView(posts: rightColumn)
        }
    }

    private func columnView(posts: [PostDto]) -> some View {
        LazyVStack(spacing: spacing) {
            ForEach(posts) { post in
                content(post)
                    .frame(maxWidth: .infinity)
            }
        }
        .frame(maxWidth: .infinity, alignment: .top)
    }

    private var leftColumn: [PostDto] {
        posts.enumerated().compactMap { index, post in
            index.isMultiple(of: 2) ? post : nil
        }
    }

    private var rightColumn: [PostDto] {
        posts.enumerated().compactMap { index, post in
            index.isMultiple(of: 2) ? nil : post
        }
    }
}

// MARK: - Feed card

struct XhsProfileFeedCard: View {
    let post: PostDto
    var userLat: Double?
    var userLng: Double?
    var isPinned: Bool = false
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 0) {
                coverSection
                Text(post.title)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(XhsTheme.textPrimary)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                    .lineSpacing(2)
                    .padding(.horizontal, 8)
                    .padding(.top, 8)
                    .padding(.bottom, 8)

                footerRow
                    .padding(.horizontal, 8)
                    .padding(.bottom, 10)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private var coverSection: some View {
        let ratio = feedAspectRatio(for: post.id)
        ZStack(alignment: .bottom) {
            Color.clear
                .aspectRatio(ratio, contentMode: .fit)
                .overlay(alignment: .topLeading) {
                    if isPinned {
                        Text("置顶")
                            .font(.system(size: 10, weight: .medium))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(XhsTheme.red)
                            .clipShape(RoundedRectangle(cornerRadius: 4, style: .continuous))
                            .padding(6)
                    }
                }
                .overlay {
                    if let urlString = post.images?.first, let url = URL(string: urlString) {
                        AsyncImage(url: url) { phase in
                            switch phase {
                            case .success(let image):
                                image.resizable().scaledToFill()
                            default:
                                coverPlaceholder
                            }
                        }
                    } else {
                        textNoteCover
                    }
                }
                .clipped()

            if hasValidCoordinate(latitude: post.latitude, longitude: post.longitude)
                || !(post.location?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?? true) {
                XhsFeedLocationOverlay(
                    address: post.location,
                    latitude: post.latitude,
                    longitude: post.longitude,
                    userLat: userLat,
                    userLng: userLng
                )
                .padding(.horizontal, 3)
                .padding(.bottom, 3)
            }
        }
        .frame(maxWidth: .infinity)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    private var textNoteCover: some View {
        let colors: [Color] = [
            Color(red: 1, green: 0.6, blue: 0.62),
            Color(red: 0.98, green: 0.82, blue: 0.77),
            Color(red: 0.85, green: 0.9, blue: 0.98),
            Color(red: 0.92, green: 0.88, blue: 0.98),
        ]
        let bg = colors[gradientIndexForId(post.id, size: colors.count)]
        return ZStack {
            bg
            Text(post.content.isEmpty ? post.title : post.content)
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(XhsTheme.textPrimary.opacity(0.85))
                .multilineTextAlignment(.center)
                .lineLimit(6)
                .padding(12)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var coverPlaceholder: some View {
        LinearGradient(
            colors: [Color(red: 1, green: 0.6, blue: 0.62), Color(red: 0.98, green: 0.82, blue: 0.77)],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    private var footerRow: some View {
        let authorName = post.author?.displayName ?? "用户"
        return HStack(spacing: 0) {
            XhsProfileAvatar(label: authorName, size: 18, avatarUrl: post.author?.avatarUrl)
            Text(authorName)
                .font(.system(size: 10))
                .foregroundStyle(XhsTheme.textSecondary)
                .lineLimit(1)
                .padding(.leading, 5)
            Spacer(minLength: 4)
            Image(systemName: post.isLiked ? "heart.fill" : "heart")
                .font(.system(size: 12))
                .foregroundStyle(post.isLiked ? XhsTheme.red : XhsTheme.textSecondary.opacity(0.75))
            Text(formatLikeCount(post.likeCount))
                .font(.system(size: 10))
                .foregroundStyle(post.isLiked ? XhsTheme.red : XhsTheme.textSecondary)
                .padding(.leading, 2)
        }
    }
}

struct XhsFeedLocationOverlay: View {
    let address: String?
    let latitude: Double?
    let longitude: Double?
    var userLat: Double?
    var userLng: Double?

    var body: some View {
        let addressText = formatFeedLocationChipAddress(address)
        let distanceText = formatDistanceCompact(
            userLat: userLat,
            userLng: userLng,
            targetLat: latitude,
            targetLng: longitude
        )

        if !addressText.isEmpty || distanceText != nil {
            HStack(spacing: 2) {
                Image(systemName: "location.fill")
                    .font(.system(size: 8))
                    .foregroundStyle(.white)
                if !addressText.isEmpty {
                    Text(addressText)
                        .font(.system(size: 8))
                        .foregroundStyle(.white)
                        .lineLimit(1)
                }
                if let distanceText {
                    if !addressText.isEmpty {
                        Text("|")
                            .font(.system(size: 8))
                            .foregroundStyle(.white.opacity(0.8))
                    }
                    Text(distanceText)
                        .font(.system(size: 8))
                        .foregroundStyle(.white)
                }
            }
            .padding(.horizontal, 4)
            .padding(.vertical, 2)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.black.opacity(0.45))
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
    }
}

struct XhsProfileAvatar: View {
    let label: String
    let size: CGFloat
    var avatarUrl: String?

    var body: some View {
        Group {
            if let avatarUrl, let url = URL(string: avatarUrl) {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    initialsView
                }
            } else {
                initialsView
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
    }

    private var initialsView: some View {
        Circle()
            .fill(Color(red: 0.92, green: 0.92, blue: 0.92))
            .overlay {
                Text(String(label.prefix(1)))
                    .font(.system(size: size * 0.45, weight: .medium))
                    .foregroundStyle(XhsTheme.textSecondary)
            }
    }
}
