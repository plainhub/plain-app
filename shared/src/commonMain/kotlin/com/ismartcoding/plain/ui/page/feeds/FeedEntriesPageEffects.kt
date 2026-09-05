package com.ismartcoding.plain.ui.page.feeds

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.events.FeedStatusEvent
import com.ismartcoding.plain.features.feed.FeedWorkerStatus
import com.ismartcoding.plain.features.feed.FeedWorkerState
import com.ismartcoding.plain.platform.IODispatcher
import com.ismartcoding.plain.platform.PBackHandler
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshLayoutState
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FeedEntriesViewModel
import com.ismartcoding.plain.ui.models.FeedsViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.exitSearchMode
import com.ismartcoding.plain.ui.models.exitSelectMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
        scope.launch(IODispatcher) { feedEntriesVM.loadAsync(tagsVM) }
    }

    LaunchedEffect(Channel.sharedFlow) {
        Channel.sharedFlow.collect { event ->
            if (event is FeedStatusEvent) {
                if (event.status == FeedWorkerStatus.COMPLETED) {
                    feedsVM.loadAsync(withCount = true)
                    scope.launch(IODispatcher) { feedEntriesVM.loadAsync(tagsVM) }
                    topRefreshLayoutState.setRefreshState(RefreshContentState.Finished)
                } else if (event.status == FeedWorkerStatus.ERROR) {
                    topRefreshLayoutState.setRefreshState(RefreshContentState.Failed)
                    if (feedId.isNotEmpty()) {
                        if (FeedWorkerState.statusMap[feedId] == FeedWorkerStatus.ERROR) {
                            DialogHelper.showErrorDialog(FeedWorkerState.errorMap[feedId] ?: "")
                        }
                    } else {
                        DialogHelper.showErrorDialog(FeedWorkerState.errorMap.values.joinToString("\n"))
                    }
                }
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
