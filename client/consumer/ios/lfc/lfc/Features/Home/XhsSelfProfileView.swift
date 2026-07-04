import SwiftUI

private let selfProfileFavoritesTab = 3
private let selfProfileLikesTab = 4
private let selfProfileCommentsTab = 2
private let selfBottomBarPadding: CGFloat = 56

struct XhsSelfProfileView: View {
    @Bindable var store: HomeStore
    let onOpenSideMenu: () -> Void
    let onEditProfile: () -> Void
    let onShowQr: () -> Void
    let onScanProfile: () -> Void
    let onPostTap: (Int) -> Void
    let onActivityTap: (Int) -> Void
    let onBindAlipay: () -> Void

    @State private var scrollOffset: CGFloat = 0
    @State private var pullDownOffset: CGFloat = 0
    @State private var suppressContentTapsUntil = Date.distantPast
    @State private var pendingScrollToY: CGFloat?

    private var profile: UserProfileDto? { store.profileState.myProfile }
    private var userId: Int? { profile?.id }
    private var isLoading: Bool { profile == nil && store.profileState.isLoading }
    private var selectedTab: Int { store.profileState.profileContentTab }
    private var profileTabs: ProfileTabsUiState { store.profileState.profileTabs }

    private var tabUiState: ProfileTabUiState {
        profileTabUiStateFor(selectedTab: selectedTab, profileTabs: profileTabs)
    }

    private var collapseProgress: CGFloat {
        XhsProfileLayout.collapseProgress(for: scrollOffset)
    }

    private var tabsArePinned: Bool {
        scrollOffset >= XhsProfileLayout.tabPinScrollOffset() - 2
    }

    /// Nav darkening follows scroll; avatar only when tabs are pinned.
    private var navCollapseProgress: CGFloat {
        max(collapseProgress, tabsArePinned ? 1 : 0)
    }

    private var showCollapsedNav: Bool {
        tabsArePinned
    }

    private var shouldSuppressContentTaps: Bool {
        Date() < suppressContentTapsUntil
    }

    var body: some View {
        Group {
            if isLoading {
                ProfilePageSkeleton()
            } else if let profile, let userId {
                profileScroll(profile: profile, userId: userId)
            } else if store.profileState.loadFailed {
                profileLoadError
            } else {
                ProfilePageSkeleton()
            }
        }
        .background(Color(red: 0.96, green: 0.96, blue: 0.96).ignoresSafeArea())
        .appStatusBarStyle(.lightContent)
    }

    private var profileLoadError: some View {
        VStack(spacing: 0) {
            HStack {
                Button(action: onOpenSideMenu) {
                    Image(systemName: "line.3.horizontal")
                        .font(.system(size: 20, weight: .medium))
                        .foregroundStyle(XhsTheme.textPrimary)
                        .frame(width: 44, height: 44)
                }
                .buttonStyle(.plain)
                Spacer()
            }
            .padding(.horizontal, 2)
            .padding(.top, XhsProfileLayout.statusBarTopInset)
            .frame(height: XhsProfileLayout.topNavTotalHeight)
            .background(Color.white)

            ContentUnavailableView {
                Label("加载失败", systemImage: "person.crop.circle.badge.exclamationmark")
            } description: {
                Text("个人资料加载失败，请检查网络后重试")
            } actions: {
                Button("重试") {
                    Task { await store.loadProfile() }
                }
                .buttonStyle(.borderedProminent)
                .tint(XhsTheme.red)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    @ViewBuilder
    private func profileScroll(profile: UserProfileDto, userId: Int) -> some View {
        let coverUrl = profile.coverUrl ?? profile.posts.first?.images?.first
        ZStack(alignment: .top) {
            ScrollView {
                LazyVStack(spacing: 0, pinnedViews: [.sectionHeaders]) {
                    profileCoverBlock(profile: profile, coverUrl: coverUrl)
                        .padding(.bottom, -XhsProfileLayout.sheetOverlap)

                    if !profile.alipayBound {
                        alipayBanner
                            .padding(.horizontal, 12)
                            .padding(.top, 8)
                    }

                    Section {
                        tabContent(profile: profile, userId: userId)
                            .allowsHitTesting(!shouldSuppressContentTaps)
                        Color.clear.frame(height: selfBottomBarPadding)
                    } header: {
                        pinnedTabBarHeader(profile: profile, userId: userId)
                    }
                }
                .trackProfileScrollOffset($scrollOffset, pullDownOffset: $pullDownOffset, scrollToY: pendingScrollToY)
            }
            .scrollContentBackground(.hidden)
            .background {
                Color(red: 0.96, green: 0.96, blue: 0.96)
            }
            .background(alignment: .top) {
                XhsProfileLayout.coverDark
                    .frame(
                        height: XhsProfileLayout.coverHeight
                            + XhsProfileLayout.statusBarTopInset
                            + (scrollOffset <= 1 ? pullDownOffset : 0)
                    )
                    .frame(maxWidth: .infinity, alignment: .top)
                    .ignoresSafeArea(edges: .top)
            }
            .profileScrollRefreshStyle()
            .refreshable {
                await store.refreshSelfProfilePage(tab: selectedTab, userId: userId)
            }
            .onChange(of: userId) { _, _ in
                scrollOffset = 0
                pullDownOffset = 0
                pendingScrollToY = nil
            }
            .onChange(of: selectedTab) { _, _ in
                suppressContentTapsUntil = Date().addingTimeInterval(0.35)
                let pinOffset = XhsProfileLayout.tabPinScrollOffset()
                guard scrollOffset >= pinOffset - 2 else { return }
                pendingScrollToY = pinOffset
                Task { @MainActor in
                    try? await Task.sleep(nanoseconds: 120_000_000)
                    pendingScrollToY = nil
                }
            }
            .onChange(of: tabUiState.isInitialLoading) { wasLoading, isLoading in
                guard wasLoading, !isLoading else { return }
                let pinOffset = XhsProfileLayout.tabPinScrollOffset()
                guard scrollOffset >= pinOffset - 2 else { return }
                pendingScrollToY = pinOffset
                Task { @MainActor in
                    try? await Task.sleep(nanoseconds: 80_000_000)
                    pendingScrollToY = nil
                }
            }

            XhsProfileRefreshIndicator(
                isRefreshing: tabUiState.isRefreshing,
                pullDownOffset: scrollOffset <= 1 ? pullDownOffset : 0
            )
            .allowsHitTesting(false)
            .zIndex(5)

            XhsProfileGradientTopNav(
                profile: profile,
                mode: .selfProfile,
                collapseProgress: navCollapseProgress,
                showAvatar: showCollapsedNav,
                onMenu: onOpenSideMenu,
                onBack: nil,
                onEditProfile: onEditProfile,
                onScanProfile: onScanProfile,
                onShare: { ProfileShareHelper.presentShareSheet(for: profile) }
            )
            .zIndex(10)
        }
        .ignoresSafeArea(edges: .top)
    }

    private func profileCoverBlock(profile: UserProfileDto, coverUrl: String?) -> some View {
        let stretch = scrollOffset <= 1 ? max(0, pullDownOffset) : 0
        let topInset = XhsProfileLayout.statusBarTopInset
        let imageBase = XhsProfileLayout.coverHeight + topInset
        let scaleY = (imageBase + stretch) / imageBase

        return ZStack(alignment: .bottom) {
            XhsProfileCoverImage(coverUrl: coverUrl)
                .frame(height: imageBase)
                .scaleEffect(x: 1, y: scaleY, anchor: .top)
                .frame(height: imageBase + stretch, alignment: .top)

            XhsProfileCoverGradient()
                .frame(height: imageBase)
                .scaleEffect(x: 1, y: scaleY, anchor: .top)
                .frame(height: imageBase + stretch, alignment: .top)

            VStack(alignment: .leading, spacing: 0) {
                Spacer(minLength: XhsProfileLayout.topNavTotalHeight + 24)
                identityBlock(profile: profile)
                statsRow(profile: profile)
                    .padding(.top, 14)
                Text(displayBio(for: profile))
                    .font(.system(size: 13))
                    .foregroundStyle(.white.opacity(0.88))
                    .lineLimit(2)
                    .padding(.top, 10)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.bottom, 72)
        }
        .frame(height: XhsProfileLayout.coverHeight + stretch)
        .clipped()
        .offset(y: stretch > 0 ? -stretch : 0)
        .padding(.bottom, stretch > 0 ? -stretch : 0)
    }

    private func pinnedTabBarHeader(profile: UserProfileDto, userId: Int) -> some View {
        profileTabBar(
            profile: profile,
            userId: userId,
            roundedTop: !tabsArePinned
        )
        .background(Color.white)
        .padding(.top, tabsArePinned ? XhsProfileLayout.topNavTotalHeight : 0)
    }

    private func profileTabBar(profile: UserProfileDto, userId: Int, roundedTop: Bool) -> some View {
        XhsProfileTabBarView(
            tabs: buildSelfProfileTabItems(profile: profile),
            selectedTab: selectedTab,
            roundedTop: roundedTop,
            onSelect: { tab in
                guard tab != selectedTab else { return }
                suppressContentTapsUntil = Date().addingTimeInterval(0.35)
                Task { await store.selectSelfProfileTab(tab, userId: userId) }
            }
        )
    }

    private func identityBlock(profile: UserProfileDto) -> some View {
        HStack(alignment: .center, spacing: 14) {
            XhsProfileAvatar(
                label: profile.displayName,
                size: 68,
                avatarUrl: profile.avatarUrl
            )
            .overlay(Circle().stroke(Color.white, lineWidth: 2))

            VStack(alignment: .leading, spacing: 4) {
                Text(profile.displayName)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(.white)
                    .lineLimit(1)
                HStack(spacing: 4) {
                    Text("莲峰号：\(profile.lfcNo)")
                        .font(.system(size: 12))
                        .foregroundStyle(.white.opacity(0.78))
                    Button(action: onShowQr) {
                        Image(systemName: "qrcode")
                            .font(.system(size: 12))
                            .foregroundStyle(.white.opacity(0.85))
                    }
                    .buttonStyle(.plain)
                }
            }
            Spacer(minLength: 0)
        }
    }

    private func displayBio(for profile: UserProfileDto) -> String {
        if let bio = profile.bio?.trimmingCharacters(in: .whitespacesAndNewlines), !bio.isEmpty {
            return bio
        }
        return "莲峰校园 · 记录校园生活"
    }

    private func statsRow(profile: UserProfileDto) -> some View {
        HStack(spacing: 28) {
            selfProfileStatWhite(count: profile.followingCount, label: "关注")
            selfProfileStatWhite(count: profile.followerCount, label: "粉丝")
            selfProfileStatWhite(count: profile.likeAndFavoriteCount, label: "获赞与收藏")
        }
    }

    private var alipayBanner: some View {
        Button(action: onBindAlipay) {
            HStack(spacing: 10) {
                Image(systemName: "creditcard.fill")
                    .font(.system(size: 16))
                    .foregroundStyle(XhsTheme.red)
                VStack(alignment: .leading, spacing: 2) {
                    Text("绑定支付宝收款")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(XhsTheme.textPrimary)
                    Text("发布付费内容前需完成绑定")
                        .font(.system(size: 12))
                        .foregroundStyle(XhsTheme.textSecondary)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 12))
                    .foregroundStyle(XhsTheme.textSecondary)
            }
            .padding(12)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private func tabContent(profile: UserProfileDto, userId: Int) -> some View {
        if tabUiState.isInitialLoading {
            FeedGridSkeletonStatic(itemCount: 4)
                .background(Color.white)
                .padding(.top, 8)
        } else if isSelfTabContentEmpty(tabIndex: selectedTab) {
            emptyHint(selfProfileTabEmptyMessage(selectedTab))
                .frame(minHeight: 280)
                .frame(maxWidth: .infinity)
                .background(Color.white)
        } else {
            switch selectedTab {
            case 0:
                notesGrid(profile: profile, userId: userId)
            case 1:
                activitiesGrid(profile: profile, userId: userId)
            case selfProfileCommentsTab:
                commentsList(userId: userId)
            case selfProfileFavoritesTab:
                libraryGrid(
                    items: buildProfileLibraryFeed(
                        posts: store.profileState.profileFavoritePosts,
                        activities: store.profileState.profileFavoriteActivities
                    ),
                    profile: profile,
                    userId: userId
                )
            case selfProfileLikesTab:
                libraryGrid(
                    items: buildProfileLibraryFeed(
                        posts: store.profileState.profileLikedPosts,
                        activities: store.profileState.profileLikedActivities
                    ),
                    profile: profile,
                    userId: userId
                )
            default:
                EmptyView()
            }

            ProfileTabPaginationFooter(
                tabState: tabUiState,
                isContentEmpty: isSelfTabContentEmpty(tabIndex: selectedTab),
                onLoadMore: {
                    Task { await store.loadMoreSelfProfileTab(tab: selectedTab, userId: userId) }
                }
            )
        }
    }

    private func notesGrid(profile: UserProfileDto, userId: Int) -> some View {
        let posts = store.profileState.profileNotes
        let firstPostId = posts.first?.id
        return WaterfallFeedGrid(
            posts: posts,
            spacing: 8
        ) { post in
            XhsProfileFeedCard(
                post: post,
                userLat: store.userLatitude,
                userLng: store.userLongitude,
                isPinned: post.id == firstPostId,
                onTap: { onPostTap(post.id) }
            )
            .onAppear {
                triggerLoadMoreIfNeeded(
                    itemId: post.id,
                    ids: posts.map(\.id),
                    userId: userId
                )
            }
        }
        .padding(8)
        .background(Color.white)
    }

    private func activitiesGrid(profile: UserProfileDto, userId: Int) -> some View {
        let activities = store.profileState.profileActivities
        let columns = WaterfallColumnSplit.split(activities, spacing: 8, estimatedHeight: estimatedActivityCardHeight(for:))
        return HStack(alignment: .top, spacing: 8) {
            LazyVStack(spacing: 8) {
                ForEach(columns.left) { activity in
                    activityCard(activity: activity, profile: profile, userId: userId)
                }
            }
            .frame(maxWidth: .infinity, alignment: .top)

            LazyVStack(spacing: 8) {
                ForEach(columns.right) { activity in
                    activityCard(activity: activity, profile: profile, userId: userId)
                }
            }
            .frame(maxWidth: .infinity, alignment: .top)
        }
        .padding(8)
        .background(Color.white)
    }

    private func activityCard(activity: ActivityDto, profile: UserProfileDto, userId: Int) -> some View {
        XhsProfileActivityCard(
            activity: activity,
            authorLabel: activity.author?.displayName ?? profile.displayName,
            onTap: { onActivityTap(activity.id) }
        )
        .frame(maxWidth: .infinity)
        .onAppear {
            triggerLoadMoreIfNeeded(
                itemId: activity.id,
                ids: store.profileState.profileActivities.map(\.id),
                userId: userId
            )
        }
    }

    private func commentsList(userId: Int) -> some View {
        let comments = store.profileState.profileComments
        return LazyVStack(spacing: 8) {
            ForEach(comments) { comment in
                Button {
                    onPostTap(comment.postId)
                } label: {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(comment.content)
                            .font(.system(size: 14))
                            .foregroundStyle(XhsTheme.textPrimary)
                            .lineLimit(3)
                            .multilineTextAlignment(.leading)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        Text(comment.post?.title ?? "查看原笔记")
                            .font(.system(size: 12))
                            .foregroundStyle(XhsTheme.textSecondary)
                            .lineLimit(1)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        Text(formatSelfProfileDate(comment.createdAt))
                            .font(.system(size: 11))
                            .foregroundStyle(XhsTheme.textSecondary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .padding(12)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                }
                .buttonStyle(.plain)
                .onAppear {
                    triggerLoadMoreIfNeeded(
                        itemId: comment.id,
                        ids: comments.map(\.id),
                        userId: userId
                    )
                }
            }
        }
        .padding(.horizontal, 8)
        .padding(.top, 8)
        .background(Color(red: 0.96, green: 0.96, blue: 0.96))
    }

    private func libraryGrid(
        items: [ProfileLibraryFeedItem],
        profile: UserProfileDto,
        userId: Int
    ) -> some View {
        let columns = WaterfallColumnSplit.split(items, spacing: 8, estimatedHeight: estimatedProfileLibraryItemHeight)

        return HStack(alignment: .top, spacing: 8) {
            LazyVStack(spacing: 8) {
                ForEach(columns.left) { item in
                    libraryItemView(item: item, profile: profile, userId: userId, items: items)
                }
            }
            .frame(maxWidth: .infinity, alignment: .top)

            LazyVStack(spacing: 8) {
                ForEach(columns.right) { item in
                    libraryItemView(item: item, profile: profile, userId: userId, items: items)
                }
            }
            .frame(maxWidth: .infinity, alignment: .top)
        }
        .padding(8)
        .background(Color.white)
    }

    @ViewBuilder
    private func libraryItemView(
        item: ProfileLibraryFeedItem,
        profile: UserProfileDto,
        userId: Int,
        items: [ProfileLibraryFeedItem]
    ) -> some View {
        switch item {
        case .post(let post):
            XhsProfileFeedCard(
                post: post,
                userLat: store.userLatitude,
                userLng: store.userLongitude,
                onTap: { onPostTap(post.id) }
            )
            .frame(maxWidth: .infinity)
            .onAppear { triggerLibraryLoadMoreIfNeeded(item: item, items: items, userId: userId) }
        case .activity(let activity):
            XhsProfileActivityCard(
                activity: activity,
                authorLabel: activity.author?.displayName ?? profile.displayName,
                onTap: { onActivityTap(activity.id) }
            )
            .frame(maxWidth: .infinity)
            .onAppear { triggerLibraryLoadMoreIfNeeded(item: item, items: items, userId: userId) }
        }
    }

    private func emptyHint(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 14))
            .foregroundStyle(XhsTheme.textSecondary)
    }

    private func triggerLoadMoreIfNeeded(itemId: Int, ids: [Int], userId: Int) {
        guard let index = ids.firstIndex(of: itemId) else { return }
        guard profileShouldTriggerLoadMore(
            itemIndex: index,
            totalCount: ids.count,
            tabState: tabUiState
        ) else { return }
        Task { await store.loadMoreSelfProfileTab(tab: selectedTab, userId: userId) }
    }

    private func triggerLibraryLoadMoreIfNeeded(
        item: ProfileLibraryFeedItem,
        items: [ProfileLibraryFeedItem],
        userId: Int
    ) {
        guard let index = items.firstIndex(where: { $0.id == item.id }) else { return }
        guard profileShouldTriggerLoadMore(
            itemIndex: index,
            totalCount: items.count,
            tabState: tabUiState
        ) else { return }
        Task { await store.loadMoreSelfProfileTab(tab: selectedTab, userId: userId) }
    }

    private func buildSelfProfileTabItems(profile: UserProfileDto) -> [XhsProfileTabBarItem] {
        let counts = resolveSelfProfileTabCounts(profile: profile)
        return [
            XhsProfileTabBarItem(label: "笔记", contentIndex: 0, count: counts.notes),
            XhsProfileTabBarItem(label: "活动", contentIndex: 1, count: counts.activities),
            XhsProfileTabBarItem(
                label: "评论",
                contentIndex: selfProfileCommentsTab,
                count: counts.comments,
                locked: isSelfTabLocked(profile: profile, tabIndex: selfProfileCommentsTab)
            ),
            XhsProfileTabBarItem(
                label: "收藏",
                contentIndex: selfProfileFavoritesTab,
                count: counts.favorites,
                locked: isSelfTabLocked(profile: profile, tabIndex: selfProfileFavoritesTab)
            ),
            XhsProfileTabBarItem(
                label: "赞",
                contentIndex: selfProfileLikesTab,
                count: counts.likes,
                locked: isSelfTabLocked(profile: profile, tabIndex: selfProfileLikesTab)
            )
        ]
    }

    private func resolveSelfProfileTabCounts(profile: UserProfileDto) -> (
        notes: Int, activities: Int, comments: Int, favorites: Int, likes: Int
    ) {
        func tabTotal(_ index: Int, listSize: Int, profileFallback: Int = 0) -> Int {
            if let fromTab = profileTabs.tabs[safe: index]?.totalCount { return fromTab }
            if profileFallback > 0 { return profileFallback }
            if listSize > 0 { return listSize }
            return 0
        }

        let favoritesTotal: Int = {
            if profileTabs.favoritesPostsTotal != nil || profileTabs.favoritesActivitiesTotal != nil {
                return (profileTabs.favoritesPostsTotal ?? 0) + (profileTabs.favoritesActivitiesTotal ?? 0)
            }
            let posts = store.profileState.profileFavoritePosts.count
            let activities = store.profileState.profileFavoriteActivities.count
            if posts + activities > 0 { return posts + activities }
            return profileTabs.tabs[safe: selfProfileFavoritesTab]?.totalCount ?? 0
        }()

        let likesTotal: Int = {
            if profileTabs.likedPostsTotal != nil || profileTabs.likedActivitiesTotal != nil {
                return (profileTabs.likedPostsTotal ?? 0) + (profileTabs.likedActivitiesTotal ?? 0)
            }
            let posts = store.profileState.profileLikedPosts.count
            let activities = store.profileState.profileLikedActivities.count
            if posts + activities > 0 { return posts + activities }
            return profileTabs.tabs[safe: selfProfileLikesTab]?.totalCount ?? 0
        }()

        return (
            notes: tabTotal(0, listSize: store.profileState.profileNotes.count, profileFallback: profile.postCount),
            activities: tabTotal(
                1,
                listSize: store.profileState.profileActivities.count,
                profileFallback: profile.activities.count
            ),
            comments: tabTotal(2, listSize: store.profileState.profileComments.count),
            favorites: favoritesTotal,
            likes: likesTotal
        )
    }

    private func isSelfTabLocked(profile: UserProfileDto, tabIndex: Int) -> Bool {
        switch tabIndex {
        case selfProfileCommentsTab: !profile.showCommentsPublic
        case selfProfileFavoritesTab: !profile.showFavoritesPublic
        case selfProfileLikesTab: !profile.showLikesPublic
        default: false
        }
    }

    private func isSelfTabContentEmpty(tabIndex: Int) -> Bool {
        switch tabIndex {
        case 0: store.profileState.profileNotes.isEmpty
        case 1: store.profileState.profileActivities.isEmpty
        case selfProfileCommentsTab: store.profileState.profileComments.isEmpty
        case selfProfileFavoritesTab:
            store.profileState.profileFavoritePosts.isEmpty
                && store.profileState.profileFavoriteActivities.isEmpty
        case selfProfileLikesTab:
            store.profileState.profileLikedPosts.isEmpty
                && store.profileState.profileLikedActivities.isEmpty
        default: true
        }
    }

    private func selfProfileTabEmptyMessage(_ tabIndex: Int) -> String {
        switch tabIndex {
        case 0: "还没有发布笔记"
        case 1: "还没有发布活动"
        case selfProfileCommentsTab: "还没有发表评论"
        case selfProfileFavoritesTab: "还没有收藏内容"
        case selfProfileLikesTab: "还没有赞过内容"
        default: ""
        }
    }
}

private func selfProfileStatWhite(count: Int, label: String) -> some View {
    HStack(alignment: .lastTextBaseline, spacing: 2) {
        Text(formatProfileStatCount(count))
            .font(.system(size: 15, weight: .bold))
            .foregroundStyle(.white)
            .shadow(color: Color.black.opacity(0.35), radius: 2, y: 1)
        Text(label)
            .font(.system(size: 12))
            .foregroundStyle(.white.opacity(0.82))
            .shadow(color: Color.black.opacity(0.35), radius: 2, y: 1)
    }
}

private func formatSelfProfileDate(_ iso: String) -> String {
    let normalized = iso.replacingOccurrences(of: "T", with: " ").prefix(10)
    return normalized.count >= 10 ? String(normalized) : iso
}
