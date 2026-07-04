import Combine
import Foundation

@MainActor
final class SessionManager: ObservableObject {
  static let shared = SessionManager()

  @Published private(set) var isLoggedIn = false
  @Published private(set) var userSession: UserSession?
  @Published private(set) var isRefreshing = false
  @Published var lastError: String?

  private let tokenStore: TokenStore
  private let api: LFCAPIService
  private let client: APIClient
  private var cancellables = Set<AnyCancellable>()

  init(
    tokenStore: TokenStore = .shared,
    api: LFCAPIService = .shared,
    client: APIClient = .shared
  ) {
    self.tokenStore = tokenStore
    self.api = api
    self.client = client
    syncFromTokenStore()

    tokenStore.$isLoggedIn
      .receive(on: DispatchQueue.main)
      .sink { [weak self] loggedIn in
        self?.isLoggedIn = loggedIn
      }
      .store(in: &cancellables)

    tokenStore.$userSession
      .receive(on: DispatchQueue.main)
      .sink { [weak self] session in
        self?.userSession = session
      }
      .store(in: &cancellables)

    tokenStore.sessionExpiredPublisher
      .receive(on: DispatchQueue.main)
      .sink { [weak self] in
        self?.handleSessionExpired()
      }
      .store(in: &cancellables)
  }

  func syncFromTokenStore() {
    isLoggedIn = tokenStore.isLoggedIn
    userSession = tokenStore.userSession
  }

  func login(email: String, password: String) async throws {
    lastError = nil
    let response = try await api.login(LoginRequest(email: email, password: password))
    tokenStore.saveSession(
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      userId: response.user.id,
      email: response.user.email,
      studentId: response.user.studentId
    )
    AnalyticsTracker.shared.track(AnalyticsEvents.userLogin)
  }

  func register(
    email: String,
    password: String,
    realName: String,
    studentId: String,
    studentCardData: Data,
    studentCardFilename: String,
    studentCardMimeType: String = "image/jpeg"
  ) async throws {
    lastError = nil
    let response = try await api.register(
      email: email,
      password: password,
      studentId: studentId,
      realName: realName,
      studentCardData: studentCardData,
      studentCardFilename: studentCardFilename,
      studentCardMimeType: studentCardMimeType
    )
    tokenStore.saveSession(
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      userId: response.user.id,
      email: response.user.email,
      studentId: response.user.studentId
    )
    AnalyticsTracker.shared.track(AnalyticsEvents.userRegister)
  }

  func refreshSession() async throws {
    isRefreshing = true
    defer { isRefreshing = false }
    _ = try await client.refreshAccessToken()
  }

  func logout() {
    lastError = nil
    tokenStore.clearSession()
    Task {
      await AnalyticsTracker.shared.flush()
    }
  }

  func handleSessionExpired() {
    isLoggedIn = false
    userSession = nil
    lastError = "登录已过期，请重新登录"
  }

  func parseErrorMessage(from error: Error, fallback: String) -> String {
    if let apiError = error as? ApiError {
      return apiError.message
    }
    if case APIClientError.decodingFailed(let underlying) = error {
      return underlying.localizedDescription
    }
    return (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
  }
}
