package com.ismartcoding.plain.ui.images.editor


import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.ui.state.launchSafe
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.features.images.editor.ImageEditorProjectsClient
import com.ismartcoding.plain.models.ImageEditorProjectSummary
import com.ismartcoding.plain.lib.withIO
import kotlinx.coroutines.ensureActive
import com.ismartcoding.plain.rust.graphql.SubscriptionUpdate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ImageEditorViewModel(private val projects: ImageEditorProjectsClient) : ViewModel() {
    private val _itemsFlow = MutableStateFlow<List<ImageEditorProjectSummary>>(emptyList())
    val itemsFlow: StateFlow<List<ImageEditorProjectSummary>> = _itemsFlow

    val loadState = mutableStateOf<EditorLoadState>(EditorLoadState.Loading)

    private val lock = Mutex()

    suspend fun loadAsync() = withIO {
        lock.withLock {
            try {
                val rows = projects.listAsync()
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                _itemsFlow.value = rows
                loadState.value = EditorLoadState.Ready
            } catch (canceled: kotlinx.coroutines.CancellationException) { throw canceled }
            catch (error: Exception) { kotlinx.coroutines.currentCoroutineContext().ensureActive(); loadState.value = EditorLoadState.Failed(error.message.orEmpty()) }
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launchSafe { withIO { projects.deleteAsync(id) } }
    }

    suspend fun observe(updates: Flow<SubscriptionUpdate<*>>) {
        updates.collect { update ->
            when (update) {
                is SubscriptionUpdate.Failed -> loadState.value = EditorLoadState.Failed(update.error.message.orEmpty())
                else -> loadAsync()
            }
        }
    }

}
