package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.db.DNote
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.extensions.getMarkdownTitle
import com.ismartcoding.plain.lib.JsonHelper.jsonEncode
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.NoteHelper
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.feed.FeedEntryHelper
import com.ismartcoding.plain.httpserver.loaders.TagsLoader
import com.ismartcoding.plain.httpserver.models.ActionResult
import com.ismartcoding.plain.httpserver.models.ID
import com.ismartcoding.plain.httpserver.models.Note
import com.ismartcoding.plain.httpserver.models.NoteInput
import com.ismartcoding.plain.httpserver.models.toExportModel
import com.ismartcoding.plain.httpserver.models.toModel
import com.ismartcoding.plain.ui.models.NotesViewModel

@GraphQLQuery
suspend fun noteCount(query: String): Int {
    return NoteHelper.count(query)
}

@GraphQLQuery
suspend fun note(id: ID): Note? {
    val data = NoteHelper.getById(id.value)
    return data?.toModel()
}

@GraphQLMutation
suspend fun createNote(input: NoteInput): Note {
    return upsertNote("") { title = input.title; content = input.content }
}

@GraphQLMutation
suspend fun updateNote(id: ID, input: NoteInput): Note {
    if (AppDatabase.instance.noteDao().getById(id.value) == null) {
        throw GraphQLError("Note ${'$'}{id.value} not found")
    }
    return upsertNote(id.value) { title = input.title; content = input.content }
}

private suspend fun upsertNote(id: String, updateItem: DNote.() -> Unit): Note {
    val item = NoteHelper.addOrUpdateAsync(id, updateItem)
    NotesViewModel.reloadAsync()
    return NoteHelper.getById(item.id)?.toModel()
        ?: throw GraphQLError("Note ${'$'}{item.id} not found after save")
}

@GraphQLMutation
suspend fun saveFeedEntriesToNotes(query: String): List<ID> {
    val entries = FeedEntryHelper.search(query, Int.MAX_VALUE, 0)
    val ids = mutableListOf<ID>()
    entries.forEach { m ->
        val c = "# ${m.title}\n\n" + m.content.ifEmpty { m.description }
        NoteHelper.saveToNotesAsync(m.id) {
            title = c.getMarkdownTitle()
            content = c
        }
        ids.add(ID(m.id))
    }
    NotesViewModel.reloadAsync()
    return ids
}

@GraphQLMutation
suspend fun trashNotes(query: String): ActionResult {
    val ids = NoteHelper.getIdsAsync(query)
    TagHelper.deleteTagRelationByKeys(ids, DataType.NOTE)
    NoteHelper.trashAsync(ids)
    NotesViewModel.reloadAsync()
    return ActionResult(ids.size)
}

@GraphQLMutation
suspend fun restoreNotes(query: String): ActionResult {
    val ids = NoteHelper.getTrashedIdsAsync(query)
    NoteHelper.restoreAsync(ids)
    NotesViewModel.reloadAsync()
    return ActionResult(ids.size)
}

@GraphQLMutation
suspend fun deleteNotes(query: String): ActionResult {
    val ids = NoteHelper.getTrashedIdsAsync(query)
    TagHelper.deleteTagRelationByKeys(ids, DataType.NOTE)
    NoteHelper.deleteAsync(ids)
    NotesViewModel.reloadAsync()
    return ActionResult(ids.size)
}

@GraphQLMutation
suspend fun exportNotes(query: String): String {
    val items = NoteHelper.search(query, Int.MAX_VALUE, 0)
    val keys = items.map { it.id }
    val allTags = TagHelper.getAll(DataType.NOTE)
    val map = TagHelper.getTagRelationsByKeys(keys.toSet(), DataType.NOTE).groupBy { it.key }
    return jsonEncode(items.map {
        val tagIds = map[it.id]?.map { t -> t.tagId } ?: emptyList()
        it.toExportModel(if (tagIds.isNotEmpty()) allTags.filter { tagIds.contains(it.id) }.map { t -> t.toModel() } else emptyList())
    })
}

@GraphQLQuery
suspend fun notes(offset: Int, limit: Int, query: String): List<Note> {
    val items = NoteHelper.search(query, limit, offset)
    return items.map { it.toModel() }
}

fun SchemaBuilder.addNoteSchema() {
    type<Note> {
        dataProperty("tags") {
            prepare { item -> item.id.value }
            loader { ids ->
                TagsLoader.load(ids, DataType.NOTE)
            }
        }
    }
}
