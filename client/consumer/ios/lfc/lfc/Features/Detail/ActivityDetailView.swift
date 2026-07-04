import SwiftUI

struct ActivityDetailView: View {
    @Bindable var store: HomeStore
    let currentUserId: Int?
    let onBack: () -> Void
    let onAuthorTap: (Int) -> Void
    let onEdit: (() -> Void)?
    let onDeleted: () -> Void

    private var activity: ActivityDto? { store.selectedActivity }
    private var isLoading: Bool { store.isActivityLoading }

    var body: some View {
        VStack(spacing: 0) {
            if isLoading, activity == nil {
                ActivityDetailSkeleton()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let activity {
                content(activity: activity)
            } else {
                ContentUnavailableView("活动不存在", systemImage: "calendar")
            }
        }
        .background(Color.white)
        .safeAreaInset(edge: .bottom) {
            if let activity, currentUserId != activity.authorId {
                joinBar(activity: activity)
            }
        }
    }

    @ViewBuilder
    private func content(activity: ActivityDto) -> some View {
        let isSelf = currentUserId == activity.authorId
        let authorLabel = activity.author?.displayName ?? "同学\(activity.authorId)"
        let participants = activity.participants ?? []

        VStack(spacing: 0) {
            DetailAuthorHeader(
                authorLabel: authorLabel,
                avatarUrl: activity.author?.avatarUrl,
                onBack: onBack,
                onAuthorTap: { onAuthorTap(activity.authorId) }
            ) {
                if isSelf, let onEdit {
                    DetailOwnerMenu(
                        contentLabel: "活动",
                        isOffShelf: activity.status.uppercased() == "OFF_SHELF",
                        onEdit: onEdit,
                        onDelete: {
                            Task {
                                if await store.deleteActivity(id: activity.id) {
                                    onDeleted()
                                }
                            }
                        },
                        onOffShelf: { Task { await store.offShelfActivity(id: activity.id) } },
                        onOnShelf: { Task { await store.onShelfActivity(id: activity.id) } }
                    )
                } else if !isSelf, let following = store.detailAuthorFollowing {
                    DetailFollowButton(isFollowing: following) {
                        Task { await store.toggleDetailAuthorFollow(authorId: activity.authorId) }
                    }
                }
            }

            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    DetailImageCarousel(images: activity.images ?? [], height: 280)

                    VStack(alignment: .leading, spacing: 16) {
                        if let badge = activity.promotionBadge {
                            Text(badge)
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundStyle(XhsTheme.red)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 3)
                                .background(XhsTheme.red.opacity(0.1))
                                .clipShape(Capsule())
                        }

                        Text(activity.title)
                            .font(.system(size: 20, weight: .bold))
                            .foregroundStyle(XhsTheme.textPrimary)

                        if let description = activity.description, !description.isEmpty {
                            Text(description)
                                .font(.system(size: 15))
                                .foregroundStyle(XhsTheme.textPrimary)
                                .lineSpacing(4)
                        }

                        infoCard(activity: activity)

                        DetailSocialBar(
                            isLiked: activity.isLiked,
                            likeCount: activity.likeCount,
                            isFavorited: activity.isFavorited,
                            favoriteCount: activity.favoriteCount,
                            commentCount: 0,
                            isSubmitting: store.isActivitySocialSubmitting,
                            onLike: { Task { await store.toggleActivityLike(activityId: activity.id) } },
                            onFavorite: { Task { await store.toggleActivityFavorite(activityId: activity.id) } },
                            onComment: {}
                        )

                        participantsSection(participants: participants, max: activity.maxParticipants)
                    }
                    .padding(16)
                }
            }
        }
    }

    private func infoCard(activity: ActivityDto) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Label(formatDateTimeLabel(activity.startTime), systemImage: "calendar")
            Label(formatDateTimeLabel(activity.endTime), systemImage: "clock")
            Label(activity.location, systemImage: "mappin.and.ellipse")
            HStack {
                Label(
                    activity.isPaidActivity ? formatPriceYuan(activity.fee ?? "0") : "免费",
                    systemImage: "yensign.circle"
                )
                Spacer()
                Text("名额 \(participantsCount(activity))/\(activity.maxParticipants)")
                    .font(.system(size: 13))
                    .foregroundStyle(XhsTheme.textSecondary)
            }
        }
        .font(.system(size: 14))
        .foregroundStyle(XhsTheme.textPrimary)
        .padding(14)
        .background(Color(red: 0.97, green: 0.97, blue: 0.97))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func participantsSection(participants: [ActivityParticipantDto], max: Int) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("参与者 (\(participants.count)/\(max))")
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(XhsTheme.textPrimary)

            if participants.isEmpty {
                Text("还没有人报名")
                    .font(.system(size: 14))
                    .foregroundStyle(XhsTheme.textSecondary)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(participants) { participant in
                            VStack(spacing: 4) {
                                DetailRemoteImage(urlString: participant.user?.avatarUrl, cornerRadius: 20)
                                    .frame(width: 40, height: 40)
                                    .clipShape(Circle())
                                Text(participant.user?.displayName ?? "同学")
                                    .font(.system(size: 11))
                                    .foregroundStyle(XhsTheme.textSecondary)
                                    .lineLimit(1)
                                    .frame(width: 48)
                            }
                        }
                    }
                }
            }
        }
        .padding(.top, 8)
    }

    private func joinBar(activity: ActivityDto) -> some View {
        HStack(spacing: 12) {
            if activity.isJoined {
                Button("取消报名") {
                    Task { await store.leaveActivity(id: activity.id) }
                }
                .buttonStyle(DetailPrimaryButtonStyle(filled: false))
                .disabled(store.isJoiningActivity)
            } else {
                Button(joinButtonTitle(activity: activity)) {
                    Task { await store.joinActivity(id: activity.id) }
                }
                .buttonStyle(DetailPrimaryButtonStyle(filled: true))
                .disabled(store.isJoiningActivity || participantsCount(activity) >= activity.maxParticipants)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(Color.white)
        .overlay(alignment: .top) { Divider().overlay(XhsTheme.divider) }
    }

    private func joinButtonTitle(activity: ActivityDto) -> String {
        if store.isJoiningActivity { return "处理中…" }
        if participantsCount(activity) >= activity.maxParticipants { return "名额已满" }
        if activity.isPaidActivity { return "支付并报名 \(formatPriceYuan(activity.fee ?? "0"))" }
        return "立即报名"
    }

    private func participantsCount(_ activity: ActivityDto) -> Int {
        activity.participants?.count ?? 0
    }
}
