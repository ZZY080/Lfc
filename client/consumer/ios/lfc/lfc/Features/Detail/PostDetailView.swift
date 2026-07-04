import SwiftUI

struct PostDetailView: View {
    let postId: Int
    @Bindable var store: HomeStore
    let currentUserId: Int?
    let onBack: () -> Void
    let onAuthorTap: (Int) -> Void
    let onProductTap: (Int) -> Void
    let onEdit: (() -> Void)?
    let onDeleted: () -> Void

    @State private var commentInput = ""
    @State private var replyParentId: Int?
    @State private var replyLabel: String?
    @State private var showComposer = false

    private var post: PostDto? {
        if store.selectedPost?.id == postId { return store.selectedPost }
        return store.cachedPostForDetail(id: postId)
    }

    private var commentsUi: PostCommentsUiState { store.postCommentsUi }

    private var isLoadingForThisPost: Bool {
        store.isPostLoading && store.postDetailTargetId == postId
    }

    private var showSkeleton: Bool {
        isLoadingForThisPost && post == nil
    }

    private var showError: Bool {
        store.postDetailLoadFailed && store.postDetailTargetId == postId && post == nil
    }

    private var currentUserLabel: String {
        if let profile = store.profileState.myProfile {
            return profile.displayName
        }
        if let post, currentUserId == post.authorId {
            return post.author?.displayName ?? "我"
        }
        return "我"
    }

    private var currentUserAvatarUrl: String? {
        if let avatarUrl = store.profileState.myProfile?.avatarUrl, !avatarUrl.isEmpty {
            return avatarUrl
        }
        if let post, currentUserId == post.authorId {
            return post.author?.avatarUrl
        }
        return nil
    }

    var body: some View {
        VStack(spacing: 0) {
            if showSkeleton {
                PostDetailSkeleton()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let post {
                detailContent(post: post)
            } else if showError {
                errorContent
            } else {
                PostDetailSkeleton()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .background(Color.white)
        .lfcHideSystemNavigationBar()
        .task(id: postId) {
            await store.loadPostDetail(postId)
        }
    }

    private var errorContent: some View {
        VStack(spacing: 0) {
            PostDetailFallbackHeader(onBack: onBack)
            VStack(spacing: 16) {
                Image(systemName: "wifi.exclamationmark")
                    .font(.system(size: 40))
                    .foregroundStyle(XhsTheme.textSecondary)
                Text("笔记加载失败")
                    .font(.system(size: 16))
                    .foregroundStyle(XhsTheme.textSecondary)
                Button("重试") {
                    Task { await store.loadPostDetail(postId) }
                }
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(XhsTheme.red)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    @ViewBuilder
    private func detailContent(post: PostDto) -> some View {
        let isSelf = currentUserId == post.authorId
        let authorLabel = post.author?.displayName ?? "同学\(post.authorId)"

        VStack(spacing: 0) {
            XhsDetailAuthorHeader(
                authorLabel: authorLabel,
                avatarUrl: post.author?.avatarUrl,
                onBack: onBack,
                onAuthorTap: { onAuthorTap(post.authorId) }
            ) {
                if isSelf, let onEdit {
                    DetailOwnerMenu(
                        contentLabel: "笔记",
                        isOffShelf: !post.isVisible,
                        onEdit: onEdit,
                        onDelete: {
                            Task {
                                if await store.deletePost(id: post.id) {
                                    onDeleted()
                                }
                            }
                        },
                        onOffShelf: { Task { await store.offShelfPost(id: post.id) } },
                        onOnShelf: { Task { await store.onShelfPost(id: post.id) } }
                    )
                } else if !isSelf, let following = store.detailAuthorFollowing {
                    XhsDetailFollowButton(isFollowing: following) {
                        Task { await store.toggleDetailAuthorFollow(authorId: post.authorId) }
                    }
                }
            }

            ZStack {
                ScrollView {
                    VStack(spacing: 0) {
                        if let images = post.images, !images.isEmpty {
                            XhsDetailImageCarousel(images: images)
                        }

                        XhsPostDetailContentSection(post: post)

                        if let product = post.product, product.isOnSale {
                            XhsPostProductLinkCard(post: post, product: product) {
                                onProductTap(post.id)
                            }
                        }

                        commentsBlock(post: post)
                    }
                    .padding(.bottom, 8)
                }
                .scrollDismissesKeyboard(.interactively)

                if showComposer {
                    XhsPostCommentScrim {
                        dismissComposer()
                    }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if showComposer {
                XhsPostCommentExpandedPanel(
                    replyLabel: replyLabel,
                    text: $commentInput,
                    isSubmitting: store.isPostSocialSubmitting,
                    onSubmit: submitComment
                )
            } else {
                XhsPostDetailInteractionBar(
                    likeCount: post.likeCount,
                    favoriteCount: post.favoriteCount,
                    commentCount: post.commentCount,
                    isLiked: post.isLiked,
                    isFavorited: post.isFavorited,
                    isSubmitting: store.isPostSocialSubmitting,
                    onCommentTap: { openComposer() },
                    onLike: { Task { await store.togglePostLike(postId: post.id) } },
                    onFavorite: { Task { await store.togglePostFavorite(postId: post.id) } }
                )
            }
        }
    }

    @ViewBuilder
    private func commentsBlock(post: PostDto) -> some View {
        XhsPostCommentsHeader(
            commentCount: post.commentCount,
            isNewestSort: commentsUi.sort == "newest",
            onSortToggle: {
                Task {
                    await store.setPostCommentSort(
                        postId: post.id,
                        sort: nextPostCommentSort(commentsUi.sort)
                    )
                }
            }
        )

        XhsPostQuickCommentTrigger(
            userLabel: currentUserLabel,
            userAvatarUrl: currentUserAvatarUrl,
            onTap: { openComposer() }
        )

        if commentsUi.isInitialLoading {
            CommentListSkeleton()
        } else if commentsUi.comments.isEmpty {
            XhsPostCommentsEmptyState()
        } else {
            VStack(alignment: .leading, spacing: 0) {
                ForEach(Array(commentsUi.comments.enumerated()), id: \.element.id) { index, comment in
                    let previewCount = comment.previewReplies?.count ?? 0
                    let totalReplies = comment.replyCount ?? previewCount
                    let hiddenCount = max(totalReplies - previewCount, 0)
                    let hasMoreReplies = commentsUi.replyHasMore[comment.id] ?? (hiddenCount > 0)

                    XhsPostCommentThreadView(
                        root: comment,
                        postAuthorId: post.authorId,
                        isFirstComment: index == 0 && commentsUi.sort == "default",
                        isLoadingReplies: commentsUi.loadingReplyRoots.contains(comment.id),
                        hasMoreReplies: hasMoreReplies,
                        hiddenReplyCount: hiddenCount,
                        onReply: { target in
                            openComposer(
                                replyId: target.id,
                                replyLabel: target.author?.displayName ?? "同学\(target.userId)"
                            )
                        },
                        onLike: { target in
                            Task { await store.toggleCommentLike(postId: post.id, commentId: target.id) }
                        },
                        onLoadMoreReplies: {
                            Task { await store.loadMoreCommentReplies(postId: post.id, rootCommentId: comment.id) }
                        }
                    )
                    .onAppear {
                        if comment.id == commentsUi.comments.last?.id {
                            Task { await store.loadMorePostComments() }
                        }
                    }
                }
            }

            if commentsUi.isLoadingMore {
                SkeletonLoadMoreFooter()
            }
        }
    }

    private func openComposer(replyId: Int? = nil, replyLabel: String? = nil) {
        replyParentId = replyId
        self.replyLabel = replyLabel
        showComposer = true
    }

    private func dismissComposer() {
        showComposer = false
        replyParentId = nil
        replyLabel = nil
    }

    private func submitComment() {
        let content = commentInput
        let parentId = replyParentId
        commentInput = ""
        dismissComposer()
        Task { await store.createComment(postId: postId, content: content, parentId: parentId) }
    }
}

private struct PostDetailFallbackHeader: View {
    let onBack: () -> Void

    var body: some View {
        HStack {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(XhsTheme.textPrimary)
                    .frame(width: 40, height: 40)
            }
            .buttonStyle(.plain)
            Spacer()
        }
        .padding(.horizontal, 4)
        .padding(.vertical, 6)
        .background(Color.white)
    }
}
