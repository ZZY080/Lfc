import Combine
import Foundation

@MainActor
final class SearchHistoryStore: ObservableObject {
  static let shared = SearchHistoryStore()

  @Published private(set) var history: [String] = []

  private enum Keys {
    static let history = "search_history"
  }

  private static let historyDelimiter = "\u{0001}"
  private static let maxHistory = 10

  private let defaults = UserDefaults.standard

  private init() {
    reload()
  }

  func reload() {
    history = Self.parse(defaults.string(forKey: Keys.history))
  }

  func add(_ keyword: String) {
    let trimmed = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !trimmed.isEmpty else { return }

    var items = history.filter { $0 != trimmed }
    items.insert(trimmed, at: 0)
    history = Array(items.prefix(Self.maxHistory))
    defaults.set(history.joined(separator: Self.historyDelimiter), forKey: Keys.history)
  }

  func clear() {
    history = []
    defaults.removeObject(forKey: Keys.history)
  }

  private static func parse(_ raw: String?) -> [String] {
    guard let raw, !raw.isEmpty else { return [] }
    return raw
      .split(separator: Character(historyDelimiter), omittingEmptySubsequences: true)
      .map(String.init)
      .filter { !$0.isEmpty }
  }
}
