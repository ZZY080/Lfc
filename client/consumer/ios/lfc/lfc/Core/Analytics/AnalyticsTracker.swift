import Foundation

final class AnalyticsTracker: @unchecked Sendable {
  static let shared = AnalyticsTracker()

  private let maxBatch = 20
  private let flushInterval: TimeInterval = 30
  private let platform = "ios"

  private let api: LFCAPIService
  private let sessionId = UUID().uuidString
  private let lock = NSLock()
  private var buffer: [AnalyticsEventInput] = []
  private var flushTask: Task<Void, Never>?
  private var initialized = false

  private let isoFormatter: ISO8601DateFormatter = {
    let formatter = ISO8601DateFormatter()
    formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    formatter.timeZone = TimeZone(secondsFromGMT: 0)
    return formatter
  }()

  init(api: LFCAPIService = .shared) {
    self.api = api
  }

  func initialize() {
    lock.lock()
    defer { lock.unlock() }
    guard !initialized else { return }
    initialized = true
    flushTask?.cancel()
    flushTask = Task { [weak self] in
      guard let self else { return }
      while !Task.isCancelled {
        try? await Task.sleep(nanoseconds: UInt64(self.flushInterval * 1_000_000_000))
        await self.flush()
      }
    }
  }

  func track(_ event: String, properties: [String: Any]? = nil) {
    lock.lock()
    guard initialized else {
      lock.unlock()
      return
    }

    let encodedProperties = properties?.mapValues { JSONValue.from($0) }
    buffer.append(
      AnalyticsEventInput(
        event: event,
        properties: encodedProperties,
        platform: platform,
        sessionId: sessionId,
        occurredAt: isoFormatter.string(from: Date())
      )
    )

    let shouldFlush = buffer.count >= maxBatch
    lock.unlock()

    if shouldFlush {
      Task { await flush() }
    }
  }

  func flush() async {
    let batch: [AnalyticsEventInput]
    lock.lock()
    guard initialized, !buffer.isEmpty else {
      lock.unlock()
      return
    }
    batch = buffer
    buffer.removeAll()
    lock.unlock()

    _ = try? await api.ingestAnalyticsEvents(IngestAnalyticsEventsRequest(events: batch))
  }

  deinit {
    flushTask?.cancel()
  }
}
