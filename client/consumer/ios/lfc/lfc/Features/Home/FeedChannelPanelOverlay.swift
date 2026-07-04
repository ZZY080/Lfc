import SwiftUI

/// Channel panel body shown below the primary feed tabs; covers feed + bottom tab bar.
struct FeedChannelPanelOverlay: View {
    let feedState: FeedUiState
    let recommendedChannels: [String]
    let onToggleChannelEditMode: () -> Void
    let onEnterChannelEditMode: () -> Void
    let onCollapse: () -> Void
    let onChannelClick: (String) -> Void
    let onAddChannel: (String) -> Void
    let onRemoveChannel: (String) -> Void

    var body: some View {
        ScrollView {
            XhsFeedChannelPanel(
                myChannels: feedState.effectiveMyChannels,
                recommendedChannels: recommendedChannels,
                isEditMode: feedState.isChannelEditMode,
                onToggleEditMode: onToggleChannelEditMode,
                onEnterEditMode: onEnterChannelEditMode,
                onCollapse: onCollapse,
                onChannelClick: onChannelClick,
                onAddChannel: onAddChannel,
                onRemoveChannel: onRemoveChannel
            )
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(XhsTheme.background)
        .transition(.opacity)
    }
}
