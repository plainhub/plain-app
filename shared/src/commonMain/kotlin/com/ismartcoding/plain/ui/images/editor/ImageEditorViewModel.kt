package com.ismartcoding.plain.ui.images.editor

import com.ismartcoding.plain.ui.state.launchSafe

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.features.images.editor.ImageEditorProjectsClient
import com.ismartcoding.plain.features.images.editor.ImageEditorProjectSummary
import com.ismartcoding.plain.features.images.editor.toSummary
import com.ismartcoding.plain.models.ImageEditorProject
import com.ismartcoding.plain.lib.withIO
import kotlinx.coroutines.ensureActive
import com.ismartcoding.plain.rust.graphql.SubscriptionUpdate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ImageEditorViewModel(private val projects: ImageEditorProjectsClient) : ViewModel() {
    private val _itemsFlow = MutableStateFlow<List<ImageEditorProjectSummary>>(emptyList())
    val itemsFlow: StateFlow<List<ImageEditorProjectSummary>> = _itemsFlow

    val loadState = mutableStateOf<EditorLoadState>(EditorLoadState.Loading)
    var selectedItem = mutableStateOf<ImageEditorProjectSummary?>(null)

    private val lock = Mutex()

    suspend fun loadAsync() = withIO {
        lock.withLock {
            try {
                val rows = projects.listAsync()
                _itemsFlow.value = rows
                loadState.value = EditorLoadState.Ready
            } catch (canceled: kotlinx.coroutines.CancellationException) { throw canceled }
            catch (error: Exception) { loadState.value = EditorLoadState.Failed(error.message.orEmpty()) }
        }
    }

    suspend fun observe(updates: Flow<SubscriptionUpdate<*>>) {
        updates.collect { update ->
            when (update) {
                is SubscriptionUpdate.Failed -> loadState.value = EditorLoadState.Failed(update.error.message.orEmpty())
                else -> loadAsync()
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launchSafe {
            lock.withLock {
                projects.deleteAsync(id)
                _itemsFlow.update { it.filter { item -> item.id.value != id } }
            }
        }
    }

    fun updateItem(item: ImageEditorProject) {
        _itemsFlow.update {
            val index = it.indexOfFirst { i -> i.id.value == item.id }
            if (index != -1) {
                it.toMutableList().also { list -> list[index] = item.toSummary() }
            } else {
                listOf(item.toSummary()) + it
            }
        }
    }

}
