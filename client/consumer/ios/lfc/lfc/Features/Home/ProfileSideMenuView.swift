import SwiftUI

struct ProfileSideMenuView: View {
    let profile: UserProfileDto?
    let unreadCount: Int
    let onDismiss: () -> Void
    let onScan: () -> Void
    let onShowMyQr: () -> Void
    let onSettings: () -> Void
    let onOrders: () -> Void
    let onEditProfile: () -> Void
    let onSearch: () -> Void
    let onGoToMessages: () -> Void
    let onLogout: () -> Void

    @State private var dragOffset: CGFloat = 0

    private let menuWidth: CGFloat = 300

    var body: some View {
        ZStack(alignment: .leading) {
            Color.black.opacity(0.35)
                .ignoresSafeArea()
                .onTapGesture(perform: onDismiss)

            VStack(alignment: .leading, spacing: 0) {
                if profile == nil {
                    SideMenuSkeleton()
                } else {
                    header
                    Divider().overlay(XhsTheme.divider)
                    menuSection
                    Spacer()
                    logoutButton
                }
            }
            .frame(width: menuWidth)
            .frame(maxHeight: .infinity)
            .background(Color.white)
            .offset(x: dragOffset)
            .gesture(
                DragGesture()
                    .onChanged { value in
                        dragOffset = min(0, value.translation.width)
                    }
                    .onEnded { value in
                        if value.translation.width < -80 {
                            onDismiss()
                        }
                        dragOffset = 0
                    }
            )
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                avatarView
                VStack(alignment: .leading, spacing: 4) {
                    Text(profile?.displayName ?? "未登录")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(XhsTheme.textPrimary)
                    Text("LFC号：\(profile?.lfcNo ?? "—")")
                        .font(.system(size: 12))
                        .foregroundStyle(XhsTheme.textSecondary)
                }
            }
            HStack(spacing: 20) {
                statItem(value: profile?.postCount ?? 0, label: "笔记")
                statItem(value: profile?.followerCount ?? 0, label: "粉丝")
                statItem(value: profile?.followingCount ?? 0, label: "关注")
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 56)
        .padding(.bottom, 20)
    }

    private var avatarView: some View {
        Group {
            if let urlString = profile?.avatarUrl, let url = URL(string: urlString) {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    placeholderAvatar
                }
            } else {
                placeholderAvatar
            }
        }
        .frame(width: 56, height: 56)
        .clipShape(Circle())
    }

    private var placeholderAvatar: some View {
        Circle()
            .fill(XhsTheme.red.opacity(0.15))
            .overlay {
                Image(systemName: "person.fill")
                    .foregroundStyle(XhsTheme.red)
            }
    }

    private func statItem(value: Int, label: String) -> some View {
        VStack(spacing: 2) {
            Text("\(value)")
                .font(.system(size: 16, weight: .semibold))
            Text(label)
                .font(.system(size: 11))
                .foregroundStyle(XhsTheme.textSecondary)
        }
        .foregroundStyle(XhsTheme.textPrimary)
    }

    private var menuSection: some View {
        VStack(spacing: 0) {
            menuRow("qrcode.viewfinder", "扫一扫", action: onScan)
            menuRow("qrcode", "我的二维码", action: onShowMyQr)
            menuRow("magnifyingglass", "搜索我的笔记", action: onSearch)
            menuRow("message.fill", "消息", badge: unreadCount, action: onGoToMessages)
            menuRow("bag.fill", "我的订单", action: onOrders)
            menuRow("person.crop.circle", "编辑资料", action: onEditProfile)
            menuRow("gearshape.fill", "设置", action: onSettings)
        }
        .padding(.top, 8)
    }

    private func menuRow(
        _ icon: String,
        _ title: String,
        badge: Int = 0,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: 14) {
                Image(systemName: icon)
                    .font(.system(size: 18))
                    .frame(width: 24)
                    .foregroundStyle(XhsTheme.textPrimary)
                Text(title)
                    .font(.system(size: 15))
                    .foregroundStyle(XhsTheme.textPrimary)
                Spacer()
                if badge > 0 {
                    Text(badge > 99 ? "99+" : "\(badge)")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(XhsTheme.red)
                        .clipShape(Capsule())
                }
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(XhsTheme.textSecondary.opacity(0.6))
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 14)
        }
        .buttonStyle(.plain)
    }

    private var logoutButton: some View {
        Button(action: onLogout) {
            Text("退出登录")
                .font(.system(size: 15))
                .foregroundStyle(XhsTheme.red)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
        }
        .buttonStyle(.plain)
        .padding(.bottom, 24)
    }
}
