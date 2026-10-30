package com.ismartcoding.plain.ui.feeds

import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.state.ISearchableViewModel
import com.ismartcoding.plain.ui.state.ISelectableViewModel
import com.ismartcoding.plain.ui.state.launchSafe

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.features.feeds.FeedEntryFilterType
import com.ismartcoding.plain.models.FeedEntry
import com.ismartcoding.plain.models.Tag
import com.ismartcoding.plain.helpers.SearchHelper
import com.ismartcoding.plain.lib.withIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class FeedEntriesViewModel internal constructor(val source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.rust.graphql.ContentSource.Local) :
    ISelectableViewModel<FeedEntry>,
    ISearchableViewModel<FeedEntry>,
    ViewModel() {
    private val _itemsFlow = MutableStateFlow<List<FeedEntry>>(emptyList())
    override val itemsFlow: StateFlow<List<FeedEntry>> = _itemsFlow
    var showLoading = mutableStateOf(true)
    var offset = mutableIntStateOf(0)
    var limit = mutableIntStateOf(200)
    var noMore = mutableStateOf(false)
    var filterType = mutableStateOf(FeedEntryFilterType.DEFAULT)
    var total = mutableIntStateOf(0)
    var totalToday = mutableIntStateOf(0)
    var tag = mutableStateOf<Tag?>(null)
    var feedId = mutableStateOf<String>("")
    val dataType = DataType.FEED_ENTRY
    var selectedItem = mutableStateOf<FeedEntry?>(null)
    val showTagsDialog = mutableStateOf(false)

    // Cluster key ("date/feedId" from buildFeedListRows) -> explicit expanded/collapsed
    // choice. Absent = default (collapsed once the cluster has no unread entries).
    val clusterExpandedOverrides = mutableStateMapOf<String, Boolean>()

    // Discovery (first-run catalog) page state: null until the feed list has
    // loaded once, then true = show the catalog, false = normal list. The page
    // sets it back to true whenever the subscription count drops to zero, and
    // to false only when the user explicitly taps "start reading" — data
    // changes like articles syncing in must never flip the page.
    var discoveryMode = mutableStateOf<Boolean?>(null)

    // The feed filter coming from the navigation route must be applied exactly
    // once per VM lifetime; LaunchedEffect(Unit) reruns whenever the page
    // re-enters composition (e.g. back from FeedEntryPage) and must not reset
    // a filter the user picked in the drawer.
    var routeFeedIdApplied = false

    override val showSearchBar = mutableStateOf(false)
    override val searchActive = mutableStateOf(false)
    override val queryText = mutableStateOf("")

    override var selectMode = mutableStateOf(false)
    override val selectedIds = mutableStateListOf<String>()

    suspend fun moreAsync(tagsViewModel: TagsViewModel) = withIO {
        offset.value += limit.intValue
        val items = source.feedEntries.search(getQuery(), limit.intValue, offset.intValue)
        _itemsFlow.update { it + items }
        tagsViewModel.loadMoreAsync(items.map { it.id }.toSet())
        showLoading.value = false
        noMore.value = items.size < limit.intValue
    }

    suspend fun loadAsync(tagsViewModel: TagsViewModel) = withIO {
        offset.intValue = 0
        val query = getQuery()
        _itemsFlow.value = source.feedEntries.search(query, limit.intValue, offset.intValue)
        tagsViewModel.loadAsync(_itemsFlow.value.map { it.id }.toSet())
        total.intValue = source.feedEntries.count(getTotalAllQuery())
        totalToday.intValue = source.feedEntries.count(getTotalTodayQuery())
        noMore.value = _itemsFlow.value.size < limit.intValue
        showLoading.value = false
    }

    /** Optimistic read-state flip: update the loaded list immediately, persist async. */
    fun markRead(ids: Set<String>, read: Boolean = true) {
        _itemsFlow.update { list -> list.map { if (it.id in ids) it.copy(read = read) else it } }
        viewModelScope.launchSafe { source.feedEntries.markReadAsync(ids, read) }
    }

    fun sync() {
        viewModelScope.launchSafe { source.feeds.sync(feedId.value) }
    }

    fun delete(tagsVM: TagsViewModel, ids: Set<String>) {
        viewModelScope.launchSafe {
            source.feedEntries.deleteAsync(ids)
            loadAsync(tagsVM)
        }
    }

    private fun getTotalAllQuery(): String {
        var query = SearchHelper.buildTextFilter(queryText.value)
        if (feedId.value.isNotEmpty()) {
            query += " feed_id:${feedId.value}"
        }

        return query
    }

    private fun getTotalTodayQuery(): String {
        var query = "${SearchHelper.buildTextFilter(queryText.value)} today:true"
        if (feedId.value.isNotEmpty()) {
            query += " feed_id:${feedId.value}"
        }

        return query
    }

    private suspend fun getQuery(): String {
        var query = SearchHelper.buildTextFilter(queryText.value)
        if (filterType.value == FeedEntryFilterType.TODAY) {
            query += " today:true"
        }
        if (tag.value != null) {
            val tagId = tag.value!!.id
            val ids = source.tags.keys(tagId)
            query += " ids:${ids.joinToString(",")}"
        }
        if (feedId.value.isNotEmpty()) {
            query += " feed_id:${feedId.value}"
        }

        return query
    }
}
