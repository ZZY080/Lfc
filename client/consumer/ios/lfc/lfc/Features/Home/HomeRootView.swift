import SwiftUI

struct HomeRootView: View {
    @EnvironmentObject private var session: SessionManager
    @State private var store = HomeStore()
    @State private var path: [HomeRoute] = []
    @State private var showPublishHub = false
    @State private var showProfileSideMenu = false

    var onLogout: (() -> Void)?

    var body: some View {
        NavigationStack(path: $path) {
            ZStack {
                MainTabView(
                    store: store,
                    onNavigate: { route in path.append(route) },
                    onPublishTap: {
                        showPublishHub = true
                    },
                    onOpenSideMenu: {
                        showProfileSideMenu = true
                    },
                    onLogout: performLogout
                )
                .navigationBarHidden(true)

                if showPublishHub {
                    PublishHubView(
                        onDismiss: { showPublishHub = false },
                        onPublishPost: {
                            showPublishHub = false
                            path.append(.publishPost)
                        },
                        onPublishActivity: {
                            showPublishHub = false
                            path.append(.publishActivity)
                        }
                    )
                    .zIndex(100)
                }

                if showProfileSideMenu, store.selectedTab == .profile, path.isEmpty {
                    ProfileSideMenuView(
                        profile: store.profileState.myProfile,
                        unreadCount: store.unreadCount,
                        onDismiss: { showProfileSideMenu = false },
                        onScan: {
                            showProfileSideMenu = false
                            path.append(.profileQrScan)
                        },
                        onShowMyQr: {
                            showProfileSideMenu = false
                            path.append(.myQrcode)
                        },
                        onSettings: {
                            showProfileSideMenu = false
                            path.append(.settings)
                        },
                        onOrders: {
                            showProfileSideMenu = false
                            path.append(.orders)
                        },
                        onEditProfile: {
                            showProfileSideMenu = false
                            path.append(.editProfile)
                        },
                        onSearch: {
                            showProfileSideMenu = false
                            path.append(.profileSearch)
                        },
                        onGoToMessages: {
                            showProfileSideMenu = false
                            store.selectedTab = .messages
                        },
                        onLogout: performLogout
                    )
                    .zIndex(200)
                    .transition(.move(edge: .leading))
                }
            }
            .navigationDestination(for: HomeRoute.self) { route in
                destination(for: route)
                    .lfcHideSystemNavigationBar()
            }
        }
        .task { await store.bootstrap() }
        .onChange(of: store.toastMessage) { _, message in
            guard message != nil else { return }
            store.clearToast()
        }
        .onChange(of: store.toastError) { _, error in
            guard error != nil else { return }
            store.clearToast()
        }
    }

    @ViewBuilder
    private func destination(for route: HomeRoute) -> some View {
        switch route {
        case .main:
            EmptyView()

        case .search:
            SearchPageView(
                store: store,
                onBack: { pop() },
                onSearchSubmitted: { path.append(.searchResult) }
            )

        case .searchResult:
            SearchResultView(
                store: store,
                onBack: { pop() },
                onPostTap: { openPost($0) },
                onActivityTap: { openActivity($0) }
            )

        case .notifications:
            NotificationListView(
                store: store,
                onBack: { pop() },
                onNotificationTap: { path.append(.notificationDetail(id: $0)) }
            )

        case .notificationDetail(let id):
            NotificationDetailView(
                notification: store.selectedNotification,
                isLoading: store.isNotificationLoading,
                onBack: {
                    store.clearSelectedNotification()
                    pop()
                },
                onActivityTap: { openActivity($0) }
            )
            .task { await store.loadNotificationDetail(id) }

        case .chat(let conversationId):
            ChatView(
                store: store,
                currentUserId: session.userSession?.userId,
                onBack: {
                    store.clearChat()
                    pop()
                },
                onPostTap: { openPost($0) },
                onActivityTap: { openActivity($0) },
                onProductTap: { path.append(.productDetail(id: $0)) }
            )
            .task { await store.loadChat(conversationId) }

        case .postDetail(let id):
            PostDetailView(
                postId: id,
                store: store,
                currentUserId: session.userSession?.userId,
                onBack: {
                    store.clearSelectedPost()
                    pop()
                },
                onAuthorTap: { openUserProfile($0) },
                onProductTap: { path.append(.productDetail(id: $0)) },
                onEdit: { path.append(.editPost) },
                onDeleted: { pop() }
            )

        case .activityDetail(let id):
            ActivityDetailView(
                store: store,
                currentUserId: session.userSession?.userId,
                onBack: {
                    store.clearSelectedActivity()
                    pop()
                },
                onAuthorTap: { openUserProfile($0) },
                onEdit: { path.append(.editActivity) },
                onDeleted: { pop() }
            )
            .task { await store.loadActivityDetail(id) }

        case .productDetail(let id):
            ProductDetailView(
                store: store,
                currentUserId: session.userSession?.userId,
                onBack: { pop() },
                onViewNote: { path.append(.postDetail(id: id)) },
                onAuthorTap: { openUserProfile($0) },
                onOpenChat: { path.append(.chat(conversationId: $0)) }
            )
            .task { await store.loadProductDetail(id) }

        case .userProfile(let id):
            UserProfileView(
                userId: id,
                store: store,
                currentUserId: session.userSession?.userId,
                onBack: {
                    store.clearUserProfile()
                    pop()
                },
                onPostTap: { openPost($0) },
                onActivityTap: { openActivity($0) },
                onOpenChat: { path.append(.chat(conversationId: $0)) }
            )

        case .publishPost:
            PublishPostView(
                store: store,
                onBack: { popToMain() },
                onBindAlipay: { path.append(.settings) },
                onOpenLocationSearch: { path.append(.locationSearch) },
                onSubmitSuccess: { popToMain() }
            )

        case .publishActivity:
            PublishActivityView(
                store: store,
                onBack: { pop() },
                onBindAlipay: { path.append(.settings) },
                onOpenLocationSearch: { path.append(.locationSearch) },
                onSubmitSuccess: { popToMain() }
            )

        case .editPost:
            EditPostView(
                store: store,
                onBack: { pop() },
                onOpenLocationSearch: { path.append(.locationSearch) }
            )

        case .editActivity:
            EditActivityView(
                store: store,
                onBack: { pop() },
                onOpenLocationSearch: { path.append(.locationSearch) }
            )

        case .locationSearch:
            LocationSearchView(
                store: store,
                onBack: { pop() },
                onSelect: { pop() }
            )

        case .settings:
            SettingsView(
                profile: store.profileState.myProfile,
                store: store,
                onBack: { pop() },
                onOpenOrders: { path.append(.orders) },
                onOpenTransactions: { path.append(.paymentTransactions) },
                onOpenLegalDocument: { doc in
                    path.append(.legalDocument(id: doc.routeKey))
                },
                onLogout: performLogout
            )

        case .orders:
            OrderCenterView(
                store: store,
                onBack: { pop() },
                onOpenTransactions: { path.append(.paymentTransactions) }
            )

        case .paymentTransactions:
            PaymentTransactionsView(store: store, onBack: { pop() })

        case .myQrcode:
            MyQrCodeView(
                profile: store.profileState.myProfile,
                onBack: { pop() }
            )

        case .profileQrScan:
            ProfileQrScanView(
                store: store,
                onBack: { pop() },
                onProfileScanned: { userId in
                    openUserProfile(userId)
                },
                onShowMyQr: {
                    pop()
                    path.append(.myQrcode)
                }
            )

        case .editProfile:
            EditProfileView(
                profile: store.profileState.myProfile,
                store: store,
                onBack: { pop() }
            )

        case .profileSearch:
            ProfileSearchView(
                store: store,
                onBack: { pop() },
                onPostTap: { openPost($0) }
            )

        case .legalDocument(let docKey):
            if let docID = LegalDocumentId.fromRouteKey(docKey) {
                LegalDocumentView(documentId: docID, onBack: { pop() })
            } else {
                ContentUnavailableView("文档不存在", systemImage: "doc")
            }
        }
    }

    private func performLogout() {
        if let onLogout {
            onLogout()
        } else {
            session.logout()
        }
    }

    // MARK: - Navigation helpers

    private func pop() {
        guard !path.isEmpty else { return }
        path.removeLast()
    }

    private func popToMain() {
        path.removeAll()
    }

    private func openPost(_ id: Int) {
        if case .postDetail(let currentId) = path.last, currentId == id { return }
        store.preparePostDetail(id: id)
        path.append(.postDetail(id: id))
    }

    private func openActivity(_ id: Int) {
        path.append(.activityDetail(id: id))
    }

    private func openUserProfile(_ id: Int) {
        path.append(.userProfile(id: id))
    }
}

#Preview {
    HomeRootView()
}
