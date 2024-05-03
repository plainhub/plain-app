package com.ismartcoding.plain.ui.models

import android.content.Context
import android.provider.MediaStore
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.ismartcoding.lib.extensions.scanFileByConnection
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.enums.FileType
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.FileMediaStoreHelper
import com.ismartcoding.plain.features.file.FileSortBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class DocsViewModel(private val savedStateHandle: SavedStateHandle) : ISelectableViewModel<DFile>, ViewModel() {
    private val _itemsFlow = MutableStateFlow(mutableStateListOf<DFile>())
    override val itemsFlow: StateFlow<List<DFile>> get() = _itemsFlow
    val showLoading = mutableStateOf(true)
    val offset = mutableIntStateOf(0)
    val limit = mutableIntStateOf(50)
    val noMore = mutableStateOf(false)
    val sortBy = mutableStateOf(FileSortBy.DATE_DESC)
    val selectedItem = mutableStateOf<DFile?>(null)
    val showRenameDialog = mutableStateOf(false)
    val search = mutableStateOf(false)
    val showSortDialog = mutableStateOf(false)

    @OptIn(SavedStateHandleSaveableApi::class)
    var queryText by savedStateHandle.saveable { mutableStateOf("") }

    override var selectMode = mutableStateOf(false)
    override val selectedIds = mutableStateListOf<String>()

    fun moreAsync(context: Context) {
        val query = queryText
        offset.value += limit.value
        val items = FileMediaStoreHelper.getAllByFileType(context, MediaStore.VOLUME_EXTERNAL_PRIMARY, FileType.DOCUMENT, sortBy.value)
            .filter { query.isEmpty() || it.name.contains(query) }
        _itemsFlow.value.addAll(items)
        showLoading.value = false
        noMore.value = items.size < limit.value
    }

    suspend fun loadAsync(context: Context) {
        val query = queryText
        offset.value = 0
        _itemsFlow.value = FileMediaStoreHelper.getAllByFileType(context, MediaStore.VOLUME_EXTERNAL_PRIMARY, FileType.DOCUMENT, sortBy.value)
            .filter { query.isEmpty() || it.name.contains(query) }.toMutableStateList()
        noMore.value = _itemsFlow.value.size < limit.value
        showLoading.value = false
    }

    fun delete(paths: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            paths.forEach {
                File(it).deleteRecursively()
            }
            MainApp.instance.scanFileByConnection(paths.toTypedArray())
            _itemsFlow.update {
                it.toMutableStateList().apply {
                    removeIf { i -> paths.contains(i.path) }
                }
            }
        }
    }
}
