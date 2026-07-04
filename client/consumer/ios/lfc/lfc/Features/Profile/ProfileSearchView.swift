import SwiftUI

struct ProfileSearchView: View {
    @Bindable var store: HomeStore
    let onBack: () -> Void
    let onPostTap: (Int) -> Void

    @State private var keyword = ""

    private var state: ProfileSearchUiState { store.profileSearchState }

    var body: some View {
        VStack(spacing: 0) {
            HomeStubNavigationBar(title: "搜索我的笔记", onBack: onBack)

            HStack(spacing: 8) {
                TextField("搜索标题或内容", text: $keyword)
                    .textFieldStyle(.roundedBorder)
                    .submitLabel(.search)
                    .onSubmit { search() }
                Button("搜索", action: search)
                    .foregroundStyle(XhsTheme.red)
            }
            .padding()

            if state.isLoading {
                FeedGridSkeletonStatic(itemCount: 4)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            } else if state.hasSearched && state.results.isEmpty {
                ContentUnavailableView("未找到相关笔记", systemImage: "doc.text.magnifyingglass")
                Spacer()
            } else if !state.results.isEmpty {
                List(state.results) { post in
                    Button {
                        onPostTap(post.id)
                    } label: {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(post.title)
                                .font(.system(size: 15, weight: .medium))
                                .foregroundStyle(XhsTheme.textPrimary)
                            Text(post.content)
                                .font(.system(size: 12))
                                .foregroundStyle(XhsTheme.textSecondary)
                                .lineLimit(2)
                        }
                        .padding(.vertical, 4)
                    }
                }
                .listStyle(.plain)
            } else {
                Text("输入关键词搜索你发布的笔记")
                    .font(.system(size: 14))
                    .foregroundStyle(XhsTheme.textSecondary)
                    .padding(.top, 40)
                Spacer()
            }
        }
        .background(Color.white)
    }

    private func search() {
        Task { await store.searchMyPosts(keyword) }
    }
}
