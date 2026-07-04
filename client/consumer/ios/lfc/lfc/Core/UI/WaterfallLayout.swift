import Foundation

/// Splits items into two columns by always placing the next item in the shorter column.
/// Matches Android `LazyVerticalStaggeredGrid` placement behavior.
enum WaterfallColumnSplit {
    static func split<Item>(
        _ items: [Item],
        spacing: CGFloat,
        estimatedHeight: (Item) -> CGFloat
    ) -> (left: [Item], right: [Item]) {
        var left: [Item] = []
        var right: [Item] = []
        var leftHeight: CGFloat = 0
        var rightHeight: CGFloat = 0

        for item in items {
            let itemHeight = estimatedHeight(item)
            if leftHeight <= rightHeight {
                left.append(item)
                leftHeight += itemHeight + spacing
            } else {
                right.append(item)
                rightHeight += itemHeight + spacing
            }
        }
        return (left, right)
    }
}

// MARK: - Feed card height estimates

private let feedCardMetaHeight: CGFloat = 90

func estimatedFeedCardHeight(for post: PostDto) -> CGFloat {
    let coverHeight = 1 / max(feedAspectRatio(for: post.id), 0.01)
    return coverHeight + feedCardMetaHeight
}

func estimatedActivityCardHeight(for activity: ActivityDto) -> CGFloat {
    let aspectRatio = 0.68 + CGFloat(gradientIndexForId(activity.id, size: 4)) * 0.06
    let coverHeight = 1 / max(aspectRatio, 0.01)
    var metaHeight = feedCardMetaHeight
    if formatDistanceCompact(
        userLat: nil,
        userLng: nil,
        targetLat: activity.latitude,
        targetLng: activity.longitude
    ) != nil {
        metaHeight += 18
    }
    return coverHeight + metaHeight
}

func estimatedSearchFeedItemHeight(_ item: SearchFeedItem) -> CGFloat {
    switch item {
    case .post(let post):
        return estimatedFeedCardHeight(for: post)
    case .activity(let activity):
        return estimatedActivityCardHeight(for: activity)
    }
}

func estimatedProfileLibraryItemHeight(_ item: ProfileLibraryFeedItem) -> CGFloat {
    switch item {
    case .post(let post):
        return estimatedFeedCardHeight(for: post)
    case .activity(let activity):
        return estimatedActivityCardHeight(for: activity)
    }
}
