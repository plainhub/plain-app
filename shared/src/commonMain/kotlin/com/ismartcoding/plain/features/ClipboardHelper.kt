package com.ismartcoding.plain.features

import com.ismartcoding.plain.chat.peer.RustPeerStore

import com.ismartcoding.plain.api.*
import com.ismartcoding.plain.db.DClipboard
import com.ismartcoding.plain.helpers.ContentWhere
import com.ismartcoding.plain.helpers.FilterField
import kotlinx.serialization.json.*

object ClipboardHelper {
    private const val FIELDS = "id text source label sensitive createdAt"

    suspend fun getPage(limit: Int, offset: Int, query: String = ""): List<DClipboard> =
        RustContentApi.query("clipboardItems(limit: $limit, offset: $offset, query: ${gql(query)}) { $FIELDS }")
            .getValue("clipboardItems").jsonArray.map { it.clipboard() }

    suspend fun count(query: String = ""): Int = RustContentApi.query("clipboardItemCount(query: ${gql(query)})")
        .getValue("clipboardItemCount").jsonPrimitive.int

    suspend fun record(text: String, source: String = "", label: String = "", sensitive: Boolean = false): DClipboard? {
        val input = "{ text: ${gql(text)}, source: ${gql(source)}, label: ${gql(label)}, sensitive: $sensitive }"
        val result = RustContentApi.mutate("recordClipboard(input: $input) { inserted item { $FIELDS } }")
            .getValue("recordClipboard").jsonObject
        return if (result.getValue("inserted").jsonPrimitive.boolean) result.getValue("item").clipboard() else null
    }

    suspend fun deleteByIds(ids: List<String>): Int {
        if (ids.isEmpty()) return 0
        return RustContentApi.mutate("deleteClipboardItemsByIds(ids: ${gqlIds(ids)}) { affectedCount }")
            .getValue("deleteClipboardItemsByIds").jsonObject.getValue("affectedCount").jsonPrimitive.int
    }

    suspend fun delete(query: String): Int = RustContentApi.mutate("deleteClipboardItems(query: ${gql(query)}) { affectedCount }")
        .getValue("deleteClipboardItems").jsonObject.getValue("affectedCount").jsonPrimitive.int

    suspend fun clear() { delete("all:true") }

    suspend fun getSourceName(source: String): String =
        if (source.isBlank()) "" else RustPeerStore.getById(source)?.name ?: source

    internal fun applyClipboardSearch(where: ContentWhere, query: String) = LegacyClipboardHelper.applyClipboardSearch(where, query)
    internal fun applyClipboardFilterFields(where: ContentWhere, fields: List<FilterField>) = LegacyClipboardHelper.applyClipboardFilterFields(where, fields)

    private fun JsonElement.clipboard(): DClipboard = jsonObject.let {
        DClipboard(id = it.string("id"), text = it.string("text"), source = it.string("source"), label = it.string("label"),
            sensitive = it.getValue("sensitive").jsonPrimitive.boolean, createdAt = it.instant("createdAt"))
    }
}
