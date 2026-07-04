import SwiftUI

enum LfcBrand {
    static let splashGradientTop = Color(red: 0.36, green: 0.78, blue: 0.74)
    static let splashGradientBottom = Color(red: 0.14, green: 0.58, blue: 0.52)

    static var splashGradient: LinearGradient {
        LinearGradient(
            colors: [splashGradientTop, splashGradientBottom],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
}
