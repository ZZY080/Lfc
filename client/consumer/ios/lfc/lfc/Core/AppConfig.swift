import Foundation

enum AppConfig {
    /// API base URL with trailing slash, matching Android `resolveDeviceApiBaseUrl()`.
    /// Nest routes are rooted at `/consumer/*`, not `/api/consumer/*`.
    static var apiBaseURL: URL {
        #if DEBUG
        return URL(string: "http://127.0.0.1:8088/")!
        #else
        return URL(string: "https://api.lfc.app/")!
        #endif
    }

    static var isDebug: Bool {
        #if DEBUG
        return true
        #else
        return false
        #endif
    }

    static let requestTimeout: TimeInterval = 30
    static let resourceTimeout: TimeInterval = 30
}
