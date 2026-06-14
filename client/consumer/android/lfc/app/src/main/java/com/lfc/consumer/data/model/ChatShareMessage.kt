package com.lfc.consumer.data.model

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

enum class ChatShareKind {
    POST,
    ACTIVITY,
}

data class ChatShareAttachment(
    val peerUserId: Int,
    val kind: ChatShareKind,
    val postId: Int? = null,
    val activityId: Int? = null,
    val title: String,
    val coverUrl: String? = null,
    val subtitle: String? = null,
)

data class ChatPostSharePayload(
    val postId: Int,
    val title: String,
    val coverUrl: String? = null,
)

data class ChatActivitySharePayload(
    val activityId: Int,
    val title: String,
    val coverUrl: String? = null,
    val startTime: String? = null,
    val location: String? = null,
)

private val chatShareGson = Gson()

fun ChatPostSharePayload.toJson(): String = chatShareGson.toJson(this)

fun ChatActivitySharePayload.toJson(): String = chatShareGson.toJson(this)

fun parseChatPostSharePayload(content: String): ChatPostSharePayload? {
    return try {
        chatShareGson.fromJson(content, ChatPostSharePayload::class.java)
    } catch (_: JsonSyntaxException) {
        null
    }
}

fun parseChatActivitySharePayload(content: String): ChatActivitySharePayload? {
    return try {
        chatShareGson.fromJson(content, ChatActivitySharePayload::class.java)
    } catch (_: JsonSyntaxException) {
        null
    }
}

fun PostDto.shareDisplayTitle(): String {
    val trimmedTitle = title.trim()
    val trimmedContent = content.trim()
    return when {
        trimmedTitle.isNotBlank() &&
            trimmedTitle !in setOf("图片笔记", "校园笔记") &&
            trimmedTitle != trimmedContent.take(30) ->
            trimmedTitle.take(36)
        trimmedContent.isNotBlank() -> trimmedContent.lineSequence().first().take(36)
        else -> "笔记"
    }
}

fun PostDto.toChatShareAttachment(): ChatShareAttachment? {
    if (authorId <= 0) return null
    return ChatShareAttachment(
        peerUserId = authorId,
        kind = ChatShareKind.POST,
        postId = id,
        title = shareDisplayTitle(),
        coverUrl = images?.firstOrNull(),
    )
}

fun ActivityDto.toChatShareAttachment(): ChatShareAttachment? {
    if (authorId <= 0) return null
    val timeLabel = startTime.replace("T", " ").take(16)
    val locationLabel = location.trim().takeIf { it.isNotBlank() }
    val subtitle = listOfNotNull(timeLabel.takeIf { it.isNotBlank() }, locationLabel)
        .joinToString(" · ")
        .takeIf { it.isNotBlank() }
    return ChatShareAttachment(
        peerUserId = authorId,
        kind = ChatShareKind.ACTIVITY,
        activityId = id,
        title = title.trim().ifBlank { "活动" },
        coverUrl = images?.firstOrNull(),
        subtitle = subtitle,
    )
}

fun ChatShareAttachment.toSendPayloadJson(): Pair<String, String> {
    return when (kind) {
        ChatShareKind.POST -> {
            val payload = ChatPostSharePayload(
                postId = postId ?: error("missing postId"),
                title = title,
                coverUrl = coverUrl,
            )
            payload.toJson() to "POST"
        }
        ChatShareKind.ACTIVITY -> {
            val parts = subtitle?.split(" · ", limit = 2).orEmpty()
            val payload = ChatActivitySharePayload(
                activityId = activityId ?: error("missing activityId"),
                title = title,
                coverUrl = coverUrl,
                startTime = parts.getOrNull(0),
                location = parts.getOrNull(1),
            )
            payload.toJson() to "ACTIVITY"
        }
    }
}
