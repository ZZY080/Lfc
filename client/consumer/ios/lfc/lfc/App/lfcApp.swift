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
                    .allowsHitTesting(!showSplash)

                if showSplash {
                    SplashView()
                }
            }
            .task {
                try? await Task.sleep(for: .seconds(1.6))
                showSplash = false
            }
        }
    }
}
