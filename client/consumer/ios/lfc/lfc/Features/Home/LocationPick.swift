import Foundation

struct LocationPick: Equatable, Sendable {
    let label: String
    let latitude: Double
    let longitude: Double
}

func hasValidCoordinate(latitude: Double?, longitude: Double?) -> Bool {
    guard let latitude, let longitude else { return false }
    return (-90...90).contains(latitude) && (-180...180).contains(longitude)
}
