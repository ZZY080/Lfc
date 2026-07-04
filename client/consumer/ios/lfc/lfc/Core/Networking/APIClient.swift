import Foundation

enum HTTPMethod: String, Sendable {
  case get = "GET"
  case post = "POST"
  case put = "PUT"
  case patch = "PATCH"
  case delete = "DELETE"
}

struct MultipartFormPart: Sendable {
  enum Payload: Sendable {
    case data(Data, filename: String?, mimeType: String)
    case text(String)
  }

  let name: String
  let payload: Payload
}

struct ApiError: LocalizedError, Sendable {
  let statusCode: Int
  let message: String
  let body: String?

  var errorDescription: String? { message }
}

typealias LFCAPIError = ApiError

enum APIClientError: Error, Sendable {
  case invalidURL
  case invalidResponse
  case decodingFailed(Error)
  case unauthorized
}

final class APIClient: @unchecked Sendable {
  static let shared = APIClient()
  static let authService = LFCAPIService.shared

  private let tokenStore: TokenStore
  private let session: URLSession
  private let decoder: JSONDecoder
  private let encoder: JSONEncoder
  private let refreshLock = NSLock()
  private var isRefreshing = false
  private var refreshWaiters: [CheckedContinuation<Void, Error>] = []

  init(tokenStore: TokenStore = .shared) {
    self.tokenStore = tokenStore
    let configuration = URLSessionConfiguration.default
    configuration.timeoutIntervalForRequest = AppConfig.requestTimeout
    configuration.timeoutIntervalForResource = AppConfig.resourceTimeout
    self.session = URLSession(configuration: configuration)
    self.decoder = JSONDecoder()
    self.encoder = JSONEncoder()
  }

  // MARK: - Public request methods

  func request<T: Decodable>(
    method: HTTPMethod,
    path: String,
    queryItems: [URLQueryItem]? = nil,
    body: (any Encodable)? = nil,
    requiresAuth: Bool = true,
    allowRefreshRetry: Bool = true
  ) async throws -> T {
    let data = try await performRequest(
      method: method,
      path: path,
      queryItems: queryItems,
      body: body,
      requiresAuth: requiresAuth,
      allowRefreshRetry: allowRefreshRetry
    )
    do {
      return try decoder.decode(T.self, from: data)
    } catch {
      throw APIClientError.decodingFailed(error)
    }
  }

  func requestVoid(
    method: HTTPMethod,
    path: String,
    queryItems: [URLQueryItem]? = nil,
    body: (any Encodable)? = nil,
    requiresAuth: Bool = true,
    allowRefreshRetry: Bool = true
  ) async throws {
    _ = try await performRequest(
      method: method,
      path: path,
      queryItems: queryItems,
      body: body,
      requiresAuth: requiresAuth,
      allowRefreshRetry: allowRefreshRetry
    )
  }

  func requestOptional<T: Decodable>(
    method: HTTPMethod,
    path: String,
    queryItems: [URLQueryItem]? = nil,
    requiresAuth: Bool = true,
    allowRefreshRetry: Bool = true
  ) async throws -> T? {
    do {
      return try await request(
        method: method,
        path: path,
        queryItems: queryItems,
        body: Optional<String>.none,
        requiresAuth: requiresAuth,
        allowRefreshRetry: allowRefreshRetry
      )
    } catch let error as ApiError where error.statusCode == 404 {
      return nil
    }
  }

  func uploadMultipart<T: Decodable>(
    method: HTTPMethod = .post,
    path: String,
    queryItems: [URLQueryItem]? = nil,
    parts: [MultipartFormPart],
    requiresAuth: Bool = true,
    allowRefreshRetry: Bool = true
  ) async throws -> T {
    let data = try await performMultipartRequest(
      method: method,
      path: path,
      queryItems: queryItems,
      parts: parts,
      requiresAuth: requiresAuth,
      allowRefreshRetry: allowRefreshRetry
    )
    do {
      return try decoder.decode(T.self, from: data)
    } catch {
      throw APIClientError.decodingFailed(error)
    }
  }

  func uploadMultipartVoid(
    method: HTTPMethod = .post,
    path: String,
    queryItems: [URLQueryItem]? = nil,
    parts: [MultipartFormPart],
    requiresAuth: Bool = true,
    allowRefreshRetry: Bool = true
  ) async throws {
    _ = try await performMultipartRequest(
      method: method,
      path: path,
      queryItems: queryItems,
      parts: parts,
      requiresAuth: requiresAuth,
      allowRefreshRetry: allowRefreshRetry
    )
  }

  // MARK: - Token refresh (used by SessionManager and 401 retry)

  @discardableResult
  func refreshAccessToken() async throws -> AuthResponse {
    guard let refreshToken = tokenStore.refreshToken(), !refreshToken.isEmpty else {
      await tokenStore.clearSessionAndNotify()
      throw APIClientError.unauthorized
    }

    let response: AuthResponse = try await request(
      method: .post,
      path: "consumer/auth/refresh",
      body: RefreshTokenRequest(refreshToken: refreshToken),
      requiresAuth: false,
      allowRefreshRetry: false
    )

    await tokenStore.updateTokens(
      accessToken: response.accessToken,
      refreshToken: response.refreshToken
    )
    return response
  }

  // MARK: - Core networking

  private func performRequest(
    method: HTTPMethod,
    path: String,
    queryItems: [URLQueryItem]?,
    body: (any Encodable)?,
    requiresAuth: Bool,
    allowRefreshRetry: Bool,
    retryCount: Int = 0
  ) async throws -> Data {
    let request = try buildRequest(
      method: method,
      path: path,
      queryItems: queryItems,
      body: body,
      requiresAuth: requiresAuth
    )
    let (data, response) = try await session.data(for: request)
    return try await handleResponse(
      data: data,
      response: response,
      originalMethod: method,
      originalPath: path,
      originalQueryItems: queryItems,
      originalBody: body,
      requiresAuth: requiresAuth,
      allowRefreshRetry: allowRefreshRetry,
      retryCount: retryCount,
      isMultipart: false,
      multipartParts: nil
    )
  }

  private func performMultipartRequest(
    method: HTTPMethod,
    path: String,
    queryItems: [URLQueryItem]?,
    parts: [MultipartFormPart],
    requiresAuth: Bool,
    allowRefreshRetry: Bool,
    retryCount: Int = 0
  ) async throws -> Data {
    let boundary = "Boundary-\(UUID().uuidString)"
    var request = try buildURLRequest(
      method: method,
      path: path,
      queryItems: queryItems,
      requiresAuth: requiresAuth
    )
    request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
    request.httpBody = makeMultipartBody(parts: parts, boundary: boundary)

    let (data, response) = try await session.data(for: request)
    return try await handleResponse(
      data: data,
      response: response,
      originalMethod: method,
      originalPath: path,
      originalQueryItems: queryItems,
      originalBody: nil,
      requiresAuth: requiresAuth,
      allowRefreshRetry: allowRefreshRetry,
      retryCount: retryCount,
      isMultipart: true,
      multipartParts: parts
    )
  }

  private func buildRequest(
    method: HTTPMethod,
    path: String,
    queryItems: [URLQueryItem]?,
    body: (any Encodable)?,
    requiresAuth: Bool
  ) throws -> URLRequest {
    var request = try buildURLRequest(
      method: method,
      path: path,
      queryItems: queryItems,
      requiresAuth: requiresAuth
    )
    if let body {
      request.setValue("application/json", forHTTPHeaderField: "Content-Type")
      request.httpBody = try encoder.encode(AnyEncodable(body))
    }
    return request
  }

  private func buildURLRequest(
    method: HTTPMethod,
    path: String,
    queryItems: [URLQueryItem]?,
    requiresAuth: Bool
  ) throws -> URLRequest {
    guard let resolvedURL = URL(string: path, relativeTo: AppConfig.apiBaseURL)?.absoluteURL else {
      throw APIClientError.invalidURL
    }
    guard var components = URLComponents(url: resolvedURL, resolvingAgainstBaseURL: false) else {
      throw APIClientError.invalidURL
    }
    if let queryItems, !queryItems.isEmpty {
      components.queryItems = queryItems
    }
    guard let url = components.url else {
      throw APIClientError.invalidURL
    }

    var request = URLRequest(url: url)
    request.httpMethod = method.rawValue
    request.setValue("application/json", forHTTPHeaderField: "Accept")

    if requiresAuth, let token = tokenStore.accessToken(), !token.isEmpty {
      request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
    }
    return request
  }

  private func handleResponse(
    data: Data,
    response: URLResponse,
    originalMethod: HTTPMethod,
    originalPath: String,
    originalQueryItems: [URLQueryItem]?,
    originalBody: (any Encodable)?,
    requiresAuth: Bool,
    allowRefreshRetry: Bool,
    retryCount: Int,
    isMultipart: Bool,
    multipartParts: [MultipartFormPart]?
  ) async throws -> Data {
    guard let http = response as? HTTPURLResponse else {
      throw APIClientError.invalidResponse
    }

    if (200 ... 299).contains(http.statusCode) {
      return data
    }

    if http.statusCode == 401,
      allowRefreshRetry,
      requiresAuth,
      retryCount < 2,
      !isAuthPath(originalPath)
    {
      let refreshed = try await coordinateRefresh()
      if refreshed {
        if isMultipart, let multipartParts {
          return try await performMultipartRequest(
            method: originalMethod,
            path: originalPath,
            queryItems: originalQueryItems,
            parts: multipartParts,
            requiresAuth: requiresAuth,
            allowRefreshRetry: false,
            retryCount: retryCount + 1
          )
        }
        return try await performRequest(
          method: originalMethod,
          path: originalPath,
          queryItems: originalQueryItems,
          body: originalBody,
          requiresAuth: requiresAuth,
          allowRefreshRetry: false,
          retryCount: retryCount + 1
        )
      }
    }

    if http.statusCode == 401, originalPath.contains("consumer/auth/refresh") {
      await tokenStore.clearSessionAndNotify()
    }

    throw parseApiError(statusCode: http.statusCode, data: data)
  }

  private func coordinateRefresh() async throws -> Bool {
    refreshLock.lock()
    if isRefreshing {
      refreshLock.unlock()
      try await withCheckedThrowingContinuation { continuation in
        refreshLock.lock()
        refreshWaiters.append(continuation)
        refreshLock.unlock()
      }
      return tokenStore.accessToken() != nil
    }

    isRefreshing = true
    refreshLock.unlock()

    do {
      _ = try await refreshAccessToken()
      finishRefresh(success: true)
      return true
    } catch {
      finishRefresh(success: false)
      return false
    }
  }

  private func finishRefresh(success: Bool) {
    refreshLock.lock()
    isRefreshing = false
    let waiters = refreshWaiters
    refreshWaiters.removeAll()
    refreshLock.unlock()

    for waiter in waiters {
      if success {
        waiter.resume()
      } else {
        waiter.resume(throwing: APIClientError.unauthorized)
      }
    }
  }

  private func isAuthPath(_ path: String) -> Bool {
    path.contains("consumer/auth/login")
      || path.contains("consumer/auth/register")
      || path.contains("consumer/auth/refresh")
  }

  private func parseApiError(statusCode: Int, data: Data) -> ApiError {
    let body = String(data: data, encoding: .utf8)
    if let body, !body.isEmpty,
      let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
    {
      if let message = json["message"] as? String {
        return ApiError(statusCode: statusCode, message: message, body: body)
      }
      if let messages = json["message"] as? [String], let first = messages.first {
        return ApiError(statusCode: statusCode, message: first, body: body)
      }
    }
    return ApiError(
      statusCode: statusCode,
      message: HTTPURLResponse.localizedString(forStatusCode: statusCode),
      body: body
    )
  }

  private func makeMultipartBody(parts: [MultipartFormPart], boundary: String) -> Data {
    var body = Data()
    let lineBreak = "\r\n"

    for part in parts {
      body.append("--\(boundary)\(lineBreak)")
      switch part.payload {
      case .text(let value):
        body.append("Content-Disposition: form-data; name=\"\(part.name)\"\(lineBreak)\(lineBreak)")
        body.append("\(value)\(lineBreak)")
      case .data(let data, let filename, let mimeType):
        var disposition = "Content-Disposition: form-data; name=\"\(part.name)\""
        if let filename {
          disposition += "; filename=\"\(filename)\""
        }
        body.append("\(disposition)\(lineBreak)")
        body.append("Content-Type: \(mimeType)\(lineBreak)\(lineBreak)")
        body.append(data)
        body.append(lineBreak)
      }
    }

    body.append("--\(boundary)--\(lineBreak)")
    return body
  }
}

private struct AnyEncodable: Encodable {
  private let encodeClosure: (Encoder) throws -> Void

  init(_ value: any Encodable) {
    encodeClosure = value.encode
  }

  func encode(to encoder: Encoder) throws {
    try encodeClosure(encoder)
  }
}

private extension Data {
  mutating func append(_ string: String) {
    if let data = string.data(using: .utf8) {
      append(data)
    }
  }
}
