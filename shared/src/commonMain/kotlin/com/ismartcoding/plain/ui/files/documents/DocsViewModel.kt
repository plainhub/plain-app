package com.ismartcoding.plain.ui.files.documents

import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.media.BaseMediaViewModel
import com.ismartcoding.plain.ui.state.VTabData

import com.ismartcoding.plain.models.Doc as Document
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.platform.system.has

class DocsViewModel internal constructor(source: ContentSource = ContentSource.Local) : BaseMediaViewModel<Document>(source) {
    override val dataType = DataType.DOC
    override val showFoldersAsList = true
    val scrollStateMap = mutableStateMapOf<Int, LazyListState>()
    val fileType = mutableStateOf("")
    var tabs = mutableStateOf(listOf<VTabData>())

    fun prepareFile(path: String, onReady: (String) -> Unit) {
        if (source is ContentSource.Local) {
            onReady(path)
        } else {
            viewModelScope.launchSafe {
                DialogHelper.showLoading()
                try { onReady(source.fileForOpen(path)) }
                finally { DialogHelper.hideLoading() }
            }
        }
    }

    override suspend fun getQuery(): String {
        val query = super.getQuery()
        if (fileType.value.isNotEmpty()) {
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
            val extGroups = source.catalog.docExtGroups(super.getQuery())
            val trashTabs = if (AppFeatureType.MEDIA_TRASH.has(source.capabilities)) listOf(VTabData(LocaleHelper.getStringAsync(Res.string.trash), "trash", totalTrash.intValue)) else emptyList()
            val extensions = extGroups.map { VTabData(it.first, it.first.lowercase(), it.second) }
            tabs.value = listOf(VTabData(LocaleHelper.getStringAsync(Res.string.all), "", total.intValue)) + trashTabs + extensions
        }
        showLoading.value = false
    }
}
