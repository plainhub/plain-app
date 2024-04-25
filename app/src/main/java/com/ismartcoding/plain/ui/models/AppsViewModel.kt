package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.ismartcoding.plain.features.PackageHelper
import com.ismartcoding.plain.features.file.FileSortBy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(SavedStateHandleSaveableApi::class)
class AppsViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _itemsFlow = MutableStateFlow(mutableStateListOf<VPackage>())
    val itemsFlow: StateFlow<List<VPackage>> get() = _itemsFlow
    var showLoading = mutableStateOf(true)
    var offset = mutableIntStateOf(0)
    var limit = mutableIntStateOf(50)
    var noMore = mutableStateOf(false)
    var appType = mutableStateOf("")
    var total = mutableIntStateOf(0)
    var totalSystem = mutableIntStateOf(0)
    var queryText by savedStateHandle.saveable { mutableStateOf("") }
    val showSortDialog = mutableStateOf(false)
    val sortBy = mutableStateOf(FileSortBy.NAME_ASC)

    fun moreAsync() {
        offset.value += limit.value
        val items = PackageHelper.search(getQuery(), limit.value, offset.value, sortBy.value).map { VPackage.from(it) }
        _itemsFlow.value.addAll(items)
        showLoading.value = false
        noMore.value = items.size < limit.value
    }

    fun loadAsync() {
        offset.value = 0
        _itemsFlow.value = PackageHelper.search(getQuery(), limit.value, 0, sortBy.value).map { VPackage.from(it) }.toMutableStateList()
        total.value = PackageHelper.count("")
        totalSystem.value = PackageHelper.count("type:system")
        noMore.value = _itemsFlow.value.size < limit.value
        showLoading.value = false
    }

    private fun getQuery(): String {
        var query = queryText
        if (appType.value.isNotEmpty()) {
            query += " type:${appType.value}"
        }
        return query
    }
}
