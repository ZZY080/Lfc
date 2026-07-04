import SwiftUI

struct UserProfileView: View {
    let userId: Int
    @Bindable var store: HomeStore
    let currentUserId: Int?
    let onBack: () -> Void
    let onPostTap: (Int) -> Void
    let onActivityTap: (Int) -> Void
    let onOpenChat: (Int) -> Void

    var body: some View {
        XhsVisitorProfileView(
            userId: userId,
            store: store,
            currentUserId: currentUserId,
            onBack: onBack,
            onPostTap: onPostTap,
            onActivityTap: onActivityTap,
            onOpenChat: onOpenChat
        )
        .task(id: userId) {
            let state = store.profileState
            if state.selectedUserProfile?.id != userId
                || state.visitorProfileTabs.targetUserId != userId
                || store.visitorProfileLoadFailed {
                await store.enterUserProfile(userId)
            }
        }
    }
}
