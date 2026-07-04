import SwiftUI

struct SettingsView: View {
    let profile: UserProfileDto?
    @Bindable var store: HomeStore
    let onBack: () -> Void
    let onOpenOrders: () -> Void
    let onOpenTransactions: () -> Void
    let onOpenLegalDocument: (LegalDocumentId) -> Void
    let onLogout: () -> Void

    @State private var showCommentsPublic = false
    @State private var showFavoritesPublic = false
    @State private var showLikesPublic = false
    @State private var showLogoutConfirm = false

    var body: some View {
        VStack(spacing: 0) {
            HomeStubNavigationBar(title: "设置", onBack: onBack)

            if profile == nil {
                SettingsSkeleton()
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            } else {
                List {
                    Section {
                        settingsRow(icon: "doc.text", title: "我的订单", action: onOpenOrders)
                        settingsRow(icon: "yensign.circle", title: "收支流水", action: onOpenTransactions)
                    }

                    Section("收款账号") {
                        HStack {
                            Text(profile?.alipayBound == true ? "已绑定" : "未绑定")
                                .foregroundStyle(profile?.alipayBound == true ? .green : XhsTheme.textSecondary)
                            Spacer()
                            if let masked = profile?.alipayLoginIdMasked, profile?.alipayBound == true {
                                Text(masked)
                                    .foregroundStyle(XhsTheme.textSecondary)
                            }
                        }
                        Text("买家支付后平台托管，确认收货后分账到你的支付宝（扣除\(store.paymentConfig?.platformFeeRateLabel ?? "服务费")）。")
                            .font(.system(size: 12))
                            .foregroundStyle(XhsTheme.textSecondary)
                        if profile?.alipayBound == true {
                            Button("解绑收款账号", role: .destructive) {
                                Task { await store.unbindAlipayAccount() }
                            }
                            .disabled(store.isPrivacyUpdating)
                        } else {
                            Text("iOS 端暂不支持跳转支付宝授权，请使用 Android 客户端完成绑定。")
                                .font(.system(size: 12))
                                .foregroundStyle(XhsTheme.textSecondary)
                        }
                    }

                    Section {
                        Text("关闭后，他人访问你的主页时将无法查看对应内容")
                            .font(.system(size: 12))
                            .foregroundStyle(XhsTheme.textSecondary)
                        privacyToggle("公开我的评论", isOn: $showCommentsPublic)
                        privacyToggle("公开我的收藏", isOn: $showFavoritesPublic)
                        privacyToggle("公开我的赞过", isOn: $showLikesPublic)
                    } header: {
                        Text("隐私设置")
                    }

                    Section("法律与隐私") {
                        ForEach(LegalDocumentId.allCases) { doc in
                            Button(doc.title) { onOpenLegalDocument(doc) }
                                .foregroundStyle(XhsTheme.textPrimary)
                        }
                    }

                    Section {
                        Button("退出登录", role: .destructive) {
                            showLogoutConfirm = true
                        }
                    }
                }
                .listStyle(.insetGrouped)
            }
        }
        .background(Color.white)
        .onAppear { syncPrivacy() }
        .onChange(of: profile?.id) { _, _ in syncPrivacy() }
        .confirmationDialog("确定退出登录？", isPresented: $showLogoutConfirm, titleVisibility: .visible) {
            Button("退出登录", role: .destructive, action: onLogout)
            Button("取消", role: .cancel) {}
        }
    }

    private func settingsRow(icon: String, title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                Image(systemName: icon)
                    .foregroundStyle(XhsTheme.red)
                    .frame(width: 24)
                Text(title)
                    .foregroundStyle(XhsTheme.textPrimary)
            }
        }
    }

    private func privacyToggle(_ title: String, isOn: Binding<Bool>) -> some View {
        Toggle(title, isOn: isOn)
            .tint(XhsTheme.red)
            .disabled(store.isPrivacyUpdating)
            .onChange(of: isOn.wrappedValue) { _, _ in
                Task {
                    await store.updatePrivacySettings(
                        showCommentsPublic: showCommentsPublic,
                        showFavoritesPublic: showFavoritesPublic,
                        showLikesPublic: showLikesPublic
                    )
                }
            }
    }

    private func syncPrivacy() {
        guard let profile else { return }
        showCommentsPublic = profile.showCommentsPublic
        showFavoritesPublic = profile.showFavoritesPublic
        showLikesPublic = profile.showLikesPublic
    }
}
