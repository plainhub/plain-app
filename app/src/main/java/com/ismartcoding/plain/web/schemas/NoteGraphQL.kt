package com.ismartcoding.plain.web.schemas

import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.lib.kgraphql.schema.execution.Executor
import com.ismartcoding.lib.extensions.cut
import com.ismartcoding.lib.helpers.JsonHelper.jsonEncode
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.NoteHelper
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.feed.FeedEntryHelper
import com.ismartcoding.plain.web.loaders.TagsLoader
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.Note
import com.ismartcoding.plain.web.models.NoteInput
import com.ismartcoding.plain.web.models.toExportModel
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addNoteSchema() {
    query("notes") {
        configure {
            executor = Executor.DataLoaderPrepared
        }
        resolver { offset: Int, limit: Int, query: String ->
            val items = NoteHelper.search(query, limit, offset)
            items.map { it.toModel() }
        }
        type<Note> {
            dataProperty("tags") {
                prepare { item -> item.id.value }
                loader { ids ->
                    TagsLoader.load(ids, DataType.NOTE)
                }
            }
        }
    }
    query("noteCount") {
        resolver { query: String ->
            NoteHelper.count(query)
        }
    }
    query("note") {
        resolver { id: ID ->
            val data = NoteHelper.getById(id.value)
            data?.toModel()
        }
    }
    mutation("saveNote") {
        resolver { id: ID, input: NoteInput ->
            val item =
                NoteHelper.addOrUpdateAsync(id.value) {
                    title = input.title
                    content = input.content
                }
            NoteHelper.getById(item.id)?.toModel()
        }
    }
    mutation("saveFeedEntriesToNotes") {
        resolver { query: String ->
            val entries = FeedEntryHelper.search(query, Int.MAX_VALUE, 0)
            val ids = mutableListOf<String>()
            entries.forEach { m ->
                val c = "# ${m.title}\n\n" + m.content.ifEmpty { m.description }
                NoteHelper.saveToNotesAsync(m.id) {
                    title = c.cut(250).replace("\n", "")
                    content = c
                }
                ids.add(m.id)
            }
            ids
        }
    }
    mutation("trashNotes") {
        resolver { query: String ->
            val ids = NoteHelper.getIdsAsync(query)
            TagHelper.deleteTagRelationByKeys(ids, DataType.NOTE)
            NoteHelper.trashAsync(ids)
            query
        }
    }
    mutation("restoreNotes") {
        resolver { query: String ->
            val ids = NoteHelper.getTrashedIdsAsync(query)
            NoteHelper.restoreAsync(ids)
            query
        }
    }
    mutation("deleteNotes") {
        resolver { query: String ->
            val ids = NoteHelper.getTrashedIdsAsync(query)
            TagHelper.deleteTagRelationByKeys(ids, DataType.NOTE)
            NoteHelper.deleteAsync(ids)
            query
        }
    }
    mutation("exportNotes") {
        resolver { query: String ->
            val items = NoteHelper.search(query, Int.MAX_VALUE, 0)
            val keys = items.map { it.id }
            val allTags = TagHelper.getAll(DataType.NOTE)
            val map = TagHelper.getTagRelationsByKeys(keys.toSet(), DataType.NOTE).groupBy { it.key }
            jsonEncode(items.map {
                val tagIds = map[it.id]?.map { t -> t.tagId } ?: emptyList()
                it.toExportModel(if (tagIds.isNotEmpty()) allTags.filter { tagIds.contains(it.id) }.map { t -> t.toModel() } else emptyList())
            })
        }
    }
}
