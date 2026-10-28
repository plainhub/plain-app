package com.ismartcoding.plain.ui.tools

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.features.files.FileWriteAction
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.platform.files.readTextFile
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.rust.graphql.selectionQuery

internal class ManagedDocumentViewModel(
    private val device: ManagedDeviceViewModel,
    val feature: AppFeatureType,
    private val id: String,
    private val path: String,
) : ViewModel() {
    val title = mutableStateOf("")
    val content = mutableStateOf("")
    val loading = mutableStateOf(true)

    suspend fun load() {
        val client = device.client.await()
        when (feature) {
            AppFeatureType.NOTES -> if (id.isNotEmpty()) {
                val note = checkNotNull(client.notes.getById(id)) { "not_found" }
                title.value = note.title
                content.value = note.content
            }
            AppFeatureType.FILES -> {
                title.value = path.substringAfterLast('/')
                content.value = readTextFile(client.cacheFile(path, title.value))
            }
            AppFeatureType.FEEDS -> {
                val entry = checkNotNull(client.api.execute(Query.feedEntry(id))) { "not_found" }
                title.value = entry.title
                content.value = entry.content.ifEmpty { entry.description }
                client.api.execute(Mutation.markFeedEntriesRead(selectionQuery(setOf(id)), true))
            }
            else -> error("Unsupported managed document")
        }
        loading.value = false
    }

    suspend fun save() {
        val client = device.client.await()
        when (feature) {
            AppFeatureType.NOTES -> client.notes.save(id, title.value, content.value)
            AppFeatureType.FILES -> client.api.execute(Mutation.fileWrite(FileWriteAction.WRITE_TEXT, path, content.value, true))
            else -> error("Managed document is read-only")
        }
    }
}
