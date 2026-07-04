import SwiftUI

@main
struct lfcApp: App {
    @StateObject private var session = SessionManager.shared
    @State private var showSplash = true

    var body: some Scene {
        WindowGroup {
            ZStack {
                AppRootView()
                    .environmentObject(session)

                if showSplash {
                    SplashView()
                        .transition(.opacity)
                        .zIndex(1)
                }
            }
            .animation(.easeOut(duration: 0.35), value: showSplash)
            .task {
                try? await Task.sleep(for: .seconds(1.6))
                showSplash = false
            }
        }
    }
}
