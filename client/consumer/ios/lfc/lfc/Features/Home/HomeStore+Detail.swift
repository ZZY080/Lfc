import Foundation

private let postCommentsPageSize = 20

// MARK: - Model copy helpers

extension PostDto {
    func applying(social state: PostSocialStateDto) -> PostDto {
        PostDto(
            id: id,
            title: title,
            category: category,
            content: content,
            authorId: authorId,
            createdAt: createdAt,
            updatedAt: updatedAt,
            images: images,
            likeCount: state.likeCount,
            favoriteCount: state.favoriteCount,
            commentCount: state.commentCount,
            viewCount: viewCount,
            isLiked: state.isLiked,
            isFavorited: state.isFavorited,
            savedAt: savedAt,
            author: author,
            product: product,
            promotion: promotion,
            isVisible: isVisible,
            latitude: latitude,
            longitude: longitude,
            location: location
        )
    }

    func withCommentCount(_ count: Int) -> PostDto {
        PostDto(
            id: id,
            title: title,
            category: category,
            content: content,
            authorId: authorId,
            createdAt: createdAt,
            updatedAt: updatedAt,
            images: images,
            likeCount: likeCount,
            favoriteCount: favoriteCount,
            commentCount: count,
            viewCount: viewCount,
            isLiked: isLiked,
            isFavorited: isFavorited,
            savedAt: savedAt,
            author: author,
            product: product,
            promotion: promotion,
            isVisible: isVisible,
            latitude: latitude,
            longitude: longitude,
            location: location
        )
    }
}

extension ActivityDto {
    func applying(social state: ActivitySocialStateDto) -> ActivityDto {
        ActivityDto(
            id: id,
            title: title,
            description: description,
            images: images,
            location: location,
            latitude: latitude,
            longitude: longitude,
            startTime: startTime,
            endTime: endTime,
            maxParticipants: maxParticipants,
            fee: fee,
            status: status,
            authorId: authorId,
            createdAt: createdAt,
            updatedAt: updatedAt,
            author: author,
            participants: participants,
            isJoined: isJoined,
            likeCount: state.likeCount,
            favoriteCount: state.favoriteCount,
            isLiked: state.isLiked,
            isFavorited: state.isFavorited,
            savedAt: savedAt,
            promotion: promotion
        )
    }

    func withJoinState(isJoined: Bool, participants: [ActivityParticipantDto]?) -> ActivityDto {
        ActivityDto(
            id: id,
            title: title,
            description: description,
            images: images,
            location: location,
            latitude: latitude,
            longitude: longitude,
            startTime: startTime,
            endTime: endTime,
            maxParticipants: maxParticipants,
            fee: fee,
            status: status,
            authorId: authorId,
            createdAt: createdAt,
            updatedAt: updatedAt,
            author: author,
            participants: participants,
            isJoined: isJoined,
            likeCount: likeCount,
            favoriteCount: favoriteCount,
            isLiked: isLiked,
            isFavorited: isFavorited,
            savedAt: savedAt,
            promotion: promotion
        )
    }
}

extension PostCommentDto {
    func applying(like state: CommentLikeStateDto) -> PostCommentDto {
        PostCommentDto(
            id: id,
            postId: postId,
            userId: userId,
            content: content,
            parentId: parentId,
            rootId: rootId,
            likeCount: state.likeCount,
            isLiked: state.isLiked,
            createdAt: createdAt,
            author: author,
            replyCount: replyCount,
            previewReplies: previewReplies
        )
    }
}

// MARK: - HomeStore detail interactions

extension HomeStore {
    // MARK: Loaders

    func loadPostDetail(_ id: Int) async {
        postDetailRequestID += 1
        let requestID = postDetailRequestID
        postDetailTargetId = id
        postDetailLoadFailed = false

        async let profileTask: Void = ensureMyProfileLoaded()

        if selectedPost?.id != id {
            selectedPost = cachedPostForDetail(id: id)
        }
        isPostLoading = true
        postCommentsUi = PostCommentsUiState(isInitialLoading: true)
        detailAuthorFollowing = nil

        do {
            let post = try await api.getPost(id: id)
            guard requestID == postDetailRequestID else { return }
            selectedPost = post
            detailAuthorFollowing = await loadAuthorFollowState(authorId: post.authorId)
            isPostLoading = false
            AnalyticsTracker.shared.track(
                AnalyticsEvents.postView,
                properties: ["postId": id, "category": post.category]
            )
            await loadPostComments(postId: id, refresh: true)
        } catch is CancellationError {
            guard requestID == postDetailRequestID else { return }
            isPostLoading = false
            return
        } catch {
            guard requestID == postDetailRequestID else { return }
            isPostLoading = false
            postCommentsUi = PostCommentsUiState()
            if selectedPost?.id == id {
                toastError = parseError(error, fallback: "刷新笔记失败")
            } else {
                postDetailLoadFailed = true
                toastError = parseError(error, fallback: "加载笔记失败")
            }
        }

        _ = await profileTask
    }

    /// Call synchronously before navigation so the detail page never flashes empty.
    func preparePostDetail(id: Int) {
        postDetailTargetId = id
        postDetailLoadFailed = false
        if selectedPost?.id != id {
            selectedPost = cachedPostForDetail(id: id)
        }
    }

    func cachedPostForDetail(id: Int) -> PostDto? {
        cachedPost(id: id)
    }

    func clearSelectedPost() {
        postDetailRequestID += 1
        selectedPost = nil
        postDetailTargetId = nil
        postDetailLoadFailed = false
        isPostLoading = false
        postCommentsUi = PostCommentsUiState()
        detailAuthorFollowing = nil
    }

    private func cachedPost(id: Int) -> PostDto? {
        if let post = feedState.posts.first(where: { $0.id == id }) { return post }
        if let post = searchState.posts.first(where: { $0.id == id }) { return post }
        if let post = profileState.profileNotes.first(where: { $0.id == id }) { return post }
        if let post = profileState.visitorProfileNotes.first(where: { $0.id == id }) { return post }
        if let post = profileState.profileFavoritePosts.first(where: { $0.id == id }) { return post }
        if let post = profileState.profileLikedPosts.first(where: { $0.id == id }) { return post }
        if let post = profileState.visitorProfileFavoritePosts.first(where: { $0.id == id }) { return post }
        if let post = profileState.visitorProfileLikedPosts.first(where: { $0.id == id }) { return post }
        if let post = profileState.myProfile?.posts.first(where: { $0.id == id }) { return post }
        if let post = profileState.selectedUserProfile?.posts.first(where: { $0.id == id }) { return post }
        return nil
    }

    func loadActivityDetail(_ id: Int) async {
        isActivityLoading = true
        selectedActivity = nil
        detailAuthorFollowing = nil

        do {
            let activity = try await api.getActivity(id: id)
            selectedActivity = activity
            detailAuthorFollowing = await loadAuthorFollowState(authorId: activity.authorId)
            AnalyticsTracker.shared.track(
                AnalyticsEvents.activityView,
                properties: ["activityId": id]
            )
        } catch {
            toastError = parseError(error, fallback: "加载活动失败")
        }
        isActivityLoading = false
    }

    func clearSelectedActivity() {
        selectedActivity = nil
        detailAuthorFollowing = nil
    }

    func loadProductDetail(_ id: Int) async {
        isPostLoading = true
        selectedPost = nil
        productPurchaseOrder = nil

        do {
            let post = try await api.getPost(id: id)
            selectedPost = post
            if post.product != nil {
                productPurchaseOrder = try await api.getPostProductOrder(postId: id)
            }
            AnalyticsTracker.shared.track(
                AnalyticsEvents.productView,
                properties: ["postId": id]
            )
        } catch {
            toastError = parseError(error, fallback: "加载商品失败")
        }
        isPostLoading = false
    }

    func loadChat(_ conversationId: Int) async {
        isChatLoading = true
        chatMessages = []
        selectedConversation = messagesState.conversations.first(where: { $0.id == conversationId })

        do {
            if selectedConversation == nil {
                let response = try await api.getConversations(page: 1, limit: 50)
                selectedConversation = response.items.first(where: { $0.id == conversationId })
            }
            chatMessages = try await api.getChatMessages(id: conversationId)
            AnalyticsTracker.shared.track(
                AnalyticsEvents.chatOpen,
                properties: ["conversationId": conversationId]
            )
        } catch {
            toastError = parseError(error, fallback: "加载聊天失败")
        }
        isChatLoading = false
    }

    func clearChat() {
        selectedConversation = nil
        chatMessages = []
    }

    // MARK: Post comments

    func loadPostComments(postId: Int, refresh: Bool = false) async {
        let currentUi = postCommentsUi
        if !refresh, currentUi.isLoadingMore || currentUi.isInitialLoading || !currentUi.hasMore {
            return
        }

        let page = refresh ? 1 : currentUi.page + 1
        postCommentsUi = currentUi.copy(
            isInitialLoading: refresh,
            isLoadingMore: !refresh
        )

        do {
            let response = try await api.getPostComments(
                id: postId,
                page: page,
                limit: postCommentsPageSize,
                sort: currentUi.sort
            )
            let merged: [PostCommentDto]
            if refresh {
                merged = response.items
            } else {
                let existingIDs = Set(currentUi.comments.map(\.id))
                merged = currentUi.comments + response.items.filter { !existingIDs.contains($0.id) }
            }

            var replyHasMore = refresh ? [:] : currentUi.replyHasMore
            for comment in response.items {
                let previewCount = comment.previewReplies?.count ?? 0
                let totalReplies = comment.replyCount ?? previewCount
                if totalReplies > previewCount {
                    replyHasMore[comment.id] = true
                }
            }

            postCommentsUi = postCommentsUi.copy(
                comments: merged,
                page: page,
                hasMore: response.hasMore,
                isInitialLoading: false,
                isLoadingMore: false,
                replyHasMore: replyHasMore
            )
        } catch {
            postCommentsUi = currentUi.copy(
                isInitialLoading: false,
                isLoadingMore: false
            )
            toastError = parseError(error, fallback: "加载评论失败")
        }
    }

    func loadMorePostComments() async {
        guard let postId = selectedPost?.id else { return }
        await loadPostComments(postId: postId, refresh: false)
    }

    func setPostCommentSort(postId: Int, sort: String) async {
        guard postCommentsUi.sort != sort else { return }
        postCommentsUi = PostCommentsUiState(sort: sort, isInitialLoading: true)
        await loadPostComments(postId: postId, refresh: true)
    }

    func loadMoreCommentReplies(postId: Int, rootCommentId: Int) async {
        var currentUi = postCommentsUi
        guard !currentUi.loadingReplyRoots.contains(rootCommentId) else { return }
        if currentUi.replyHasMore[rootCommentId] == false { return }

        let nextPage = (currentUi.replyHasMore[rootCommentId] == nil ? 0 : 1) + 1
        currentUi.loadingReplyRoots.insert(rootCommentId)
        postCommentsUi = currentUi

        do {
            let response = try await api.getPostCommentReplies(
                postId: postId,
                commentId: rootCommentId,
                page: nextPage,
                limit: 10
            )
            currentUi = postCommentsUi
            currentUi.loadingReplyRoots.remove(rootCommentId)
            currentUi.replyHasMore[rootCommentId] = response.hasMore

            let updatedComments = currentUi.comments.map { comment -> PostCommentDto in
                guard comment.id == rootCommentId else { return comment }
                var existing = comment.previewReplies ?? []
                let existingIDs = Set(existing.map(\.id))
                existing.append(contentsOf: response.items.filter { !existingIDs.contains($0.id) })
                return PostCommentDto(
                    id: comment.id,
                    postId: comment.postId,
                    userId: comment.userId,
                    content: comment.content,
                    parentId: comment.parentId,
                    rootId: comment.rootId,
                    likeCount: comment.likeCount,
                    isLiked: comment.isLiked,
                    createdAt: comment.createdAt,
                    author: comment.author,
                    replyCount: comment.replyCount,
                    previewReplies: existing
                )
            }
            currentUi.comments = updatedComments
            postCommentsUi = currentUi
        } catch {
            currentUi = postCommentsUi
            currentUi.loadingReplyRoots.remove(rootCommentId)
            postCommentsUi = currentUi
            toastError = parseError(error, fallback: "加载回复失败")
        }
    }

    // MARK: Post social

    func togglePostLike(postId: Int) async {
        isPostSocialSubmitting = true
        defer { isPostSocialSubmitting = false }

        do {
            let state = try await api.togglePostLike(id: postId)
            updatePostSocialState(postId: postId, state: state)
            AnalyticsTracker.shared.track(
                AnalyticsEvents.postLike,
                properties: ["postId": postId, "liked": state.isLiked]
            )
        } catch {
            toastError = parseError(error, fallback: "操作失败")
        }
    }

    func togglePostFavorite(postId: Int) async {
        isPostSocialSubmitting = true
        defer { isPostSocialSubmitting = false }

        do {
            let state = try await api.togglePostFavorite(id: postId)
            updatePostSocialState(postId: postId, state: state)
            AnalyticsTracker.shared.track(
                AnalyticsEvents.postFavorite,
                properties: ["postId": postId, "favorited": state.isFavorited]
            )
        } catch {
            toastError = parseError(error, fallback: "操作失败")
        }
    }

    func toggleCommentLike(postId: Int, commentId: Int) async {
        do {
            let state = try await api.toggleCommentLike(commentId: commentId)
            postCommentsUi.comments = postCommentsUi.comments.map { comment in
                if comment.id == commentId {
                    return comment.applying(like: state)
                }
                if let replies = comment.previewReplies {
                    let updatedReplies = replies.map { reply in
                        reply.id == commentId ? reply.applying(like: state) : reply
                    }
                    return PostCommentDto(
                        id: comment.id,
                        postId: comment.postId,
                        userId: comment.userId,
                        content: comment.content,
                        parentId: comment.parentId,
                        rootId: comment.rootId,
                        likeCount: comment.likeCount,
                        isLiked: comment.isLiked,
                        createdAt: comment.createdAt,
                        author: comment.author,
                        replyCount: comment.replyCount,
                        previewReplies: updatedReplies
                    )
                }
                return comment
            }
            AnalyticsTracker.shared.track(
                AnalyticsEvents.commentLike,
                properties: ["postId": postId, "commentId": commentId, "liked": state.isLiked]
            )
        } catch {
            toastError = parseError(error, fallback: "操作失败")
        }
    }

    func createComment(
        postId: Int,
        content: String,
        parentId: Int? = nil,
        image: PendingCommentImage? = nil
    ) async {
        let trimmed = content.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty || image != nil else { return }

        isPostSocialSubmitting = true
        defer { isPostSocialSubmitting = false }

        do {
            var imageUrl: String?
            if let image {
                let upload = try await api.uploadImage(
                    data: image.data,
                    filename: image.filename,
                    mimeType: "image/jpeg",
                    scope: "post"
                )
                imageUrl = upload.url
            }
            let finalContent = CommentContentHelper.build(text: trimmed, imageUrl: imageUrl)
            guard !finalContent.isEmpty else { return }

            let comment = try await api.createPostComment(
                id: postId,
                request: CreatePostCommentRequest(content: finalContent, parentId: parentId)
            )
            if parentId == nil {
                postCommentsUi.comments.insert(comment, at: 0)
            } else if let rootId = comment.rootId ?? parentId {
                postCommentsUi.comments = postCommentsUi.comments.map { root in
                    guard root.id == rootId else { return root }
                    var replies = root.previewReplies ?? []
                    replies.append(comment)
                    return PostCommentDto(
                        id: root.id,
                        postId: root.postId,
                        userId: root.userId,
                        content: root.content,
                        parentId: root.parentId,
                        rootId: root.rootId,
                        likeCount: root.likeCount,
                        isLiked: root.isLiked,
                        createdAt: root.createdAt,
                        author: root.author,
                        replyCount: (root.replyCount ?? 0) + 1,
                        previewReplies: replies
                    )
                }
            }
            if selectedPost?.id == postId, let post = selectedPost {
                selectedPost = post.withCommentCount(post.commentCount + 1)
            }
            AnalyticsTracker.shared.track(
                AnalyticsEvents.postComment,
                properties: [
                    "postId": postId,
                    "hasReply": parentId != nil,
                    "hasImage": imageUrl != nil,
                ]
            )
        } catch {
            toastError = parseError(error, fallback: "评论失败")
        }
    }

    func deletePost(id: Int) async -> Bool {
        do {
            try await api.deletePost(id: id)
            toastMessage = "笔记已删除"
            await loadFeed(refresh: true)
            await loadProfile()
            return true
        } catch {
            toastError = parseError(error, fallback: "删除失败")
            return false
        }
    }

    func offShelfPost(id: Int) async {
        do {
            let updated = try await api.offShelfPost(id: id)
            if selectedPost?.id == id { selectedPost = updated }
            toastMessage = "笔记已下架"
            await loadFeed(refresh: true)
        } catch {
            toastError = parseError(error, fallback: "下架失败")
        }
    }

    func onShelfPost(id: Int) async {
        do {
            let updated = try await api.onShelfPost(id: id)
            if selectedPost?.id == id { selectedPost = updated }
            toastMessage = "笔记已重新上架"
            await loadFeed(refresh: true)
        } catch {
            toastError = parseError(error, fallback: "上架失败")
        }
    }

    // MARK: Activity

    func toggleActivityLike(activityId: Int) async {
        isActivitySocialSubmitting = true
        defer { isActivitySocialSubmitting = false }

        do {
            let state = try await api.toggleActivityLike(id: activityId)
            updateActivitySocialState(activityId: activityId, state: state)
            AnalyticsTracker.shared.track(
                AnalyticsEvents.activityLike,
                properties: ["activityId": activityId, "liked": state.isLiked]
            )
        } catch {
            toastError = parseError(error, fallback: "操作失败")
        }
    }

    func toggleActivityFavorite(activityId: Int) async {
        isActivitySocialSubmitting = true
        defer { isActivitySocialSubmitting = false }

        do {
            let state = try await api.toggleActivityFavorite(id: activityId)
            updateActivitySocialState(activityId: activityId, state: state)
            AnalyticsTracker.shared.track(
                AnalyticsEvents.activityFavorite,
                properties: ["activityId": activityId, "favorited": state.isFavorited]
            )
        } catch {
            toastError = parseError(error, fallback: "操作失败")
        }
    }

    func joinActivity(id: Int) async {
        isJoiningActivity = true
        defer { isJoiningActivity = false }

        do {
            let activity = selectedActivity?.id == id ? selectedActivity! : try await api.getActivity(id: id)
            if activity.isPaidActivity {
                let order = try await api.createActivityPaymentOrder(activityId: id)
                toastMessage = "订单已创建（\(order.subject) ¥\(order.amount)），请在支付宝完成支付"
                AnalyticsTracker.shared.track(
                    AnalyticsEvents.paymentStart,
                    properties: ["activityId": id, "outTradeNo": order.outTradeNo]
                )
            } else {
                _ = try await api.joinActivity(id: id)
                toastMessage = "报名成功"
                AnalyticsTracker.shared.track(
                    AnalyticsEvents.activityJoin,
                    properties: ["activityId": id, "paid": false]
                )
            }
            let refreshed = try await api.getActivity(id: id)
            if selectedActivity?.id == id { selectedActivity = refreshed }
            await loadActivityFeed(refresh: true)
        } catch {
            toastError = parseError(error, fallback: "报名失败")
        }
    }

    func leaveActivity(id: Int) async {
        do {
            try await api.leaveActivity(id: id)
            toastMessage = "已取消报名"
            let refreshed = try await api.getActivity(id: id)
            if selectedActivity?.id == id { selectedActivity = refreshed }
        } catch {
            toastError = parseError(error, fallback: "取消报名失败")
        }
    }

    func deleteActivity(id: Int) async -> Bool {
        do {
            try await api.deleteActivity(id: id)
            toastMessage = "活动已删除"
            await loadActivityFeed(refresh: true)
            return true
        } catch {
            toastError = parseError(error, fallback: "删除失败")
            return false
        }
    }

    func offShelfActivity(id: Int) async {
        do {
            let updated = try await api.offShelfActivity(id: id)
            if selectedActivity?.id == id { selectedActivity = updated }
            toastMessage = "活动已下架"
            await loadActivityFeed(refresh: true)
        } catch {
            toastError = parseError(error, fallback: "下架失败")
        }
    }

    func onShelfActivity(id: Int) async {
        do {
            let updated = try await api.onShelfActivity(id: id)
            if selectedActivity?.id == id { selectedActivity = updated }
            toastMessage = "活动已重新上架"
            await loadActivityFeed(refresh: true)
        } catch {
            toastError = parseError(error, fallback: "上架失败")
        }
    }

    // MARK: Product

    func purchasePostProduct(postId: Int) async {
        isPurchasingProduct = true
        defer { isPurchasingProduct = false }

        do {
            AnalyticsTracker.shared.track(
                AnalyticsEvents.productPurchaseStart,
                properties: ["postId": postId]
            )
            let order = try await api.createPostProductOrder(postId: postId)
            toastMessage = "订单已创建（\(order.subject) ¥\(order.amount)），请在支付宝完成支付"
            productPurchaseOrder = try await api.getPostProductOrder(postId: postId)
            AnalyticsTracker.shared.track(
                AnalyticsEvents.paymentStart,
                properties: ["postId": postId, "outTradeNo": order.outTradeNo]
            )
        } catch {
            toastError = parseError(error, fallback: "购买失败")
        }
    }

    // MARK: Chat

    func sendChatMessage(conversationId: Int, content: String) async {
        let trimmed = content.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }

        isChatSending = true
        defer { isChatSending = false }

        do {
            let message = try await api.sendChatMessage(
                id: conversationId,
                request: SendChatMessageRequest(content: trimmed, messageType: "TEXT")
            )
            chatMessages.append(message)
            if let conversation = selectedConversation {
                selectedConversation = ConversationDto(
                    id: conversation.id,
                    peerUserId: conversation.peerUserId,
                    peerStudentId: conversation.peerStudentId,
                    peerNickname: conversation.peerNickname,
                    peerAvatarUrl: conversation.peerAvatarUrl,
                    lastMessageContent: message.content,
                    lastMessageAt: message.createdAt,
                    unreadCount: conversation.unreadCount,
                    createdAt: conversation.createdAt,
                    updatedAt: message.createdAt
                )
            }
            AnalyticsTracker.shared.track(
                AnalyticsEvents.chatSend,
                properties: ["conversationId": conversationId, "messageType": "TEXT"]
            )
        } catch {
            toastError = parseError(error, fallback: "发送失败")
        }
    }

    func startConversation(peerUserId: Int) async -> Int? {
        do {
            let conversation = try await api.createConversation(CreateConversationRequest(peerUserId: peerUserId))
            await loadMessages(refresh: true)
            return conversation.id
        } catch {
            toastError = parseError(error, fallback: "发起私信失败")
            return nil
        }
    }

    func startConversationWithProduct(post: PostDto) async -> Int? {
        guard let payload = post.toChatProductPayload() else { return nil }
        do {
            let conversation = try await api.createConversation(
                CreateConversationRequest(peerUserId: post.authorId)
            )
            let encoder = JSONEncoder()
            let data = try encoder.encode(payload)
            let json = String(data: data, encoding: .utf8) ?? "{}"
            _ = try await api.sendChatMessage(
                id: conversation.id,
                request: SendChatMessageRequest(content: json, messageType: "PRODUCT")
            )
            await loadMessages(refresh: true)
            return conversation.id
        } catch {
            toastError = parseError(error, fallback: "联系卖家失败")
            return nil
        }
    }

    // MARK: Profile / follow

    func toggleFollow(userId: Int) async {
        do {
            let result = try await api.toggleFollow(id: userId)
            if profileState.selectedUserProfile?.id == userId {
                let profile = try await api.getUserProfile(id: userId)
                profileState.selectedUserProfile = profile
            }
            if isDetailAuthor(userId) {
                detailAuthorFollowing = result.isFollowing
            }
            AnalyticsTracker.shared.track(
                AnalyticsEvents.userFollow,
                properties: ["userId": userId, "following": result.isFollowing]
            )
        } catch {
            toastError = parseError(error, fallback: "操作失败")
        }
    }

    func toggleDetailAuthorFollow(authorId: Int) async {
        await toggleFollow(userId: authorId)
    }

    // MARK: Private helpers

    private func loadAuthorFollowState(authorId: Int) async -> Bool? {
        guard let myId = profileState.myProfile?.id, authorId != myId else { return nil }
        do {
            let profile = try await api.getUserProfile(id: authorId)
            return profile.isFollowing
        } catch {
            return nil
        }
    }

    private func isDetailAuthor(_ userId: Int) -> Bool {
        selectedPost?.authorId == userId || selectedActivity?.authorId == userId
    }

    private func updatePostSocialState(postId: Int, state: PostSocialStateDto) {
        if selectedPost?.id == postId, let post = selectedPost {
            selectedPost = post.applying(social: state)
        }
        feedState.posts = feedState.posts.map { $0.id == postId ? $0.applying(social: state) : $0 }
        searchState.posts = searchState.posts.map { $0.id == postId ? $0.applying(social: state) : $0 }
        profileState.profileNotes = profileState.profileNotes.map { $0.id == postId ? $0.applying(social: state) : $0 }
        profileState.visitorProfileNotes = profileState.visitorProfileNotes.map { $0.id == postId ? $0.applying(social: state) : $0 }
        profileState.visitorProfileFavoritePosts = profileState.visitorProfileFavoritePosts.map { $0.id == postId ? $0.applying(social: state) : $0 }
        profileState.visitorProfileLikedPosts = profileState.visitorProfileLikedPosts.map { $0.id == postId ? $0.applying(social: state) : $0 }
    }

    private func updateActivitySocialState(activityId: Int, state: ActivitySocialStateDto) {
        if selectedActivity?.id == activityId, let activity = selectedActivity {
            selectedActivity = activity.applying(social: state)
        }
        activityFeedState.activities = activityFeedState.activities.map {
            $0.id == activityId ? $0.applying(social: state) : $0
        }
        searchState.activities = searchState.activities.map {
            $0.id == activityId ? $0.applying(social: state) : $0
        }
        profileState.profileActivities = profileState.profileActivities.map {
            $0.id == activityId ? $0.applying(social: state) : $0
        }
        profileState.visitorProfileActivities = profileState.visitorProfileActivities.map {
            $0.id == activityId ? $0.applying(social: state) : $0
        }
        profileState.visitorProfileFavoriteActivities = profileState.visitorProfileFavoriteActivities.map {
            $0.id == activityId ? $0.applying(social: state) : $0
        }
        profileState.visitorProfileLikedActivities = profileState.visitorProfileLikedActivities.map {
            $0.id == activityId ? $0.applying(social: state) : $0
        }
    }
}

// MARK: - PostCommentsUiState copy helper

private extension PostCommentsUiState {
    func copy(
        comments: [PostCommentDto]? = nil,
        page: Int? = nil,
        hasMore: Bool? = nil,
        sort: String? = nil,
        isInitialLoading: Bool? = nil,
        isLoadingMore: Bool? = nil,
        loadingReplyRoots: Set<Int>? = nil,
        replyHasMore: [Int: Bool]? = nil
    ) -> PostCommentsUiState {
        PostCommentsUiState(
            comments: comments ?? self.comments,
            page: page ?? self.page,
            hasMore: hasMore ?? self.hasMore,
            sort: sort ?? self.sort,
            isInitialLoading: isInitialLoading ?? self.isInitialLoading,
            isLoadingMore: isLoadingMore ?? self.isLoadingMore,
            loadingReplyRoots: loadingReplyRoots ?? self.loadingReplyRoots,
            replyHasMore: replyHasMore ?? self.replyHasMore
        )
    }
}
