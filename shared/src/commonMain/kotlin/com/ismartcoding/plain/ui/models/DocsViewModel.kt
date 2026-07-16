package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.LocaleHelper

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.docs.DDoc
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.enums.has
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.platform.deleteMedia
import com.ismartcoding.plain.platform.getDocExtGroups
import com.ismartcoding.plain.ui.helpers.DialogHelper

class DocsViewModel : BaseMediaViewModel<DDoc>() {
    override val dataType = DataType.DOC
    val scrollStateMap = mutableStateMapOf<Int, LazyListState>()
    val fileType = mutableStateOf("")
    var tabsShowTags = mutableStateOf(false)
    var tabs = mutableStateOf(listOf<VTabData>())

    override suspend fun getQuery(): String {
        val query = super.getQuery()
        if (!tabsShowTags.value && fileType.value.isNotEmpty()) {
            return "$query ext:${fileType.value}"
        }
        return query
    }

    override suspend fun loadAsync(tagsVM: TagsViewModel) = withIO {
        offset.intValue = 0
        _itemsFlow.value = searchMediaAsync(getQuery())
        tagsVM.loadAsync(_itemsFlow.value.map { it.id }.toSet())
        total.intValue = countMediaAsync(getTotalQuery())
        totalTrash.intValue = countMediaAsync(getTrashQuery())
        noMore.value = _itemsFlow.value.size < limit.intValue
        if (!trash.value) {
            val extGroups = getDocExtGroups(super.getQuery())
            val trashTabs = if (AppFeatureType.MEDIA_TRASH.has()) listOf(VTabData(LocaleHelper.getStringAsync(Res.string.trash), "trash", totalTrash.intValue)) else emptyList()
            if (tabsShowTags.value) {
                val tagsState = tagsVM.itemsFlow.value
                tabs.value = listOf(VTabData(LocaleHelper.getStringAsync(Res.string.all), "all", total.intValue)) + trashTabs + tagsState.map { VTabData(it.name, it.id, it.count) }
            } else {
                val extensions = extGroups.map { VTabData(it.first, it.first.lowercase(), it.second) }
                tabs.value = listOf(VTabData(LocaleHelper.getStringAsync(Res.string.all), "", total.intValue)) + trashTabs + extensions
            }
        }
        showLoading.value = false
    }

    override fun delete(tagsVM: TagsViewModel, ids: Set<String>) {
        launchSafe {
            DialogHelper.showLoading()
            TagHelper.deleteTagRelationByKeys(ids, dataType)
            deleteMedia(dataType, ids, trash.value)
            loadAsync(tagsVM)
            DialogHelper.hideLoading()
        }
    }
}
