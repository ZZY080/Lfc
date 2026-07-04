import SwiftUI

struct LocationSearchView: View {
    @Bindable var store: HomeStore
    let onBack: () -> Void
    let onSelect: () -> Void

    @State private var keyword = ""
    @State private var items: [PlaceSuggestionDto] = []
    @State private var page = 1
    @State private var hasMore = false
    @State private var isSearching = false
    @State private var isLoadingMore = false
    @State private var searchTask: Task<Void, Never>?

    var body: some View {
        VStack(spacing: 0) {
            HomeStubNavigationBar(title: "选择位置", onBack: onBack)

            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(XhsTheme.textSecondary)
                TextField("搜索地点、商圈、学校", text: $keyword)
                    .submitLabel(.search)
                    .onSubmit { performSearch(reset: true) }
                if !keyword.isEmpty {
                    Button {
                        keyword = ""
                        items = []
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundStyle(XhsTheme.textSecondary)
                    }
                }
            }
            .padding(10)
            .background(Color.gray.opacity(0.08))
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .padding()

            if isSearching && items.isEmpty {
                ListRowSkeleton()
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            } else if items.isEmpty {
                ContentUnavailableView(
                    keyword.isEmpty ? "输入关键词搜索地点" : "未找到相关地点",
                    systemImage: "mappin.slash"
                )
                Spacer()
            } else {
                List {
                    ForEach(items) { place in
                        Button {
                            select(place)
                        } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(place.name)
                                    .font(.system(size: 15, weight: .medium))
                                    .foregroundStyle(XhsTheme.textPrimary)
                                Text(place.address)
                                    .font(.system(size: 12))
                                    .foregroundStyle(XhsTheme.textSecondary)
                                    .lineLimit(2)
                            }
                            .padding(.vertical, 4)
                        }
                    }

                    if hasMore {
                        HStack {
                            Spacer()
                            if isLoadingMore {
                                SkeletonLoadMoreFooter()
                            } else {
                                Color.clear.frame(height: 1)
                            }
                            Spacer()
                        }
                        .listRowSeparator(.hidden)
                        .onAppear {
                            if !isLoadingMore { performSearch(reset: false) }
                        }
                    }
                }
                .listStyle(.plain)
            }
        }
        .background(Color.white)
        .onChange(of: keyword) { _, value in
            searchTask?.cancel()
            let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !trimmed.isEmpty else {
                items = []
                return
            }
            searchTask = Task {
                try? await Task.sleep(nanoseconds: 350_000_000)
                guard !Task.isCancelled else { return }
                performSearch(reset: true)
            }
        }
    }

    private func performSearch(reset: Bool) {
        let trimmed = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }

        let nextPage = reset ? 1 : page
        if reset {
            isSearching = true
            page = 1
        } else {
            isLoadingMore = true
        }

        Task {
            do {
                let response = try await LFCAPIService.shared.searchPlaces(
                    keyword: trimmed,
                    page: nextPage,
                    limit: 20
                )
                if reset {
                    items = response.items
                } else {
                    let existing = Set(items.map(\.id))
                    items.append(contentsOf: response.items.filter { !existing.contains($0.id) })
                }
                hasMore = response.hasMore
                page = nextPage + 1
            } catch {
                store.toastError = store.parseError(error, fallback: "搜索失败")
            }
            isSearching = false
            isLoadingMore = false
        }
    }

    private func select(_ place: PlaceSuggestionDto) {
        store.pendingLocationPick = LocationPick(
            label: place.name,
            latitude: place.latitude,
            longitude: place.longitude
        )
        onSelect()
    }
}
