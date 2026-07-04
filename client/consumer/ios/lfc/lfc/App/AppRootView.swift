import Combine
import SwiftUI

enum AppRoute: Hashable {
    case legalConsent
    case login
    case register
    case home
    case legalDocument(LegalDocumentId)
}

struct AppRootView: View {
    @EnvironmentObject private var session: SessionManager
    @StateObject private var legalConsentStore = LegalConsentStore.shared

    @State private var path = NavigationPath()
    @State private var rootRoute: AppRoute = .login

    private var needsLegalConsent: Bool {
        let agreedVersion = legalConsentStore.agreedVersion
        if agreedVersion >= LegalConsentStore.currentVersion { return false }
        if session.isLoggedIn && agreedVersion == 0 { return false }
        return true
    }

    var body: some View {
        NavigationStack(path: $path) {
            rootView(for: rootRoute)
                .navigationDestination(for: AppRoute.self) { route in
                    switch route {
                    case .register:
                        RegisterView(
                            session: session,
                            onRegisterSuccess: handleAuthSuccess,
                            onNavigateToLogin: { path.removeLast() },
                            onOpenLegalDocument: openLegalDocument
                        )
                        .lfcHideSystemNavigationBar()
                    case .legalDocument(let documentId):
                        LegalDocumentView(documentId: documentId, onBack: { path.removeLast() })
                            .lfcHideSystemNavigationBar()
                    default:
                        ContentUnavailableView("页面不存在", systemImage: "exclamationmark.triangle")
                            .lfcHideSystemNavigationBar()
                    }
                }
        }
        .onAppear {
            resolveRootRoute()
        }
        .onChange(of: session.isLoggedIn) { _, _ in
            resolveRootRoute()
        }
        .onChange(of: legalConsentStore.agreedVersion) { _, _ in
            resolveRootRoute()
        }
        .onChange(of: needsLegalConsent) { _, needsConsent in
            if !needsConsent {
                autoMarkAgreedForLoggedInUser()
            }
        }
        .onReceive(TokenStore.shared.sessionExpiredPublisher) { _ in
            navigateToLogin(clearStack: true)
        }
    }

    @ViewBuilder
    private func rootView(for route: AppRoute) -> some View {
        switch route {
        case .legalConsent:
            LegalConsentView(
                onAgreed: handleLegalConsentAgreed,
                onOpenDocument: openLegalDocument
            )
        case .login:
            LoginView(
                session: session,
                onLoginSuccess: handleAuthSuccess,
                onNavigateToRegister: { path.append(AppRoute.register) },
                onOpenLegalDocument: openLegalDocument
            )
        case .register:
            RegisterView(
                session: session,
                onRegisterSuccess: handleAuthSuccess,
                onNavigateToLogin: { rootRoute = .login },
                onOpenLegalDocument: openLegalDocument
            )
        case .home:
            HomeRootView(onLogout: handleLogout)
        case .legalDocument(let documentId):
            LegalDocumentView(documentId: documentId, onBack: {
                if path.isEmpty {
                    rootRoute = session.isLoggedIn ? .home : .login
                } else {
                    path.removeLast()
                }
            })
        }
    }

    private func resolveRootRoute() {
        if needsLegalConsent {
            if rootRoute != .legalConsent || !path.isEmpty {
                path = NavigationPath()
                rootRoute = .legalConsent
            }
            return
        }

        if session.isLoggedIn {
            if rootRoute != .home || !path.isEmpty {
                path = NavigationPath()
                rootRoute = .home
            }
        } else if rootRoute == .home {
            navigateToLogin(clearStack: true)
        } else if rootRoute == .legalConsent {
            path = NavigationPath()
            rootRoute = .login
        }
    }

    private func handleLegalConsentAgreed() {
        legalConsentStore.markAgreed()
        if session.isLoggedIn {
            path = NavigationPath()
            rootRoute = .home
        } else {
            path = NavigationPath()
            rootRoute = .login
        }
    }

    private func handleAuthSuccess() {
        legalConsentStore.markAgreed()
        path = NavigationPath()
        rootRoute = .home
    }

    private func handleLogout() {
        session.logout()
        navigateToLogin(clearStack: true)
    }

    private func navigateToLogin(clearStack: Bool) {
        if clearStack {
            path = NavigationPath()
        }
        rootRoute = .login
    }

    private func openLegalDocument(_ documentId: LegalDocumentId) {
        path.append(AppRoute.legalDocument(documentId))
    }

    private func autoMarkAgreedForLoggedInUser() {
        guard session.isLoggedIn,
              legalConsentStore.agreedVersion < LegalConsentStore.currentVersion else {
            return
        }
        legalConsentStore.markAgreed()
    }
}

#Preview {
    AppRootView()
        .environmentObject(SessionManager.shared)
}
