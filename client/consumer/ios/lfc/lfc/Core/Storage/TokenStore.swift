import Combine
import Foundation

struct UserSession: Equatable, Sendable {
  let userId: Int
  let email: String
  let studentId: String
}

@MainActor
final class TokenStore: ObservableObject {
  static let shared = TokenStore()

  @Published private(set) var isLoggedIn = false
  @Published private(set) var userSession: UserSession?

  private enum Keys {
    static let accessToken = "access_token"
    static let refreshToken = "refresh_token"
    static let userId = "user_id"
    static let email = "user_email"
    static let studentId = "student_id"
    static let isLoggedIn = "is_logged_in"
  }

  private let defaults = UserDefaults.standard
  private let sessionExpiredSubject = PassthroughSubject<Void, Never>()

  var sessionExpiredPublisher: AnyPublisher<Void, Never> {
    sessionExpiredSubject.eraseToAnyPublisher()
  }

  private init() {
    reloadFromStorage()
  }

  func reloadFromStorage() {
    let token = KeychainHelper.read(Keys.accessToken)
    isLoggedIn = defaults.bool(forKey: Keys.isLoggedIn) && !(token?.isEmpty ?? true)
    if isLoggedIn {
      userSession = UserSession(
        userId: defaults.integer(forKey: Keys.userId),
        email: defaults.string(forKey: Keys.email) ?? "",
        studentId: defaults.string(forKey: Keys.studentId) ?? ""
      )
    } else {
      userSession = nil
    }
  }

  nonisolated func accessToken() -> String? {
    KeychainHelper.read(Keys.accessToken)
  }

  nonisolated func refreshToken() -> String? {
    KeychainHelper.read(Keys.refreshToken)
  }

  func saveSession(
    accessToken: String,
    refreshToken: String,
    userId: Int,
    email: String,
    studentId: String
  ) {
    try? KeychainHelper.save(accessToken, for: Keys.accessToken)
    try? KeychainHelper.save(refreshToken, for: Keys.refreshToken)
    defaults.set(userId, forKey: Keys.userId)
    defaults.set(email, forKey: Keys.email)
    defaults.set(studentId, forKey: Keys.studentId)
    defaults.set(true, forKey: Keys.isLoggedIn)
    isLoggedIn = true
    userSession = UserSession(userId: userId, email: email, studentId: studentId)
  }

  func updateTokens(accessToken: String, refreshToken: String) {
    try? KeychainHelper.save(accessToken, for: Keys.accessToken)
    try? KeychainHelper.save(refreshToken, for: Keys.refreshToken)
  }

  func clearSession() {
    KeychainHelper.clearAll(keys: [Keys.accessToken, Keys.refreshToken])
    defaults.removeObject(forKey: Keys.userId)
    defaults.removeObject(forKey: Keys.email)
    defaults.removeObject(forKey: Keys.studentId)
    defaults.set(false, forKey: Keys.isLoggedIn)
    isLoggedIn = false
    userSession = nil
  }

  func clearSessionAndNotify() {
    clearSession()
    sessionExpiredSubject.send()
  }
}
