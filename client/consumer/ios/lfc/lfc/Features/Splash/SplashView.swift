import SwiftUI

struct SplashView: View {
    @State private var logoScale: CGFloat = 0.88
    @State private var contentOpacity: Double = 0

    var body: some View {
        ZStack {
            LfcBrand.splashGradient
                .ignoresSafeArea()

            VStack(spacing: 20) {
                Image("LaunchLogo")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 112, height: 112)
                    .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
                    .shadow(color: .black.opacity(0.12), radius: 16, y: 8)
                    .scaleEffect(logoScale)

                VStack(spacing: 8) {
                    Text("莲峰校园")
                        .font(.system(size: 28, weight: .semibold))
                        .foregroundStyle(.white)

                    Text("校园生活，从这里开始")
                        .font(.system(size: 15, weight: .regular))
                        .foregroundStyle(.white.opacity(0.88))
                }
            }
            .opacity(contentOpacity)
        }
        .onAppear {
            withAnimation(.easeOut(duration: 0.55)) {
                logoScale = 1
                contentOpacity = 1
            }
        }
    }
}

#Preview {
    SplashView()
}
