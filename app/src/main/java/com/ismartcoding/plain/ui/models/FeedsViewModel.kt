package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.rss.model.RssChannel
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.features.feed.FeedEntryHelper
import com.ismartcoding.plain.features.feed.FeedHelper
import com.ismartcoding.plain.features.TagHelper
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
    internal var editId = mutableStateOf("")
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

    fun updateFetchContent(id: String, value: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            FeedHelper.updateAsync(id) {
                this.fetchContent = value
            }
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
}
