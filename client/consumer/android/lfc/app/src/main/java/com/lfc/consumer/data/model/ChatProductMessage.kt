package com.lfc.consumer.data.model

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

private val chatProductGson = Gson()

fun ChatProductPayload.toJson(): String = chatProductGson.toJson(this)

fun parseChatProductPayload(content: String): ChatProductPayload? {
    return try {
        chatProductGson.fromJson(content, ChatProductPayload::class.java)
    } catch (_: JsonSyntaxException) {
        null
    }
}
