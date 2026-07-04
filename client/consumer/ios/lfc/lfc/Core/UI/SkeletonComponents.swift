import SwiftUI

enum SkeletonColors {
    /// Android `SkeletonBase` — `0xFFE8E8E8`
    static let base = Color(red: 232 / 255, green: 232 / 255, blue: 232 / 255)
    /// Android `SkeletonHighlight` — `0xFFF5F5F5`
    static let highlight = Color(red: 245 / 255, green: 245 / 255, blue: 245 / 255)
    /// Android profile skeleton page background — `0xFFF5F5F5`
    static let pageBackground = highlight
    /// Android product detail skeleton background — `0xFFF4F4F5`
    static let productPageBackground = Color(red: 244 / 255, green: 244 / 255, blue: 245 / 255)
}

private struct SkeletonShimmerModifier: ViewModifier {
    @State private var translate: CGFloat = 0

    func body(content: Content) -> some View {
        content
            .overlay {
                GeometryReader { geo in
                    let width = max(geo.size.width, 1)
                    let height = max(geo.size.height, 1)
                    LinearGradient(
                        colors: [SkeletonColors.base, SkeletonColors.highlight, SkeletonColors.base],
                        startPoint: UnitPoint(
                            x: (translate - 400) / width,
                            y: (translate - 400) / height
                        ),
                        endPoint: UnitPoint(
                            x: translate / width,
                            y: translate / height
                        )
                    )
                }
                .allowsHitTesting(false)
            }
            .mask(content)
            .onAppear {
                withAnimation(.linear(duration: 1.1).repeatForever(autoreverses: false)) {
                    translate = 1200
                }
            }
    }
}

extension View {
    func skeletonShimmer() -> some View {
        modifier(SkeletonShimmerModifier())
    }
}

struct SkeletonBox: View {
    var cornerRadius: CGFloat = 8

    var body: some View {
        RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
            .fill(SkeletonColors.base)
            .skeletonShimmer()
    }
}

struct SkeletonCircle: View {
    let size: CGFloat

    var body: some View {
        Circle()
            .fill(SkeletonColors.base)
            .frame(width: size, height: size)
            .skeletonShimmer()
    }
}

struct SkeletonLine: View {
    var height: CGFloat = 12
    var widthFraction: CGFloat = 1
    var fixedWidth: CGFloat?

    var body: some View {
        Group {
            if let fixedWidth {
                SkeletonBox(cornerRadius: 6)
                    .frame(width: fixedWidth, height: height)
            } else {
                SkeletonBox(cornerRadius: 6)
                    .frame(height: height)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .mask(alignment: .leading) {
                        Rectangle()
                            .scaleEffect(x: max(widthFraction, 0.2), y: 1, anchor: .leading)
                    }
            }
        }
        .frame(height: height)
    }
}

struct SkeletonLoadMoreFooter: View {
    var body: some View {
        SkeletonLine(height: 10, fixedWidth: 120)
            .frame(maxWidth: .infinity)
            .padding(16)
    }
}
