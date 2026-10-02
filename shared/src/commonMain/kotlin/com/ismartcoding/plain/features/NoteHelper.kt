package com.ismartcoding.plain.features

import com.ismartcoding.plain.api.*
import com.ismartcoding.plain.db.DNote
import com.ismartcoding.plain.helpers.ContentWhere
import com.ismartcoding.plain.helpers.FilterField
import kotlinx.serialization.json.*

object NoteHelper {
    suspend fun count(query: String): Int = RustContentApi.query("noteCount(query: ${gql(query)})").getValue("noteCount").jsonPrimitive.int
    suspend fun search(query: String, limit: Int, offset: Int): List<DNote> = RustContentApi.query("notes(query: ${gql(query)}, limit: $limit, offset: $offset) { $NOTE_FIELDS }").getValue("notes").jsonArray.map { it.note() }
    suspend fun getById(id: String): DNote? = RustContentApi.query("note(id: ${gql(id)}) { $NOTE_FIELDS }")["note"]?.takeUnless { it is JsonNull }?.note()
    suspend fun getIdsAsync(query: String): Set<String> = ids(query)
    suspend fun getTrashedIdsAsync(query: String): Set<String> = ids("$query trash:true")
    private suspend fun ids(query: String): Set<String> = RustContentApi.query("notes(query: ${gql(query)}, limit: ${Int.MAX_VALUE}, offset: 0) { id }").getValue("notes").jsonArray.map { it.jsonObject.string("id") }.toSet()
    suspend fun addOrUpdateAsync(id: String, updateItem: DNote.() -> Unit): DNote {
        val item = if (id.isEmpty()) DNote() else getById(id) ?: error("Note $id not found")
        item.updateItem()
        val input = "{ title: ${gql(item.title)}, content: ${gql(item.content)} }"
        val field = if (id.isEmpty()) "createNote" else "updateNote"
        val args = if (id.isEmpty()) "input: $input" else "id: ${gql(id)}, input: $input"
        return RustContentApi.mutate("$field($args) { $NOTE_FIELDS }").getValue(field).note()
    }
    suspend fun saveFeedEntryAsync(id: String) {
        RustContentApi.mutate("saveFeedEntriesToNotes(query: ${gql(selectionQuery(setOf(id)))})")
    }
    suspend fun trashAsync(ids: Set<String>) { if (ids.isNotEmpty()) RustContentApi.mutate("trashNotes(query: ${gql(selectionQuery(ids))}) { affectedCount }") }
    suspend fun restoreAsync(ids: Set<String>) { if (ids.isNotEmpty()) RustContentApi.mutate("restoreNotes(query: ${gql(selectionQuery(ids))}) { affectedCount }") }
    suspend fun deleteAsync(ids: Set<String>) { if (ids.isNotEmpty()) RustContentApi.mutate("deleteNotes(query: ${gql(selectionQuery(ids))}) { affectedCount }") }
    internal fun applyNotesFilterFields(where: ContentWhere, fields: List<FilterField>) = LegacyNoteHelper.applyNotesFilterFields(where, fields)
}
