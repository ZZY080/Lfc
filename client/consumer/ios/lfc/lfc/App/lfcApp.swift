import SwiftUI

@main
struct lfcApp: App {
    @StateObject private var session = SessionManager.shared

    var body: some Scene {
        WindowGroup {
            AppRootView()
                .environmentObject(session)
        }
    }
}
