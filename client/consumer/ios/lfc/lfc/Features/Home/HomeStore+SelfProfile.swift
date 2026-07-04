import Foundation

private let selfProfileFavoritesTab = 3
private let selfProfileLikesTab = 4
private let selfProfileCommentsTab = 2
private let selfProfileTabPageSize = 10

extension HomeStore {
    func selectSelfProfileTab(_ tab: Int, userId: Int) async {
        guard profileState.profileContentTab != tab else { return }
        profileState.profileContentTab = tab
        await loadSelfProfileTab(tab: tab, userId: userId)
    }

    func refreshSelfProfileTab(tab: Int, userId: Int) async {
        await loadSelfProfileTab(tab: tab, userId: userId, refresh: true)
    }

    /// Pull-to-refresh on profile page: refresh header stats and the active tab list.
    func refreshSelfProfilePage(tab: Int, userId: Int) async {
        async let header: Void = refreshSelfProfileHeader()
        async let tabData: Void = refreshSelfProfileTab(tab: tab, userId: userId)
        async let totals: Void = preloadSelfProfileTabTotals(userId: userId)
        _ = await (header, tabData, totals)
    }

    private func refreshSelfProfileHeader() async {
        do {
            profileState.myProfile = try await api.getMyProfile()
        } catch {
            toastError = parseError(error, fallback: "刷新资料失败")
        }
    }

    func loadMoreSelfProfileTab(tab: Int, userId: Int) async {
        await loadSelfProfileTab(tab: tab, userId: userId, loadMore: true)
    }

    func prepareSelfProfileTabs(userId: Int) {
        if profileState.profileTabs.targetUserId != userId {
            if let profile = profileState.myProfile {
                profileState.profileTabs = seedSelfProfileTabs(userId: userId, profile: profile)
            } else {
                profileState.profileTabs = ProfileTabsUiState(targetUserId: userId)
            }
        }
    }

    func ensureMyProfileTabCounts(userId: Int) async {
        prepareSelfProfileTabs(userId: userId)
        if let profile = profileState.myProfile,
           profileState.profileTabs.tabs[safe: 0]?.totalCount == nil {
            profileState.profileTabs = seedSelfProfileTabs(userId: userId, profile: profile)
        }
        await preloadSelfProfileTabTotals(userId: userId)
    }

    func loadSelfProfileTab(
        tab: Int,
        userId: Int,
        refresh: Bool = false,
        loadMore: Bool = false
    ) async {
        guard tab >= 0, tab <= selfProfileLikesTab else { return }

        if tab == selfProfileFavoritesTab || tab == selfProfileLikesTab {
            await loadSelfProfileLibraryTab(
                tab: tab,
                userId: userId,
                refresh: refresh,
                loadMore: loadMore
            )
            return
        }

        var tabsState = profileState.profileTabs
        if tabsState.targetUserId != userId {
            tabsState = ProfileTabsUiState(targetUserId: userId)
        }
        let tabState = tabsState.tabs[safe: tab] ?? ProfileTabUiState()

        if loadMore {
            guard tabState.hasMore, !tabState.isLoadingMore, !tabState.isRefreshing else { return }
        } else if !refresh {
            let hasData: Bool = switch tab {
            case 0: !profileState.profileNotes.isEmpty
            case 1: !profileState.profileActivities.isEmpty
            case selfProfileCommentsTab: !profileState.profileComments.isEmpty
            default: false
            }
            if hasData || tabState.isInitialLoading || tabState.isRefreshing { return }
        }

        let page = refresh || !loadMore ? 1 : tabState.page
        profileState.profileTabs = tabsState.withTab(
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
                let response = try await api.getUserPosts(id: userId, page: page, limit: selfProfileTabPageSize)
                applySelfProfileTabResult(
                    tab: tab,
                    page: page,
                    hasMore: response.hasMore,
                    total: response.total,
                    replace: replace
                ) {
                    profileState.profileNotes = mergeSelfPosts(
                        profileState.profileNotes,
                        response.items,
                        replace: replace
                    )
                }
            case 1:
                let response = try await api.getUserActivities(id: userId, page: page, limit: selfProfileTabPageSize)
                applySelfProfileTabResult(
                    tab: tab,
                    page: page,
                    hasMore: response.hasMore,
                    total: response.total,
                    replace: replace
                ) {
                    profileState.profileActivities = mergeSelfActivities(
                        profileState.profileActivities,
                        response.items,
                        replace: replace
                    )
                }
            case selfProfileCommentsTab:
                let response = try await api.getMyProfileComments(page: page, limit: selfProfileTabPageSize)
                applySelfProfileTabResult(
                    tab: tab,
                    page: page,
                    hasMore: response.hasMore,
                    total: response.total,
                    replace: replace
                ) {
                    profileState.profileComments = mergeSelfComments(
                        profileState.profileComments,
                        response.items,
                        replace: replace
                    )
                }
            default:
                break
            }
        } catch {
            resetSelfProfileTabLoading(tab: tab)
            toastError = parseError(error, fallback: "加载内容失败")
        }
    }

    func preloadSelfProfileTabTotals(userId: Int) async {
        guard profileState.profileTabs.targetUserId == userId else { return }

        async let comments = try? await api.getMyProfileComments(page: 1, limit: 1)
        async let favoritePosts = try? await api.getMyFavoritePosts(page: 1, limit: 1)
        async let favoriteActivities = try? await api.getMyFavoriteActivities(page: 1, limit: 1)
        async let likedPosts = try? await api.getMyLikedPosts(page: 1, limit: 1)
        async let likedActivities = try? await api.getMyLikedActivities(page: 1, limit: 1)

        let (commentPage, favPosts, favActivities, likePosts, likeActivities) = await (
            comments, favoritePosts, favoriteActivities, likedPosts, likedActivities
        )

        guard profileState.profileTabs.targetUserId == userId else { return }

        var tabsState = profileState.profileTabs
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
            selfProfileCommentsTab,
            (tabsState.tabs[safe: selfProfileCommentsTab] ?? ProfileTabUiState())
                .copy(totalCount: commentPage?.total ?? tabsState.tabs[safe: selfProfileCommentsTab]?.totalCount)
        )
        tabsState = tabsState.withTab(
            selfProfileFavoritesTab,
            (tabsState.tabs[safe: selfProfileFavoritesTab] ?? ProfileTabUiState())
                .copy(totalCount: favoritesCombined)
        )
        tabsState = tabsState.withTab(
            selfProfileLikesTab,
            (tabsState.tabs[safe: selfProfileLikesTab] ?? ProfileTabUiState())
                .copy(totalCount: likesCombined)
        )
        profileState.profileTabs = tabsState
    }

    // MARK: - Library tabs

    private func loadSelfProfileLibraryTab(
        tab: Int,
        userId: Int,
        refresh: Bool,
        loadMore: Bool
    ) async {
        var tabsState = profileState.profileTabs
        if tabsState.targetUserId != userId {
            tabsState = ProfileTabsUiState(targetUserId: userId)
        }

        let isFavoritesTab = tab == selfProfileFavoritesTab
        let postsState = tabsState.tabs[safe: tab] ?? ProfileTabUiState()
        let activitiesState = isFavoritesTab ? tabsState.favoriteActivities : tabsState.likedActivities

        if loadMore {
            let loading = postsState.isLoadingMore || activitiesState.isLoadingMore
                || postsState.isRefreshing || activitiesState.isRefreshing
            guard !loading else { return }

            if postsState.hasMore {
                await loadSelfProfileLibrarySource(
                    tab: tab,
                    isFavoritesTab: isFavoritesTab,
                    loadPosts: true,
                    page: postsState.page,
                    replace: false
                )
            } else if activitiesState.hasMore {
                await loadSelfProfileLibrarySource(
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

        profileState.profileTabs = {
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
                await loadSelfProfileLibrarySource(
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
                await loadSelfProfileLibrarySource(
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

    private func loadSelfProfileLibrarySource(
        tab: Int,
        isFavoritesTab: Bool,
        loadPosts: Bool,
        page: Int,
        replace: Bool
    ) async {
        var tabsState = profileState.profileTabs
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
            profileState.profileTabs = tabsState
        }

        do {
            if loadPosts {
                let response: PaginatedResponse<PostDto>
                if isFavoritesTab {
                    response = try await api.getMyFavoritePosts(page: page, limit: selfProfileTabPageSize)
                } else {
                    response = try await api.getMyLikedPosts(page: page, limit: selfProfileTabPageSize)
                }
                applySelfProfileLibraryPostsResult(
                    tab: tab,
                    page: page,
                    hasMore: response.hasMore,
                    total: response.total,
                    replace: replace
                ) {
                    if isFavoritesTab {
                        profileState.profileFavoritePosts = mergeSelfPosts(
                            profileState.profileFavoritePosts,
                            response.items,
                            replace: replace
                        )
                    } else {
                        profileState.profileLikedPosts = mergeSelfPosts(
                            profileState.profileLikedPosts,
                            response.items,
                            replace: replace
                        )
                    }
                }
            } else {
                let response: PaginatedResponse<ActivityDto>
                if isFavoritesTab {
                    response = try await api.getMyFavoriteActivities(page: page, limit: selfProfileTabPageSize)
                } else {
                    response = try await api.getMyLikedActivities(page: page, limit: selfProfileTabPageSize)
                }
                applySelfProfileLibraryActivitiesResult(
                    isFavorites: isFavoritesTab,
                    page: page,
                    hasMore: response.hasMore,
                    total: response.total,
                    replace: replace
                ) {
                    if isFavoritesTab {
                        profileState.profileFavoriteActivities = mergeSelfActivities(
                            profileState.profileFavoriteActivities,
                            response.items,
                            replace: replace
                        )
                    } else {
                        profileState.profileLikedActivities = mergeSelfActivities(
                            profileState.profileLikedActivities,
                            response.items,
                            replace: replace
                        )
                    }
                }
            }
        } catch {
            resetSelfProfileLibraryLoading(tab: tab)
            toastError = parseError(error, fallback: "加载内容失败")
        }
    }

    // MARK: - Helpers

    func seedSelfProfileTabs(userId: Int, profile: UserProfileDto) -> ProfileTabsUiState {
        var tabs = Array(repeating: ProfileTabUiState(), count: 5)
        tabs[0] = ProfileTabUiState(totalCount: profile.postCount)
        tabs[1] = ProfileTabUiState(totalCount: profile.activities.count)
        return ProfileTabsUiState(targetUserId: userId, tabs: tabs)
    }

    private func applySelfProfileTabResult(
        tab: Int,
        page: Int,
        hasMore: Bool,
        total: Int,
        replace: Bool,
        updateLists: () -> Void
    ) {
        updateLists()
        var tabsState = profileState.profileTabs
        let currentTabState = tabsState.tabs[safe: tab] ?? ProfileTabUiState()
        profileState.profileTabs = tabsState.withTab(
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

    private func applySelfProfileLibraryPostsResult(
        tab: Int,
        page: Int,
        hasMore: Bool,
        total: Int,
        replace: Bool,
        updateLists: () -> Void
    ) {
        updateLists()
        var tabsState = profileState.profileTabs
        let currentTabState = tabsState.tabs[safe: tab] ?? ProfileTabUiState()
        let favoritesPostsTotal = tab == selfProfileFavoritesTab ? total : tabsState.favoritesPostsTotal
        let likedPostsTotal = tab == selfProfileLikesTab ? total : tabsState.likedPostsTotal
        let combinedTotal: Int = switch tab {
        case selfProfileFavoritesTab:
            total + (tabsState.favoritesActivitiesTotal ?? 0)
        case selfProfileLikesTab:
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
        profileState.profileTabs = tabsState.withTab(
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

    private func applySelfProfileLibraryActivitiesResult(
        isFavorites: Bool,
        page: Int,
        hasMore: Bool,
        total: Int,
        replace: Bool,
        updateLists: () -> Void
    ) {
        updateLists()
        var tabsState = profileState.profileTabs
        let postsTab = isFavorites ? selfProfileFavoritesTab : selfProfileLikesTab
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
        profileState.profileTabs = tabsState.withTab(
            postsTab,
            postsTabState.copy(totalCount: combinedTotal)
        )
    }

    private func resetSelfProfileTabLoading(tab: Int) {
        var tabsState = profileState.profileTabs
        let current = tabsState.tabs[safe: tab] ?? ProfileTabUiState()
        profileState.profileTabs = tabsState.withTab(
            tab,
            ProfileTabUiState(page: current.page, hasMore: current.hasMore)
        )
    }

    private func resetSelfProfileLibraryLoading(tab: Int) {
        var tabsState = profileState.profileTabs
        let postsState = tabsState.tabs[safe: tab] ?? ProfileTabUiState()
        let activityState = tab == selfProfileFavoritesTab
            ? tabsState.favoriteActivities
            : tabsState.likedActivities
        tabsState = tabsState.withTab(
            tab,
            ProfileTabUiState(page: postsState.page, hasMore: postsState.hasMore)
        )
        profileState.profileTabs = tab == selfProfileFavoritesTab
            ? tabsState.withFavoriteActivities(
                ProfileTabUiState(page: activityState.page, hasMore: activityState.hasMore)
            )
            : tabsState.withLikedActivities(
                ProfileTabUiState(page: activityState.page, hasMore: activityState.hasMore)
            )
    }

    private func mergeSelfPosts(_ existing: [PostDto], _ incoming: [PostDto], replace: Bool) -> [PostDto] {
        if replace { return incoming }
        return existing + incoming.filter { new in existing.allSatisfy { $0.id != new.id } }
    }

    private func mergeSelfActivities(
        _ existing: [ActivityDto],
        _ incoming: [ActivityDto],
        replace: Bool
    ) -> [ActivityDto] {
        if replace { return incoming }
        return existing + incoming.filter { new in existing.allSatisfy { $0.id != new.id } }
    }

    private func mergeSelfComments(
        _ existing: [ProfileCommentDto],
        _ incoming: [ProfileCommentDto],
        replace: Bool
    ) -> [ProfileCommentDto] {
        if replace { return incoming }
        return existing + incoming.filter { new in existing.allSatisfy { $0.id != new.id } }
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
