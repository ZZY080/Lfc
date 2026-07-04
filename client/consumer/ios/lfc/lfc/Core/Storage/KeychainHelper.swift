import Foundation
import Security

enum KeychainHelper {
  private static let service = "com.lfc.consumer"

  static func save(_ value: String, for key: String) throws {
    let data = Data(value.utf8)
    let query: [String: Any] = [
      kSecClass as String: kSecClassGenericPassword,
      kSecAttrService as String: service,
      kSecAttrAccount as String: key,
    ]

    let status: OSStatus
    if SecItemCopyMatching(query as CFDictionary, nil) == errSecSuccess {
      let attributes: [String: Any] = [kSecValueData as String: data]
      status = SecItemUpdate(query as CFDictionary, attributes as CFDictionary)
    } else {
      var insert = query
      insert[kSecValueData as String] = data
      status = SecItemAdd(insert as CFDictionary, nil)
    }

    guard status == errSecSuccess else {
      throw KeychainError.unhandled(status)
    }
  }

  static func read(_ key: String) -> String? {
    let query: [String: Any] = [
      kSecClass as String: kSecClassGenericPassword,
      kSecAttrService as String: service,
      kSecAttrAccount as String: key,
      kSecReturnData as String: true,
      kSecMatchLimit as String: kSecMatchLimitOne,
    ]

    var item: CFTypeRef?
    let status = SecItemCopyMatching(query as CFDictionary, &item)
    guard status == errSecSuccess, let data = item as? Data else {
      return nil
    }
    return String(data: data, encoding: .utf8)
  }

  static func delete(_ key: String) {
    let query: [String: Any] = [
      kSecClass as String: kSecClassGenericPassword,
      kSecAttrService as String: service,
      kSecAttrAccount as String: key,
    ]
    SecItemDelete(query as CFDictionary)
  }

  static func clearAll(keys: [String]) {
    keys.forEach { delete($0) }
  }

  enum KeychainError: Error {
    case unhandled(OSStatus)
  }
}
