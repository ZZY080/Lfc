import SwiftUI

struct SplashView: View {
    var body: some View {
        // Same asset as LaunchScreen.storyboard — keeps system launch and SwiftUI splash identical.
        Image("LaunchScreen")
            .resizable()
            .scaledToFill()
            .frame(minWidth: 0, maxWidth: .infinity, minHeight: 0, maxHeight: .infinity)
            .clipped()
            .ignoresSafeArea()
    }
}

#Preview {
    SplashView()
}
