package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.features.locale.LocaleHelper

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.features.PackageHelper
import com.ismartcoding.plain.features.file.FileSortBy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class AppsViewModel : ISearchableViewModel<VPackage>, ViewModel() {
    private val _itemsFlow = MutableStateFlow<List<VPackage>>(emptyList())
    val itemsFlow: StateFlow<List<VPackage>> = _itemsFlow
    var showLoading = mutableStateOf(true)
    var offset = mutableIntStateOf(0)
    var limit = mutableIntStateOf(50)
    var noMore = mutableStateOf(false)
    var appType = mutableStateOf("")
    var total = mutableIntStateOf(0)
    var totalSystem = mutableIntStateOf(0)
    val showSortDialog = mutableStateOf(false)
    val sortBy = mutableStateOf(FileSortBy.NAME_ASC)
    var tabs = mutableStateOf(listOf<VTabData>())

    override val showSearchBar = mutableStateOf(false)
    override val searchActive = mutableStateOf(false)
    override val queryText = mutableStateOf("")

    suspend fun moreAsync() = withIO {
        offset.value += limit.intValue
        val items = PackageHelper.searchAsync(getQuery(), limit.intValue, offset.intValue, sortBy.value).map { VPackage.from(it) }
        _itemsFlow.update { it + items }
        showLoading.value = false
        noMore.value = items.size < limit.intValue
    }

    suspend fun loadAsync() = withIO {
        offset.intValue = 0
        _itemsFlow.value = PackageHelper.searchAsync(getQuery(), limit.intValue, 0, sortBy.value).map { VPackage.from(it) }
        total.intValue = PackageHelper.count(queryText.value)
        totalSystem.intValue = PackageHelper.count("${queryText.value} type:system")
        noMore.value = _itemsFlow.value.size < limit.intValue
        tabs.value = listOf(
            VTabData(LocaleHelper.getStringAsync(Res.string.all), "", total.intValue),
            VTabData(LocaleHelper.getStringAsync(Res.string.app_type_system), "system", totalSystem.intValue),
            VTabData(LocaleHelper.getStringAsync(Res.string.app_type_user), "user", total.intValue - totalSystem.intValue)
        )
        showLoading.value = false
    }

    private fun getQuery(): String {
        var query = queryText.value
        if (appType.value.isNotEmpty()) {
            query += " type:${appType.value}"
        }
        return query
    }
}
