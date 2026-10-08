package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.db.DImageEditorProject
import com.ismartcoding.plain.features.ImageEditorProjectHelper
import com.ismartcoding.plain.features.imageeditor.ImageEditorProjectSummary
import com.ismartcoding.plain.features.imageeditor.toSummary
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.ismartcoding.plain.lib.withIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class ImageEditorViewModel : ViewModel() {
    private val _itemsFlow = MutableStateFlow<List<ImageEditorProjectSummary>>(emptyList())
    val itemsFlow: StateFlow<List<ImageEditorProjectSummary>> = _itemsFlow

    var showLoading = mutableStateOf(true)
    var selectedItem = mutableStateOf<ImageEditorProjectSummary?>(null)

    private val lock = Mutex()

    suspend fun loadAsync() = withIO {
        lock.withLock {
            _itemsFlow.value = ImageEditorProjectHelper.listAsync(LIST_LIMIT)
            showLoading.value = false
        }
    }

    fun delete(id: String) {
        viewModelScope.launchSafe {
            lock.withLock {
                ImageEditorProjectHelper.deleteAsync(id)
                _itemsFlow.update { it.filter { item -> item.id.value != id } }
            }
        }
    }

    fun updateItem(item: DImageEditorProject) {
        _itemsFlow.update {
            val index = it.indexOfFirst { i -> i.id.value == item.id }
            if (index != -1) {
                it.toMutableList().also { list -> list[index] = item.toSummary() }
            } else {
                listOf(item.toSummary()) + it
            }
        }
    }

    companion object {
        private const val LIST_LIMIT = 50
    }
}
