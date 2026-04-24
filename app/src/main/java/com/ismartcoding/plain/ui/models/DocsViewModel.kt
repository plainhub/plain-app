package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.R
import com.ismartcoding.plain.docs.DDoc
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.docs.DocMediaStoreHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DocsViewModel : BaseMediaViewModel<DDoc>() {
    override val dataType = DataType.DOC
    val scrollStateMap = mutableStateMapOf<Int, LazyListState>()
    val fileType = mutableStateOf("")
    var tabsShowTags = mutableStateOf(false)
    var tabs = mutableStateOf(listOf<VTabData>())

    override fun getQuery(): String {
        val query = super.getQuery()
        if (!tabsShowTags.value && fileType.value.isNotEmpty()) {
            return "$query ext:${fileType.value}"
        }
        return query
    }

    override suspend fun loadAsync(context: Context, tagsVM: TagsViewModel) {
        offset.intValue = 0
        _itemsFlow.value = searchMediaAsync(context, getQuery()).toMutableStateList()
        tagsVM.loadAsync(_itemsFlow.value.map { it.id }.toSet())
        total.intValue = countMediaAsync(context, getTotalQuery())
        totalTrash.intValue = countMediaAsync(context, getTrashQuery())
        noMore.value = _itemsFlow.value.size < limit.intValue
        if (!trash.value) {
            val extGroups = DocMediaStoreHelper.getDocExtGroupsAsync(context, super.getQuery())
            val trashTabs = if (AppFeatureType.MEDIA_TRASH.has()) listOf(VTabData(getString(R.string.trash), "trash", totalTrash.intValue)) else emptyList()
            if (tabsShowTags.value) {
                val tagsState = tagsVM.itemsFlow.value
                tabs.value = listOf(VTabData(getString(R.string.all), "all", total.intValue)) + trashTabs + tagsState.map { VTabData(it.name, it.id, it.count) }
            } else {
                val extensions = extGroups.map { VTabData(it.first, it.first.lowercase(), it.second) }
                tabs.value = listOf(VTabData(getString(R.string.all), "", total.intValue)) + trashTabs + extensions
            }
        }
        showLoading.value = false
    }

    fun delete(context: Context, tagsVM: TagsViewModel, ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            DialogHelper.showLoading()
            TagHelper.deleteTagRelationByKeys(ids, dataType)
            DocMediaStoreHelper.deleteRecordsAndFilesByIdsAsync(context, ids, trash.value)
            loadAsync(context, tagsVM)
            DialogHelper.hideLoading()
        }
    }
}
