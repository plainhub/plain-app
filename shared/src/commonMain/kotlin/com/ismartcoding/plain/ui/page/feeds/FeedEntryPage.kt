package com.ismartcoding.plain.ui.page.feeds

import com.ismartcoding.plain.i18n.*

import com.ismartcoding.plain.features.feed.FeedFontScale

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.db.DFeedEntry
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.lib.JsonHelper.jsonEncode
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.extensions.timeAgo
import com.ismartcoding.plain.platform.fetchContentAsync
import com.ismartcoding.plain.platform.IODispatcher
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.platform.PBackHandler
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.markdowntext.MarkdownText
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.platform.MediaPreviewer
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.nav.navigateText
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.models.FeedEntryPagerViewModel
import com.ismartcoding.plain.ui.models.FeedEntryViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.page.tags.SelectTagsDialog
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.secondaryTextColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun FeedEntryPage(
    navController: NavHostController,
    id: String,
    tagsVM: TagsViewModel,
    pagerVM: FeedEntryPagerViewModel,
    feedEntryVM: FeedEntryViewModel = viewModel { FeedEntryViewModel() },
) {
    val scope = rememberCoroutineScope()
    val tagsState by tagsVM.itemsFlow.collectAsState()
    val tagsMapState by tagsVM.tagsMapFlow.collectAsState()

    // The filtered list snapshot taken by FeedEntriesPage; falls back to a
    // single page when this page was opened without list context.
    val pagerIds = remember { pagerVM.ids.toList().ifEmpty { listOf(id) } }
    val pagerState = rememberPagerState(initialPage = pagerIds.indexOf(id).coerceAtLeast(0), pageCount = { pagerIds.size })
    val scrollStates = remember(pagerIds) { List(pagerIds.size) { LazyListState() } }
    val scrollState = scrollStates[pagerState.currentPage]
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = { scrollStates[pagerState.currentPage].firstVisibleItemIndex > 0 })

    // Article text scale, set manually from the top bar menu and persisted.
    val fontScale = FeedFontScale.value(feedEntryVM.fontScaleIndex.intValue)

    val previewerState = rememberPreviewerState()
    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launch {
            feedEntryVM.item.value?.let { m -> val r = m.fetchContentAsync(); if (r.isOk()) { feedEntryVM.content.value = m.content; pagerVM.cacheContent(m.id, m.content); setRefreshState(RefreshContentState.Finished) } else { setRefreshState(RefreshContentState.Failed); DialogHelper.showErrorDialog(r.errorMessage()) } }
                .also { if (it == null) setRefreshState(RefreshContentState.Finished) }
        }
    }

    LaunchedEffect(Unit) {
        tagsVM.dataType.value = DataType.FEED_ENTRY
    }

    // Mirror the visible page into feedEntryVM for the top bar and fetch
    // actions, then preload neighbor rows and neighbor full content.
    LaunchedEffect(pagerState.currentPage) {
        val pageId = pagerIds.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        feedEntryVM.content.value = ""
        feedEntryVM.fetchingContent.value = false
        val m = pagerVM.entryAsync(pageId) ?: return@LaunchedEffect
        feedEntryVM.item.value = m
        feedEntryVM.feed.value = pagerVM.feedAsync(m.feedId)
        feedEntryVM.content.value = m.content
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun FeedEntryArticle(
    pageId: String,
    pagerVM: FeedEntryPagerViewModel,
    tagsState: List<DTag>,
    tagsMapState: Map<String, List<DTagRelation>>,
    scrollState: LazyListState,
    scrollBehavior: TopAppBarScrollBehavior,
    fontScale: Float,
    feedEntryVM: FeedEntryViewModel,
    isCurrent: Boolean,
    topRefreshLayoutState: RefreshLayoutState,
    previewerState: MediaPreviewerState,
    paddingValues: PaddingValues,
    navController: NavHostController,
    scope: CoroutineScope,
) {
    val entryState = remember(pageId) { mutableStateOf<DFeedEntry?>(null) }
    val feedState = remember(pageId) { mutableStateOf<DFeed?>(null) }
    LaunchedEffect(pageId) {
        val m = pagerVM.entryAsync(pageId) ?: return@LaunchedEffect
        entryState.value = m
        feedState.value = pagerVM.feedAsync(m.feedId)
    }
    val m = entryState.value ?: return

    // Only the visible page honors the mirrored VM content (manual/pull fetch
    // result); neighbor pages render from the pager cache while swiping.
    val contentOverride = if (isCurrent) feedEntryVM.content.value else ""
    val content = contentOverride.ifEmpty { pagerVM.cachedContent(pageId).ifEmpty { m.description } }
    val tagIds = tagsMapState[m.id]?.map { it.tagId } ?: emptyList()

    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density = density.density, fontScale = density.fontScale * fontScale)) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            state = scrollState,
        ) {
            item {
                Box(modifier = Modifier.padding(horizontal = 8.dp).clip(RoundedCornerShape(PlainTheme.CARD_RADIUS)).combinedClickable(onDoubleClick = { navController.navigateText("JSON", jsonEncode(m, pretty = true), "json") }, onClick = { WebHelper.open(m.url) })) {
                    Text(text = m.title, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold))
                }
            }
            item {
                VerticalSpace(dp = 8.dp)
                val tags = tagsState.filter { tagIds.contains(it.id) }
                FlowRow(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = arrayOf(feedState.value?.name ?: "", m.author, m.publishedAt.timeAgo()).filter { it.isNotEmpty() }.joinToString(" \u00b7 "), style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, color = MaterialTheme.colorScheme.secondaryTextColor))
                    tags.forEach { tag -> Text(text = AnnotatedString("#" + tag.name), modifier = Modifier.wrapContentHeight().align(Alignment.Bottom), style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)) }
                }
                VerticalSpace(dp = 16.dp)
            }
            item {
                SelectionContainer {
                    MarkdownText(text = content, modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN), previewerState = previewerState)
                }
            }
            if (isCurrent && feedEntryVM.content.value.isEmpty() && topRefreshLayoutState.refreshContentState.value == RefreshContentState.Finished) {
                item {
                    // Keep the button on the article's base density so its label
                    // never truncates when the user scales the text up.
                    CompositionLocalProvider(LocalDensity provides density) {
                        VerticalSpace(dp = 32.dp)
                        if (feedEntryVM.fetchingContent.value) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { CircularProgressIndicator(modifier = Modifier.size(32.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp) } }
                        else { POutlinedButton(text = stringResource(Res.string.load_full_content), modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN).fillMaxWidth(), enabled = !feedEntryVM.fetchingContent.value, onClick = { scope.launch { feedEntryVM.item.value?.let { mm -> feedEntryVM.fetchingContent.value = true; val r = mm.fetchContentAsync(); feedEntryVM.fetchingContent.value = false; if (r.isOk()) { feedEntryVM.content.value = mm.content; pagerVM.cacheContent(mm.id, mm.content) } else DialogHelper.showErrorDialog(r.errorMessage()) } } }) }
                    }
                }
            }
            item { BottomSpace(paddingValues) }
        }
    }
}
