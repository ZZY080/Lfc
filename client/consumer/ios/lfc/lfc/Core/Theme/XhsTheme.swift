import SwiftUI

enum XhsTheme {
    static let red = Color(red: 0.98, green: 0.18, blue: 0.28)
    static let textPrimary = Color(red: 0.13, green: 0.13, blue: 0.13)
    static let textSecondary = Color(red: 0.55, green: 0.55, blue: 0.55)
    static let divider = Color(red: 0.92, green: 0.92, blue: 0.92)
    static let background = Color(red: 0.97, green: 0.97, blue: 0.97)
}

extension View {
    /// Extends page background into the status bar / notch area for a seamless top edge.
    func lfcImmersiveBackground(_ color: Color = XhsTheme.background) -> some View {
        background {
            color.ignoresSafeArea()
        }
    }
}
