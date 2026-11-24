package com.ismartcoding.plain.ui.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.ismartcoding.plain.models.File
import com.ismartcoding.plain.rust.graphql.AppSubscriptions
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.rust.graphql.GraphqlOperation
import com.ismartcoding.plain.rust.graphql.SubscriptionUpdate
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.state.exitSelectMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class FilesTrashState(
    private val source: ContentSource,
    private val onFilesChanged: (List<File>) -> Unit,
) {
    private val refreshMutex = Mutex()
    var files by mutableStateOf<List<File>?>(null)
        private set

    suspend fun refresh() {
        refreshMutex.withLock {
            val entries = source.files.trashed()
            currentCoroutineContext().ensureActive()
            files = entries
            onFilesChanged(entries)
        }
    }
    suspend fun observe() {
        val updates = when (source) {
            ContentSource.Local -> merge(
                AppSubscriptions.subscription<Boolean>("contentChanged"),
                AppSubscriptions.mediaUpdated.filter { it != SubscriptionUpdate.Attached },
            )
            is ContentSource.Remote -> source.client.subscriptionUpdates(
                listOf(
                    GraphqlOperation("subscription { contentChanged }"),
                    GraphqlOperation("subscription { mediaUpdated { types } }"),
                ),
            )
        }
        updates.collect { update ->
            if (update is SubscriptionUpdate.Failed) {
                DialogHelper.showMessage(update.error)
            } else {
                try {
                    refresh()
                } catch (canceled: CancellationException) {
                    throw canceled
                } catch (error: Exception) {
                    DialogHelper.showMessage(error)
                }
            }
        }
    }
}

@Composable
internal fun rememberFilesTrash(filesVM: FilesViewModel): FilesTrashState {
    val trash = remember(filesVM) {
        FilesTrashState(filesVM.source) { entries ->
            val ids = entries.mapTo(mutableSetOf()) { it.id }
            if (filesVM.selectedIds.retainAll(ids) && filesVM.selectedIds.isEmpty()) {
                filesVM.exitSelectMode()
            }
            filesVM.selectedFile.value?.let { if (it.id !in ids) filesVM.selectedFile.value = null }
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(trash, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { trash.observe() }
    }
    return trash
}
