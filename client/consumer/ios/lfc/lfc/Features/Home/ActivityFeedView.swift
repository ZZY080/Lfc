import SwiftUI

struct ActivityFeedView: View {
    let feedState: ActivityFeedUiState
    let onRefresh: () async -> Void
    let onLoadMore: () async -> Void
    let onActivityTap: (Int) -> Void

    var body: some View {
        VStack(spacing: 0) {
            Text("活动")
                .font(.system(size: 20, weight: .bold))
                .foregroundStyle(XhsTheme.red)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 16)
                .padding(.vertical, 10)

            content
        }
        .lfcImmersiveBackground()
    }

    @ViewBuilder
    private var content: some View {
        if feedState.isInitialLoading || (feedState.isRefreshing && feedState.activities.isEmpty) {
            ActivityFeedSkeleton()
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        } else if feedState.activities.isEmpty, let error = feedState.loadError {
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
        } else if feedState.activities.isEmpty {
            ContentUnavailableView("暂无活动", systemImage: "calendar", description: Text("下拉刷新试试"))
                .refreshable { await onRefresh() }
        } else {
            ScrollView {
                LazyVStack(spacing: 10) {
                    ForEach(feedState.activities) { activity in
                        ActivityRow(activity: activity) {
                            onActivityTap(activity.id)
                        }
                        .onAppear {
                            if activity.id == feedState.activities.last?.id {
                                Task { await onLoadMore() }
                            }
                        }
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .id(feedState.listResetNonce)

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

private struct ActivityRow: View {
    let activity: ActivityDto
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(alignment: .top, spacing: 12) {
                cover
                VStack(alignment: .leading, spacing: 6) {
                    Text(activity.title)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(XhsTheme.textPrimary)
                        .lineLimit(2)
                    Label(activity.location, systemImage: "mappin.and.ellipse")
                        .font(.system(size: 12))
                        .foregroundStyle(XhsTheme.textSecondary)
                        .lineLimit(1)
                    HStack {
                        Text(activity.startTime.prefix(16).replacingOccurrences(of: "T", with: " "))
                            .font(.system(size: 11))
                            .foregroundStyle(XhsTheme.textSecondary)
                        Spacer()
                        if let fee = activity.fee, fee != "0", !fee.isEmpty {
                            Text("¥\(fee)")
                                .font(.system(size: 13, weight: .bold))
                                .foregroundStyle(XhsTheme.red)
                        } else {
                            Text("免费")
                                .font(.system(size: 12, weight: .medium))
                                .foregroundStyle(.green)
                        }
                    }
                }
            }
            .padding(12)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private var cover: some View {
        if let urlString = activity.images?.first, let url = URL(string: urlString) {
            AsyncImage(url: url) { image in
                image.resizable().scaledToFill()
            } placeholder: {
                coverPlaceholder
            }
            .frame(width: 88, height: 88)
            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
        } else {
            coverPlaceholder
                .frame(width: 88, height: 88)
                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
        }
    }

    private var coverPlaceholder: some View {
        FeedImagePlaceholder()
    }
}
