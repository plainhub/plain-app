package com.ismartcoding.plain.chat.peer

import com.ismartcoding.plain.chat.callChatStore
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.enums.PeerStatus
import kotlinx.serialization.json.*
import kotlin.time.Instant

object RustPeerStore {
    suspend fun getAll(): List<DPeer> = list(emptyList())
    suspend fun getAllPaired(): List<DPeer> = list(listOf(PeerStatus.PAIRED))
    suspend fun getAllWithPublicKey(): List<DPeer> = list(listOf(PeerStatus.PAIRED, PeerStatus.CHANNEL))
    private suspend fun list(statuses: List<PeerStatus>): List<DPeer> = callChatStore("peers") { put("statuses", JsonArray(statuses.map { JsonPrimitive(it.name) })) }.jsonArray.map(::decode)
    suspend fun getById(id: String): DPeer? = callChatStore("peer") { put("id", id) }.takeUnless { it is JsonNull }?.let(::decode)
    suspend fun getByIds(ids: List<String>): List<DPeer> = callChatStore("peersByIds") { put("ids", JsonArray(ids.map(::JsonPrimitive))) }.jsonArray.map(::decode)
    suspend fun insert(vararg item: DPeer) { save(item.toList(), "INSERT") }
    suspend fun update(vararg item: DPeer) { save(item.toList(), "UPDATE") }
    suspend fun upsert(vararg item: DPeer) { save(item.toList(), "UPSERT") }
    private suspend fun save(items: List<DPeer>, mode: String) { callChatStore("savePeers") { put("mode", mode); put("items", JsonArray(items.map(::encode))) } }
    suspend fun patch(before: DPeer, after: DPeer): DPeer? = callChatStore("patchPeer") { put("before", encode(before)); put("after", encode(after)) }.takeUnless { it is JsonNull }?.let(::decode)
    suspend fun remove(id: String): Boolean = callChatStore("removePeer") { put("id", id) }.jsonPrimitive.boolean
    suspend fun unpair(id: String): Boolean = callChatStore("unpairPeer") { put("id", id) }.jsonPrimitive.boolean
    suspend fun discovered(id: String, ips: List<String>, port: Int, name: String, type: DeviceType): DPeer? = callChatStore("discoverPeer") {
        put("id", id); put("ips", JsonArray(ips.map(::JsonPrimitive))); put("port", port); put("name", name); put("device_type", type.name)
    }.takeUnless { it is JsonNull }?.let(::decode)
    suspend fun delete(id: String) { deleteByIds(listOf(id)) }
    suspend fun deleteByIds(ids: List<String>) { callChatStore("deletePeers") { put("ids", JsonArray(ids.map(::JsonPrimitive))) } }
    private fun encode(row: DPeer): JsonObject = buildJsonObject {
        put("id", row.id); put("name", row.name); put("ip", row.ip); put("key", row.key); put("public_key", row.publicKey)
        put("status", row.status.name); put("port", row.port); put("device_type", row.deviceType.name); put("token", "")
        put("created_at", row.createdAt.toString()); put("updated_at", row.updatedAt.toString())
    }
    private fun decode(value: JsonElement): DPeer = value.jsonObject.let { row ->
        fun string(key: String) = row.getValue(key).jsonPrimitive.content
        DPeer(string("id"), string("name"), string("ip"), string("key"), string("public_key"), PeerStatus.valueOf(string("status")), row.getValue("port").jsonPrimitive.int, DeviceType.valueOf(string("device_type")), Instant.parse(string("created_at")), Instant.parse(string("updated_at")))
    }
}
