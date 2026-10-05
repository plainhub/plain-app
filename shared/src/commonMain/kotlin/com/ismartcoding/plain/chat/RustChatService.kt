package com.ismartcoding.plain.chat

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.toJSONString
import com.ismartcoding.plain.db.DMessageFile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

internal object RustChatService {
    private val json = Json { encodeDefaults = true }

    suspend fun items(target: String, offset: Int, limit: Int, query: String): List<DChat> = call("items") {
        put("target", target); put("offset", offset); put("limit", limit); put("query", query)
    }.jsonArray.map(RustChatStore::decode)

    suspend fun send(target: ChatTarget, content: DMessageContent): DChat = RustChatStore.decode(call("send") {
        put("target", target.encodedToId); put("content", content.toJSONString())
    })
    suspend fun sendText(targets: List<ChatTarget>, text: String): List<DChat> = call("sendText") {
        put("targets", JsonArray(targets.map { JsonPrimitive(it.encodedToId) })); put("text", text)
    }.jsonArray.map(RustChatStore::decode)
    suspend fun sendMany(targets: List<ChatTarget>, content: DMessageContent): List<DChat> = call("sendMany") {
        put("targets", JsonArray(targets.map { JsonPrimitive(it.encodedToId) })); put("content", content.toJSONString())
    }.jsonArray.map(RustChatStore::decode)
    suspend fun forward(id: String, target: ChatTarget): DChat = RustChatStore.decode(call("forward") {
        put("id", id); put("target", target.encodedToId)
    })
    suspend fun share(targets: List<ChatTarget>, uris: List<String>, text: String?, caption: String?, images: Boolean? = null, normalize: Boolean = false): Boolean {
        val result = call("share") {
        put("targets", JsonArray(targets.map { JsonPrimitive(it.encodedToId) })); put("uris", JsonArray(uris.map(::JsonPrimitive)))
        put("text", text?.let(::JsonPrimitive) ?: JsonNull); put("caption", caption?.let(::JsonPrimitive) ?: JsonNull)
        put("images", images?.let(::JsonPrimitive) ?: JsonNull); put("normalize", normalize)
        }.jsonObject
        result["warnings"]?.jsonArray?.forEach { com.ismartcoding.plain.ui.helpers.DialogHelper.showMessage(it.jsonPrimitive.content) }
        return result.getValue("ok").jsonPrimitive.boolean
    }
    suspend fun folder(targets: List<ChatTarget>, path: String, name: String, expiry: String?): Boolean = call("folder") {
        put("targets", JsonArray(targets.map { JsonPrimitive(it.encodedToId) })); put("path", path); put("name", name)
        put("expires_at", expiry?.let(::JsonPrimitive) ?: JsonNull)
    }.jsonObject.getValue("ok").jsonPrimitive.boolean
    suspend fun shareContent(id: String): DMessageContent {
        val content = call("shareContent") { put("id", id) }
        return DChat.parseContent(content.toString())
    }
    suspend fun retry(id: String): DChat = RustChatStore.decode(call("retry") { put("id", id) })
    suspend fun delete(ids: Set<String>) { call("delete") { put("ids", JsonArray(ids.map(::JsonPrimitive))) } }
    suspend fun deleteQuery(query: String): Int = call("deleteQuery") { put("query", query) }.jsonPrimitive.int

    suspend fun create(target: ChatTarget, content: DMessageContent): DChat = RustChatStore.decode(call("create") {
        put("target", target.encodedToId)
        put("content", content.toJSONString())
    })

    suspend fun createFiles(target: ChatTarget, files: List<DMessageFile>, images: Boolean): DChat = RustChatStore.decode(call("createFiles") {
        put("target", target.encodedToId)
        put("items", json.parseToJsonElement(json.encodeToString(files)))
        put("images", images)
    })

    suspend fun replaceFiles(id: String, files: List<DMessageFile>): DChat? = call("replaceFiles") {
        put("id", id)
        put("items", json.parseToJsonElement(json.encodeToString(files)))
    }.takeUnless { it is JsonNull }?.let(RustChatStore::decode)

    suspend fun replaceMany(ids: List<String>, files: List<DMessageFile>): List<DChat?> = call("replaceFilesMany") {
        put("ids", JsonArray(ids.map(::JsonPrimitive)))
        put("items", json.parseToJsonElement(json.encodeToString(files)))
    }.jsonArray.map { it.takeUnless { value -> value is JsonNull }?.let(RustChatStore::decode) }

    suspend fun clear(target: ChatTarget) { call("clear") { put("target", target.encodedToId) } }

    private suspend fun call(action: String, fields: JsonObjectBuilder.() -> Unit): JsonElement =
        RustContentApi.postJson("chat/service", buildJsonObject { put("action", action); fields() }, longRunning = true).getValue("result")
}
