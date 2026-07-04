import SwiftUI

// MARK: - Feed

private struct FeedCardSkeleton: View {
    let aspectRatio: CGFloat

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack(alignment: .bottom) {
                SkeletonBox(cornerRadius: 10)
                    .aspectRatio(aspectRatio, contentMode: .fit)
                    .frame(maxWidth: .infinity)
                SkeletonBox(cornerRadius: 10)
                    .frame(height: 16)
                    .padding(.horizontal, 3)
                    .padding(.bottom, 3)
            }
            SkeletonLine(height: 12, widthFraction: 0.92)
                .padding(.horizontal, 8)
                .padding(.top, 8)
            SkeletonLine(height: 10, widthFraction: 0.65)
                .padding(.horizontal, 8)
                .padding(.top, 6)
            HStack(spacing: 6) {
                SkeletonCircle(size: 16)
                SkeletonLine(height: 9, widthFraction: 0.4)
                    .frame(maxWidth: .infinity, alignment: .leading)
                SkeletonCircle(size: 11)
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 8)
        }
        .frame(maxWidth: .infinity)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }
}

struct FeedGridSkeletonStatic: View {
    var itemCount: Int = 6
    var spacing: CGFloat = 8

    var body: some View {
        VStack(spacing: spacing) {
            ForEach(0 ..< (itemCount + 1) / 2, id: \.self) { row in
                HStack(alignment: .top, spacing: spacing) {
                    FeedCardSkeleton(aspectRatio: 0.68 + CGFloat(row % 3) * 0.06)
                        .frame(maxWidth: .infinity)
                    if row * 2 + 1 < itemCount {
                        FeedCardSkeleton(aspectRatio: 0.68 + CGFloat((row + 1) % 3) * 0.06)
                            .frame(maxWidth: .infinity)
                    } else {
                        Color.clear.frame(maxWidth: .infinity)
                    }
                }
            }
        }
        .padding(8)
        .frame(maxWidth: .infinity, alignment: .top)
    }
}

// MARK: - Activity

private struct ActivityCardSkeleton: View {
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            SkeletonBox(cornerRadius: 6)
                .frame(width: 96, height: 96)
            VStack(alignment: .leading, spacing: 6) {
                SkeletonLine(height: 16, widthFraction: 0.95)
                SkeletonLine(height: 16, widthFraction: 0.7)
                SkeletonLine(height: 13, widthFraction: 0.8)
                    .padding(.top, 2)
                HStack(alignment: .top, spacing: 4) {
                    SkeletonBox(cornerRadius: 3)
                        .frame(width: 14, height: 14)
                    VStack(alignment: .leading, spacing: 4) {
                        SkeletonLine(height: 13, widthFraction: 0.9)
                        SkeletonLine(height: 13, widthFraction: 0.55)
                    }
                }
                HStack {
                    SkeletonLine(height: 13, widthFraction: 0.45)
                    Spacer()
                    SkeletonBox(cornerRadius: 14)
                        .frame(width: 76, height: 28)
                }
                .padding(.top, 4)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
    }
}

struct ActivityFeedSkeleton: View {
    var itemCount: Int = 3

    var body: some View {
        VStack(spacing: 0) {
            ForEach(0 ..< itemCount, id: \.self) { index in
                ActivityCardSkeleton()
                if index < itemCount - 1 {
                    Divider().overlay(XhsTheme.divider)
                }
            }
        }
        .background(Color.white)
    }
}

// MARK: - Profile

struct ProfilePageSkeleton: View {
    var body: some View {
        VStack(spacing: 0) {
            ZStack(alignment: .bottomLeading) {
                SkeletonBox(cornerRadius: 0)
                    .frame(height: 220)
                SkeletonCircle(size: 72)
                    .padding(.leading, 16)
                    .padding(.bottom, 16)
            }
            VStack(alignment: .leading, spacing: 8) {
                SkeletonLine(height: 18, widthFraction: 0.35)
                SkeletonLine(height: 12, widthFraction: 0.55)
                HStack {
                    ForEach(0 ..< 3, id: \.self) { _ in
                        VStack(spacing: 4) {
                            SkeletonLine(height: 16, fixedWidth: 36)
                            SkeletonLine(height: 10, fixedWidth: 28)
                        }
                        .frame(maxWidth: .infinity)
                    }
                }
                .padding(.top, 6)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color.white)

            HStack(spacing: 24) {
                ForEach(0 ..< 4, id: \.self) { _ in
                    SkeletonLine(height: 12, fixedWidth: 36)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(Color.white)

            FeedGridSkeletonStatic(itemCount: 4)
        }
        .background(SkeletonColors.pageBackground)
    }
}

// MARK: - Detail

struct CommentListSkeleton: View {
    var body: some View {
        VStack(spacing: 18) {
            ForEach(0 ..< 3, id: \.self) { _ in
                HStack(alignment: .top, spacing: 10) {
                    SkeletonCircle(size: 34)
                    VStack(alignment: .leading, spacing: 6) {
                        SkeletonLine(height: 11, widthFraction: 0.28)
                        SkeletonLine(height: 12, widthFraction: 0.95)
                        SkeletonLine(height: 12, widthFraction: 0.72)
                    }
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color.white)
    }
}

struct PostDetailSkeleton: View {
    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 10) {
                SkeletonCircle(size: 32)
                VStack(alignment: .leading, spacing: 6) {
                    SkeletonLine(height: 12, widthFraction: 0.35)
                    SkeletonLine(height: 10, widthFraction: 0.22)
                }
                Spacer()
                SkeletonBox(cornerRadius: 14)
                    .frame(width: 56, height: 28)
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 10)

            SkeletonBox(cornerRadius: 0)
                .aspectRatio(1, contentMode: .fit)

            VStack(alignment: .leading, spacing: 8) {
                SkeletonLine(height: 18, widthFraction: 0.88)
                SkeletonLine(height: 14)
                SkeletonLine(height: 14)
                SkeletonLine(height: 14, widthFraction: 0.76)
                SkeletonLine(height: 11, widthFraction: 0.3)
                    .padding(.top, 8)
            }
            .padding(16)

            CommentListSkeleton()
            Spacer(minLength: 0)
        }
        .background(Color.white)
    }
}

struct ActivityDetailSkeleton: View {
    var body: some View {
        VStack(spacing: 0) {
            SkeletonBox(cornerRadius: 0)
                .frame(height: 260)
            VStack(alignment: .leading, spacing: 12) {
                SkeletonLine(height: 20, widthFraction: 0.82)
                HStack(spacing: 8) {
                    SkeletonBox(cornerRadius: 14).frame(width: 72, height: 28)
                    SkeletonBox(cornerRadius: 14).frame(width: 56, height: 28)
                }
                ForEach(0 ..< 2, id: \.self) { _ in
                    VStack(alignment: .leading, spacing: 10) {
                        SkeletonLine(height: 12, widthFraction: 0.25)
                        SkeletonLine(height: 15, widthFraction: 0.7)
                        SkeletonLine(height: 13, widthFraction: 0.55)
                    }
                    .padding(14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(XhsTheme.background)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
            }
            .padding(16)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .offset(y: -16)
            Spacer(minLength: 0)
        }
        .background(XhsTheme.background)
    }
}

struct ProductDetailSkeleton: View {
    var body: some View {
        VStack(spacing: 0) {
            HStack {
                SkeletonCircle(size: 36)
                Spacer()
                SkeletonBox(cornerRadius: 18)
                    .frame(width: 88, height: 32)
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 8)
            .background(Color.white)

            SkeletonBox(cornerRadius: 0)
                .aspectRatio(1, contentMode: .fit)

            VStack(spacing: 10) {
                SkeletonBox(cornerRadius: 16).frame(height: 148)
                SkeletonBox(cornerRadius: 16).frame(height: 108)
                SkeletonBox(cornerRadius: 16).frame(height: 72)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            Spacer(minLength: 0)
        }
        .background(SkeletonColors.productPageBackground)
    }
}

enum DetailSkeletonStyle {
    case post, activity, product

    @ViewBuilder
    var view: some View {
        switch self {
        case .post: PostDetailSkeleton()
        case .activity: ActivityDetailSkeleton()
        case .product: ProductDetailSkeleton()
        }
    }
}

// MARK: - Messages

struct ChatMessageListSkeleton: View {
    var body: some View {
        VStack(spacing: 14) {
            HStack {
                SkeletonCircle(size: 32)
                SkeletonBox(cornerRadius: 12)
                    .frame(width: 180, height: 44)
                Spacer()
            }
            HStack {
                Spacer()
                SkeletonBox(cornerRadius: 12)
                    .frame(width: 140, height: 40)
            }
            HStack {
                SkeletonCircle(size: 32)
                SkeletonBox(cornerRadius: 12)
                    .frame(width: 220, height: 56)
                Spacer()
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(XhsTheme.background)
    }
}

private struct NotificationListItemSkeleton: View {
    var body: some View {
        HStack(spacing: 12) {
            SkeletonCircle(size: 40)
            VStack(alignment: .leading, spacing: 8) {
                SkeletonLine(height: 14, widthFraction: 0.55)
                SkeletonLine(height: 12, widthFraction: 0.85)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
    }
}

struct NotificationListSkeleton: View {
    var itemCount: Int = 8

    var body: some View {
        VStack(spacing: 0) {
            ForEach(0 ..< itemCount, id: \.self) { _ in
                NotificationListItemSkeleton()
            }
        }
    }
}

private struct ConversationListItemSkeleton: View {
    var body: some View {
        HStack(spacing: 12) {
            SkeletonCircle(size: 48)
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    SkeletonLine(height: 14, widthFraction: 0.5)
                    Spacer()
                    SkeletonLine(height: 10, fixedWidth: 36)
                }
                SkeletonLine(height: 12, widthFraction: 0.75)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }
}

struct ConversationListSkeleton: View {
    var body: some View {
        VStack(spacing: 0) {
            SkeletonLine(height: 12, widthFraction: 0.22)
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
            ForEach(0 ..< 3, id: \.self) { _ in
                NotificationListItemSkeleton()
            }
            SkeletonBox(cornerRadius: 0)
                .frame(height: 6)
                .padding(.top, 8)
            SkeletonLine(height: 12, widthFraction: 0.18)
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
            ForEach(0 ..< 5, id: \.self) { _ in
                ConversationListItemSkeleton()
            }
        }
        .background(Color.white)
    }
}

struct MessageDetailSkeleton: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            SkeletonBox(cornerRadius: 8)
                .frame(height: 36)
            SkeletonLine(height: 18, widthFraction: 0.75)
            ForEach(0 ..< 4, id: \.self) { _ in
                SkeletonLine(height: 14)
            }
            SkeletonLine(height: 14, widthFraction: 0.6)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 20)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(XhsTheme.background)
    }
}

// MARK: - Orders

struct OrderListSkeleton: View {
    var itemCount: Int = 4

    var body: some View {
        VStack(spacing: 12) {
            ForEach(0 ..< itemCount, id: \.self) { _ in
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        SkeletonLine(height: 12, widthFraction: 0.35)
                        Spacer()
                        SkeletonLine(height: 12, fixedWidth: 56)
                    }
                    HStack(spacing: 12) {
                        SkeletonBox(cornerRadius: 10)
                            .frame(width: 64, height: 64)
                        VStack(alignment: .leading, spacing: 8) {
                            SkeletonLine(height: 14, widthFraction: 0.9)
                            SkeletonLine(height: 12, widthFraction: 0.5)
                        }
                    }
                    SkeletonLine(height: 10, widthFraction: 0.45)
                }
                .padding(14)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
        }
        .padding(12)
    }
}

struct PaymentLedgerSkeleton: View {
    var itemCount: Int = 5

    var body: some View {
        VStack(spacing: 10) {
            ForEach(0 ..< itemCount, id: \.self) { _ in
                HStack(spacing: 12) {
                    SkeletonCircle(size: 36)
                    VStack(alignment: .leading, spacing: 6) {
                        SkeletonLine(height: 13, widthFraction: 0.7)
                        SkeletonLine(height: 11, widthFraction: 0.45)
                    }
                    Spacer()
                    SkeletonLine(height: 14, fixedWidth: 52)
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 14)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
        }
        .padding(12)
    }
}

// MARK: - Forms & lists

struct ListRowSkeleton: View {
    var itemCount: Int = 6

    var body: some View {
        VStack(spacing: 4) {
            ForEach(0 ..< itemCount, id: \.self) { _ in
                HStack(spacing: 12) {
                    SkeletonCircle(size: 36)
                    VStack(alignment: .leading, spacing: 6) {
                        SkeletonLine(height: 14, widthFraction: 0.7)
                        SkeletonLine(height: 11, widthFraction: 0.45)
                    }
                }
                .padding(.vertical, 12)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }
}

struct EditProfileSkeleton: View {
    var body: some View {
        VStack(spacing: 0) {
            HStack {
                SkeletonCircle(size: 36)
                Spacer()
                SkeletonLine(height: 16, fixedWidth: 88)
                Spacer()
                SkeletonLine(height: 16, fixedWidth: 44)
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 8)
            .background(Color.white)

            SkeletonBox(cornerRadius: 0)
                .frame(height: 160)

            VStack(alignment: .leading, spacing: 28) {
                SkeletonCircle(size: 80)
                ForEach(0 ..< 4, id: \.self) { _ in
                    SkeletonBox(cornerRadius: 8)
                        .frame(height: 52)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 20)
            Spacer(minLength: 0)
        }
        .background(XhsTheme.background)
    }
}

struct SettingsSkeleton: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            ForEach(0 ..< 3, id: \.self) { _ in
                SkeletonBox(cornerRadius: 12)
                    .frame(height: 56)
            }
            SkeletonLine(height: 14, widthFraction: 0.25)
                .padding(.top, 8)
            ForEach(0 ..< 3, id: \.self) { _ in
                SkeletonBox(cornerRadius: 8)
                    .frame(height: 48)
            }
        }
        .padding(16)
    }
}

struct EditFormSkeleton: View {
    var body: some View {
        VStack(spacing: 12) {
            SkeletonBox(cornerRadius: 12).frame(height: 120)
            ForEach(0 ..< 5, id: \.self) { _ in
                SkeletonBox(cornerRadius: 10).frame(height: 52)
            }
        }
        .padding(16)
    }
}

struct QrCodeSkeleton: View {
    var body: some View {
        VStack(spacing: 20) {
            SkeletonCircle(size: 64)
            SkeletonBox(cornerRadius: 12)
                .frame(width: 220, height: 220)
            SkeletonLine(height: 14, widthFraction: 0.5)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct AuthFormSkeleton: View {
    var body: some View {
        VStack(spacing: 16) {
            SkeletonLine(height: 28, widthFraction: 0.5)
                .padding(.bottom, 24)
            ForEach(0 ..< 3, id: \.self) { _ in
                SkeletonBox(cornerRadius: 10).frame(height: 48)
            }
            SkeletonBox(cornerRadius: 24)
                .frame(height: 48)
                .padding(.top, 12)
        }
        .padding(24)
    }
}
