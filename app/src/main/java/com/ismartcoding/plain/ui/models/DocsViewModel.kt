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
import com.ismartcoding.lib.extensions.scanFileByConnection
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.FileType
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.media.FileMediaStoreHelper
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

@OptIn(SavedStateHandleSaveableApi::class)
class DocsViewModel(private val savedStateHandle: SavedStateHandle) :
    ISelectableViewModel<DFile>,
    ISearchableViewModel<DFile>,
    ViewModel() {
    private val _itemsFlow = MutableStateFlow(mutableStateListOf<DFile>())
    override val itemsFlow: StateFlow<List<DFile>> get() = _itemsFlow
    val showLoading = mutableStateOf(true)
    val offset = mutableIntStateOf(0)
    val limit = mutableIntStateOf(1000)
    val noMore = mutableStateOf(false)
    var total = mutableIntStateOf(0)
    val sortBy = mutableStateOf(FileSortBy.DATE_DESC)
    val selectedItem = mutableStateOf<DFile?>(null)
    val showRenameDialog = mutableStateOf(false)
    val showSortDialog = mutableStateOf(false)
    val fileType = mutableStateOf("")
    var tabs = mutableStateOf(listOf<VTabData>())

    override val showSearchBar = mutableStateOf(false)
    override val searchActive = mutableStateOf(false)
    override val queryText = mutableStateOf("")

    override var selectMode = mutableStateOf(false)
    override val selectedIds = mutableStateListOf<String>()

    fun moreAsync(context: Context) {
        val query = getQuery()
        offset.value += limit.intValue
        val items = FileMediaStoreHelper.getAllByFileTypeAsync(context, MediaStore.VOLUME_EXTERNAL_PRIMARY, FileType.DOCUMENT, sortBy.value)
            .filter { query.isEmpty() || it.name.contains(query) }.drop(offset.intValue).take(limit.intValue)
        _itemsFlow.value.addAll(items)
        showLoading.value = false
        noMore.value = items.size < limit.intValue
    }

    fun loadAsync(context: Context) {
        val query = getQuery()
        offset.intValue = 0
        val items = FileMediaStoreHelper.getAllByFileTypeAsync(context, MediaStore.VOLUME_EXTERNAL_PRIMARY, FileType.DOCUMENT, sortBy.value)
            .filter { query.isEmpty() || it.name.contains(query) }
        _itemsFlow.value = items.take(limit.intValue).toMutableStateList()
        total.intValue = items.size
        noMore.value = _itemsFlow.value.size < limit.intValue
        val extensions = items
            .groupBy { it.extension }.map { VTabData(it.key.uppercase(), it.key, it.value.size) }
            .sortedBy { it.title }
        tabs.value = listOf(VTabData(getString(R.string.all), "", total.intValue), *extensions.toTypedArray())
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

    private fun getQuery(): String {
        return queryText.value
    }
}