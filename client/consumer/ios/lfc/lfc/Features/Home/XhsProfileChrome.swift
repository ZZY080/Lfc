import SwiftUI

enum XhsProfileLayout {
    static let coverHeight: CGFloat = 300
    static let sheetOverlap: CGFloat = 14
    static let sheetTopRadius: CGFloat = 14
    static let tabBarHeight: CGFloat = 44
    static let topBarHeight: CGFloat = 48
    static let coverDark = Color(red: 0.10, green: 0.14, blue: 0.16)

    static var statusBarTopInset: CGFloat {
        UIApplication.shared.connectedScenes
            .compactMap { ($0 as? UIWindowScene)?.keyWindow?.safeAreaInsets.top }
            .first ?? 0
    }

    static var topNavTotalHeight: CGFloat {
        statusBarTopInset + topBarHeight
    }

    static var stickyChromeHeight: CGFloat {
        topNavTotalHeight + tabBarHeight
    }

    /// Scroll offset when the tab bar reaches the bottom edge of the fixed top nav.
    static func tabPinScrollOffset(coverHeight: CGFloat = coverHeight) -> CGFloat {
        max(0, coverHeight - sheetOverlap - topNavTotalHeight)
    }

    static func visitorTabPinScrollOffset(gap: CGFloat = 12) -> CGFloat {
        max(0, coverHeight + gap - topNavTotalHeight)
    }

    static func visitorCollapseProgress(for scrollOffset: CGFloat, gap: CGFloat = 12) -> CGFloat {
        let pinOffset = visitorTabPinScrollOffset(gap: gap)
        let fadeStart: CGFloat = 24
        guard pinOffset > fadeStart else { return scrollOffset > 0 ? 1 : 0 }
        return min(1, max(0, (scrollOffset - fadeStart) / (pinOffset - fadeStart)))
    }

    static func collapseProgress(for scrollOffset: CGFloat, coverHeight: CGFloat = coverHeight) -> CGFloat {
        let pinOffset = tabPinScrollOffset(coverHeight: coverHeight)
        let fadeStart: CGFloat = 24
        guard pinOffset > fadeStart else { return scrollOffset > 0 ? 1 : 0 }
        return min(1, max(0, (scrollOffset - fadeStart) / (pinOffset - fadeStart)))
    }

    /// Matches Android: dark nav once header scrolls ~120pt or tabs are pinned.
    static func navIsCollapsed(scrollOffset: CGFloat, tabsArePinned: Bool) -> Bool {
        tabsArePinned || scrollOffset > 120
    }
}

/// White spinner centered on the cover during pull-to-refresh.
struct XhsProfileRefreshIndicator: View {
    let isRefreshing: Bool
    var pullDownOffset: CGFloat = 0

    private var shouldShow: Bool {
        isRefreshing || pullDownOffset > 24
    }

    var body: some View {
        if shouldShow {
            ProgressView()
                .progressViewStyle(.circular)
                .tint(.white)
                .scaleEffect(isRefreshing ? 1.15 : min(1.0, 0.6 + pullDownOffset / 120))
                .frame(maxWidth: .infinity)
                .padding(.top, XhsProfileLayout.topNavTotalHeight + XhsProfileLayout.coverHeight * 0.38)
                .opacity(isRefreshing ? 1 : min(1, pullDownOffset / 72))
                .transition(.opacity)
                .accessibilityLabel("正在刷新")
        }
    }
}

struct XhsProfileCoverImage: View {
    let coverUrl: String?

    var body: some View {
        Group {
            if let coverUrl, let url = URL(string: coverUrl) {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    XhsProfileCoverFallbackGradient()
                }
            } else {
                XhsProfileCoverFallbackGradient()
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .clipped()
    }
}

struct XhsProfileCoverFallbackGradient: View {
    var body: some View {
        LinearGradient(
            colors: [
                Color(red: 0.24, green: 0.33, blue: 0.38),
                Color(red: 0.14, green: 0.19, blue: 0.22),
                Color(red: 0.10, green: 0.14, blue: 0.16)
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }
}

struct XhsProfileCoverGradient: View {
    var body: some View {
        LinearGradient(
            colors: [
                Color.black.opacity(0.08),
                Color.black.opacity(0.28),
                Color.black.opacity(0.72)
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }
}

struct XhsProfileTabBarItem: Identifiable {
    let label: String
    let contentIndex: Int
    let count: Int
    var locked: Bool = false
    var id: Int { contentIndex }
}

struct XhsProfileTabBarView: View {
    let tabs: [XhsProfileTabBarItem]
    let selectedTab: Int
    var roundedTop: Bool = false
    var topCornerRadius: CGFloat = XhsProfileLayout.sheetTopRadius
    let onSelect: (Int) -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                ForEach(tabs) { tab in
                    tabButton(tab)
                }
            }
            Divider().background(Color(red: 0.94, green: 0.94, blue: 0.94))
        }
        .frame(height: XhsProfileLayout.tabBarHeight)
        .background(Color.white)
        .contentShape(Rectangle())
        .clipShape(
            UnevenRoundedRectangle(
                topLeadingRadius: roundedTop ? topCornerRadius : 0,
                bottomLeadingRadius: 0,
                bottomTrailingRadius: 0,
                topTrailingRadius: roundedTop ? topCornerRadius : 0
            )
        )
    }

    private func tabButton(_ tab: XhsProfileTabBarItem) -> some View {
        let selected = selectedTab == tab.contentIndex
        return Button {
            onSelect(tab.contentIndex)
        } label: {
            VStack(spacing: 0) {
                HStack(spacing: 2) {
                    if tab.locked {
                        Image(systemName: "lock.fill")
                            .font(.system(size: 8))
                            .foregroundStyle(XhsTheme.textSecondary.opacity(0.7))
                    }
                    Text(tab.label)
                        .font(.system(size: 14, weight: selected ? .bold : .regular))
                        .foregroundStyle(selected ? XhsTheme.textPrimary : XhsTheme.textSecondary)
                    Text(formatProfileStatCount(tab.count))
                        .font(.system(size: 12, weight: selected ? .semibold : .regular))
                        .foregroundStyle(
                            selected ? XhsTheme.textPrimary : XhsTheme.textSecondary.opacity(0.85)
                        )
                }
                .padding(.top, 10)
                .padding(.bottom, 6)

                Rectangle()
                    .fill(selected ? XhsTheme.textPrimary : Color.clear)
                    .frame(width: 28, height: 2)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .frame(maxWidth: .infinity, minHeight: XhsProfileLayout.tabBarHeight)
        .contentShape(Rectangle())
    }
}

enum XhsProfileTopBarMode {
    case selfProfile
    case visitor
}

/// Floating top navigation with gradient-to-solid transition driven by scroll progress.
struct XhsProfileGradientTopNav: View {
    let profile: UserProfileDto
    let mode: XhsProfileTopBarMode
    let collapseProgress: CGFloat
    var showAvatar: Bool = false
    let onMenu: (() -> Void)?
    let onBack: (() -> Void)?
    let onEditProfile: (() -> Void)?
    let onScanProfile: (() -> Void)?
    let onShare: (() -> Void)?

    var body: some View {
        ZStack(alignment: .top) {
            if collapseProgress < 1 {
                LinearGradient(
                    colors: [
                        Color.black.opacity(0.52),
                        Color.black.opacity(0.22),
                        Color.clear
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: XhsProfileLayout.topNavTotalHeight + 36)
                .opacity(Double(1 - collapseProgress))
            }

            if collapseProgress > 0 {
                XhsProfileLayout.coverDark
                    .opacity(collapseProgress >= 1 ? 1 : Double(collapseProgress))
                    .frame(height: XhsProfileLayout.topNavTotalHeight)
                    .frame(maxWidth: .infinity)
                    .ignoresSafeArea(edges: .top)
            }

            HStack(spacing: 4) {
                leadingButton

                if showAvatar {
                    Spacer()
                    XhsProfileAvatar(
                        label: profile.displayName,
                        size: 26,
                        avatarUrl: profile.avatarUrl
                    )
                    Spacer()
                } else {
                    Spacer()
                }

                trailingButtons
            }
            .padding(.horizontal, 2)
            .frame(height: XhsProfileLayout.topBarHeight)
            .padding(.top, XhsProfileLayout.statusBarTopInset)
        }
        .frame(height: XhsProfileLayout.topNavTotalHeight)
        .animation(.easeInOut(duration: 0.18), value: collapseProgress)
        .animation(.easeInOut(duration: 0.18), value: showAvatar)
    }

    @ViewBuilder
    private var leadingButton: some View {
        if let onBack {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 44, height: 44)
            }
            .buttonStyle(.plain)
        } else if let onMenu {
            Button(action: onMenu) {
                Image(systemName: "line.3.horizontal")
                    .font(.system(size: 20, weight: .medium))
                    .foregroundStyle(.white)
                    .frame(width: 44, height: 44)
            }
            .buttonStyle(.plain)
        } else {
            Color.clear.frame(width: 44, height: 44)
        }
    }

    @ViewBuilder
    private var trailingButtons: some View {
        if mode == .selfProfile {
            HStack(spacing: 4) {
                if let onEditProfile {
                    Button(action: onEditProfile) {
                        HStack(spacing: 4) {
                            Image(systemName: "pencil")
                                .font(.system(size: 12))
                            Text("编辑资料")
                                .font(.system(size: 12))
                        }
                        .foregroundStyle(.white)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(Color.black.opacity(0.28))
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }

                if let onScanProfile {
                    Button(action: onScanProfile) {
                        Image(systemName: "qrcode.viewfinder")
                            .font(.system(size: 18))
                            .foregroundStyle(.white)
                            .frame(width: 36, height: 36)
                    }
                    .buttonStyle(.plain)
                }

                if let onShare {
                    Button(action: onShare) {
                        Image(systemName: "square.and.arrow.up")
                            .font(.system(size: 18))
                            .foregroundStyle(.white)
                            .frame(width: 36, height: 36)
                    }
                    .buttonStyle(.plain)
                }
            }
        } else {
            Color.clear.frame(width: 44, height: 44)
        }
    }
}

func formatProfileStatCount(_ count: Int) -> String {
    if count < 10_000 { return "\(count)" }
    return String(format: "%.1fw", Double(count) / 10_000.0)
}

func profileShouldTriggerLoadMore(
    itemIndex: Int,
    totalCount: Int,
    tabState: ProfileTabUiState
) -> Bool {
    guard totalCount > 0 else { return false }
    guard itemIndex >= max(0, totalCount - 3) else { return false }
    guard tabState.hasMore, !tabState.isLoadingMore, !tabState.isRefreshing else { return false }
    return true
}

/// Bottom load-more sentinel + loading footer for profile tab lists.
struct ProfileTabPaginationFooter: View {
    let tabState: ProfileTabUiState
    var isContentEmpty: Bool = false
    let onLoadMore: () -> Void

    var body: some View {
        Group {
            if tabState.isLoadingMore {
                SkeletonLoadMoreFooter()
                    .padding(.vertical, 12)
            } else if tabState.hasMore && !tabState.isInitialLoading && !isContentEmpty {
                Color.clear
                    .frame(height: 1)
                    .onAppear(perform: onLoadMore)
            }
        }
    }
}

private struct ProfileTabBarMinYKey: PreferenceKey {
    static var defaultValue: CGFloat = .infinity
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

/// Reads UIScrollView.contentOffset so profile chrome reacts on every scroll frame.
private struct ProfileScrollOffsetTracker: UIViewRepresentable {
    @Binding var offset: CGFloat
    var pullDownOffset: Binding<CGFloat>?
    var scrollToY: CGFloat?

    func makeCoordinator() -> Coordinator {
        Coordinator(offset: $offset, pullDownOffset: pullDownOffset)
    }

    func makeUIView(context: Context) -> UIView {
        let view = UIView(frame: .zero)
        view.isUserInteractionEnabled = false
        view.backgroundColor = .clear
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        context.coordinator.attach(to: uiView)
        if let scrollToY {
            context.coordinator.scrollTo(y: scrollToY)
        }
    }

    final class Coordinator: NSObject {
        private var offset: Binding<CGFloat>
        private var pullDownOffset: Binding<CGFloat>?
        private weak var scrollView: UIScrollView?
        private var contentOffsetObservation: NSKeyValueObservation?

        init(offset: Binding<CGFloat>, pullDownOffset: Binding<CGFloat>?) {
            self.offset = offset
            self.pullDownOffset = pullDownOffset
        }

        private func publishScrollOffsets(from scrollView: UIScrollView) {
            let rawY = scrollView.contentOffset.y
            offset.wrappedValue = max(0, rawY)
            pullDownOffset?.wrappedValue = max(0, -rawY)
        }

        func attach(to view: UIView) {
            DispatchQueue.main.async { [weak self, weak view] in
                guard let self, let view else { return }
                guard let scrollView = view.enclosingScrollView else { return }
                if self.scrollView !== scrollView {
                    self.scrollView = scrollView
                    scrollView.delaysContentTouches = true
                    scrollView.canCancelContentTouches = true
                    self.contentOffsetObservation = scrollView.observe(\.contentOffset, options: [.new]) { [weak self] scrollView, _ in
                        self?.publishScrollOffsets(from: scrollView)
                    }
                }
                self.publishScrollOffsets(from: scrollView)
            }
        }

        func scrollTo(y: CGFloat) {
            guard let scrollView else { return }
            DispatchQueue.main.async { [weak self] in
                guard let self, let scrollView = self.scrollView else { return }
                scrollView.layoutIfNeeded()
                let maxOffset = max(
                    0,
                    scrollView.contentSize.height
                        - scrollView.bounds.height
                        + scrollView.adjustedContentInset.bottom
                )
                let target = min(max(0, y), maxOffset)
                guard abs(scrollView.contentOffset.y - target) > 1 else { return }
                scrollView.setContentOffset(CGPoint(x: 0, y: target), animated: false)
                self.publishScrollOffsets(from: scrollView)
            }
        }

        deinit {
            contentOffsetObservation?.invalidate()
        }
    }
}

private extension UIView {
    var enclosingScrollView: UIScrollView? {
        sequence(first: self.superview, next: { $0?.superview })
            .compactMap { $0 as? UIScrollView }
            .first
    }
}

extension View {
    func trackProfileScrollOffset(
        _ offset: Binding<CGFloat>,
        pullDownOffset: Binding<CGFloat>? = nil,
        scrollToY: CGFloat? = nil
    ) -> some View {
        background {
            ProfileScrollOffsetTracker(
                offset: offset,
                pullDownOffset: pullDownOffset,
                scrollToY: scrollToY
            )
            .frame(width: 0, height: 0)
        }
    }

    /// Fallback anchor for layouts that cannot attach a scroll tracker directly.
    func reportProfileTabBarMinY() -> some View {
        overlay {
            GeometryReader { proxy in
                Color.clear.preference(
                    key: ProfileTabBarMinYKey.self,
                    value: proxy.frame(in: .global).minY
                )
            }
        }
    }

    func observeProfileTabBarScroll(
        baselineMinY: Binding<CGFloat?>,
        scrollOffset: Binding<CGFloat>
    ) -> some View {
        onPreferenceChange(ProfileTabBarMinYKey.self) { minY in
            guard minY.isFinite else { return }
            if baselineMinY.wrappedValue == nil {
                baselineMinY.wrappedValue = minY
            }
            if let baseline = baselineMinY.wrappedValue {
                scrollOffset.wrappedValue = max(0, baseline - minY)
            }
        }
    }

    /// Hides the system refresh spinner; profile pages use a cover-centered indicator instead.
    func profileScrollRefreshStyle() -> some View {
        background {
            ProfileRefreshControlStyler()
                .frame(width: 0, height: 0)
        }
    }
}

private struct ProfileRefreshControlStyler: UIViewRepresentable {
    func makeUIView(context: Context) -> UIView {
        let view = UIView(frame: .zero)
        view.isUserInteractionEnabled = false
        view.backgroundColor = .clear
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        DispatchQueue.main.async {
            guard let scrollView = uiView.enclosingScrollView else { return }
            scrollView.backgroundColor = .clear
            guard let refreshControl = scrollView.refreshControl else { return }
            refreshControl.tintColor = .clear
            refreshControl.subviews.forEach { $0.isHidden = true }
        }
    }
}
