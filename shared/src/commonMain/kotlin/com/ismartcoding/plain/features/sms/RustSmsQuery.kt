package com.ismartcoding.plain.features.sms

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.*

object RustSmsQuery {
    private suspend fun call(action: String, query: String, build: JsonObjectBuilder.() -> Unit = {}): JsonObject =
        RustContentApi.postJson("system/sms", buildJsonObject { put("action", action); if (action != "counts" && action != "textIds") put("query", query); build() })
    suspend fun search(query: String, limit: Int, offset: Int, includeTrashed: Boolean): List<DMessage> = JsonHelper.jsonDecode(
        call("search", query) { put("limit", limit); put("offset", offset); put("includeTrashed", includeTrashed) }.getValue("items").toString())
    suspend fun count(query: String): Int = call("count", query).getValue("count").jsonPrimitive.int
    suspend fun ids(query: String, includeTrashed: Boolean): Set<String> = call("ids", query) { put("includeTrashed", includeTrashed) }.getValue("ids").jsonArray.map { it.jsonPrimitive.content }.toSet()
    suspend fun conversations(query: String, limit: Int, offset: Int): List<DMessageConversation> = JsonHelper.jsonDecode(
        call("conversations", query) { put("limit", limit); put("offset", offset) }.getValue("items").toString())
    suspend fun conversationCount(query: String): Int = call("conversationCount", query).getValue("count").jsonPrimitive.int
    suspend fun archivedConversations(): List<DMessageConversation> = call("archivedConversations", "").getValue("items").let { JsonHelper.jsonDecode(it.toString()) }
    suspend fun conversationDate(id: String): kotlin.time.Instant? = call("conversationDate", "") { put("id", id) }.getValue("date").let { if(it is JsonNull) null else kotlin.time.Instant.parse(it.jsonPrimitive.content) }
    suspend fun counts(): JsonObject = call("counts", "")
    suspend fun textIds(filters: List<String>): Set<String> = call("textIds", "") { put("filters", JsonArray(filters.map(::JsonPrimitive))) }.getValue("ids").jsonArray.map { it.jsonPrimitive.content }.toSet()
}
