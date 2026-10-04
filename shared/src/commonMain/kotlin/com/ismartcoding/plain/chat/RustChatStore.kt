package com.ismartcoding.plain.chat

import com.ismartcoding.plain.db.*
import com.ismartcoding.plain.enums.ChatStatus
import kotlinx.serialization.json.*
import kotlin.time.Instant

object RustChatStore {
    suspend fun create(toId: String, channelId: String, content: DMessageContent): DChat = decode(callChatStore("createChat") {
        put("to_id", toId); put("channel_id", channelId); put("content", content.toJSONString())
    })
    suspend fun receive(fromId: String, channelId: String, content: String, signature: String, timestamp: Long): DReceivedChat? {
        val result = callChatStore("receiveChat") {
            put("from_id", fromId); put("channel_id", channelId); put("content", content); put("signature", signature); put("timestamp", timestamp)
        }.takeUnless { it is JsonNull }?.jsonObject ?: return null
        return DReceivedChat(decode(result.getValue("chat")), com.ismartcoding.plain.chat.peer.RustPeerStore.decode(result.getValue("peer")), result.getValue("channel").takeUnless { it is JsonNull }?.let(com.ismartcoding.plain.chat.channel.RustChannelStore::decode))
    }
    suspend fun delivery(id: String, results: List<DMessageDeliveryResult>?, retry: Boolean = false): DChat? = callChatStore("chatDelivery") {
        put("id", id); put("retry", retry)
        put("results", results?.let { JsonArray(it.map { result -> buildJsonObject {
            put("peerId", result.peerId); put("peerName", result.peerName); put("error", result.error?.let(::JsonPrimitive) ?: JsonNull)
        } }) } ?: JsonNull)
    }.takeUnless { it is JsonNull }?.let(::decode)
    suspend fun getAll(): List<DChat> = page()
    suspend fun getByPeerId(id: String): List<DChat> = page(peer = id)
    suspend fun getByChannelId(id: String): List<DChat> = page(channel = id)
    suspend fun getByPeerIdPage(id: String, limit: Int, offset: Int): List<DChat> = page(peer = id, limit = limit, offset = offset, descending = true)
    suspend fun getByChannelIdPage(id: String, limit: Int, offset: Int): List<DChat> = page(channel = id, limit = limit, offset = offset, descending = true)
    suspend fun getByPeerIdPageText(id: String, text: String, limit: Int, offset: Int): List<DChat> = page(peer = id, text = text, limit = limit, offset = offset, descending = true)
    suspend fun getByChannelIdPageText(id: String, text: String, limit: Int, offset: Int): List<DChat> = page(channel = id, text = text, limit = limit, offset = offset, descending = true)
    suspend fun getAllLatestChats(): List<DChat> = page(latest = true)
    suspend fun search(text: String, limit: Int, offset: Int): List<DChat> = page(text = text, limit = limit, offset = offset, descending = true)
    suspend fun count(text: String): Int = query(text = text, countOnly = true).jsonPrimitive.int
    suspend fun getIds(query: String): Set<String> = callChatStore("chatIds") { put("query", query) }.jsonArray.map { it.jsonPrimitive.content }.toSet()
    suspend fun page(peer: String? = null, channel: String? = null, text: String = "", offset: Int = 0, limit: Int? = null, descending: Boolean = false, latest: Boolean = false): List<DChat> = query(peer, channel, text, offset, limit, descending, latest).jsonArray.map(::decode)
    private suspend fun query(peer: String? = null, channel: String? = null, text: String = "", offset: Int = 0, limit: Int? = null, descending: Boolean = false, latest: Boolean = false, countOnly: Boolean = false): JsonElement = callChatStore("chats") { put("filter", buildJsonObject {
        put("peer", peer?.let(::JsonPrimitive) ?: JsonNull); put("channel", channel?.let(::JsonPrimitive) ?: JsonNull); put("text", text); put("offset", offset)
        put("limit", limit?.let(::JsonPrimitive) ?: JsonNull); put("descending", descending); put("latest", latest); put("countOnly", countOnly)
    }) }
    suspend fun getById(id: String): DChat? = callChatStore("chat") { put("id", id) }.takeUnless { it is JsonNull }?.let(::decode)
    suspend fun insert(vararg item: DChat) { save(item.toList(), "INSERT") }
    suspend fun update(vararg item: DChat) { save(item.toList(), "UPDATE") }
    private suspend fun save(items: List<DChat>, mode: String) { callChatStore("saveChats") { put("mode", mode); put("items", JsonArray(items.map(::encode))) } }
    suspend fun updateStatus(id: String, status: ChatStatus) { status(id, status, null) }
    suspend fun updateStatusAndData(id: String, status: ChatStatus, data: String) { status(id, status, data) }
    private suspend fun status(id: String, status: ChatStatus, data: String?) { check(callChatStore("chatStatus") { put("id", id); put("status", status.name); put("data", data?.let(::JsonPrimitive) ?: JsonNull) }.jsonPrimitive.boolean) { "Chat unavailable" } }
    suspend fun updateData(item: ChatItemDataUpdate) { check(callChatStore("chatContent") { put("id", item.id); put("content", item.content.toJSONString()) }.jsonPrimitive.boolean) { "Chat unavailable" } }
    suspend fun delete(id: String) { deleteByIds(listOf(id)) }
    suspend fun deleteByIds(ids: List<String>) { callChatStore("deleteChats") { put("ids", JsonArray(ids.map(::JsonPrimitive))) } }
    suspend fun deleteByPeerId(id: String) { callChatStore("deletePeerChats") { put("id", id) } }
    suspend fun deleteByChannelId(id: String) { callChatStore("deleteChannelChats") { put("id", id) } }
    private fun encode(row: DChat): JsonObject = buildJsonObject {
        put("id", row.id); put("from_id", row.fromId); put("to_id", row.toId); put("channel_id", row.channelId); put("content", row.content.toJSONString())
        put("status", row.status.name); put("status_data", row.statusData); put("created_at", row.createdAt.toString()); put("updated_at", row.updatedAt.toString())
    }
    internal fun decode(value: JsonElement): DChat = value.jsonObject.let { row ->
        fun string(key: String) = row.getValue(key).jsonPrimitive.content
        DChat(id = string("id"), fromId = string("from_id"), toId = string("to_id"), channelId = string("channel_id"), status = ChatStatus.valueOf(string("status")), statusData = string("status_data"), content = DChat.parseContent(string("content")), createdAt = Instant.parse(string("created_at")), updatedAt = Instant.parse(string("updated_at")))
    }
}
