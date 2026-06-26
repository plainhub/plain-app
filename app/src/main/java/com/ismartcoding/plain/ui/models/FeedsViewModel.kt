package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.lib.extensions.isUrl
import com.ismartcoding.plain.lib.rss.model.RssChannel
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.features.feed.FeedEntryHelper
import com.ismartcoding.plain.features.feed.FeedHelper
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.already_added
import com.ismartcoding.plain.i18n.error
import com.ismartcoding.plain.i18n.invalid_url
import com.ismartcoding.plain.workers.FeedFetchWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class FeedsViewModel : ISelectableViewModel<DFeed>, ViewModel() {
    private val _itemsFlow = MutableStateFlow<List<DFeed>>(emptyList())
    override val itemsFlow: StateFlow<List<DFeed>> = _itemsFlow
    var showLoading = mutableStateOf(true)
    var showAddDialog = mutableStateOf(false)
    var showEditDialog = mutableStateOf(false)
    var selectedItem = mutableStateOf<DFeed?>(null)
    internal var editId = mutableStateOf("")
    var editUrl = mutableStateOf("")
    var editName = mutableStateOf("")
    var editFetchContent = mutableStateOf(false)
    var editUrlError = mutableStateOf("")
    var rssChannel = mutableStateOf<RssChannel?>(null)

    override var selectMode = mutableStateOf(false)
    override val selectedIds = mutableStateListOf<String>()

    fun loadAsync(withCount: Boolean = false) {
        launchSafe {
            val countMap = if (withCount) {
                FeedHelper.getFeedCounts().associate { it.id to it.count }
            } else {
                emptyMap()
            }
            _itemsFlow.value = FeedHelper.getAll().map {
                it.count = countMap[it.id] ?: 0
                it
            }
            showLoading.value = false
        }
    }

    fun updateFetchContent(id: String, value: Boolean) {
        launchSafe {
            FeedHelper.updateAsync(id) {
                this.fetchContent = value
            }
        }
    }

    fun delete(ids: Set<String>) {
        launchSafe {
            val entryIds = FeedEntryHelper.feedEntryDao.getIds(ids)
            if (entryIds.isNotEmpty()) {
                TagHelper.deleteTagRelationByKeys(entryIds.toSet(), DataType.FEED_ENTRY)
                FeedEntryHelper.feedEntryDao.deleteByFeedIds(ids)
            }
            FeedHelper.deleteAsync(ids)
            _itemsFlow.update { it.filterNot { i -> ids.contains(i.id) } }
        }
    }

    fun add() {
        editUrlError.value = ""
        launchSafe {
            val id = FeedHelper.addAsync {
                this.url = editUrl.value
                this.name = editName.value
                this.fetchContent = editFetchContent.value
            }
            FeedFetchWorker.oneTimeRequest(id)
            loadAsync(withCount = true)
            showAddDialog.value = false
        }
    }

    fun fetchChannel() {
        editUrlError.value = ""
        if (!editUrl.value.isUrl()) {
            editUrlError.value = LocaleHelper.getString(Res.string.invalid_url)
            return
        }
        launchSafe {
            if (FeedHelper.getByUrl(editUrl.value) != null) {
                editUrlError.value = LocaleHelper.getStringAsync(Res.string.already_added)
                return@launchSafe
            }
            try {
                rssChannel.value = FeedHelper.fetchAsync(editUrl.value)
                rssChannel.value?.let {
                    editName.value = it.title ?: ""
                }
            } catch (e: Exception) {
                editUrlError.value = e.message ?: LocaleHelper.getStringAsync(Res.string.error)
            }
        }
    }

    fun edit() {
        editUrlError.value = ""
        if (!editUrl.value.isUrl()) {
            editUrlError.value = LocaleHelper.getString(Res.string.invalid_url)
            return
        }
        launchSafe {
            val a = FeedHelper.getByUrl(editUrl.value)
            if (a != null && a.id != editId.value) {
                editUrlError.value = LocaleHelper.getStringAsync(Res.string.already_added)
                return@launchSafe
            }
            FeedHelper.updateAsync(editId.value) {
                this.name = editName.value
                this.url = editUrl.value
                this.fetchContent = editFetchContent.value
            }
            loadAsync(withCount = true)
            showEditDialog.value = false
        }
    }

    fun showAddDialog() {
        rssChannel.value = null
        editUrlError.value = ""
        editUrl.value = ""
        editName.value = ""
        editFetchContent.value = false
        showAddDialog.value = true
    }

    fun showEditDialog(item: DFeed) {
        editUrlError.value = ""
        editId.value = item.id
        editUrl.value = item.url
        editName.value = item.name
        editFetchContent.value = item.fetchContent
        showEditDialog.value = true
    }

}
