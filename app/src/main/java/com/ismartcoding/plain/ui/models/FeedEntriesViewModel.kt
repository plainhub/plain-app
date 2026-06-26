package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.enums.FeedEntryFilterType
import com.ismartcoding.plain.db.DFeedEntry
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.features.feed.FeedEntryHelper
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.workers.FeedFetchWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class FeedEntriesViewModel :
    ISelectableViewModel<DFeedEntry>,
    ISearchableViewModel<DFeedEntry>,
    ViewModel() {
    private val _itemsFlow = MutableStateFlow<List<DFeedEntry>>(emptyList())
    override val itemsFlow: StateFlow<List<DFeedEntry>> = _itemsFlow
    var showLoading = mutableStateOf(true)
    var offset = mutableIntStateOf(0)
    var limit = mutableIntStateOf(200)
    var noMore = mutableStateOf(false)
    var filterType = mutableStateOf(FeedEntryFilterType.DEFAULT)
    var total = mutableIntStateOf(0)
    var totalToday = mutableIntStateOf(0)
    var tag = mutableStateOf<DTag?>(null)
    var feedId = mutableStateOf<String>("")
    val dataType = DataType.FEED_ENTRY
    var selectedItem = mutableStateOf<DFeedEntry?>(null)
    val showTagsDialog = mutableStateOf(false)

    override val showSearchBar = mutableStateOf(false)
    override val searchActive = mutableStateOf(false)
    override val queryText = mutableStateOf("")

    override var selectMode = mutableStateOf(false)
    override val selectedIds = mutableStateListOf<String>()

    suspend fun moreAsync(tagsViewModel: TagsViewModel) = withIO {
        offset.value += limit.intValue
        val items = FeedEntryHelper.search(getQuery(), limit.intValue, offset.intValue)
        _itemsFlow.update { it + items }
        tagsViewModel.loadMoreAsync(items.map { it.id }.toSet())
        showLoading.value = false
        noMore.value = items.size < limit.intValue
    }

    suspend fun loadAsync(tagsViewModel: TagsViewModel) = withIO {
        offset.intValue = 0
        val query = getQuery()
        _itemsFlow.value = FeedEntryHelper.search(query, limit.intValue, offset.intValue)
        tagsViewModel.loadAsync(_itemsFlow.value.map { it.id }.toSet())
        total.intValue = FeedEntryHelper.count(getTotalAllQuery())
        totalToday.intValue = FeedEntryHelper.count(getTotalTodayQuery())
        noMore.value = _itemsFlow.value.size < limit.intValue
        showLoading.value = false
    }

    fun sync() {
        FeedFetchWorker.oneTimeRequest(feedId.value)
    }

    fun delete(tagsVM: TagsViewModel, ids: Set<String>) {
        launchSafe {
            TagHelper.deleteTagRelationByKeys(
                ids,
                dataType,
            )
            FeedEntryHelper.deleteAsync(ids)
            loadAsync(tagsVM)
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

    private suspend fun getQuery(): String {
        var query = queryText.value
        if (filterType.value == FeedEntryFilterType.TODAY) {
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
