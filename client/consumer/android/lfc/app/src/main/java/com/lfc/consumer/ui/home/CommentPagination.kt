package com.lfc.consumer.ui.home

import com.lfc.consumer.data.model.PostCommentDto
import com.lfc.consumer.data.model.PostCommentsUiState

internal const val POST_COMMENTS_PAGE_SIZE = 20
internal const val POST_COMMENT_REPLY_PAGE_SIZE = 20
internal const val POST_COMMENT_REPLY_PREVIEW_SIZE = 2

internal fun PostCommentDto.withoutMeta(): PostCommentDto =
    copy(replyCount = null, previewReplies = null)

internal data class CommentPageMergeResult(
    val comments: List<PostCommentDto>,
    val topLevelIds: List<Int>,
    val replyCounts: Map<Int, Int>,
    val replyPages: Map<Int, Int>,
    val replyHasMore: Map<Int, Boolean>,
)

internal fun mergeCommentThreadPage(
    existing: PostCommentsUiState,
    threads: List<PostCommentDto>,
    replace: Boolean,
): CommentPageMergeResult {
    val byId = LinkedHashMap<Int, PostCommentDto>()
    if (!replace) {
        existing.comments.forEach { byId[it.id] = it.withoutMeta() }
    }

    val newTopLevelIds = mutableListOf<Int>()
    val replyCounts = if (replace) mutableMapOf() else existing.replyCounts.toMutableMap()
    val replyPages = if (replace) mutableMapOf() else existing.replyPages.toMutableMap()
    val replyHasMore = if (replace) mutableMapOf() else existing.replyHasMore.toMutableMap()

    threads.forEach { thread ->
        val root = thread.withoutMeta()
        byId[root.id] = root
        newTopLevelIds += root.id

        val totalReplies = thread.replyCount ?: 0
        replyCounts[root.id] = totalReplies

        val preview = thread.previewReplies.orEmpty().map { it.withoutMeta() }
        preview.forEach { byId[it.id] = it }

        val previewSize = preview.size
        replyPages[root.id] = 0
        replyHasMore[root.id] = previewSize < totalReplies
    }

    val topLevelIds = if (replace) newTopLevelIds else existing.topLevelIds + newTopLevelIds
    return CommentPageMergeResult(
        comments = buildOrderedComments(byId, topLevelIds),
        topLevelIds = topLevelIds,
        replyCounts = replyCounts,
        replyPages = replyPages,
        replyHasMore = replyHasMore,
    )
}

internal fun mergeCommentRepliesPage(
    existing: PostCommentsUiState,
    rootCommentId: Int,
    replies: List<PostCommentDto>,
    page: Int,
    hasMore: Boolean,
): PostCommentsUiState {
    val byId = existing.comments.associateBy { it.id }.toMutableMap()
    replies.forEach { reply ->
        byId[reply.id] = reply.withoutMeta()
    }
    return existing.copy(
        comments = buildOrderedComments(byId, existing.topLevelIds),
        replyPages = existing.replyPages + (rootCommentId to page),
        replyHasMore = existing.replyHasMore + (rootCommentId to hasMore),
    )
}

internal fun appendSubmittedComment(
    existing: PostCommentsUiState,
    comment: PostCommentDto,
): PostCommentsUiState {
    val normalized = comment.withoutMeta()
    val byId = existing.comments.associateBy { it.id }.toMutableMap()
    byId[normalized.id] = normalized

    val topLevelIds = if (normalized.parentId == null) {
        existing.topLevelIds + normalized.id
    } else {
        existing.topLevelIds
    }

    val rootId = normalized.rootId ?: normalized.parentId
    val replyCounts = if (rootId != null) {
        existing.replyCounts.toMutableMap().apply {
            this[rootId] = (this[rootId] ?: 0) + 1
        }
    } else {
        existing.replyCounts
    }

    return existing.copy(
        comments = buildOrderedComments(byId, topLevelIds),
        topLevelIds = topLevelIds,
        replyCounts = replyCounts,
    )
}

internal fun updateCommentLike(
    existing: PostCommentsUiState,
    commentId: Int,
    likeCount: Int,
    isLiked: Boolean,
): PostCommentsUiState {
    return existing.copy(
        comments = existing.comments.map { comment ->
            if (comment.id == commentId) {
                comment.copy(likeCount = likeCount, isLiked = isLiked)
            } else {
                comment
            }
        },
    )
}

internal fun buildCommentThreads(ui: PostCommentsUiState): List<PostCommentThreadUi> {
    val index = ui.comments.associateBy { it.id }
    return ui.topLevelIds.mapNotNull { rootId ->
        val root = index[rootId] ?: return@mapNotNull null
        val replies = repliesForRoot(ui.comments, rootId)
        PostCommentThreadUi(
            root = root,
            replies = replies,
            replyCount = ui.replyCounts[rootId] ?: replies.size,
            hasMoreReplies = ui.replyHasMore[rootId] == true,
            isLoadingReplies = rootId in ui.loadingReplyRoots,
        )
    }
}

internal data class PostCommentThreadUi(
    val root: PostCommentDto,
    val replies: List<PostCommentDto>,
    val replyCount: Int,
    val hasMoreReplies: Boolean,
    val isLoadingReplies: Boolean,
)

private fun buildOrderedComments(
    byId: Map<Int, PostCommentDto>,
    topLevelIds: List<Int>,
): List<PostCommentDto> {
    val result = mutableListOf<PostCommentDto>()
    topLevelIds.forEach { rootId ->
        byId[rootId]?.let(result::add)
        byId.values
            .filter { it.rootId == rootId || (it.rootId == null && it.parentId != null && belongsToRoot(byId, it, rootId)) }
            .sortedBy { it.createdAt }
            .forEach { reply ->
                if (reply.id != rootId) {
                    result.add(reply)
                }
            }
    }
    return result
}

private fun belongsToRoot(
    byId: Map<Int, PostCommentDto>,
    comment: PostCommentDto,
    rootId: Int,
): Boolean {
    var current: PostCommentDto? = comment
    var depth = 0
    while (current != null && depth < 32) {
        if (current.id == rootId) return true
        current = current.parentId?.let(byId::get)
        depth++
    }
    return false
}

private fun repliesForRoot(comments: List<PostCommentDto>, rootId: Int): List<PostCommentDto> {
    val byRootId = comments.filter { it.rootId == rootId }
    if (byRootId.isNotEmpty()) {
        return byRootId.sortedBy { it.createdAt }
    }
    return groupCommentReplies(comments)[rootId].orEmpty()
}

internal fun groupCommentReplies(comments: List<PostCommentDto>): Map<Int, List<PostCommentDto>> {
    val index = comments.associateBy { it.id }
    fun rootId(comment: PostCommentDto): Int {
        var current = comment
        while (current.parentId != null) {
            val parent = index[current.parentId] ?: break
            current = parent
        }
        return current.id
    }
    return comments
        .asSequence()
        .filter { it.parentId != null }
        .groupBy { rootId(it) }
        .mapValues { (_, replies) -> replies.sortedBy { it.createdAt } }
}
