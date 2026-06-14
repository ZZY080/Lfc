package com.lfc.consumer.ui.home

import com.lfc.consumer.data.model.PostCommentDto
import com.lfc.consumer.data.model.displayName

data class ParsedCommentContent(
    val text: String,
    val imageUrl: String? = null,
)

object CommentContentHelper {
    private const val IMAGE_MARKER = "\n__LFC_IMG__"

    fun build(text: String, imageUrl: String?): String {
        val trimmed = text.trim()
        if (imageUrl.isNullOrBlank()) return trimmed
        return when {
            trimmed.isBlank() -> "[图片]$IMAGE_MARKER$imageUrl"
            else -> "$trimmed$IMAGE_MARKER$imageUrl"
        }
    }

    fun parse(content: String): ParsedCommentContent {
        val markerIndex = content.indexOf(IMAGE_MARKER)
        if (markerIndex < 0) return ParsedCommentContent(text = content)
        val text = content.substring(0, markerIndex).trim()
        val imageUrl = content.substring(markerIndex + IMAGE_MARKER.length).trim()
        return ParsedCommentContent(
            text = text,
            imageUrl = imageUrl.takeIf { it.isNotBlank() },
        )
    }
}

data class CommentMentionCandidate(
    val userId: Int,
    val label: String,
)

fun buildCommentMentionCandidates(
    postAuthorId: Int,
    postAuthorLabel: String,
    comments: List<PostCommentDto>,
): List<CommentMentionCandidate> {
    val seen = linkedSetOf<Int>()
    val result = mutableListOf<CommentMentionCandidate>()
    fun add(userId: Int, label: String) {
        if (userId !in seen) {
            seen += userId
            result += CommentMentionCandidate(userId, label)
        }
    }
    add(postAuthorId, postAuthorLabel)
    comments.forEach { comment ->
        val label = comment.author?.displayName() ?: "同学${comment.userId}"
        add(comment.userId, label)
    }
    return result
}
