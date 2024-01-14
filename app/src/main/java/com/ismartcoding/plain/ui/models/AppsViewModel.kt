package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.features.pkg.PackageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppsViewModel : ViewModel() {
    private val _itemsFlow = MutableStateFlow(mutableStateListOf<VPackage>())
    val itemsFlow: StateFlow<List<VPackage>> get() = _itemsFlow
    var showLoading = mutableStateOf(true)
    var offset = mutableIntStateOf(0)
    var limit = mutableIntStateOf(50)
    var noMore = mutableStateOf(false)

    fun loadMore() {
        viewModelScope.launch(Dispatchers.IO) {
            offset.value += limit.value
            val items = PackageHelper.search("", limit.value, offset.value).map { VPackage.from(it) }
            _itemsFlow.value.addAll(items)
            showLoading.value = false
            noMore.value = items.size < limit.value
        }
    }

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            offset.value = 0
            _itemsFlow.value = PackageHelper.search("", limit.value, 0).map { VPackage.from(it) }.toMutableStateList()
            showLoading.value = false
            noMore.value = _itemsFlow.value.size < limit.value
        }
    }

    fun uninstall(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
        }
    }
}
