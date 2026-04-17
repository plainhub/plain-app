package com.ismartcoding.plain.ui.models

import android.content.Context
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
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.features.media.DocsHelper
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

    suspend fun moreAsync(context: Context) {
        val query = buildQuery()
        offset.value += limit.intValue
        val items = DocsHelper.searchAsync(context, query, limit.intValue, offset.intValue, sortBy.value)
        _itemsFlow.value.addAll(items)
        showLoading.value = false
        noMore.value = items.size < limit.intValue
    }

    suspend fun loadAsync(context: Context) {
        val query = buildQuery()
        offset.intValue = 0
        val items = DocsHelper.searchAsync(context, query, limit.intValue, 0, sortBy.value)
        _itemsFlow.value = items.toMutableStateList()
        noMore.value = items.size < limit.intValue
        val extGroups = DocsHelper.getDocExtGroupsAsync(context, query)
        total.intValue = extGroups.sumOf { it.second }
        val extensions = extGroups.map { VTabData(it.first, it.first.lowercase(), it.second) }
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

    private fun buildQuery(): String {
        val text = queryText.value.trim()
        return if (text.isEmpty()) "" else "text:$text"
    }
}
