import SwiftUI

struct ProductDetailView: View {
    let postId: Int
    @Bindable var store: HomeStore
    let currentUserId: Int?
    let onBack: () -> Void
    let onViewNote: () -> Void
    let onAuthorTap: (Int) -> Void
    let onOpenChat: (Int) -> Void

    private var post: PostDto? {
        if store.selectedPost?.id == postId { return store.selectedPost }
        return store.cachedPostForDetail(id: postId)
    }

    private var isLoadingForThisPost: Bool {
        store.isPostLoading && store.postDetailTargetId == postId
    }

    private var showSkeleton: Bool {
        isLoadingForThisPost && (post == nil || post?.product == nil)
    }

    private var showError: Bool {
        store.postDetailLoadFailed && store.postDetailTargetId == postId && post?.product == nil
    }

    var body: some View {
        VStack(spacing: 0) {
            if let post, let product = post.product {
                content(post: post, product: product)
            } else if showError {
                errorContent
            } else if showSkeleton {
                ProductDetailSkeleton()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ProductDetailSkeleton()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .background(XhsTheme.background)
        .lfcHideSystemNavigationBar()
        .safeAreaInset(edge: .bottom) {
            if let post, let product = post.product, currentUserId != post.authorId, product.isOnSale {
                purchaseBar(post: post, product: product)
            }
        }
        .task(id: postId) {
            await store.loadProductDetail(postId)
        }
    }

    private var errorContent: some View {
        VStack(spacing: 0) {
            DetailFallbackHeader(onBack: onBack)
            DetailPageErrorView(message: "商品加载失败") {
                Task { await store.loadProductDetail(postId) }
            }
        }
    }

    @ViewBuilder
    private func content(post: PostDto, product: PostProductDto) -> some View {
        let isSelf = currentUserId == post.authorId
        let authorLabel = post.author?.displayName ?? "同学\(post.authorId)"

        VStack(spacing: 0) {
            DetailAuthorHeader(
                authorLabel: authorLabel,
                avatarUrl: post.author?.avatarUrl,
                onBack: onBack,
                onAuthorTap: { onAuthorTap(post.authorId) }
            ) {
                EmptyView()
            }

            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    DetailImageCarousel(images: post.images ?? [], height: 340)

                    VStack(alignment: .leading, spacing: 14) {
                        HStack(alignment: .firstTextBaseline) {
                            Text(formatPriceYuan(product.price))
                                .font(.system(size: 28, weight: .bold))
                                .foregroundStyle(XhsTheme.red)
                            if let original = product.originalPrice, !original.isEmpty, original != product.price {
                                Text(formatPriceYuan(original))
                                    .font(.system(size: 14))
                                    .foregroundStyle(XhsTheme.textSecondary)
                                    .strikethrough()
                            }
                            Spacer()
                            statusBadge(product: product)
                        }

                        Text(post.productDisplayTitle())
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundStyle(XhsTheme.textPrimary)

                        productMeta(product: product)

                        if !post.content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                            Text(post.content)
                                .font(.system(size: 14))
                                .foregroundStyle(XhsTheme.textSecondary)
                                .lineSpacing(3)
                        }

                        if store.productPurchaseOrder != nil {
                            HStack(spacing: 8) {
                                Image(systemName: "checkmark.seal.fill")
                                    .foregroundStyle(.green)
                                Text("你已下单，请面交/收货后确认")
                                    .font(.system(size: 13))
                                    .foregroundStyle(XhsTheme.textPrimary)
                            }
                            .padding(12)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color.green.opacity(0.08))
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                        }

                        Button(action: onViewNote) {
                            HStack {
                                Text("查看原笔记")
                                Spacer()
                                Image(systemName: "chevron.right")
                            }
                            .font(.system(size: 14))
                            .foregroundStyle(XhsTheme.textPrimary)
                            .padding(14)
                            .background(Color.white)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        }
                        .buttonStyle(.plain)

                        if !isSelf {
                            Button {
                                Task {
                                    if let conversationId = await store.startConversationWithProduct(post: post) {
                                        onOpenChat(conversationId)
                                    }
                                }
                            } label: {
                                HStack {
                                    Image(systemName: "message")
                                    Text("联系卖家")
                                }
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(XhsTheme.textPrimary)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(Color.white)
                                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 14)
                    .padding(.bottom, 20)
                }
            }
        }
    }

    private func productMeta(product: PostProductDto) -> some View {
        HStack(spacing: 8) {
            metaChip(productConditionLabel(product.condition))
            metaChip(product.category)
            metaChip(productDeliveryLabel(product.deliveryMethod))
        }
    }

    private func metaChip(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 12))
            .foregroundStyle(XhsTheme.textSecondary)
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(Color(red: 0.95, green: 0.95, blue: 0.95))
            .clipShape(Capsule())
    }

    private func statusBadge(product: PostProductDto) -> some View {
        Group {
            if product.isSold {
                Text("已售出")
                    .foregroundStyle(XhsTheme.textSecondary)
            } else if product.isOnSale {
                Text("在售")
                    .foregroundStyle(XhsTheme.red)
            } else {
                Text("已下架")
                    .foregroundStyle(XhsTheme.textSecondary)
            }
        }
        .font(.system(size: 13, weight: .medium))
    }

    private func purchaseBar(post: PostDto, product: PostProductDto) -> some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text(formatPriceYuan(product.price))
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(XhsTheme.red)
                Text("含平台服务费")
                    .font(.system(size: 11))
                    .foregroundStyle(XhsTheme.textSecondary)
            }
            Button(store.isPurchasingProduct ? "处理中…" : "立即购买") {
                Task { await store.purchasePostProduct(postId: post.id) }
            }
            .buttonStyle(DetailPrimaryButtonStyle(filled: true))
            .disabled(store.isPurchasingProduct || product.isSold)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(Color.white)
        .overlay(alignment: .top) { Divider().overlay(XhsTheme.divider) }
    }
}
