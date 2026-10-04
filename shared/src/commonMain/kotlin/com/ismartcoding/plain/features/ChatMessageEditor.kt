package com.ismartcoding.plain.features

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.chat.RustChatStore
import com.ismartcoding.plain.chat.ChatCacher
import com.ismartcoding.plain.db.DChat
import kotlinx.serialization.json.*

object ChatMessageEditor {
    suspend fun updateTextAsync(item: DChat, newText: String): Boolean {
        val result = RustContentApi.postJson("chat/link-preview", buildJsonObject {
            put("action", "edit"); put("id", item.id); put("text", newText)
        }).getValue("result").jsonObject
        val current = RustChatStore.decode(result.getValue("chat"))
        item.fromId = current.fromId
        item.toId = current.toId
        item.channelId = current.channelId
        item.createdAt = current.createdAt
        item.status = current.status
        item.statusData = current.statusData
        item.content = current.content
        item.updatedAt = current.updatedAt
        ChatCacher.load()
        return result.getValue("changed").jsonPrimitive.boolean
    }
}
