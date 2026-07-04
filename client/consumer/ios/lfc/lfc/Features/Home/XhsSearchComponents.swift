import SwiftUI

let xhsSearchTabs = ["综合", "笔记", "活动"]

let xhsHotSearches = [
    "校园活动", "食堂推荐", "考研资料", "社团招新",
    "二手闲置", "学习笔记", "图书馆", "篮球赛",
]

// MARK: - Top bar

struct XhsSearchTopBar: View {
    @Binding var text: String
    let placeholder: String
    let onBack: () -> Void
    let onSearch: () -> Void
    var requestFocus: Bool = false

    @FocusState private var isFocused: Bool

    var body: some View {
        HStack(spacing: 0) {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(XhsTheme.textPrimary)
                    .frame(width: 40, height: 40)
            }
            .buttonStyle(.plain)

            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 16))
                    .foregroundStyle(XhsTheme.textSecondary)

                TextField(placeholder, text: $text)
                    .font(.system(size: 14))
                    .foregroundStyle(XhsTheme.textPrimary)
                    .submitLabel(.search)
                    .focused($isFocused)
                    .onSubmit(onSearch)

                if !text.isEmpty {
                    Button {
                        text = ""
                    } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(XhsTheme.textSecondary)
                            .frame(width: 18, height: 18)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 12)
            .frame(height: 36)
            .background(Color(red: 0.96, green: 0.96, blue: 0.96))
            .clipShape(Capsule())

            Button(action: onSearch) {
                Text("搜索")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(XhsTheme.red)
                    .padding(.horizontal, 4)
                    .padding(.vertical, 8)
            }
            .buttonStyle(.plain)
            .padding(.leading, 10)
        }
        .padding(.leading, 4)
        .padding(.trailing, 12)
        .padding(.top, 6)
        .padding(.bottom, 8)
        .background(Color.white)
        .onAppear {
            guard requestFocus else { return }
            DispatchQueue.main.async {
                isFocused = true
            }
        }
    }
}

// MARK: - Chips

struct XhsSearchChip: View {
    let text: String
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(text)
                .font(.system(size: 13))
                .foregroundStyle(XhsTheme.textPrimary)
                .lineLimit(1)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(Color(red: 0.96, green: 0.96, blue: 0.96))
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Text tabs

struct XhsTextTabRow: View {
    let tabs: [String]
    let selectedTab: String
    let onTabSelected: (String) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 18) {
                ForEach(tabs, id: \.self) { title in
                    let selected = selectedTab == title
                    Text(title)
                        .font(.system(size: 14, weight: selected ? .bold : .regular))
                        .foregroundStyle(selected ? XhsTheme.red : XhsTheme.textSecondary)
                        .padding(.vertical, 8)
                        .onTapGesture { onTabSelected(title) }
                }
            }
            .padding(.horizontal, 14)
        }
        .background(Color.white)
    }
}

// MARK: - Mixed feed

enum SearchFeedItem: Identifiable {
    case post(PostDto)
    case activity(ActivityDto)

    var id: String {
        switch self {
        case .post(let post): "post-\(post.id)"
        case .activity(let activity): "activity-\(activity.id)"
        }
    }

    var createdAt: String {
        switch self {
        case .post(let post): post.createdAt
        case .activity(let activity): activity.createdAt
        }
    }
}

func buildMixedSearchFeed(posts: [PostDto], activities: [ActivityDto]) -> [SearchFeedItem] {
    let postItems = posts.map(SearchFeedItem.post)
    let activityItems = activities.map(SearchFeedItem.activity)
    return (postItems + activityItems).sorted { $0.createdAt > $1.createdAt }
}

struct SearchResultWaterfallGrid: View {
    let items: [SearchFeedItem]
    let spacing: CGFloat
    let onPostTap: (Int) -> Void
    let onActivityTap: (Int) -> Void
    var onItemAppear: ((SearchFeedItem) -> Void)?

    var body: some View {
        let columns = WaterfallColumnSplit.split(items, spacing: spacing, estimatedHeight: estimatedSearchFeedItemHeight)
        HStack(alignment: .top, spacing: spacing) {
            columnView(items: columns.left)
            columnView(items: columns.right)
        }
    }

    private func columnView(items: [SearchFeedItem]) -> some View {
        LazyVStack(spacing: spacing) {
            ForEach(items) { item in
                itemView(item)
                    .frame(maxWidth: .infinity)
                    .onAppear { onItemAppear?(item) }
            }
        }
        .frame(maxWidth: .infinity, alignment: .top)
    }

    @ViewBuilder
    private func itemView(_ item: SearchFeedItem) -> some View {
        switch item {
        case .post(let post):
            XhsProfileFeedCard(post: post, onTap: { onPostTap(post.id) })
        case .activity(let activity):
            XhsProfileActivityCard(
                activity: activity,
                authorLabel: activity.author?.displayName ?? "同学\(activity.authorId)",
                onTap: { onActivityTap(activity.id) }
            )
        }
    }
}

// MARK: - Activity card

func profileActivityStatusLabel(_ status: String) -> String {
    switch status.uppercased() {
    case "PENDING": "待审核"
    case "APPROVED": "进行中"
    case "REJECTED": "已拒绝"
    case "OFF_SHELF": "已下架"
    default: status
    }
}

struct XhsProfileActivityCard: View {
    let activity: ActivityDto
    let authorLabel: String
    let onTap: () -> Void

    private var aspectRatio: CGFloat {
        0.68 + CGFloat(gradientIndexForId(activity.id, size: 4)) * 0.06
    }

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 0) {
                coverSection
                Text(activity.title)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(XhsTheme.textPrimary)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                    .lineSpacing(3)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 7)

                if let distance = formatDistanceCompact(
                    userLat: nil,
                    userLng: nil,
                    targetLat: activity.latitude,
                    targetLng: activity.longitude
                ) {
                    Text(distance)
                        .font(.system(size: 9))
                        .foregroundStyle(XhsTheme.textSecondary)
                        .padding(.horizontal, 8)
                        .padding(.bottom, 4)
                }

                footerRow
                    .padding(.horizontal, 8)
                    .padding(.bottom, 8)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private var coverSection: some View {
        ZStack(alignment: .topLeading) {
            Color.clear
                .aspectRatio(aspectRatio, contentMode: .fit)
                .overlay {
                    if let urlString = activity.images?.first, let url = URL(string: urlString) {
                        AsyncImage(url: url) { phase in
                            switch phase {
                            case .success(let image):
                                image.resizable().scaledToFill()
                            default:
                                coverPlaceholder
                            }
                        }
                    } else {
                        coverPlaceholder
                    }
                }
                .clipped()

            Text(profileActivityStatusLabel(activity.status))
                .font(.system(size: 9))
                .foregroundStyle(.white)
                .padding(.horizontal, 5)
                .padding(.vertical, 2)
                .background(Color.black.opacity(0.42))
                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
                .padding(6)

            if let badge = activity.promotionBadge {
                Text(badge)
                    .font(.system(size: 9, weight: .semibold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 5)
                    .padding(.vertical, 2)
                    .background(XhsTheme.red.opacity(0.9))
                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                    .padding(6)
            }

            let participantCount = activity.participants?.count ?? 0
            if participantCount > 0 {
                HStack(spacing: 2) {
                    Image(systemName: "calendar")
                        .font(.system(size: 8))
                        .foregroundStyle(.white)
                    Text(formatLikeCount(participantCount))
                        .font(.system(size: 9))
                        .foregroundStyle(.white)
                }
                .padding(.horizontal, 5)
                .padding(.vertical, 2)
                .background(Color.black.opacity(0.42))
                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
                .padding(6)
            }
        }
        .frame(maxWidth: .infinity)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    private var coverPlaceholder: some View {
        FeedImagePlaceholder()
    }

    private var footerAuthor: String {
        activity.author?.displayName ?? authorLabel
    }

    private var footerRow: some View {
        HStack(spacing: 0) {
            XhsProfileAvatar(
                label: footerAuthor,
                size: 16,
                avatarUrl: activity.author?.avatarUrl
            )
            Text(footerAuthor)
                .font(.system(size: 9))
                .foregroundStyle(XhsTheme.textSecondary)
                .lineLimit(1)
                .padding(.leading, 4)
            Spacer(minLength: 4)
            Image(systemName: activity.isLiked ? "heart.fill" : "heart")
                .font(.system(size: 11))
                .foregroundStyle(activity.isLiked ? XhsTheme.red : XhsTheme.textSecondary.opacity(0.7))
            if activity.likeCount > 0 {
                Text(formatLikeCount(activity.likeCount))
                    .font(.system(size: 9))
                    .foregroundStyle(activity.isLiked ? XhsTheme.red : XhsTheme.textSecondary)
                    .padding(.leading, 2)
            }
        }
    }
}

// MARK: - Flow layout for chips

struct XhsFlowLayout: Layout {
    var horizontalSpacing: CGFloat = 10
    var verticalSpacing: CGFloat = 10

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? 0
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > 0, x + size.width > width {
                x = 0
                y += rowHeight + verticalSpacing
                rowHeight = 0
            }
            rowHeight = max(rowHeight, size.height)
            x += size.width + horizontalSpacing
        }

        return CGSize(width: width, height: y + rowHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX
        var y = bounds.minY
        var rowHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > bounds.minX, x + size.width > bounds.maxX {
                x = bounds.minX
                y += rowHeight + verticalSpacing
                rowHeight = 0
            }
            subview.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            rowHeight = max(rowHeight, size.height)
            x += size.width + horizontalSpacing
        }
    }
}
