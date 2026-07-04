import SwiftUI

struct ProfileTabView: View {
    @Bindable var store: HomeStore
    let onOpenSideMenu: () -> Void
    let onEditProfile: () -> Void
    let onShowQr: () -> Void
    let onScanProfile: () -> Void
    let onPostTap: (Int) -> Void
    let onActivityTap: (Int) -> Void
    let onBindAlipay: () -> Void

    var body: some View {
        XhsSelfProfileView(
            store: store,
            onOpenSideMenu: onOpenSideMenu,
            onEditProfile: onEditProfile,
            onShowQr: onShowQr,
            onScanProfile: onScanProfile,
            onPostTap: onPostTap,
            onActivityTap: onActivityTap,
            onBindAlipay: onBindAlipay
        )
        .task {
            await store.ensureMyProfileReady()
        }
    }
}
