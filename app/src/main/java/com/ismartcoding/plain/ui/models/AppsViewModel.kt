package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.features.pkg.PackageHelper
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

class AppsViewModel : ViewModel() {
    private val _itemsFlow = MutableStateFlow(mutableStateListOf<VPackage>())
    val itemsFlow: StateFlow<List<VPackage>> get() = _itemsFlow
    var showLoading = mutableStateOf(true)
    var offset = mutableIntStateOf(0)
    var limit = mutableIntStateOf(50)
    var noMore = mutableStateOf(false)

    fun moreAsync(query: String = "") {
        offset.value += limit.value
        val items = PackageHelper.search(query, limit.value, offset.value).map { VPackage.from(it) }
        _itemsFlow.value.addAll(items)
        showLoading.value = false
        noMore.value = items.size < limit.value
    }

    suspend fun loadAsync(query: String = "") {
        offset.value = 0
        _itemsFlow.value = PackageHelper.search(query, limit.value, 0).map { VPackage.from(it) }.toMutableStateList()
        noMore.value = _itemsFlow.value.size < limit.value
        showLoading.value = false
    }

    fun uninstall(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
        }
    }
}
