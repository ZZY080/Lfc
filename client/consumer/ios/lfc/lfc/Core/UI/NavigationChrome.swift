import SwiftUI

extension View {
    /// Hides the system navigation bar and back button. Use on pushed screens that provide their own header.
    func lfcHideSystemNavigationBar() -> some View {
        navigationBarBackButtonHidden(true)
            .toolbar(.hidden, for: .navigationBar)
    }
}

struct DetailFallbackHeader: View {
    let onBack: () -> Void

    var body: some View {
        HStack {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(XhsTheme.textPrimary)
                    .frame(width: 40, height: 40)
            }
            .buttonStyle(.plain)
            Spacer()
        }
        .padding(.horizontal, 4)
        .padding(.vertical, 6)
        .background(Color.white)
    }
}
