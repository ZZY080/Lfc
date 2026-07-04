import SwiftUI

struct PublishHubView: View {
    let onDismiss: () -> Void
    let onPublishPost: () -> Void
    let onPublishActivity: () -> Void

    var body: some View {
        ZStack(alignment: .bottom) {
            Color.black.opacity(0.45)
                .ignoresSafeArea()
                .onTapGesture(perform: onDismiss)

            VStack(spacing: 0) {
                Capsule()
                    .fill(Color(red: 0.88, green: 0.88, blue: 0.88))
                    .frame(width: 36, height: 4)
                    .padding(.top, 10)
                    .padding(.bottom, 8)

                HStack {
                    Spacer()
                    Button(action: onDismiss) {
                        Image(systemName: "xmark")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(XhsTheme.textSecondary)
                            .frame(width: 32, height: 32)
                            .background(Color(red: 0.95, green: 0.95, blue: 0.95))
                            .clipShape(Circle())
                    }
                }
                .padding(.horizontal, 16)

                Text("发布内容")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(XhsTheme.textPrimary)
                    .padding(.bottom, 20)

                HStack(spacing: 16) {
                    publishOption(
                        title: "发笔记",
                        subtitle: "分享校园生活",
                        icon: "square.and.pencil",
                        action: onPublishPost
                    )
                    publishOption(
                        title: "发活动",
                        subtitle: "组织线下活动",
                        icon: "calendar.badge.plus",
                        action: onPublishActivity
                    )
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 28)
            }
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .padding(.horizontal, 0)
        }
        .transition(.opacity.combined(with: .move(edge: .bottom)))
    }

    private func publishOption(
        title: String,
        subtitle: String,
        icon: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 10) {
                Image(systemName: icon)
                    .font(.system(size: 24))
                    .foregroundStyle(XhsTheme.red)
                    .frame(width: 44, height: 44)
                    .background(XhsTheme.red.opacity(0.1))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

                Text(title)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(XhsTheme.textPrimary)
                Text(subtitle)
                    .font(.system(size: 12))
                    .foregroundStyle(XhsTheme.textSecondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .background(Color(red: 0.98, green: 0.98, blue: 0.98))
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}
