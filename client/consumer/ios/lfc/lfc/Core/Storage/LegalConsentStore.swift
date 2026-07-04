import Combine
import Foundation

@MainActor
final class LegalConsentStore: ObservableObject {
  static let shared = LegalConsentStore()

  /// Bump when legal copy changes to require re-confirmation.
  static let currentVersion = 2

  @Published private(set) var agreedVersion = 0

  private enum Keys {
    static let agreedVersion = "legal_agreed_version"
  }

  private let defaults = UserDefaults.standard

  var hasAgreed: Bool {
    agreedVersion >= Self.currentVersion
  }

  private init() {
    agreedVersion = defaults.integer(forKey: Keys.agreedVersion)
  }

  func markAgreed() {
    agreedVersion = Self.currentVersion
    defaults.set(Self.currentVersion, forKey: Keys.agreedVersion)
  }

  func reload() {
    agreedVersion = defaults.integer(forKey: Keys.agreedVersion)
  }
}
