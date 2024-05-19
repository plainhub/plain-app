package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.extensions.isUrl
import com.ismartcoding.lib.helpers.ValidateHelper
import com.ismartcoding.lib.rss.model.RssChannel
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.features.feed.FeedEntryHelper
import com.ismartcoding.plain.features.feed.FeedHelper
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.workers.FeedFetchWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi::class)
class FeedsViewModel(private val savedStateHandle: SavedStateHandle) : ISelectableViewModel<DFeed>, ViewModel() {
    private val _itemsFlow = MutableStateFlow(mutableStateListOf<DFeed>())
    override val itemsFlow: StateFlow<List<DFeed>> get() = _itemsFlow
    var showLoading = mutableStateOf(true)
    var showAddDialog = mutableStateOf(false)
    var showEditDialog = mutableStateOf(false)
    var selectedItem = mutableStateOf<DFeed?>(null)
    private var editId = mutableStateOf("")
    var editUrl = mutableStateOf("")
    var editName = mutableStateOf("")
    var editFetchContent = mutableStateOf(false)
    var editUrlError = mutableStateOf("")
    var rssChannel = mutableStateOf<RssChannel?>(null)

    override var selectMode = mutableStateOf(false)
    override val selectedIds = mutableStateListOf<String>()

    fun loadAsync(withCount: Boolean = false) {
        val countMap = if (withCount) {
            FeedHelper.getFeedCounts().associate { it.id to it.count }
        } else {
            emptyMap()
        }
        _itemsFlow.value = FeedHelper.getAll().map {
            it.count = countMap[it.id] ?: 0
            it
        }.toMutableStateList()
        showLoading.value = false
    }

    fun add() {
        editUrlError.value = ""
        viewModelScope.launch(Dispatchers.IO) {
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
            editUrlError.value = getString(R.string.invalid_url)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            if (FeedHelper.getByUrl(editUrl.value) != null) {
                editUrlError.value = getString(R.string.already_added)
                return@launch
            }
            try {
                rssChannel.value = FeedHelper.fetchAsync(editUrl.value)
                rssChannel.value?.let {
                    editName.value = it.title ?: ""
                }
            } catch (e: Exception) {
                editUrlError.value = e.message ?: getString(R.string.error)
            }
        }
    }

    fun updateFetchContent(id: String, value: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            FeedHelper.updateAsync(id) {
                this.fetchContent = value
            }
        }
    }

    fun edit() {
        editUrlError.value = ""
        if (!editUrl.value.isUrl()) {
            editUrlError.value = getString(R.string.invalid_url)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val a = FeedHelper.getByUrl(editUrl.value)
            if (a != null && a.id != editId.value) {
                editUrlError.value = getString(R.string.already_added)
                return@launch
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

    fun delete(ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val entryIds = FeedEntryHelper.feedEntryDao.getIds(ids)
            if (entryIds.isNotEmpty()) {
                TagHelper.deleteTagRelationByKeys(entryIds.toSet(), DataType.FEED_ENTRY)
                FeedEntryHelper.feedEntryDao.deleteByFeedIds(ids)
            }
            FeedHelper.deleteAsync(ids)
            _itemsFlow.update {
                it.toMutableStateList().apply {
                    removeIf { i -> ids.contains(i.id) }
                }
            }
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
