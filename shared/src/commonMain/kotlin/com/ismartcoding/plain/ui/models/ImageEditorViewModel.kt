package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.db.DImageEditorProject
import com.ismartcoding.plain.features.ImageEditorProjectHelper
import com.ismartcoding.plain.lib.withIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class ImageEditorViewModel : ViewModel() {
    private val _itemsFlow = MutableStateFlow<List<DImageEditorProject>>(emptyList())
    val itemsFlow: StateFlow<List<DImageEditorProject>> = _itemsFlow

    var showLoading = mutableStateOf(true)
    var selectedItem = mutableStateOf<DImageEditorProject?>(null)

    suspend fun loadAsync() = withIO {
        _itemsFlow.value = ImageEditorProjectHelper.listAsync(LIST_LIMIT)
        showLoading.value = false
    }

    fun delete(id: String) {
        viewModelScope.launchSafe {
            ImageEditorProjectHelper.deleteAsync(id)
            _itemsFlow.update { it.filter { item -> item.id != id } }
        }
    }

    fun updateItem(item: DImageEditorProject) {
        _itemsFlow.update {
            val index = it.indexOfFirst { i -> i.id == item.id }
            if (index != -1) {
                it.toMutableList().also { list -> list[index] = item }
            } else {
                listOf(item) + it
            }
        }
    }

    companion object {
        private const val LIST_LIMIT = 50
    }
}
