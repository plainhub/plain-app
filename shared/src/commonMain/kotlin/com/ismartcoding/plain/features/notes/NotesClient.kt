package com.ismartcoding.plain.features.notes

import com.ismartcoding.plain.features.settings.SavedValuesClient
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.Note
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.graphql.selectionQuery

object NotesClient : NotesApi() {

    suspend fun readDraft(): String = SavedValuesClient.get("quick_note_draft", "")

    suspend fun writeDraft(value: String) = SavedValuesClient.set("quick_note_draft", value)
}
open class NotesApi internal constructor(
    private val api: GraphqlClient = GraphqlApi,
) {

    suspend fun count(query: String): Int =
        api.execute(Query.noteCount(query))

    suspend fun search(query: String, limit: Int, offset: Int): List<Note> =
        api.execute(Query.notes(query, limit, offset))

    suspend fun getById(id: String): Note? =
        api.execute(Query.note(id))

    suspend fun save(id: String, title: String, content: String): Note {
        return api.execute(Mutation.saveNote(id, title, content))
    }

    suspend fun saveFeedEntryAsync(id: String) {
        api.execute(Mutation.saveFeedEntriesToNotes(selectionQuery(setOf(id))))
    }

    suspend fun trashAsync(ids: Set<String>) {
        if (ids.isNotEmpty()) api.execute(Mutation.trashNotes(selectionQuery(ids)))
    }

    suspend fun restoreAsync(ids: Set<String>) {
        if (ids.isNotEmpty()) api.execute(Mutation.restoreNotes(selectionQuery(ids)))
    }

    suspend fun deleteAsync(ids: Set<String>) {
        if (ids.isNotEmpty()) api.execute(Mutation.deleteNotes(selectionQuery(ids)))
    }
}
