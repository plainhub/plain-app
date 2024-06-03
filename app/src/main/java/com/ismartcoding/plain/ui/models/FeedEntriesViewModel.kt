package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.saveable
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.enums.FeedEntryFilterType
import com.ismartcoding.plain.db.DFeedEntry
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.features.feed.FeedEntryHelper
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.workers.FeedFetchWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi::class)
class FeedEntriesViewModel(private val savedStateHandle: SavedStateHandle) :
    ISelectableViewModel<DFeedEntry>,
    ISearchableViewModel<DFeedEntry>,
    ViewModel() {
    private val _itemsFlow = MutableStateFlow(mutableStateListOf<DFeedEntry>())
    override val itemsFlow: StateFlow<List<DFeedEntry>> get() = _itemsFlow
    var showLoading = mutableStateOf(true)
    var offset = mutableIntStateOf(0)
    var limit = mutableIntStateOf(200)
    var noMore = mutableStateOf(false)
    var filterType by savedStateHandle.saveable { mutableStateOf(FeedEntryFilterType.DEFAULT) }
    var total = mutableIntStateOf(0)
    private var totalToday = mutableIntStateOf(0)
    var tag = mutableStateOf<DTag?>(null)
    var feedId = mutableStateOf<String>("")
    val dataType = DataType.FEED_ENTRY
    var selectedItem = mutableStateOf<DFeedEntry?>(null)
    var tabs = mutableStateOf(listOf<VTabData>())

    override val showSearchBar = mutableStateOf(false)
    override val searchActive = mutableStateOf(false)
    override val queryText = mutableStateOf("")

    override var selectMode = mutableStateOf(false)
    override val selectedIds = mutableStateListOf<String>()

    fun moreAsync(tagsViewModel: TagsViewModel) {
        offset.value += limit.value
        val items = FeedEntryHelper.search(getQuery(), limit.value, offset.value)
        _itemsFlow.value.addAll(items)
        tagsViewModel.loadMoreAsync(items.map { it.id }.toSet())
        showLoading.value = false
        noMore.value = items.size < limit.value
    }

    fun loadAsync(tagsViewModel: TagsViewModel) {
        offset.value = 0
        val query = getQuery()
        _itemsFlow.value = FeedEntryHelper.search(query, limit.value, offset.value).toMutableStateList()
        refreshTabsAsync(tagsViewModel)
        noMore.value = _itemsFlow.value.size < limit.value
        showLoading.value = false
    }

    fun sync() {
        FeedFetchWorker.oneTimeRequest(feedId.value)
    }

    // for updating tags, delete items
    fun refreshTabsAsync(tagsViewModel: TagsViewModel) {
        tagsViewModel.loadAsync(_itemsFlow.value.map { it.id }.toSet())
        total.value = FeedEntryHelper.count(getTotalAllQuery())
        totalToday.value = FeedEntryHelper.count(getTotalTodayQuery())
        tabs.value = listOf(
            VTabData(LocaleHelper.getString(R.string.all), "all", total.value),
            VTabData(LocaleHelper.getString(R.string.today), "today", totalToday.value),
            * tagsViewModel.itemsFlow.value.map { VTabData(it.name, it.id, it.count) }.toTypedArray()
        )
    }

    fun delete(tagsViewModel: TagsViewModel, ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            TagHelper.deleteTagRelationByKeys(
                ids,
                dataType,
            )
            FeedEntryHelper.deleteAsync(ids)
            refreshTabsAsync(tagsViewModel)
            _itemsFlow.update {
                val mutableList = it.toMutableStateList()
                mutableList.removeIf { m -> ids.contains(m.id) }
                mutableList
            }
        }
    }

    private fun getTotalAllQuery(): String {
        var query = queryText.value
        if (feedId.value.isNotEmpty()) {
            query += " feed_id:${feedId.value}"
        }

        return query
    }

    private fun getTotalTodayQuery(): String {
        var query = "${queryText.value} today:true"
        if (feedId.value.isNotEmpty()) {
            query += " feed_id:${feedId.value}"
        }

        return query
    }

    private fun getQuery(): String {
        var query = queryText.value
        if (filterType == FeedEntryFilterType.TODAY) {
            query += " today:true"
        }
        if (tag.value != null) {
            val tagId = tag.value!!.id
            val ids = TagHelper.getKeysByTagId(tagId)
            query += " ids:${ids.joinToString(",")}"
        }
        if (feedId.value.isNotEmpty()) {
            query += " feed_id:${feedId.value}"
        }

        return query
    }
}
