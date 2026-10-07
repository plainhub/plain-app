package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.*

internal object SystemChatsHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "systemChatSend" -> {
            val item = com.ismartcoding.plain.chat.ChatManager.sendContent(
                com.ismartcoding.plain.chat.data.ChatTarget.parseId(
                    params.getValue("target").jsonPrimitive.content),
                com.ismartcoding.plain.db.DChat.parseContent(
                    params.getValue("content").jsonPrimitive.content))
            com.ismartcoding.plain.chat.ChatViewModel.onMessagesCreated(
                com.ismartcoding.plain.chat.data.ChatTarget.parseId(
                    params.getValue("target").jsonPrimitive.content), listOf(item))
            JsonHelper.jsonEncodeToElement(listOf(item))
        }
        "systemChatDeleteOne" -> {
            val id = params.getValue("id").jsonPrimitive.content
            com.ismartcoding.plain.chat.ChatManager.getChatItem(id)?.let {
                com.ismartcoding.plain.chat.ChatManager.deleteOne(it.id)
                com.ismartcoding.plain.chat.ChatViewModel.onMessagesDeleted(setOf(it.id))
            }
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemChatDeleteQuery" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.chat.ChatManager.deleteQuery(
                params.getValue("query").jsonPrimitive.content))
        "systemChatRetry" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.chat.ChatManager.retry(
                params.getValue("id").jsonPrimitive.content))
        else -> error("Unsupported provider operation")
    }
}
