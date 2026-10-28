package com.ismartcoding.plain.features.chat

import com.ismartcoding.plain.features.settings.SavedValuesClient
import com.ismartcoding.plain.features.chat.data.ChatTarget
import com.ismartcoding.plain.features.chat.data.ChatTargetType
import com.ismartcoding.plain.features.chat.models.MessageContent
import com.ismartcoding.plain.features.chat.models.MessageText
import com.ismartcoding.plain.features.chat.models.MessageType
import com.ismartcoding.plain.features.chat.models.toJSONString
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.models.ChatItem
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.selectionQuery

object ChatClient {
    suspend fun readDraft(id: String): String = SavedValuesClient.get(draftKey(id), "")

    suspend fun writeDraft(id: String, value: String) = SavedValuesClient.set(draftKey(id), value)

    private fun draftKey(id: String) = "chat_input_text:${ChatTarget.parseId(id).encodedToId}"

    suspend fun getChatItem(id: String): ChatItem? = ChatQueries.getChatItem(id)

    suspend fun sendContent(target: ChatTarget, content: MessageContent): ChatItem =
        GraphqlApi.execute(Mutation.sendChatItem(target.encodedToId, content.toJSONString())).single()

    suspend fun sendText(target: ChatTarget, text: String): ChatItem =
        sendContent(target, MessageContent(MessageType.TEXT, MessageText(text)))

    suspend fun forward(id: String, targets: List<ChatTarget>): List<ChatItem> =
        GraphqlApi.execute(Mutation.forwardChatItems(id, targets.map { it.encodedToId }))

    suspend fun deleteQuery(query: String): Int =
        GraphqlApi.execute(Mutation.deleteChatItems(query)).affectedCount

    suspend fun deliver(id: String, recipients: List<String>? = null): ChatItem =
        GraphqlApi.execute(Mutation.deliverChatItem(id, recipients))

    suspend fun deleteOne(id: String) {
        GraphqlApi.execute(Mutation.deleteChatItem(id))
    }

    suspend fun deleteByIds(ids: Set<String>) {
        if (ids.isNotEmpty()) deleteQuery(selectionQuery(ids))
    }

    suspend fun clearAllMessages(target: ChatTarget) = withIO {
        require(target.toId.isNotEmpty())
        val field = if (target.type == ChatTargetType.CHANNEL) "channel" else "peer"
        val id = target.toId.replace("\\", "\\\\").replace("\"", "\\\"")
        deleteQuery("$field:=\"$id\"")
    }
}
