package com.ismartcoding.plain.chat

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.discover.MdnsDiscoverManager
import com.ismartcoding.plain.lib.withIO
import kotlinx.serialization.json.*

object ChatSender {
    suspend fun send(item: DChat) = withIO {
        deliver(item, null)
    }

    suspend fun sendToChannelMembers(item: DChat, peerIds: List<String>) = withIO {
        deliver(item, peerIds)
    }

    private suspend fun deliver(item: DChat, peerIds: List<String>?) {
        val result = RustContentApi.postJson("chat/send", buildJsonObject {
            put("id", item.id)
            put("recipients", peerIds?.let { JsonArray(it.map(::JsonPrimitive)) } ?: JsonNull)
        }, longRunning = true).getValue("result").jsonObject
        if (result.getValue("rediscover").jsonPrimitive.boolean) MdnsDiscoverManager.browse()
        val saved = result.getValue("chat").takeUnless { it is JsonNull }?.let(RustChatStore::decode)
            ?: error("Chat unavailable")
        item.fromId = saved.fromId
        item.toId = saved.toId
        item.channelId = saved.channelId
        item.content = saved.content
        item.createdAt = saved.createdAt
        item.status = saved.status
        item.statusData = saved.statusData
        item.updatedAt = saved.updatedAt
    }
}
