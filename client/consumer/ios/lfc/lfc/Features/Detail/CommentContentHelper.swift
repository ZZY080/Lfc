import Foundation

struct ParsedCommentContent: Equatable, Sendable {
    let text: String
    let imageUrl: String?
}

enum CommentContentHelper {
    private static let imageMarker = "\n__LFC_IMG__"

    static func build(text: String, imageUrl: String?) -> String {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let imageUrl, !imageUrl.isEmpty else { return trimmed }
        if trimmed.isEmpty {
            return "[图片]\(imageMarker)\(imageUrl)"
        }
        return "\(trimmed)\(imageMarker)\(imageUrl)"
    }

    static func parse(_ content: String) -> ParsedCommentContent {
        guard let markerIndex = content.range(of: imageMarker)?.lowerBound else {
            return ParsedCommentContent(text: content, imageUrl: nil)
        }
        let text = String(content[..<markerIndex]).trimmingCharacters(in: .whitespacesAndNewlines)
        let imageStart = content.index(markerIndex, offsetBy: imageMarker.count)
        let imageUrl = String(content[imageStart...]).trimmingCharacters(in: .whitespacesAndNewlines)
        return ParsedCommentContent(
            text: text,
            imageUrl: imageUrl.isEmpty ? nil : imageUrl
        )
    }
}

struct PendingCommentImage: Sendable {
    let data: Data
    let filename: String
}
