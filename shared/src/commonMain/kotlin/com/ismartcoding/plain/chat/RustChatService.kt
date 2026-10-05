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
