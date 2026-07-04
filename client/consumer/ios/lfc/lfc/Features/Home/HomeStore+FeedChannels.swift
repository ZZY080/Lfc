import Foundation
import SwiftUI

extension HomeStore {
    var feedCityLabel: String {
        extractFeedCityLabel(address: userLocationAddress)
    }

    func loadFeedChannels() async {
        do {
            let catalog = try await api.getFeedChannelCatalog()
            applyFeedChannelConfig(
                allChannels: catalog.allChannels,
                defaultMyChannels: catalog.defaultMyChannels,
                publishCategories: catalog.publishCategories
            )
        } catch {
            applyFeedChannelConfig(
                allChannels: fallbackMyChannels,
                defaultMyChannels: fallbackMyChannels,
                publishCategories: publishCategories.isEmpty ? fallbackPublishCategories : publishCategories
            )
        }

        if let response = try? await api.getMyFeedChannels() {
            applyFeedChannelConfig(
                myChannels: response.myChannels,
                allChannels: response.allChannels,
                recommendedChannels: response.recommendedChannels,
                publishCategories: response.publishCategories
            )
        }
    }

    func applyFeedChannelConfig(
        myChannels: [String]? = nil,
        allChannels: [String]? = nil,
        recommendedChannels: [String]? = nil,
        defaultMyChannels: [String]? = nil,
        publishCategories: [String]? = nil
    ) {
        if let myChannels { feedState.myChannels = myChannels }
        if let allChannels { feedState.allChannels = allChannels }
        if let recommendedChannels { feedState.recommendedChannels = recommendedChannels }
        if let defaultMyChannels { feedState.defaultMyChannels = defaultMyChannels }
        if let publishCategories, !publishCategories.isEmpty {
            self.publishCategories = publishCategories
        }
    }

    func toggleFeedChannelPanel() {
        withAnimation(.easeInOut(duration: 0.24)) {
            feedState.isChannelPanelExpanded.toggle()
            if !feedState.isChannelPanelExpanded {
                feedState.isChannelEditMode = false
            }
        }
    }

    func collapseFeedChannelPanel() {
        guard feedState.isChannelPanelExpanded || feedState.isChannelEditMode else { return }
        withAnimation(.easeInOut(duration: 0.24)) {
            feedState.isChannelPanelExpanded = false
            feedState.isChannelEditMode = false
        }
    }

    func enterFeedChannelEditMode() {
        withAnimation(.easeInOut(duration: 0.24)) {
            feedState.isChannelPanelExpanded = true
            feedState.isChannelEditMode = true
        }
    }

    func toggleFeedChannelEditMode() {
        feedState.isChannelEditMode.toggle()
    }

    var recommendedFeedChannels: [String] {
        feedState.recommendedChannels
    }

    func addFeedChannel(_ channel: String) {
        var current = feedState.effectiveMyChannels
        guard !current.contains(channel) else { return }
        current.append(channel)
        Task { await persistMyChannels(current) }
    }

    func removeFeedChannel(_ channel: String) {
        guard channel != feedRecommendChannel else { return }
        let selectedTab = feedState.selectedTab
        let updated = feedState.effectiveMyChannels.filter { $0 != channel }
        Task {
            await persistMyChannels(updated)
            if selectedTab == channel {
                selectFeedTab(feedRecommendChannel)
            }
        }
    }

    func persistMyChannels(_ channels: [String]) async {
        let previous = feedState.effectiveMyChannels
        feedState.myChannels = channels
        do {
            let response = try await api.updateMyFeedChannels(UpdateFeedChannelsRequest(channels: channels))
            applyFeedChannelConfig(
                myChannels: response.myChannels,
                allChannels: response.allChannels,
                recommendedChannels: response.recommendedChannels,
                publishCategories: response.publishCategories
            )
        } catch {
            feedState.myChannels = previous
            toastError = parseError(error, fallback: "频道同步失败")
        }
    }

    func feedCityQueryParamForCurrentTab() -> String? {
        feedCityQueryParam(feedState.primaryTab)
    }

    private func feedCityQueryParam(_ primaryTab: String) -> String? {
        if primaryTab == "关注" || primaryTab == "发现" { return nil }
        let trimmed = primaryTab.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty || trimmed == "同城" { return nil }
        return trimmed
    }
}
