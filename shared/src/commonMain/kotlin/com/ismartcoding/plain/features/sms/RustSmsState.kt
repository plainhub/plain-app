package com.ismartcoding.plain.features.sms

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.db.DArchivedConversation
import kotlin.time.Instant
import kotlinx.serialization.json.*

object RustSmsState {
    private suspend fun call(action: String, build: JsonObjectBuilder.() -> Unit = {}): JsonObject =
        RustContentApi.postJson("system/sms-state", buildJsonObject { put("action", action); build() })

    suspend fun archives(): List<DArchivedConversation> = call("archives").getValue("items").jsonArray.map {
        val row = it.jsonObject
        DArchivedConversation(row.getValue("conversation_id").jsonPrimitive.content,
            Instant.parse(row.getValue("conversation_date").jsonPrimitive.content))
    }

    suspend fun archive(id: String, date: Instant) { call("archive") { put("id", id); put("date", date.toString()) } }
    suspend fun unarchive(id: String) { call("unarchive") { put("id", id) } }
    suspend fun trashed(): Set<String> = call("trashed").getValue("ids").jsonArray.map { it.jsonPrimitive.content }.toSet()
    suspend fun trash(ids: Collection<String>): Int = call("trash") { put("ids", JsonArray(ids.map(::JsonPrimitive))) }.getValue("count").jsonPrimitive.int
    suspend fun restore(ids: Collection<String>): Int = call("restore") { put("ids", JsonArray(ids.map(::JsonPrimitive))) }.getValue("count").jsonPrimitive.int
}
