import Foundation

private let profileFavoritesTab = 3
private let profileLikesTab = 4
private let profileTabPageSize = 10

extension HomeStore {
    func enterUserProfile(_ userId: Int) async {
        let requestID = visitorProfileRequestID + 1
        visitorProfileRequestID = requestID
        visitorProfileTargetId = userId
        visitorProfileLoadFailed = false

        profileState.visitorProfileContentTab = 0
        profileState.isUserProfileLoading = true
        profileState.selectedUserProfile = nil
        profileState.visitorProfileNotes = []
        profileState.visitorProfileActivities = []
        profileState.visitorProfileComments = []
        profileState.visitorProfileFavoritePosts = []
        profileState.visitorProfileFavoriteActivities = []
        profileState.visitorProfileLikedPosts = []
        profileState.visitorProfileLikedActivities = []
        profileState.visitorProfileTabs = ProfileTabsUiState(targetUserId: userId)

        let seedFollowing = detailAuthorFollowing.flatMap { following in
            isVisitorDetailAuthor(userId) ? following : nil
        }

        do {
            var profile = try await api.getUserProfile(id: userId)
            if let seedFollowing {
                profile = profile.withFollowing(seedFollowing)
            }
            guard requestID == visitorProfileRequestID, visitorProfileTargetId == userId else {
                if requestID == visitorProfileRequestID {
                    profileState.isUserProfileLoading = false
                }
                return
            }

            profileState.selectedUserProfile = profile
            profileState.isUserProfileLoading = false
            profileState.visitorProfileTabs = seedProfileTabsFromProfile(userId: userId, profile: profile)

            AnalyticsTracker.shared.track(
                AnalyticsEvents.profileView,
                properties: ["userId": userId]
            )

            await loadVisitorProfileTab(tab: 0, userId: userId)
            await preloadVisitorProfileTabTotals(userId: userId)
        } catch {
            guard requestID == visitorProfileRequestID else { return }
            profileState.isUserProfileLoading = false
            visitorProfileLoadFailed = true
            toastError = parseError(error, fallback: "加载用户主页失败")
        }
    }

    func clearUserProfile() {
        visitorProfileTargetId = nil
        visitorProfileLoadFailed = false
        profileState.selectedUserProfile = nil
        profileState.isUserProfileLoading = false
        profileState.visitorProfileNotes = []
        profileState.visitorProfileActivities = []
        profileState.visitorProfileComments = []
        profileState.visitorProfileFavoritePosts = []
        profileState.visitorProfileFavoriteActivities = []
        profileState.visitorProfileLikedPosts = []
        profileState.visitorProfileLikedActivities = []
        profileState.visitorProfileTabs = ProfileTabsUiState()
        profileState.visitorProfileContentTab = 0
    }

    func selectVisitorProfileTab(_ tab: Int, userId: Int) async {
        guard profileState.visitorProfileContentTab != tab else { return }
        profileState.visitorProfileContentTab = tab
        await loadVisitorProfileTab(tab: tab, userId: userId)
    }

    func refreshVisitorProfileTab(tab: Int, userId: Int) async {
        await loadVisitorProfileTab(tab: tab, userId: userId, refresh: true)
    }

    /// Pull-to-refresh on visitor profile: refresh header stats and the active tab list.
    func refreshVisitorProfilePage(tab: Int, userId: Int) async {
        async let header: Void = refreshVisitorProfileHeader(userId: userId)
        async let tabData: Void = refreshVisitorProfileTab(tab: tab, userId: userId)
        async let totals: Void = preloadVisitorProfileTabTotals(userId: userId)
        _ = await (header, tabData, totals)
    }

    private func refreshVisitorProfileHeader(userId: Int) async {
        do {
            let previousFollowing = profileState.selectedUserProfile?.isFollowing
            var profile = try await api.getUserProfile(id: userId)
            if let previousFollowing {
                profile = profile.withFollowing(previousFollowing)
            }
            guard visitorProfileTargetId == userId else { return }
            profileState.selectedUserProfile = profile
        } catch {
            toastError = parseError(error, fallback: "刷新资料失败")
        }
    }

    func loadMoreVisitorProfileTab(tab: Int, userId: Int) async {
        await loadVisitorProfileTab(tab: tab, userId: userId, loadMore: true)
    }

    // MARK: - Tab loading

    func loadVisitorProfileTab(
        tab: Int,
        userId: Int,
        refresh: Bool = false,
        loadMore: Bool = false
    ) async {
        guard tab >= 0, tab <= profileLikesTab else { return }

        if tab == profileFavoritesTab || tab == profileLikesTab {
            await loadVisitorProfileLibraryTab(
                tab: tab,
                userId: userId,
                refresh: refresh,
                loadMore: loadMore
            )
            return
        }

        var tabsState = profileState.visitorProfileTabs
        if tabsState.targetUserId != userId {
            tabsState = ProfileTabsUiState(targetUserId: userId)
        }
        let tabState = tabsState.tabs[safe: tab] ?? ProfileTabUiState()

        if loadMore {
            guard tabState.hasMore, !tabState.isLoadingMore, !tabState.isRefreshing else { return }
        } else if !refresh {
            let hasData: Bool = switch tab {
            case 0: !profileState.visitorProfileNotes.isEmpty
            case 1: !profileState.visitorProfileActivities.isEmpty
            default: false
            }
            if hasData || tabState.isInitialLoading || tabState.isRefreshing { return }
        }

        let page = refresh || !loadMore ? 1 : tabState.page
        profileState.visitorProfileTabs = tabsState.withTab(
            tab,
            tabState.copy(
                isRefreshing: refresh,
                isLoadingMore: loadMore,
                isInitialLoading: !refresh && !loadMore
            )
        )

        do {
            let replace = !loadMore
            switch tab {
            case 0:
                let response = try await api.getUserPosts(id: userId, page: page, limit: profileTabPageSize)
                applyVisitorProfileTabResult(
                    tab: tab,
                    page: page,
                    hasMore: response.hasMore,
                    total: response.total,
                    replace: replace
                ) {
                    profileState.visitorProfileNotes = mergePosts(
                        profileState.visitorProfileNotes,
                        response.items,
                        replace: replace
                    )
                }
            case 1:
                let response = try await api.getUserActivities(id: userId, page: page, limit: profileTabPageSize)
                applyVisitorProfileTabResult(
                    tab: tab,
                    page: page,
                    hasMore: response.hasMore,
                    total: response.total,
                    replace: replace
                ) {
                    profileState.visitorProfileActivities = mergeActivities(
                        profileState.visitorProfileActivities,
                        response.items,
                        replace: replace
                    )
                }
            default:
                break
            }
        } catch {
            resetVisitorProfileTabLoading(tab: tab)
            toastError = parseError(error, fallback: "加载内容失败")
        }
    }

    private func loadVisitorProfileLibraryTab(
        tab: Int,
        userId: Int,
        refresh: Bool,
        loadMore: Bool
    ) async {
        let profile = profileState.selectedUserProfile
        let isFavoritesTab = tab == profileFavoritesTab
        let canLoad = isFavoritesTab
            ? (profile?.showFavoritesPublic == true)
            : (profile?.showLikesPublic == true)
        guard canLoad else { return }

        var tabsState = profileState.visitorProfileTabs
        if tabsState.targetUserId != userId {
            tabsState = ProfileTabsUiState(targetUserId: userId)
        }

        let postsState = tabsState.tabs[safe: tab] ?? ProfileTabUiState()
        let activitiesState = isFavoritesTab ? tabsState.favoriteActivities : tabsState.likedActivities

        if loadMore {
            let loading = postsState.isLoadingMore || activitiesState.isLoadingMore
                || postsState.isRefreshing || activitiesState.isRefreshing
            guard !loading else { return }

            if postsState.hasMore {
                await loadVisitorProfileLibrarySource(
                    userId: userId,
                    tab: tab,
                    isFavoritesTab: isFavoritesTab,
                    loadPosts: true,
                    page: postsState.page,
                    replace: false
                )
            } else if activitiesState.hasMore {
                await loadVisitorProfileLibrarySource(
                    userId: userId,
                    tab: tab,
                    isFavoritesTab: isFavoritesTab,
                    loadPosts: false,
                    page: activitiesState.page,
                    replace: false
                )
            }
            return
        }

        if !refresh {
            let busy = postsState.isInitialLoading || postsState.isRefreshing
                || postsState.isLoadingMore || activitiesState.isInitialLoading
                || activitiesState.isRefreshing || activitiesState.isLoadingMore
            if busy { return }
            if postsState.hasLoadedOnce && activitiesState.hasLoadedOnce { return }
        }

        profileState.visitorProfileTabs = {
            let updated = tabsState.withTab(
                tab,
                postsState.copy(
                    isRefreshing: refresh,
                    isLoadingMore: false,
                    isInitialLoading: !refresh
                )
            )
            if isFavoritesTab {
                return updated.withFavoriteActivities(
                    activitiesState.copy(
                        isRefreshing: refresh,
                        isLoadingMore: false,
                        isInitialLoading: !refresh
                    )
                )
            }
            return updated.withLikedActivities(
                activitiesState.copy(
                    isRefreshing: refresh,
                    isLoadingMore: false,
                    isInitialLoading: !refresh
                )
            )
        }()

        async let postsTask: Void = {
            if refresh || !postsState.hasLoadedOnce {
                await loadVisitorProfileLibrarySource(
                    userId: userId,
                    tab: tab,
                    isFavoritesTab: isFavoritesTab,
                    loadPosts: true,
                    page: 1,
                    replace: true
                )
            }
        }()

        async let activitiesTask: Void = {
            if refresh || !activitiesState.hasLoadedOnce {
                await loadVisitorProfileLibrarySource(
                    userId: userId,
                    tab: tab,
                    isFavoritesTab: isFavoritesTab,
                    loadPosts: false,
                    page: 1,
                    replace: true
                )
            }
        }()

        _ = await (postsTask, activitiesTask)
    }

    private func loadVisitorProfileLibrarySource(
        userId: Int,
        tab: Int,
        isFavoritesTab: Bool,
        loadPosts: Bool,
        page: Int,
        replace: Bool
    ) async {
        var tabsState = profileState.visitorProfileTabs
        if !replace {
            if loadPosts {
                tabsState = tabsState.withTab(
                    tab,
                    tabsState.tabs[tab].copy(isLoadingMore: true)
                )
            } else if isFavoritesTab {
                tabsState = tabsState.withFavoriteActivities(
                    tabsState.favoriteActivities.copy(isLoadingMore: true)
                )
            } else {
                tabsState = tabsState.withLikedActivities(
                    tabsState.likedActivities.copy(isLoadingMore: true)
                )
            }
            profileState.visitorProfileTabs = tabsState
        }

        do {
            if loadPosts {
                let response: PaginatedResponse<PostDto>
                if isFavoritesTab {
                    response = try await api.getUserFavoritePosts(id: userId, page: page, limit: profileTabPageSize)
                } else {
                    response = try await api.getUserLikedPosts(id: userId, page: page, limit: profileTabPageSize)
                }
                applyVisitorProfileLibraryPostsResult(
                    tab: tab,
                    page: page,
                    hasMore: response.hasMore,
                    total: response.total,
                    replace: replace
                ) {
                    if isFavoritesTab {
                        profileState.visitorProfileFavoritePosts = mergePosts(
                            profileState.visitorProfileFavoritePosts,
                            response.items,
                            replace: replace
                        )
                    } else {
                        profileState.visitorProfileLikedPosts = mergePosts(
                            profileState.visitorProfileLikedPosts,
                            response.items,
                            replace: replace
                        )
                    }
                }
            } else {
                let response: PaginatedResponse<ActivityDto>
                if isFavoritesTab {
                    response = try await api.getUserFavoriteActivities(id: userId, page: page, limit: profileTabPageSize)
                } else {
                    response = try await api.getUserLikedActivities(id: userId, page: page, limit: profileTabPageSize)
                }
                applyVisitorProfileLibraryActivitiesResult(
                    isFavorites: isFavoritesTab,
                    page: page,
                    hasMore: response.hasMore,
                    total: response.total,
                    replace: replace
                ) {
                    if isFavoritesTab {
                        profileState.visitorProfileFavoriteActivities = mergeActivities(
                            profileState.visitorProfileFavoriteActivities,
                            response.items,
                            replace: replace
                        )
                    } else {
                        profileState.visitorProfileLikedActivities = mergeActivities(
                            profileState.visitorProfileLikedActivities,
                            response.items,
                            replace: replace
                        )
                    }
                }
            }
        } catch {
            resetVisitorProfileLibraryLoading(tab: tab)
            toastError = parseError(error, fallback: "加载内容失败")
        }
    }

    private func preloadVisitorProfileTabTotals(userId: Int) async {
        guard profileState.visitorProfileTabs.targetUserId == userId else { return }

        async let favoritePosts = try? await api.getUserFavoritePosts(id: userId, page: 1, limit: 1)
        async let favoriteActivities = try? await api.getUserFavoriteActivities(id: userId, page: 1, limit: 1)
        async let likedPosts = try? await api.getUserLikedPosts(id: userId, page: 1, limit: 1)
        async let likedActivities = try? await api.getUserLikedActivities(id: userId, page: 1, limit: 1)

        let (favPosts, favActivities, likePosts, likeActivities) = await (
            favoritePosts, favoriteActivities, likedPosts, likedActivities
        )

        guard profileState.visitorProfileTabs.targetUserId == userId else { return }

        var tabsState = profileState.visitorProfileTabs
        let favoritesCombined = (favPosts?.total ?? tabsState.favoritesPostsTotal ?? 0)
            + (favActivities?.total ?? tabsState.favoritesActivitiesTotal ?? 0)
        let likesCombined = (likePosts?.total ?? tabsState.likedPostsTotal ?? 0)
            + (likeActivities?.total ?? tabsState.likedActivitiesTotal ?? 0)

        tabsState = tabsState.copy(
            favoritesPostsTotal: favPosts?.total ?? tabsState.favoritesPostsTotal,
            favoritesActivitiesTotal: favActivities?.total ?? tabsState.favoritesActivitiesTotal,
            likedPostsTotal: likePosts?.total ?? tabsState.likedPostsTotal,
            likedActivitiesTotal: likeActivities?.total ?? tabsState.likedActivitiesTotal
        )
        tabsState = tabsState.withTab(
            profileFavoritesTab,
            (tabsState.tabs[safe: profileFavoritesTab] ?? ProfileTabUiState())
                .copy(totalCount: favoritesCombined)
        )
        tabsState = tabsState.withTab(
            profileLikesTab,
            (tabsState.tabs[safe: profileLikesTab] ?? ProfileTabUiState())
                .copy(totalCount: likesCombined)
        )
        profileState.visitorProfileTabs = tabsState
    }

    // MARK: - Helpers

    private func seedProfileTabsFromProfile(userId: Int, profile: UserProfileDto) -> ProfileTabsUiState {
        var tabs = Array(repeating: ProfileTabUiState(), count: 5)
        tabs[0] = ProfileTabUiState(totalCount: profile.postCount)
        tabs[1] = ProfileTabUiState(totalCount: profile.activities.count)
        return ProfileTabsUiState(targetUserId: userId, tabs: tabs)
    }

    private func applyVisitorProfileTabResult(
        tab: Int,
        page: Int,
        hasMore: Bool,
        total: Int,
        replace: Bool,
        updateLists: () -> Void
    ) {
        updateLists()
        var tabsState = profileState.visitorProfileTabs
        let currentTabState = tabsState.tabs[safe: tab] ?? ProfileTabUiState()
        profileState.visitorProfileTabs = tabsState.withTab(
            tab,
            currentTabState.copy(
                page: page + 1,
                hasMore: hasMore,
                isRefreshing: false,
                isLoadingMore: false,
                isInitialLoading: false,
                hasLoadedOnce: true,
                totalCount: total
            )
        )
    }

    private func applyVisitorProfileLibraryPostsResult(
        tab: Int,
        page: Int,
        hasMore: Bool,
        total: Int,
        replace: Bool,
        updateLists: () -> Void
    ) {
        updateLists()
        var tabsState = profileState.visitorProfileTabs
        let currentTabState = tabsState.tabs[safe: tab] ?? ProfileTabUiState()
        let favoritesPostsTotal = tab == profileFavoritesTab ? total : tabsState.favoritesPostsTotal
        let likedPostsTotal = tab == profileLikesTab ? total : tabsState.likedPostsTotal
        let combinedTotal: Int = switch tab {
        case profileFavoritesTab:
            total + (tabsState.favoritesActivitiesTotal ?? 0)
        case profileLikesTab:
            total + (tabsState.likedActivitiesTotal ?? 0)
        default:
            currentTabState.totalCount ?? total
        }

        tabsState = ProfileTabsUiState(
            targetUserId: tabsState.targetUserId,
            tabs: tabsState.tabs,
            favoriteActivities: tabsState.favoriteActivities,
            likedActivities: tabsState.likedActivities,
            favoritesPostsTotal: favoritesPostsTotal,
            favoritesActivitiesTotal: tabsState.favoritesActivitiesTotal,
            likedPostsTotal: likedPostsTotal,
            likedActivitiesTotal: tabsState.likedActivitiesTotal
        )
        profileState.visitorProfileTabs = tabsState.withTab(
            tab,
            currentTabState.copy(
                page: page + 1,
                hasMore: hasMore,
                isRefreshing: false,
                isLoadingMore: false,
                isInitialLoading: false,
                hasLoadedOnce: true,
                totalCount: combinedTotal
            )
        )
    }

    private func applyVisitorProfileLibraryActivitiesResult(
        isFavorites: Bool,
        page: Int,
        hasMore: Bool,
        total: Int,
        replace: Bool,
        updateLists: () -> Void
    ) {
        updateLists()
        var tabsState = profileState.visitorProfileTabs
        let postsTab = isFavorites ? profileFavoritesTab : profileLikesTab
        let currentActivityState = isFavorites ? tabsState.favoriteActivities : tabsState.likedActivities
        let nextActivityState = currentActivityState.copy(
            page: page + 1,
            hasMore: hasMore,
            isRefreshing: false,
            isLoadingMore: false,
            isInitialLoading: false,
            hasLoadedOnce: true
        )

        let favoritesActivitiesTotal = isFavorites ? total : tabsState.favoritesActivitiesTotal
        let likedActivitiesTotal = isFavorites ? tabsState.likedActivitiesTotal : total
        let combinedTotal = isFavorites
            ? (tabsState.favoritesPostsTotal ?? 0) + total
            : (tabsState.likedPostsTotal ?? 0) + total

        tabsState = ProfileTabsUiState(
            targetUserId: tabsState.targetUserId,
            tabs: tabsState.tabs,
            favoriteActivities: isFavorites ? nextActivityState : tabsState.favoriteActivities,
            likedActivities: isFavorites ? tabsState.likedActivities : nextActivityState,
            favoritesPostsTotal: tabsState.favoritesPostsTotal,
            favoritesActivitiesTotal: favoritesActivitiesTotal,
            likedPostsTotal: tabsState.likedPostsTotal,
            likedActivitiesTotal: likedActivitiesTotal
        )
        let postsTabState = tabsState.tabs[safe: postsTab] ?? ProfileTabUiState()
        profileState.visitorProfileTabs = tabsState.withTab(
            postsTab,
            postsTabState.copy(totalCount: combinedTotal)
        )
    }

    private func resetVisitorProfileTabLoading(tab: Int) {
        var tabsState = profileState.visitorProfileTabs
        let current = tabsState.tabs[safe: tab] ?? ProfileTabUiState()
        profileState.visitorProfileTabs = tabsState.withTab(
            tab,
            ProfileTabUiState(page: current.page, hasMore: current.hasMore)
        )
    }

    private func resetVisitorProfileLibraryLoading(tab: Int) {
        var tabsState = profileState.visitorProfileTabs
        let postsState = tabsState.tabs[safe: tab] ?? ProfileTabUiState()
        let activityState = tab == profileFavoritesTab
            ? tabsState.favoriteActivities
            : tabsState.likedActivities
        tabsState = tabsState.withTab(
            tab,
            ProfileTabUiState(page: postsState.page, hasMore: postsState.hasMore)
        )
        profileState.visitorProfileTabs = tab == profileFavoritesTab
            ? tabsState.withFavoriteActivities(
                ProfileTabUiState(page: activityState.page, hasMore: activityState.hasMore)
            )
            : tabsState.withLikedActivities(
                ProfileTabUiState(page: activityState.page, hasMore: activityState.hasMore)
            )
    }

    private func mergePosts(_ existing: [PostDto], _ incoming: [PostDto], replace: Bool) -> [PostDto] {
        if replace { return incoming }
        return existing + incoming.filter { new in existing.allSatisfy { $0.id != new.id } }
    }

    private func mergeActivities(
        _ existing: [ActivityDto],
        _ incoming: [ActivityDto],
        replace: Bool
    ) -> [ActivityDto] {
        if replace { return incoming }
        return existing + incoming.filter { new in existing.allSatisfy { $0.id != new.id } }
    }

    private func isVisitorDetailAuthor(_ userId: Int) -> Bool {
        selectedPost?.authorId == userId || selectedActivity?.authorId == userId
    }
}

private extension ProfileTabUiState {
    func copy(
        page: Int? = nil,
        hasMore: Bool? = nil,
        isRefreshing: Bool? = nil,
        isLoadingMore: Bool? = nil,
        isInitialLoading: Bool? = nil,
        hasLoadedOnce: Bool? = nil,
        totalCount: Int?? = nil
    ) -> ProfileTabUiState {
        ProfileTabUiState(
            page: page ?? self.page,
            hasMore: hasMore ?? self.hasMore,
            isRefreshing: isRefreshing ?? self.isRefreshing,
            isLoadingMore: isLoadingMore ?? self.isLoadingMore,
            isInitialLoading: isInitialLoading ?? self.isInitialLoading,
            hasLoadedOnce: hasLoadedOnce ?? self.hasLoadedOnce,
            totalCount: totalCount ?? self.totalCount
        )
    }
}

private extension ProfileTabsUiState {
    func copy(
        favoritesPostsTotal: Int?? = nil,
        favoritesActivitiesTotal: Int?? = nil,
        likedPostsTotal: Int?? = nil,
        likedActivitiesTotal: Int?? = nil
    ) -> ProfileTabsUiState {
        ProfileTabsUiState(
            targetUserId: targetUserId,
            tabs: tabs,
            favoriteActivities: favoriteActivities,
            likedActivities: likedActivities,
            favoritesPostsTotal: favoritesPostsTotal ?? self.favoritesPostsTotal,
            favoritesActivitiesTotal: favoritesActivitiesTotal ?? self.favoritesActivitiesTotal,
            likedPostsTotal: likedPostsTotal ?? self.likedPostsTotal,
            likedActivitiesTotal: likedActivitiesTotal ?? self.likedActivitiesTotal
        )
    }
}

private extension UserProfileDto {
    func withFollowing(_ following: Bool) -> UserProfileDto {
        UserProfileDto(
            id: id,
            lfcNo: lfcNo,
            studentId: studentId,
            nickname: nickname,
            bio: bio,
            avatarUrl: avatarUrl,
            coverUrl: coverUrl,
            postCount: postCount,
            followingCount: followingCount,
            followerCount: followerCount,
            likeAndFavoriteCount: likeAndFavoriteCount,
            isFollowing: following,
            isSelf: isSelf,
            showCommentsPublic: showCommentsPublic,
            showFavoritesPublic: showFavoritesPublic,
            showLikesPublic: showLikesPublic,
            alipayBound: alipayBound,
            alipayLoginIdMasked: alipayLoginIdMasked,
            posts: posts,
            activities: activities,
            participationCount: participationCount
        )
    }
}
