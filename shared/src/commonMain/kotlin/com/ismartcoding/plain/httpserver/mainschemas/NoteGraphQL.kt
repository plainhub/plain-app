package com.ismartcoding.plain.httpserver.mainschemas

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
import com.ismartcoding.plain.httpserver.models.ID
import com.ismartcoding.plain.httpserver.models.Note
import com.ismartcoding.plain.httpserver.models.NoteInput
import com.ismartcoding.plain.httpserver.models.toExportModel
import com.ismartcoding.plain.httpserver.models.toModel

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
suspend fun saveNote(id: ID, input: NoteInput): Note? {
    val item =
        NoteHelper.addOrUpdateAsync(id.value) {
            title = input.title
            content = input.content
        }
    return NoteHelper.getById(item.id)?.toModel()
}

@GraphQLMutation
suspend fun saveFeedEntriesToNotes(query: String): List<String> {
    val entries = FeedEntryHelper.search(query, Int.MAX_VALUE, 0)
    val ids = mutableListOf<String>()
    entries.forEach { m ->
        val c = "# ${m.title}\n\n" + m.content.ifEmpty { m.description }
        NoteHelper.saveToNotesAsync(m.id) {
            title = c.getMarkdownTitle()
            content = c
        }
        ids.add(m.id)
    }
    return ids
}

@GraphQLMutation
suspend fun trashNotes(query: String): String {
    val ids = NoteHelper.getIdsAsync(query)
    TagHelper.deleteTagRelationByKeys(ids, DataType.NOTE)
    NoteHelper.trashAsync(ids)
    return query
}

@GraphQLMutation
suspend fun restoreNotes(query: String): String {
    val ids = NoteHelper.getTrashedIdsAsync(query)
    NoteHelper.restoreAsync(ids)
    return query
}

@GraphQLMutation
suspend fun deleteNotes(query: String): String {
    val ids = NoteHelper.getTrashedIdsAsync(query)
    TagHelper.deleteTagRelationByKeys(ids, DataType.NOTE)
    NoteHelper.deleteAsync(ids)
    return query
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
