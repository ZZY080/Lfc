import CoreImage
import UIKit

enum ProfileShareHelper {
    private static let lfcProfileURLPattern = try! NSRegularExpression(
        pattern: #"(?:https?://)?lfc\.campus/lfc/([1-9]\d{9})"#,
        options: .caseInsensitive
    )
    private static let legacyProfileURLPattern = try! NSRegularExpression(
        pattern: #"(?:https?://)?lfc\.campus/user/(\d+)"#,
        options: .caseInsensitive
    )
    private static let lfcNoPattern = try! NSRegularExpression(pattern: #"^[1-9]\d{9}$"#)

    static func profileLink(lfcNo: String) -> String {
        "https://lfc.campus/lfc/\(lfcNo)"
    }

    static func presentShareSheet(for profile: UserProfileDto) {
        let link = profileLink(lfcNo: profile.lfcNo)
        let text = "来看看 \(profile.displayName) 的莲峰主页 \(link)"
        let controller = UIActivityViewController(activityItems: [text], applicationActivities: nil)

        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let root = scene.keyWindow?.rootViewController else { return }

        var presenter = root
        while let presented = presenter.presentedViewController {
            presenter = presented
        }
        presenter.present(controller, animated: true)
    }

    static func parseLfcNo(from content: String) -> String? {
        let trimmed = content.trimmingCharacters(in: .whitespacesAndNewlines)
        let range = NSRange(trimmed.startIndex..., in: trimmed)

        if let match = lfcProfileURLPattern.firstMatch(in: trimmed, range: range),
           let capture = Range(match.range(at: 1), in: trimmed) {
            return String(trimmed[capture])
        }
        if lfcNoPattern.firstMatch(in: trimmed, range: range) != nil {
            return trimmed
        }
        return nil
    }

    static func parseUserId(from content: String) -> Int? {
        let trimmed = content.trimmingCharacters(in: .whitespacesAndNewlines)
        let range = NSRange(trimmed.startIndex..., in: trimmed)
        if let match = legacyProfileURLPattern.firstMatch(in: trimmed, range: range),
           let capture = Range(match.range(at: 1), in: trimmed),
           let id = Int(trimmed[capture]), (1...999_999_999).contains(id) {
            return id
        }
        if let id = Int(trimmed), (1...999_999_999).contains(id) {
            return id
        }
        return nil
    }

    static func generateQRCode(from string: String, size: CGFloat = 240) -> UIImage? {
        guard let data = string.data(using: .utf8),
              let filter = CIFilter(name: "CIQRCodeGenerator") else { return nil }
        filter.setValue(data, forKey: "inputMessage")
        filter.setValue("M", forKey: "inputCorrectionLevel")
        guard let output = filter.outputImage else { return nil }

        let scale = size / output.extent.width
        let transformed = output.transformed(by: CGAffineTransform(scaleX: scale, y: scale))
        let context = CIContext()
        guard let cgImage = context.createCGImage(transformed, from: transformed.extent) else { return nil }
        return UIImage(cgImage: cgImage)
    }
}
