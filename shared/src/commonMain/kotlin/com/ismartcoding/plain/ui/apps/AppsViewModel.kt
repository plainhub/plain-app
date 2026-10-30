package com.ismartcoding.plain.ui.apps

import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.state.ISearchableViewModel
import com.ismartcoding.plain.ui.state.VTabData

import com.ismartcoding.plain.models.Package as AppPackage

import com.ismartcoding.plain.i18n.*

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.helpers.SearchHelper
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import com.ismartcoding.plain.platform.system.PackageType
import com.ismartcoding.plain.features.files.FileSortBy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class AppsViewModel internal constructor(val source: ContentSource = ContentSource.Local) : ISearchableViewModel<AppPackage>, ViewModel() {
    private val _itemsFlow = MutableStateFlow<List<AppPackage>>(emptyList())
    val itemsFlow: StateFlow<List<AppPackage>> = _itemsFlow
    val hasPermission = mutableStateOf(source is ContentSource.Local || source is ContentSource.Remote && com.ismartcoding.plain.platform.permissions.Permission.QUERY_ALL_PACKAGES in source.info.permissions)
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
        val items = source.packages.search(getQuery(), limit.intValue, offset.intValue, sortBy.value)
        _itemsFlow.update { it + items }
        showLoading.value = false
        noMore.value = items.size < limit.intValue
    }

    suspend fun loadAsync() = withIO {
        offset.intValue = 0
        _itemsFlow.value = source.packages.search(getQuery(), limit.intValue, 0, sortBy.value)
        total.intValue = source.packages.count(SearchHelper.buildTextFilter(queryText.value))
        totalSystem.intValue = source.packages.count("${SearchHelper.buildTextFilter(queryText.value)} type:${PackageType.SYSTEM.name}")
        noMore.value = _itemsFlow.value.size < limit.intValue
        tabs.value = listOf(
            VTabData(LocaleHelper.getStringAsync(Res.string.all), "", total.intValue),
            VTabData(LocaleHelper.getStringAsync(Res.string.app_type_system), PackageType.SYSTEM.name, totalSystem.intValue),
            VTabData(LocaleHelper.getStringAsync(Res.string.app_type_user), PackageType.USER.name, total.intValue - totalSystem.intValue)
        )
        showLoading.value = false
    }

    private fun getQuery(): String {
        var query = SearchHelper.buildTextFilter(queryText.value)
        if (appType.value.isNotEmpty()) {
            query += " type:${appType.value}"
        }
        return query
    }
}
