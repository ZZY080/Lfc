import SwiftUI

private let profileFavoritesTab = 3
private let profileLikesTab = 4
private let profileSheetTopGap: CGFloat = 12

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

    private var collapseProgress: CGFloat {
        XhsProfileLayout.visitorCollapseProgress(for: scrollOffset, gap: profileSheetTopGap)
    }

    private var tabsArePinned: Bool {
        scrollOffset >= XhsProfileLayout.visitorTabPinScrollOffset(gap: profileSheetTopGap) - 2
    }

    private var navCollapseProgress: CGFloat {
        max(collapseProgress, tabsArePinned ? 1 : 0)
    }

    private var showCollapsedNav: Bool {
        tabsArePinned
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
        .ignoresSafeArea(edges: .top)
        .appStatusBarStyle(.lightContent)
    }

    @ViewBuilder
    private func profileScroll(profile: UserProfileDto) -> some View {
        let coverUrl = profile.coverUrl ?? profile.posts.first?.images?.first
        ZStack(alignment: .top) {
            ScrollView {
                LazyVStack(spacing: 0, pinnedViews: [.sectionHeaders]) {
                    profileCoverBlock(profile: profile, coverUrl: coverUrl)

                    Color(red: 0.96, green: 0.96, blue: 0.96)
                        .frame(height: profileSheetTopGap)

                    Section {
                        tabContent(profile: profile, pinned: tabsArePinned)
                            .allowsHitTesting(!shouldSuppressContentTaps)
                    } header: {
                        pinnedTabBarHeader(profile: profile)
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
                await store.refreshVisitorProfilePage(tab: selectedTab, userId: userId)
            }
            .onChange(of: userId) { _, _ in
                scrollOffset = 0
                pullDownOffset = 0
                pendingScrollToY = nil
            }
            .onChange(of: selectedTab) { _, _ in
                suppressContentTapsUntil = Date().addingTimeInterval(0.35)
                let pinOffset = XhsProfileLayout.visitorTabPinScrollOffset(gap: profileSheetTopGap)
                guard scrollOffset >= pinOffset - 2 else { return }
                pendingScrollToY = pinOffset
                Task { @MainActor in
                    try? await Task.sleep(nanoseconds: 120_000_000)
                    pendingScrollToY = nil
                }
            }
            .onChange(of: tabUiState.isInitialLoading) { wasLoading, isLoading in
                guard wasLoading, !isLoading else { return }
                let pinOffset = XhsProfileLayout.visitorTabPinScrollOffset(gap: profileSheetTopGap)
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
                mode: .visitor,
                collapseProgress: navCollapseProgress,
                showAvatar: showCollapsedNav,
                onMenu: nil,
                onBack: onBack,
                onEditProfile: nil,
                onScanProfile: nil,
                onShare: nil
            )
            .zIndex(10)
        }
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
                if !isSelf {
                    actionRow(profile: profile)
                        .padding(.top, 14)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.bottom, 16)
        }
        .frame(height: XhsProfileLayout.coverHeight + stretch)
        .clipped()
        .offset(y: stretch > 0 ? -stretch : 0)
    }

    private func pinnedTabBarHeader(profile: UserProfileDto) -> some View {
        profileTabBar(profile: profile, roundedTop: !tabsArePinned)
            .background(Color.white)
            .padding(.top, tabsArePinned ? XhsProfileLayout.topNavTotalHeight : 0)
    }

    private func profileTabBar(profile: UserProfileDto, roundedTop: Bool) -> some View {
        XhsProfileTabBarView(
            tabs: buildVisitorProfileTabItems(profile: profile),
            selectedTab: selectedTab,
            roundedTop: roundedTop,
            topCornerRadius: 18,
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
                size: 72,
                avatarUrl: profile.avatarUrl
            )
            .overlay(Circle().stroke(Color.white, lineWidth: 2))

            VStack(alignment: .leading, spacing: 4) {
                Text(profile.displayName)
                    .font(.system(size: 22, weight: .bold))
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

    private func actionRow(profile: UserProfileDto) -> some View {
        HStack(spacing: 10) {
            Button {
                Task { await store.toggleFollow(userId: profile.id) }
            } label: {
                Text(profile.isFollowing ? "已关注" : "关注")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 38)
                    .background(profile.isFollowing ? Color.white.opacity(0.2) : XhsTheme.red)
                    .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            }
            .buttonStyle(.plain)

            Button {
                Task {
                    if let conversationId = await store.startConversation(peerUserId: profile.id) {
                        onOpenChat(conversationId)
                    }
                }
            } label: {
                Text("发私信")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 38)
                    .background(Color.white.opacity(0.18))
                    .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            }
            .buttonStyle(.plain)

            Button {
                ProfileShareHelper.presentShareSheet(for: profile)
            } label: {
                Image(systemName: "square.and.arrow.up")
                    .font(.system(size: 16))
                    .foregroundStyle(.white)
                    .frame(width: 38, height: 38)
                    .background(Color.white.opacity(0.18))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            .buttonStyle(.plain)
        }
    }

    @ViewBuilder
    private func tabContent(profile: UserProfileDto, pinned: Bool) -> some View {
        let locked = isTabLocked(profile: profile, tabIndex: selectedTab)
        let accessible = isTabAccessible(profile: profile, tabIndex: selectedTab)

        if locked || (!accessible && selectedTab >= profileFavoritesTab) {
            lockedHint
                .frame(minHeight: 280)
                .frame(maxWidth: .infinity)
                .background(Color.white)
        } else if tabUiState.isInitialLoading {
            FeedGridSkeletonStatic(itemCount: 4)
                .background(Color.white)
                .padding(.top, pinned ? 0 : 8)
        } else if isTabContentEmpty(tabIndex: selectedTab) {
            emptyHint(profileTabEmptyMessage(selectedTab))
                .frame(minHeight: 280)
                .frame(maxWidth: .infinity)
                .background(Color.white)
        } else {
            switch selectedTab {
            case 0:
                notesGrid(profile: profile, pinned: pinned)
            case 1:
                activitiesGrid(profile: profile, pinned: pinned)
            case profileFavoritesTab:
                libraryGrid(
                    items: buildProfileLibraryFeed(
                        posts: store.profileState.visitorProfileFavoritePosts,
                        activities: store.profileState.visitorProfileFavoriteActivities
                    ),
                    profile: profile,
                    pinned: pinned
                )
            case profileLikesTab:
                libraryGrid(
                    items: buildProfileLibraryFeed(
                        posts: store.profileState.visitorProfileLikedPosts,
                        activities: store.profileState.visitorProfileLikedActivities
                    ),
                    profile: profile,
                    pinned: pinned
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

    private func notesGrid(profile: UserProfileDto, pinned: Bool) -> some View {
        WaterfallFeedGrid(
            posts: store.profileState.visitorProfileNotes,
            spacing: 8
        ) { post in
            XhsProfileFeedCard(post: post, onTap: { onPostTap(post.id) })
                .onAppear {
                    triggerLoadMoreIfNeeded(
                        itemId: post.id,
                        ids: store.profileState.visitorProfileNotes.map(\.id)
                    )
                }
        }
        .padding(.init(top: pinned ? 0 : 8, leading: 8, bottom: 8, trailing: 8))
        .background(Color.white)
    }

    private func activitiesGrid(profile: UserProfileDto, pinned: Bool) -> some View {
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
        .padding(.init(top: pinned ? 0 : 8, leading: 8, bottom: 8, trailing: 8))
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

    private func libraryGrid(items: [ProfileLibraryFeedItem], profile: UserProfileDto, pinned: Bool) -> some View {
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
        .padding(.init(top: pinned ? 0 : 8, leading: 8, bottom: 8, trailing: 8))
        .background(Color.white)
    }

    @ViewBuilder
    private func libraryItemView(item: ProfileLibraryFeedItem, profile: UserProfileDto) -> some View {
        switch item {
        case .post(let post):
            XhsProfileFeedCard(post: post, onTap: { onPostTap(post.id) })
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
        notes: Int, activities: Int, favorites: Int, likes: Int
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
            favorites: favoritesTotal,
            likes: likesTotal
        )
    }

    private func isTabLocked(profile: UserProfileDto, tabIndex: Int) -> Bool {
        switch tabIndex {
        case profileFavoritesTab: !profile.showFavoritesPublic
        case profileLikesTab: !profile.showLikesPublic
        default: false
        }
    }

    private func isTabAccessible(profile: UserProfileDto, tabIndex: Int) -> Bool {
        if tabIndex < profileFavoritesTab { return true }
        return switch tabIndex {
        case profileFavoritesTab: profile.showFavoritesPublic
        case profileLikesTab: profile.showLikesPublic
        default: true
        }
    }

    private func isTabContentEmpty(tabIndex: Int) -> Bool {
        switch tabIndex {
        case 0: store.profileState.visitorProfileNotes.isEmpty
        case 1: store.profileState.visitorProfileActivities.isEmpty
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
