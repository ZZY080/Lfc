import SwiftUI

private let profileCommentsTab = 2
private let profileFavoritesTab = 3
private let profileLikesTab = 4

struct XhsVisitorProfileView: View {
    let userId: Int
    @Bindable var store: HomeStore
    let currentUserId: Int?
    let onBack: () -> Void
    let onPostTap: (Int) -> Void
    let onActivityTap: (Int) -> Void
    let onOpenChat: (Int) -> Void

    @State private var scrollOffset: CGFloat = 0
    @State private var pullDownOffset: CGFloat = 0
    @State private var suppressContentTapsUntil = Date.distantPast
    @State private var pendingScrollToY: CGFloat?
    @State private var tabBarMinY: CGFloat = .greatestFiniteMagnitude

    private var collapseProgress: CGFloat {
        XhsProfileLayout.collapseProgress(for: scrollOffset, coverHeight: activeCoverHeight)
    }

    private var activeCoverHeight: CGFloat {
        guard let profile else { return XhsProfileLayout.coverHeight }
        return coverHeight(for: profile)
    }

    private func coverHeight(for profile: UserProfileDto) -> CGFloat {
        showsVisitorActions(for: profile)
            ? XhsProfileLayout.visitorCoverHeight
            : XhsProfileLayout.coverHeight
    }

    private var sectionHeaderPinOffset: CGFloat {
        XhsProfileLayout.sectionHeaderPinOffset(coverHeight: activeCoverHeight)
    }

    /// Fixed overlay tab bar once the in-scroll header would sit under the top nav.
    private var showStickyTabBar: Bool {
        let fromScroll = scrollOffset >= sectionHeaderPinOffset - 2
        let fromGeometry = tabBarMinY.isFinite
            && tabBarMinY < 10_000
            && tabBarMinY <= XhsProfileLayout.topNavTotalHeight + 2
        return fromScroll || fromGeometry
    }

    private var navCollapseProgress: CGFloat {
        max(collapseProgress, showStickyTabBar ? 1 : 0)
    }

    private var showCollapsedNav: Bool {
        showStickyTabBar
    }

    private var shouldSuppressContentTaps: Bool {
        Date() < suppressContentTapsUntil
    }

    private var profile: UserProfileDto? {
        guard store.profileState.selectedUserProfile?.id == userId else { return nil }
        return store.profileState.selectedUserProfile
    }

    private var isSelf: Bool {
        (store.profileState.myProfile?.id ?? currentUserId) == userId
    }

    private func showsVisitorActions(for profile: UserProfileDto) -> Bool {
        !isSelf && !profile.isSelf
    }

    private var isLoading: Bool {
        profile == nil && store.profileState.isUserProfileLoading
    }

    private var showLoadError: Bool {
        profile == nil
            && !store.profileState.isUserProfileLoading
            && store.visitorProfileLoadFailed
            && store.visitorProfileTargetId == userId
    }

    private var selectedTab: Int { store.profileState.visitorProfileContentTab }

    private var profileTabs: ProfileTabsUiState { store.profileState.visitorProfileTabs }

    private var tabUiState: ProfileTabUiState {
        profileTabUiStateFor(selectedTab: selectedTab, profileTabs: profileTabs)
    }

    var body: some View {
        Group {
            if isLoading {
                ProfilePageSkeleton()
            } else if let profile {
                profileScroll(profile: profile)
            } else if showLoadError {
                errorContent
            } else {
                ProfilePageSkeleton()
            }
        }
        .background(Color(red: 0.96, green: 0.96, blue: 0.96).ignoresSafeArea())
        .navigationBarHidden(true)
        .appStatusBarStyle(.lightContent)
    }

    @ViewBuilder
    private func profileScroll(profile: UserProfileDto) -> some View {
        let coverUrl = profile.coverUrl ?? profile.posts.first?.images?.first
        let blockHeight = coverHeight(for: profile)
        ZStack(alignment: .top) {
            ScrollView {
                LazyVStack(spacing: 0, pinnedViews: [.sectionHeaders]) {
                    profileCoverBlock(
                        profile: profile,
                        coverUrl: coverUrl,
                        coverHeight: blockHeight
                    )
                    .padding(.bottom, -XhsProfileLayout.sheetOverlap)

                    Section {
                        tabContent(profile: profile)
                            .allowsHitTesting(!shouldSuppressContentTaps)
                    } header: {
                        pinnedTabBarHeader(profile: profile)
                    }
                }
                .trackProfileScrollOffset($scrollOffset, pullDownOffset: $pullDownOffset, scrollToY: pendingScrollToY)
                .onPreferenceChange(ProfileTabBarMinYKey.self) { minY in
                    guard minY.isFinite else { return }
                    tabBarMinY = minY
                }
            }
            .scrollContentBackground(.hidden)
            .background {
                Color(red: 0.96, green: 0.96, blue: 0.96)
            }
            .background(alignment: .top) {
                XhsProfileLayout.coverDark
                    .frame(
                        height: blockHeight
                            + XhsProfileLayout.statusBarTopInset
                            + (scrollOffset <= 1 ? pullDownOffset : 0)
                    )
                    .frame(maxWidth: .infinity, alignment: .top)
                    .ignoresSafeArea(edges: .top)
            }
            .profileScrollRefreshStyle()
            .refreshable {
                await store.refreshVisitorProfilePage(tab: selectedTab, userId: userId)
            }
            .onChange(of: userId) { _, _ in
                scrollOffset = 0
                pullDownOffset = 0
                pendingScrollToY = nil
                tabBarMinY = .greatestFiniteMagnitude
            }
            .onAppear {
                scrollOffset = 0
                pullDownOffset = 0
                pendingScrollToY = nil
                tabBarMinY = .greatestFiniteMagnitude
            }
            .onChange(of: profile.id) { _, _ in
                scrollOffset = 0
                pullDownOffset = 0
                pendingScrollToY = nil
                tabBarMinY = .greatestFiniteMagnitude
            }
            .onChange(of: selectedTab) { _, _ in
                suppressContentTapsUntil = Date().addingTimeInterval(0.35)
                guard scrollOffset >= sectionHeaderPinOffset - 2 else { return }
                pendingScrollToY = sectionHeaderPinOffset
                Task { @MainActor in
                    try? await Task.sleep(nanoseconds: 120_000_000)
                    pendingScrollToY = nil
                }
            }
            .onChange(of: tabUiState.isInitialLoading) { wasLoading, isLoading in
                guard wasLoading, !isLoading else { return }
                guard scrollOffset >= sectionHeaderPinOffset - 2 else { return }
                pendingScrollToY = sectionHeaderPinOffset
                Task { @MainActor in
                    try? await Task.sleep(nanoseconds: 80_000_000)
                    pendingScrollToY = nil
                }
            }

            XhsProfileRefreshIndicator(
                isRefreshing: tabUiState.isRefreshing,
                pullDownOffset: scrollOffset <= 1 ? pullDownOffset : 0,
                coverHeight: blockHeight
            )
            .allowsHitTesting(false)
            .zIndex(5)

            if showStickyTabBar {
                profileTabBar(profile: profile, roundedTop: false)
                    .background(Color.white)
                    .padding(.top, XhsProfileLayout.topNavTotalHeight)
                    .frame(maxWidth: .infinity, alignment: .top)
                    .zIndex(8)
            }

            XhsProfileGradientTopNav(
                profile: profile,
                mode: .visitor,
                collapseProgress: navCollapseProgress,
                showAvatar: showCollapsedNav,
                onMenu: nil,
                onBack: onBack,
                onEditProfile: nil,
                onScanProfile: nil,
                onShare: { ProfileShareHelper.presentShareSheet(for: profile) },
                isFollowing: profile.isFollowing,
                onFollowToggle: isSelf ? nil : { Task { await store.toggleFollow(userId: profile.id) } }
            )
            .zIndex(10)
        }
        .ignoresSafeArea(edges: .top)
    }

    private func profileCoverBlock(
        profile: UserProfileDto,
        coverUrl: String?,
        coverHeight: CGFloat
    ) -> some View {
        let stretch = scrollOffset <= 1 ? max(0, pullDownOffset) : 0
        let showActions = showsVisitorActions(for: profile)
        let topInset = XhsProfileLayout.statusBarTopInset
        let imageBase = coverHeight + topInset
        let scaleY = (imageBase + stretch) / imageBase
        let blockHeight = coverHeight + stretch

        // Rectangle keeps LazyVStack height stable. Image stays in background (clipped);
        // foreground uses bottom overlay + fixedSize so avatar/stats are never top-clipped.
        return Rectangle()
            .fill(.clear)
            .frame(height: blockHeight)
            .background {
                ZStack(alignment: .top) {
                    XhsProfileCoverImage(coverUrl: coverUrl)
                        .frame(height: imageBase)
                        .scaleEffect(x: 1, y: scaleY, anchor: .top)
                        .frame(height: imageBase + stretch, alignment: .top)

                    XhsProfileCoverGradient()
                        .frame(height: imageBase)
                        .scaleEffect(x: 1, y: scaleY, anchor: .top)
                        .frame(height: imageBase + stretch, alignment: .top)

                    if showActions {
                        XhsProfileCoverBottomScrim(coverHeight: coverHeight)
                            .frame(height: blockHeight, alignment: .bottom)
                    }
                }
                .frame(height: blockHeight, alignment: .top)
                .clipped()
            }
            .overlay(alignment: .bottom) {
                profileCoverForeground(profile: profile, showActions: showActions)
                    .fixedSize(horizontal: false, vertical: true)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .offset(y: stretch > 0 ? -stretch : 0)
            .padding(.bottom, stretch > 0 ? -stretch : 0)
    }

    private func profileCoverForeground(profile: UserProfileDto, showActions: Bool) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            identityBlock(profile: profile)
            statsRow(profile: profile)
                .padding(.top, 14)
            Text(displayBio(for: profile))
                .font(.system(size: 13))
                .foregroundStyle(.white.opacity(0.88))
                .lineLimit(2)
                .padding(.top, 10)

            if showActions {
                XhsVisitorProfileCoverActionRow(
                    isFollowing: profile.isFollowing,
                    onFollow: { Task { await store.toggleFollow(userId: profile.id) } },
                    onMessage: {
                        Task {
                            if let conversationId = await store.startConversation(peerUserId: profile.id) {
                                onOpenChat(conversationId)
                            }
                        }
                    }
                )
                .padding(.top, 10)
            }
        }
        .padding(.horizontal, 16)
        .padding(
            .bottom,
            showActions ? XhsProfileLayout.visitorCoverBottomPadding : XhsProfileLayout.sheetOverlap + 58
        )
    }

    private func pinnedTabBarHeader(profile: UserProfileDto) -> some View {
        profileTabBar(
            profile: profile,
            roundedTop: !showStickyTabBar
        )
        .background(Color.white)
        .opacity(showStickyTabBar ? 0 : 1)
        .allowsHitTesting(!showStickyTabBar)
        .reportProfileTabBarMinY()
    }

    private func profileTabBar(profile: UserProfileDto, roundedTop: Bool) -> some View {
        XhsProfileTabBarView(
            tabs: buildVisitorProfileTabItems(profile: profile),
            selectedTab: selectedTab,
            roundedTop: roundedTop,
            onSelect: { tab in
                guard tab != selectedTab else { return }
                suppressContentTapsUntil = Date().addingTimeInterval(0.35)
                Task { await store.selectVisitorProfileTab(tab, userId: userId) }
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
                    Button {
                        UIPasteboard.general.string = profile.lfcNo
                        store.toastMessage = "已复制莲峰号"
                    } label: {
                        Image(systemName: "doc.on.doc")
                            .font(.system(size: 11))
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
            profileStatWhite(count: profile.followingCount, label: "关注")
            profileStatWhite(count: profile.followerCount, label: "粉丝")
            profileStatWhite(count: profile.likeAndFavoriteCount, label: "获赞与收藏")
        }
    }

    @ViewBuilder
    private func tabContent(profile: UserProfileDto) -> some View {
        if isTabLocked(profile: profile, tabIndex: selectedTab) {
            lockedHint
                .frame(minHeight: 280)
                .frame(maxWidth: .infinity)
                .background(Color.white)
        } else if tabUiState.isInitialLoading {
            FeedGridSkeletonStatic(itemCount: 4)
                .background(Color.white)
                .padding(.top, 8)
        } else if isTabContentEmpty(tabIndex: selectedTab) {
            emptyHint(profileTabEmptyMessage(selectedTab))
                .frame(minHeight: 280)
                .frame(maxWidth: .infinity)
                .background(Color.white)
        } else {
            switch selectedTab {
            case 0:
                notesGrid(profile: profile)
            case 1:
                activitiesGrid(profile: profile)
            case profileCommentsTab:
                commentsList()
            case profileFavoritesTab:
                libraryGrid(
                    items: buildProfileLibraryFeed(
                        posts: store.profileState.visitorProfileFavoritePosts,
                        activities: store.profileState.visitorProfileFavoriteActivities
                    ),
                    profile: profile
                )
            case profileLikesTab:
                libraryGrid(
                    items: buildProfileLibraryFeed(
                        posts: store.profileState.visitorProfileLikedPosts,
                        activities: store.profileState.visitorProfileLikedActivities
                    ),
                    profile: profile
                )
            default:
                EmptyView()
            }

            ProfileTabPaginationFooter(
                tabState: tabUiState,
                isContentEmpty: isTabContentEmpty(tabIndex: selectedTab),
                onLoadMore: {
                    Task { await store.loadMoreVisitorProfileTab(tab: selectedTab, userId: userId) }
                }
            )
        }
    }

    private func notesGrid(profile: UserProfileDto) -> some View {
        let posts = store.profileState.visitorProfileNotes
        return WaterfallFeedGrid(
            posts: posts,
            spacing: 8
        ) { post in
            XhsProfileFeedCard(
                post: post,
                userLat: store.userLatitude,
                userLng: store.userLongitude,
                onTap: { onPostTap(post.id) }
            )
            .onAppear {
                triggerLoadMoreIfNeeded(
                    itemId: post.id,
                    ids: posts.map(\.id)
                )
            }
        }
        .padding(8)
        .background(Color.white)
    }

    private func activitiesGrid(profile: UserProfileDto) -> some View {
        let activities = store.profileState.visitorProfileActivities
        return HStack(alignment: .top, spacing: 8) {
            LazyVStack(spacing: 8) {
                ForEach(leftItems(activities)) { activity in
                    activityCard(activity: activity, profile: profile)
                }
            }
            .frame(maxWidth: .infinity, alignment: .top)

            LazyVStack(spacing: 8) {
                ForEach(rightItems(activities)) { activity in
                    activityCard(activity: activity, profile: profile)
                }
            }
            .frame(maxWidth: .infinity, alignment: .top)
        }
        .padding(8)
        .background(Color.white)
    }

    private func activityCard(activity: ActivityDto, profile: UserProfileDto) -> some View {
        XhsProfileActivityCard(
            activity: activity,
            authorLabel: activity.author?.displayName ?? profile.displayName,
            onTap: { onActivityTap(activity.id) }
        )
        .frame(maxWidth: .infinity)
        .onAppear {
            triggerLoadMoreIfNeeded(
                itemId: activity.id,
                ids: store.profileState.visitorProfileActivities.map(\.id)
            )
        }
    }

    private func libraryGrid(items: [ProfileLibraryFeedItem], profile: UserProfileDto) -> some View {
        let left = items.enumerated().compactMap { index, item in index.isMultiple(of: 2) ? item : nil }
        let right = items.enumerated().compactMap { index, item in index.isMultiple(of: 2) ? nil : item }

        return HStack(alignment: .top, spacing: 8) {
            LazyVStack(spacing: 8) {
                ForEach(left) { item in
                    libraryItemView(item: item, profile: profile)
                }
            }
            .frame(maxWidth: .infinity, alignment: .top)

            LazyVStack(spacing: 8) {
                ForEach(right) { item in
                    libraryItemView(item: item, profile: profile)
                }
            }
            .frame(maxWidth: .infinity, alignment: .top)
        }
        .padding(8)
        .background(Color.white)
    }

    private func commentsList() -> some View {
        let comments = store.profileState.visitorProfileComments
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
                        Text(formatVisitorProfileDate(comment.createdAt))
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
                        ids: comments.map(\.id)
                    )
                }
            }
        }
        .padding(.horizontal, 8)
        .padding(.top, 8)
        .background(Color(red: 0.96, green: 0.96, blue: 0.96))
    }

    @ViewBuilder
    private func libraryItemView(item: ProfileLibraryFeedItem, profile: UserProfileDto) -> some View {
        switch item {
        case .post(let post):
            XhsProfileFeedCard(
                post: post,
                userLat: store.userLatitude,
                userLng: store.userLongitude,
                onTap: { onPostTap(post.id) }
            )
                .frame(maxWidth: .infinity)
                .onAppear { triggerLibraryLoadMoreIfNeeded(item: item, items: libraryItemsForCurrentTab) }
        case .activity(let activity):
            XhsProfileActivityCard(
                activity: activity,
                authorLabel: activity.author?.displayName ?? profile.displayName,
                onTap: { onActivityTap(activity.id) }
            )
            .frame(maxWidth: .infinity)
            .onAppear { triggerLibraryLoadMoreIfNeeded(item: item, items: libraryItemsForCurrentTab) }
        }
    }

    private var libraryItemsForCurrentTab: [ProfileLibraryFeedItem] {
        if selectedTab == profileFavoritesTab {
            return buildProfileLibraryFeed(
                posts: store.profileState.visitorProfileFavoritePosts,
                activities: store.profileState.visitorProfileFavoriteActivities
            )
        }
        return buildProfileLibraryFeed(
            posts: store.profileState.visitorProfileLikedPosts,
            activities: store.profileState.visitorProfileLikedActivities
        )
    }

    private var errorContent: some View {
        VStack(spacing: 0) {
            HStack {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundStyle(XhsTheme.textPrimary)
                }
                Spacer()
            }
            .padding(.horizontal, 12)
            .padding(.top, XhsProfileLayout.statusBarTopInset + 8)

            Spacer()
            VStack(spacing: 16) {
                ContentUnavailableView("加载失败", systemImage: "wifi.exclamationmark")
                Button("重试") {
                    Task { await store.enterUserProfile(userId) }
                }
                .buttonStyle(.borderedProminent)
                .tint(XhsTheme.red)
            }
            Spacer()
        }
    }

    private var lockedHint: some View {
        VStack(spacing: 8) {
            Image(systemName: "lock.fill")
                .font(.system(size: 28))
                .foregroundStyle(XhsTheme.textSecondary)
            Text("该用户设置了隐私")
                .font(.system(size: 14))
                .foregroundStyle(XhsTheme.textSecondary)
        }
    }

    private func emptyHint(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 14))
            .foregroundStyle(XhsTheme.textSecondary)
    }

    private func triggerLoadMoreIfNeeded(itemId: Int, ids: [Int]) {
        guard let index = ids.firstIndex(of: itemId) else { return }
        guard profileShouldTriggerLoadMore(
            itemIndex: index,
            totalCount: ids.count,
            tabState: tabUiState
        ) else { return }
        Task { await store.loadMoreVisitorProfileTab(tab: selectedTab, userId: userId) }
    }

    private func triggerLibraryLoadMoreIfNeeded(item: ProfileLibraryFeedItem, items: [ProfileLibraryFeedItem]) {
        guard let index = items.firstIndex(where: { $0.id == item.id }) else { return }
        guard profileShouldTriggerLoadMore(
            itemIndex: index,
            totalCount: items.count,
            tabState: tabUiState
        ) else { return }
        Task { await store.loadMoreVisitorProfileTab(tab: selectedTab, userId: userId) }
    }

    private func buildVisitorProfileTabItems(profile: UserProfileDto) -> [XhsProfileTabBarItem] {
        let counts = resolveProfileTabCounts(profile: profile)
        return [
            XhsProfileTabBarItem(label: "笔记", contentIndex: 0, count: counts.notes),
            XhsProfileTabBarItem(label: "活动", contentIndex: 1, count: counts.activities),
            XhsProfileTabBarItem(
                label: "评论",
                contentIndex: profileCommentsTab,
                count: counts.comments,
                locked: isTabLocked(profile: profile, tabIndex: profileCommentsTab)
            ),
            XhsProfileTabBarItem(
                label: "收藏",
                contentIndex: profileFavoritesTab,
                count: counts.favorites,
                locked: isTabLocked(profile: profile, tabIndex: profileFavoritesTab)
            ),
            XhsProfileTabBarItem(
                label: "赞",
                contentIndex: profileLikesTab,
                count: counts.likes,
                locked: isTabLocked(profile: profile, tabIndex: profileLikesTab)
            )
        ]
    }

    private func resolveProfileTabCounts(profile: UserProfileDto) -> (
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
            let posts = store.profileState.visitorProfileFavoritePosts.count
            let activities = store.profileState.visitorProfileFavoriteActivities.count
            if posts + activities > 0 { return posts + activities }
            return profileTabs.tabs[safe: profileFavoritesTab]?.totalCount ?? 0
        }()

        let likesTotal: Int = {
            if profileTabs.likedPostsTotal != nil || profileTabs.likedActivitiesTotal != nil {
                return (profileTabs.likedPostsTotal ?? 0) + (profileTabs.likedActivitiesTotal ?? 0)
            }
            let posts = store.profileState.visitorProfileLikedPosts.count
            let activities = store.profileState.visitorProfileLikedActivities.count
            if posts + activities > 0 { return posts + activities }
            return profileTabs.tabs[safe: profileLikesTab]?.totalCount ?? 0
        }()

        return (
            notes: tabTotal(0, listSize: store.profileState.visitorProfileNotes.count, profileFallback: profile.postCount),
            activities: tabTotal(
                1,
                listSize: store.profileState.visitorProfileActivities.count,
                profileFallback: profile.activities.count
            ),
            comments: tabTotal(2, listSize: store.profileState.visitorProfileComments.count),
            favorites: favoritesTotal,
            likes: likesTotal
        )
    }

    private func isTabLocked(profile: UserProfileDto, tabIndex: Int) -> Bool {
        switch tabIndex {
        case profileCommentsTab: !profile.showCommentsPublic
        case profileFavoritesTab: !profile.showFavoritesPublic
        case profileLikesTab: !profile.showLikesPublic
        default: false
        }
    }

    private func isTabContentEmpty(tabIndex: Int) -> Bool {
        switch tabIndex {
        case 0: store.profileState.visitorProfileNotes.isEmpty
        case 1: store.profileState.visitorProfileActivities.isEmpty
        case profileCommentsTab: store.profileState.visitorProfileComments.isEmpty
        case profileFavoritesTab:
            store.profileState.visitorProfileFavoritePosts.isEmpty
                && store.profileState.visitorProfileFavoriteActivities.isEmpty
        case profileLikesTab:
            store.profileState.visitorProfileLikedPosts.isEmpty
                && store.profileState.visitorProfileLikedActivities.isEmpty
        default: true
        }
    }

    private func profileTabEmptyMessage(_ tabIndex: Int) -> String {
        switch tabIndex {
        case 0: "还没有发布笔记"
        case 1: "还没有发布活动"
        case profileCommentsTab: "还没有发表评论"
        case profileFavoritesTab: "还没有收藏内容"
        case profileLikesTab: "还没有赞过内容"
        default: ""
        }
    }

    private func leftItems<T: Identifiable>(_ items: [T]) -> [T] where T.ID: Equatable {
        items.enumerated().compactMap { index, item in index.isMultiple(of: 2) ? item : nil }
    }

    private func rightItems<T: Identifiable>(_ items: [T]) -> [T] where T.ID: Equatable {
        items.enumerated().compactMap { index, item in index.isMultiple(of: 2) ? nil : item }
    }
}

private func profileStatWhite(count: Int, label: String) -> some View {
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

private func formatVisitorProfileDate(_ iso: String) -> String {
    let normalized = iso.replacingOccurrences(of: "T", with: " ").prefix(10)
    return normalized.count >= 10 ? String(normalized) : iso
}
