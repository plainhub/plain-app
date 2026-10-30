package com.ismartcoding.plain.ui.feeds

import com.ismartcoding.plain.features.feeds.FeedWorkerStatus
import com.ismartcoding.plain.rust.graphql.AppSubscriptions
import com.ismartcoding.plain.rust.graphql.changes
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.state.exitSearchMode
import com.ismartcoding.plain.ui.state.exitSelectMode
import com.ismartcoding.plain.ui.state.launchSafe
import kotlinx.coroutines.CoroutineScope
import com.ismartcoding.plain.lib.JsonHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FeedEntriesPageEffects(
    feedEntriesVM: FeedEntriesViewModel, feedsVM: FeedsViewModel, tagsVM: TagsViewModel,
    feedId: String, scope: CoroutineScope,
    topRefreshLayoutState: RefreshLayoutState,
    onSearch: (String) -> Unit,
) {
    LaunchedEffect(Unit) {
        tagsVM.dataType.value = feedEntriesVM.dataType
        if (!feedEntriesVM.routeFeedIdApplied) {
            feedEntriesVM.routeFeedIdApplied = true
            feedEntriesVM.feedId.value = feedId
        }
        feedsVM.loadAsync(withCount = true)
        scope.launchSafe { feedsVM.syncState.refresh() }
        scope.launch(Dispatchers.IO) { feedEntriesVM.loadAsync(tagsVM) }
    }

    LaunchedEffect(feedEntriesVM, feedsVM, tagsVM) {
        fun refreshEntries() {
            feedsVM.loadAsync(withCount = true)
            feedsVM.refreshSelectedItemAsync()
            scope.launch(Dispatchers.IO) { feedEntriesVM.loadAsync(tagsVM) }
        }
        if (feedsVM.source is com.ismartcoding.plain.rust.graphql.ContentSource.Local) {
            launch { AppSubscriptions.contentChanged.changes().collect { refreshEntries() } }
        } else {
            launch {
                while (true) {
                    feedsVM.syncState.refresh()
                    kotlinx.coroutines.delay(1000)
                }
            }
        }
        val observed = feedsVM.syncState.completed().mapValues { it.value.revision }.toMutableMap()
        snapshotFlow { feedsVM.syncState.completed() }.collect { completed ->
            val latest = completed.values.filter { state ->
                state.revision > (observed[state.feedId] ?: -1L)
            }.maxByOrNull { it.revision }
            completed.forEach { (id, state) -> observed[id] = state.revision }
            if (latest != null) {
                topRefreshLayoutState.setRefreshState(
                    if (latest.status == FeedWorkerStatus.ERROR) RefreshContentState.Failed else RefreshContentState.Finished,
                )
                refreshEntries()
            }
        }
    }

    PBackHandler(enabled = feedEntriesVM.selectMode.value || feedEntriesVM.showSearchBar.value) {
        if (feedEntriesVM.selectMode.value) {
            feedEntriesVM.exitSelectMode()
        } else if (feedEntriesVM.showSearchBar.value) {
            if (!feedEntriesVM.searchActive.value || feedEntriesVM.queryText.value.isEmpty()) {
                feedEntriesVM.exitSearchMode(); onSearch("")
            }
        }
    }
}
