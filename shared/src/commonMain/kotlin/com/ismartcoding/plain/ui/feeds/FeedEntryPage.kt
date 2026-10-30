package com.ismartcoding.plain.ui.feeds

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.features.feeds.FeedFontScale
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.platform.media.MediaPreviewer
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.features.feeds.fetchContentAsync
import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.media.viewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.tags.SelectTagsDialog
import kotlin.math.abs
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun FeedEntryPage(
    navController: NavHostController,
    id: String,
    tagsVM: TagsViewModel,
    pagerVM: FeedEntryPagerViewModel,
    source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.ui.navigation.LocalContentSource.current,
    feedEntryVM: FeedEntryViewModel = viewModel { FeedEntryViewModel(source) },
) {
    val scope = rememberCoroutineScope()
    val tagsState by tagsVM.itemsFlow.collectAsState()
    val tagsMapState by tagsVM.tagsMapFlow.collectAsState()

    // The filtered list snapshot taken by FeedEntriesPage; falls back to a
    // single page when this page was opened without list context.
    val pagerIds = remember { pagerVM.ids.toList().ifEmpty { listOf(id) } }
    val pagerState = rememberPagerState(initialPage = pagerIds.indexOf(id).coerceAtLeast(0), pageCount = { pagerIds.size })
    val scrollStates = remember(pagerIds) { List(pagerIds.size) { LazyListState() } }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = {
        val s = scrollStates[pagerState.currentPage]
        // canScrollForward keeps the bar responsive on a fresh pager page sitting
        // at the top, where firstVisibleItemIndex == 0 would leave a collapsed
        // top bar stuck after switching pages.
        s.canScrollForward || s.canScrollBackward
    })

    // Article text scale, set manually from the top bar menu and persisted.
    val fontScale = FeedFontScale.value(feedEntryVM.fontScaleIndex.intValue)

    val previewerState = rememberPreviewerState()
    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launchSafe(onError = { error ->
            setRefreshState(RefreshContentState.Failed)
            DialogHelper.showErrorDialog(error.toString())
        }) {
            feedEntryVM.item.value?.let { entry ->
                entry.fetchContentAsync()
                pagerVM.cacheContent(entry.id, entry.content)
                if (feedEntryVM.item.value?.id == entry.id) feedEntryVM.content.value = entry.content
            }
            setRefreshState(RefreshContentState.Finished)
        }
    }

    LaunchedEffect(Unit) {
        tagsVM.dataType.value = DataType.FEED_ENTRY
    }

    // Mirror the visible page into feedEntryVM for the top bar and fetch
    // actions, then preload neighbor rows and neighbor full content. The
    // content mirror is only ever set by an actual fetch (pull refresh / load
    // button) — mirroring m.content here would re-parse the article after the
    // first frame already rendered it from the pager cache.
    LaunchedEffect(pagerState.currentPage) {
        val pageId = pagerIds.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        feedEntryVM.content.value = ""
        feedEntryVM.fetchingContent.value = false
        val m = pagerVM.entryAsync(pageId) ?: return@LaunchedEffect
        // Swiping onto an article reads it, same as tapping it in the list.
        if (!m.read) pagerVM.markRead(pageId)
        feedEntryVM.item.value = m
        feedEntryVM.feed.value = pagerVM.feedAsync(m.feedId)
        pagerVM.preloadAroundAsync(pageId)
    }

    if (feedEntryVM.showSelectTagsDialog.value) { feedEntryVM.item.value?.let { m -> SelectTagsDialog(tagsVM, tagsState, tagsMapState, data = m) { feedEntryVM.showSelectTagsDialog.value = false } } }
    PBackHandler(previewerState.visible) { scope.launch { previewerState.close() } }

    PScaffold(topBar = { FeedEntryTopBar(navController, feedEntryVM, scrollBehavior, scope) { scope.launch { scrollStates[pagerState.currentPage].scrollToItem(0) } } }, modifier = Modifier.imePadding(),
        content = { paddingValues ->
            PullToRefresh(modifier = Modifier.padding(top = paddingValues.calculateTopPadding()), refreshLayoutState = topRefreshLayoutState,
                refreshContent = remember { { PullToRefreshContent(createText = { when (it) { RefreshContentState.Failed -> stringResource(Res.string.fetch_failed); RefreshContentState.Finished -> stringResource(Res.string.fetched); RefreshContentState.Refreshing -> stringResource(Res.string.fetching_content); RefreshContentState.Dragging -> if (abs(getRefreshContentOffset()) < getRefreshContentThreshold()) stringResource(Res.string.pull_down_to_fetch_content) else stringResource(Res.string.release_to_fetch) } }) } }) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
                    FeedEntryArticle(
                        pageId = pagerIds[page],
                        pagerVM = pagerVM,
                        tagsState = tagsState,
                        tagsMapState = tagsMapState,
                        scrollState = scrollStates[page],
                        scrollBehavior = scrollBehavior,
                        fontScale = fontScale,
                        feedEntryVM = feedEntryVM,
                        isCurrent = page == pagerState.currentPage,
                        topRefreshLayoutState = topRefreshLayoutState,
                        previewerState = previewerState,
                        paddingValues = paddingValues,
                        navController = navController,
                        scope = scope,
                    )
                }
            }
        })
    MediaPreviewer(state = previewerState)
}
