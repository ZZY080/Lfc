import Foundation

let xhsFeedPrimaryTabs = ["关注", "发现"]

let fallbackMyChannels = ["推荐", "旅行", "手工", "社科", "情感", "校园生活"]

let feedRecommendChannel = "推荐"

func extractFeedCityLabel(address: String?) -> String {
    let compact = address?
        .trimmingCharacters(in: .whitespacesAndNewlines)
        .replacingOccurrences(of: "^中国", with: "", options: .regularExpression)
        .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    if compact.isEmpty { return "同城" }
    if let match = compact.range(of: #"[\u4e00-\u9fa5]{2,10}市"#, options: .regularExpression) {
        return String(compact[match]).replacingOccurrences(of: "市", with: "")
    }
    return "同城"
}

func formatFeedLocationChipAddress(_ address: String?, maxLength: Int = 22) -> String {
    let compact = address?
        .trimmingCharacters(in: .whitespacesAndNewlines)
        .replacingOccurrences(of: "^中国", with: "", options: .regularExpression)
        .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    if compact.isEmpty { return "" }

    let mainPart = compact
        .components(separatedBy: "（").first?
        .components(separatedBy: "(").first?
        .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""

    let display: String
    if mainPart.contains("·") {
        let parts = mainPart.split(separator: "·").map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty }
        display = parts.prefix(2).joined(separator: "·")
    } else {
        display = formatFeedLocationFromAdminAddress(mainPart)
    }

    if display.count <= maxLength { return display }
    return String(display.prefix(maxLength)) + "..."
}

private func formatFeedLocationFromAdminAddress(_ address: String) -> String {
    let withoutProvince = removeProvincePrefix(address)
    let cityShort = extractCityShortName(withoutProvince)
    if let townMatch = withoutProvince.range(of: #"[\u4e00-\u9fa5]{2,8}镇"#, options: .regularExpression, range: nil) {
        let town = String(withoutProvince[townMatch])
        let tailStart = withoutProvince.index(townMatch.upperBound, offsetBy: 0)
        let tail = String(withoutProvince[tailStart...]).trimmingCharacters(in: .whitespacesAndNewlines)
        var placeTail = cityShort
        if !tail.isEmpty { placeTail += String(tail.prefix(6)) }
        return "\(town)·\(placeTail)"
    }
    if let cityMatch = withoutProvince.range(of: #"^[\u4e00-\u9fa5]{2,10}市"#, options: .regularExpression) {
        let city = String(withoutProvince[cityMatch]).replacingOccurrences(of: "市", with: "")
        let rest = withoutProvince.replacingOccurrences(of: "\(city)市", with: "").trimmingCharacters(in: .whitespacesAndNewlines)
        return rest.isEmpty ? city : "\(city)·\(rest)"
    }
    return withoutProvince
}

private func extractCityShortName(_ address: String) -> String {
    guard let match = address.range(of: #"[\u4e00-\u9fa5]{2,10}市"#, options: .regularExpression) else { return "" }
    return String(address[match]).replacingOccurrences(of: "市", with: "")
}

private func removeProvincePrefix(_ address: String) -> String {
    guard let cityIndex = address.firstIndex(of: "市") else { return address }
    let prefix = address[..<cityIndex]
    if let provinceIndex = prefix.lastIndex(of: "省") {
        return String(address[address.index(after: provinceIndex)...])
    }
    return address
}

func distanceMeters(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double) -> Double {
    let earthRadius = 6_371_000.0
    let dLat = (toLat - fromLat) * .pi / 180
    let dLng = (toLng - fromLng) * .pi / 180
    let a = sin(dLat / 2) * sin(dLat / 2)
        + cos(fromLat * .pi / 180) * cos(toLat * .pi / 180) * sin(dLng / 2) * sin(dLng / 2)
    let c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadius * c
}

func formatDistanceMeters(_ meters: Double) -> String {
    if meters < 100 { return "100m内" }
    if meters < 1000 { return "\(Int(meters))m" }
    if meters < 10_000 { return String(format: "%.1fkm", meters / 1000) }
    return "\(Int(meters / 1000))km"
}

func formatDistanceCompact(
    userLat: Double?,
    userLng: Double?,
    targetLat: Double?,
    targetLng: Double?
) -> String? {
    guard hasValidCoordinate(latitude: targetLat, longitude: targetLng) else { return nil }
    guard hasValidCoordinate(latitude: userLat, longitude: userLng) else { return "定位中" }
    let meters = distanceMeters(
        fromLat: userLat!,
        fromLng: userLng!,
        toLat: targetLat!,
        toLng: targetLng!
    )
    return formatDistanceMeters(meters)
}

func formatLikeCount(_ count: Int) -> String {
    if count <= 0 { return "0" }
    if count < 10_000 { return "\(count)" }
    return String(format: "%.1fw", Double(count) / 10_000)
}

func gradientIndexForId(_ id: Int, size: Int) -> Int {
    ((id % size) + size) % size
}

func feedAspectRatio(for id: Int) -> CGFloat {
    0.68 + CGFloat(gradientIndexForId(id, size: 4)) * 0.06
}
