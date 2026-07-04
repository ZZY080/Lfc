import SwiftUI

extension View {
    /// Hides the system navigation bar and back button. Use on pushed screens that provide their own header.
    func lfcHideSystemNavigationBar() -> some View {
        navigationBarBackButtonHidden(true)
            .toolbar(.hidden, for: .navigationBar)
    }
}
