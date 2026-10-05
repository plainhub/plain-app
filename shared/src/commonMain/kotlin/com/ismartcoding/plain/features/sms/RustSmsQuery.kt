package com.ismartcoding.plain.features.sms

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.DSmsCounts
import kotlinx.serialization.json.*

object RustSmsQuery {
    private suspend fun call(action: String, params: JsonObjectBuilder.() -> Unit = {}): JsonObject =
        RustContentApi.postJson("system/sms", buildJsonObject {
            put("action", action)
            params()
        })

    suspend fun messages(query: String, offset: Int, limit: Int): List<DMessage> =
        search(query, limit, offset)

    suspend fun search(query: String, limit: Int, offset: Int, includeTrashed: Boolean = false): List<DMessage> =
        JsonHelper.jsonDecode(call("search") {
            put("query", query)
            put("offset", offset)
            put("limit", limit)
            put("includeTrashed", includeTrashed)
        }.getValue("items").toString())

    suspend fun ids(query: String, includeTrashed: Boolean = false): Set<String> =
        call("ids") {
            put("query", query)
            put("includeTrashed", includeTrashed)
        }.getValue("ids").jsonArray.map { it.jsonPrimitive.content }.toSet()

    suspend fun textIds(filters: List<String>): Set<String> =
        call("textIds") {
            put("filters", JsonArray(filters.map(::JsonPrimitive)))
        }.getValue("ids").jsonArray.map { it.jsonPrimitive.content }.toSet()

    suspend fun count(query: String): Int =
        call("count") { put("query", query) }.getValue("count").jsonPrimitive.int

    suspend fun conversations(query: String, offset: Int, limit: Int): List<DMessageConversation> =
        JsonHelper.jsonDecode(call("conversations") {
            put("query", query)
            put("offset", offset)
            put("limit", limit)
        }.getValue("items").toString())

    suspend fun conversationCount(query: String): Int =
        call("conversationCount") { put("query", query) }.getValue("count").jsonPrimitive.int

    suspend fun archivedConversations(): List<DMessageConversation> =
        JsonHelper.jsonDecode(call("archivedConversations").getValue("items").toString())

    suspend fun conversationDate(id: String): kotlin.time.Instant? =
        call("conversationDate") {
            put("id", id)
        }.getValue("date").let { value ->
            if (value is JsonNull) null else kotlin.time.Instant.parse(value.jsonPrimitive.content)
        }

    suspend fun conversationDateEpochMillis(id: String): Long? =
        conversationDate(id)?.toEpochMilliseconds()

    suspend fun counts(): JsonObject = call("counts")

    suspend fun countModel(): DSmsCounts {
        val result = counts()
        return DSmsCounts(
            result.getValue("total").jsonPrimitive.int,
            result.getValue("inbox").jsonPrimitive.int,
            result.getValue("sent").jsonPrimitive.int,
            result.getValue("drafts").jsonPrimitive.int,
        )
    }
}
