package com.ismartcoding.plain.chat.channel

import com.ismartcoding.plain.chat.callChatStore
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.ChannelMember
import com.ismartcoding.plain.enums.ChatChannelStatus
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.*
import kotlin.time.Instant

object RustChannelStore {
    suspend fun create(name: String): DChatChannel = decode(callChatStore("createChannel") { put("actor", com.ismartcoding.plain.TempData.clientId); put("name", name) })
    suspend fun action(id: String, kind: String, name: String? = null, peer: String? = null): DChatChannel = decode(callChatStore("channelAction") {
        put("actor", com.ismartcoding.plain.TempData.clientId); put("id", id); put("operation", buildJsonObject {
            put("kind", kind); name?.let { put("name", it) }; peer?.let { put("peer", it) }
        })
    })
    suspend fun getAll(): List<DChatChannel> = callChatStore("channels").jsonArray.map(::decode)
    suspend fun getById(id: String): DChatChannel? = callChatStore("channel") { put("id", id) }.takeUnless { it is JsonNull }?.let(::decode)
    suspend fun insert(vararg item: DChatChannel) { save(item.toList(), "INSERT") }
    suspend fun update(vararg item: DChatChannel) { save(item.toList(), "UPDATE") }
    private suspend fun save(items: List<DChatChannel>, mode: String) { callChatStore("saveChannels") { put("mode", mode); put("items", JsonArray(items.map(::encode))) } }
    suspend fun patch(before: DChatChannel, after: DChatChannel): DChatChannel = decode(callChatStore("patchChannel") { put("before", encode(before)); put("after", encode(after)) })
    suspend fun remove(id: String): DChatChannel? = callChatStore("removeChannel") { put("id", id) }.takeUnless { it is JsonNull }?.let(::decode)
    suspend fun delete(id: String) { deleteByIds(listOf(id)) }
    suspend fun deleteByIds(ids: List<String>) { callChatStore("deleteChannels") { put("ids", JsonArray(ids.map(::JsonPrimitive))) } }
    fun encode(row: DChatChannel): JsonObject = buildJsonObject {
        put("id", row.id); put("name", row.name); put("key", row.key); put("owner_id", row.ownerId); put("members", JsonHelper.jsonEncode(row.members))
        put("version", row.version); put("status", row.status.name); put("created_at", row.createdAt.toString()); put("updated_at", row.updatedAt.toString())
    }
    internal fun decode(value: JsonElement): DChatChannel = value.jsonObject.let { row ->
        fun string(key: String) = row.getValue(key).jsonPrimitive.content
        DChatChannel(string("id"), string("name"), string("key"), string("owner_id"), JsonHelper.jsonDecode<List<ChannelMember>>(string("members")), row.getValue("version").jsonPrimitive.long, ChatChannelStatus.valueOf(string("status")), Instant.parse(string("created_at")), Instant.parse(string("updated_at")))
    }
}
