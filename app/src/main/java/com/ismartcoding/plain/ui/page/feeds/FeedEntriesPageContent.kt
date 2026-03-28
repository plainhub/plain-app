package com.ismartcoding.plain.ui.page.feeds

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PScrollableTabRow
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.fastscroll.LazyColumnScrollbar
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshLayoutState
import com.ismartcoding.plain.ui.components.FeedEntryListItem
import com.ismartcoding.plain.ui.models.FeedEntriesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VTabData
import com.ismartcoding.plain.ui.models.select
import com.ismartcoding.plain.ui.nav.Routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FeedEntriesPageContent(
    feedEntriesVM: FeedEntriesViewModel, tagsVM: TagsViewModel,
    navController: NavHostController, scope: CoroutineScope,
    tabs: List<VTabData>, pagerState: PagerState,
    scrollBehavior: TopAppBarScrollBehavior,
    scrollStateMap: MutableMap<Int, LazyListState>,
    topRefreshLayoutState: RefreshLayoutState,
    feedsMap: State<Map<String, DFeed>>,
    tagsState: List<DTag>, tagsMapState: Map<String, List<DTagRelation>>,
    bottomPadding: Float,
) {
    val itemsState by feedEntriesVM.itemsFlow.collectAsState()

    if (!feedEntriesVM.selectMode.value) {
        PScrollableTabRow(selectedTabIndex = pagerState.currentPage, modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, s ->
                PFilterChip(
                    modifier = Modifier.padding(start = if (index == 0) 0.dp else 8.dp),
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.scrollToPage(index) } },
                    label = {
                        if (index < 2) Text(text = s.title + " (" + s.count + ")")
                        else Text(if (feedEntriesVM.feedId.value.isNotEmpty() || feedEntriesVM.queryText.value.isNotEmpty()) s.title else "${s.title} (${s.count})")
                    }
                )
            }
        }
    }
    if (pagerState.pageCount == 0) {
        NoDataColumn(loading = feedEntriesVM.showLoading.value, search = feedEntriesVM.showSearchBar.value)
        return
    }
    HorizontalPager(state = pagerState, userScrollEnabled = false) { index ->
        PullToRefresh(
            refreshLayoutState = topRefreshLayoutState,
            refreshContent = remember { {
                PullToRefreshContent(createText = {
                    when (it) {
                        RefreshContentState.Failed -> stringResource(id = R.string.sync_failed)
                        RefreshContentState.Finished -> stringResource(id = R.string.synced)
                        RefreshContentState.Refreshing -> stringResource(id = R.string.syncing)
                        RefreshContentState.Dragging -> {
                            if (abs(getRefreshContentOffset()) < getRefreshContentThreshold())
                                stringResource(if (feedEntriesVM.feedId.value.isNotEmpty()) R.string.pull_down_to_sync_current_feed else R.string.pull_down_to_sync_all_feeds)
                            else stringResource(if (feedEntriesVM.feedId.value.isNotEmpty()) R.string.release_to_sync_current_feed else R.string.release_to_sync_all_feeds)
                        }
                    }
                })
            } },
        ) {
            AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                if (itemsState.isNotEmpty()) {
                    val scrollState = rememberLazyListState()
                    scrollStateMap[index] = scrollState
                    LazyColumnScrollbar(state = scrollState) {
                        LazyColumn(Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection), state = scrollState) {
                            item(key = "top") { TopSpace() }
                            itemsIndexed(itemsState, key = { _, m -> m.id }) { idx, m ->
                                val tagIds = tagsMapState[m.id]?.map { it.tagId } ?: emptyList()
                                FeedEntryListItem(feedEntriesVM, idx, m, feedsMap.value[m.feedId], tagsState.filter { tagIds.contains(it.id) },
                                    onClick = { if (feedEntriesVM.selectMode.value) feedEntriesVM.select(m.id) else navController.navigate(Routing.FeedEntry(m.id)) },
                                    onLongClick = { if (!feedEntriesVM.selectMode.value) feedEntriesVM.selectedItem.value = m },
                                    onClickTag = { tag -> if (!feedEntriesVM.selectMode.value) { val i = tabs.indexOfFirst { it.value == tag.id }; if (i != -1) scope.launch { pagerState.scrollToPage(i) } } }
                                )
                                VerticalSpace(dp = 8.dp)
                            }
                            item(key = "bottom") {
                                if (itemsState.isNotEmpty() && !feedEntriesVM.noMore.value) {
                                    LaunchedEffect(Unit) { scope.launch(Dispatchers.IO) { withIO { feedEntriesVM.moreAsync(tagsVM) } } }
                                }
                                LoadMoreRefreshContent(feedEntriesVM.noMore.value)
                                VerticalSpace(dp = bottomPadding.dp)
                            }
                        }
                    }
                } else {
                    NoDataColumn(loading = feedEntriesVM.showLoading.value, search = feedEntriesVM.showSearchBar.value)
                }
            }
        }
    }
}
