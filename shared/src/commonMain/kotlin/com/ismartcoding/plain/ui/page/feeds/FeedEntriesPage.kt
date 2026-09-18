package com.ismartcoding.plain.ui.page.feeds

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.enums.ExportFileType
import com.ismartcoding.plain.enums.FeedEntryFilterType
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.events.ExportFileEvent
import com.ismartcoding.plain.events.PickFileEvent
import com.ismartcoding.plain.features.feed.FeedFetcher
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.FeedsPageEffects
import com.ismartcoding.plain.platform.IODispatcher
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.formatName
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.base.fastscroll.LazyColumnScrollbar
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.extensions.reset
import com.ismartcoding.plain.ui.models.FeedEntriesViewModel
import com.ismartcoding.plain.ui.models.FeedCatalogViewModel
import com.ismartcoding.plain.ui.models.FeedEntryPagerViewModel
import com.ismartcoding.plain.ui.models.FeedsViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.enterSearchMode
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.isAllSelected
import com.ismartcoding.plain.ui.models.select
import com.ismartcoding.plain.ui.models.showBottomActions
import com.ismartcoding.plain.ui.models.toggleSelectAll
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.tags.TagsBottomSheet
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FeedEntriesPage(
    navController: NavHostController, feedId: String, tagsVM: TagsViewModel,
    pagerVM: FeedEntryPagerViewModel,
    feedEntriesVM: FeedEntriesViewModel = viewModel { FeedEntriesViewModel() }, feedsVM: FeedsViewModel = viewModel { FeedsViewModel() },
    catalogVM: FeedCatalogViewModel = viewModel { FeedCatalogViewModel() },
) {
    val feedsState by feedsVM.itemsFlow.collectAsState()
    val feedsMap = remember(feedsState) { derivedStateOf { feedsState.associateBy { it.id } } }
    val tagsState by tagsVM.itemsFlow.collectAsState()
    val tagsMapState by tagsVM.tagsMapFlow.collectAsState()
    val itemsState by feedEntriesVM.itemsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = {
        scrollState.firstVisibleItemIndex > 0 && !feedEntriesVM.selectMode.value
    })
    val topRefreshLayoutState = rememberRefreshLayoutState { scope.launch { feedEntriesVM.sync() } }

    val applyFilter: (String, FeedEntryFilterType, DTag?) -> Unit = { newFeedId, filterType, tag ->
        feedEntriesVM.feedId.value = newFeedId
        feedEntriesVM.filterType.value = filterType
        feedEntriesVM.tag.value = tag
        feedEntriesVM.clusterExpandedOverrides.clear()
        scope.launch { scrollState.scrollToItem(0) }
        scope.launch(IODispatcher) { feedEntriesVM.loadAsync(tagsVM) }
    }
    val onSelectDrawerItem: (String, FeedEntryFilterType, DTag?) -> Unit = { newFeedId, filterType, tag ->
        applyFilter(newFeedId, filterType, tag)
        scope.launch { drawerState.close() }
    }

    FeedEntriesPageEffects(feedEntriesVM, feedsVM, tagsVM, feedId, scope, topRefreshLayoutState) {
        feedEntriesVM.showLoading.value = true
        scope.launch { scrollState.scrollToItem(0) }
        scope.launch(IODispatcher) { feedEntriesVM.loadAsync(tagsVM) }
    }
    FeedsPageEffects(feedsVM)

    // Nothing subscribed yet: preload the catalog shown as the empty state.
    LaunchedEffect(feedsState.isEmpty()) {
        if (feedsState.isEmpty()) catalogVM.loadAsync()
    }

    // Discovery mode lives in the VM; resolved once when the feed list
    // finishes loading, and re-entered whenever the subscription count drops
    // back to zero so the page never falls through to the "no data" label.
    LaunchedEffect(feedsVM.showLoading.value, feedsState.isEmpty()) {
        if (!feedsVM.showLoading.value && feedsState.isEmpty()) feedEntriesVM.discoveryMode.value = true
        else if (feedEntriesVM.discoveryMode.value == null && !feedsVM.showLoading.value) feedEntriesVM.discoveryMode.value = false
    }

    LaunchedEffect(feedEntriesVM.selectMode.value) {
        if (feedEntriesVM.selectMode.value) scrollBehavior.reset()
    }

    val feed = if (feedEntriesVM.feedId.value.isEmpty()) null else feedsMap.value[feedEntriesVM.feedId.value]
    val feedName = feed?.name ?: stringResource(Res.string.feeds)
    val pageTitle = if (feedEntriesVM.selectMode.value) LocaleHelper.getStringF(Res.string.x_selected, feedEntriesVM.selectedIds.size)
    else if (feedEntriesVM.tag.value != null) listOf(feedName, feedEntriesVM.tag.value!!.name).joinToString(" - ")
    else if (feedEntriesVM.filterType.value == FeedEntryFilterType.TODAY) feedName + " - " + stringResource(Res.string.today) else feedName

    ViewFeedEntryBottomSheet(feedEntriesVM, feedsVM, tagsVM, tagsMapState, tagsState)
    if (feedEntriesVM.showTagsDialog.value) {
        TagsBottomSheet(tagsVM) { feedEntriesVM.showTagsDialog.value = false }
    }
    AddFeedDialog(feedsVM); EditFeedDialog(feedsVM)
    ViewFeedBottomSheet(feedsVM, onDelete = { deletedFeedId ->
        // Feed deleted from the drawer: drop a now-dangling feed filter and
        // reload so the removed feed's articles leave the list immediately.
        if (feedEntriesVM.feedId.value == deletedFeedId) {
            feedEntriesVM.feedId.value = ""
            feedEntriesVM.filterType.value = FeedEntryFilterType.DEFAULT
        }
        scope.launch(IODispatcher) { feedEntriesVM.loadAsync(tagsVM) }
    })

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                FeedEntriesDrawerContent(feedEntriesVM, feedsVM, feedsState, tagsState, drawerState, onSelectDrawerItem, onOpenCatalog = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Routing.FeedCatalog)
                })
            }
        },
    ) {
        PScaffold(topBar = {
            SearchableTopBar(
                navController = navController, viewModel = feedEntriesVM, scrollBehavior = scrollBehavior, title = pageTitle,
                scrollToTop = { scope.launch { scrollState.scrollToItem(0) } },
                navigationIcon = {
                    if (feedEntriesVM.selectMode.value) NavigationCloseIcon { feedEntriesVM.exitSelectMode() }
                    else PIconButton(icon = Res.drawable.left_panel_open, contentDescription = stringResource(Res.string.feeds), click = {
                        scope.launch { if (drawerState.isOpen) drawerState.close() else drawerState.open() }
                    })
                }, actions = {
                    if (feedEntriesVM.selectMode.value) {
                        PTopRightButton(
                            label = stringResource(if (feedEntriesVM.isAllSelected()) Res.string.unselect_all else Res.string.select_all),
                            click = { feedEntriesVM.toggleSelectAll() }); HorizontalSpace(dp = 8.dp)
                    } else {
                        ActionButtonSearch { feedEntriesVM.enterSearchMode() }
                        PCapsuleMoreClose(onClose = { navController.navigateUp() }) { dismiss ->
                            PSheetActionRow(Res.drawable.settings, stringResource(Res.string.settings)) {
                                dismiss()
                                navController.navigate(Routing.FeedSettings)
                            }
                            PSheetActionRow(Res.drawable.upload, stringResource(Res.string.import_opml_file)) {
                                dismiss()
                                sendEvent(PickFileEvent(PickFileTag.FEED, PickFileType.FILE, false))
                            }
                            PSheetActionRow(Res.drawable.download, stringResource(Res.string.export_opml_file)) {
                                dismiss()
                                sendEvent(ExportFileEvent(ExportFileType.OPML, "feeds_" + TimeHelper.now().formatName() + ".opml"))
                            }
                        }
                    }
                },
                onSearchAction = {
                    feedEntriesVM.showLoading.value = true
                    applyFilter(feedEntriesVM.feedId.value, feedEntriesVM.filterType.value, feedEntriesVM.tag.value)
                })
        }, bottomBar = {
            AnimatedVisibility(visible = feedEntriesVM.showBottomActions(), enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                FeedEntriesSelectModeBottomActions(feedEntriesVM, tagsVM, tagsState)
            }
        }) { paddingValues ->
            Column(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                // State-driven sync failure banner: persisted errors on DFeed,
                // scoped to the current feed filter, retries go through FeedFetcher.
                val failingFeeds = feedsState.filter {
                    it.hasSyncError &&
                        (feedEntriesVM.feedId.value.isEmpty() || it.id == feedEntriesVM.feedId.value)
                }
                if (failingFeeds.isNotEmpty()) {
                    FeedSyncBanner(
                        failedFeeds = failingFeeds,
                        singleFeed = feedEntriesVM.feedId.value.isNotEmpty(),
                        onRetry = { id -> scope.launch(IODispatcher) { FeedFetcher.fetchOne(id) } },
                        onRetryAll = { failingFeeds.forEach { f -> scope.launch(IODispatcher) { FeedFetcher.fetchOne(f.id) } } },
                    )
                }
                PullToRefresh(
                    // No subscriptions yet: the sync worker would finish without
                    // emitting a completion event, leaving the spinner stuck, so
                    // the pull gesture stays disabled until a feed exists.
                    userEnable = feedsState.isNotEmpty(),
                    refreshLayoutState = topRefreshLayoutState,
                    refreshContent = remember {
                        {
                            PullToRefreshContent(createText = {
                                when (it) {
                                    RefreshContentState.Failed -> stringResource(Res.string.sync_failed)
                                    RefreshContentState.Finished -> stringResource(Res.string.synced)
                                    RefreshContentState.Refreshing -> stringResource(Res.string.syncing)
                                    RefreshContentState.Dragging -> {
                                        if (abs(getRefreshContentOffset()) < getRefreshContentThreshold())
                                            stringResource(if (feedEntriesVM.feedId.value.isNotEmpty()) Res.string.pull_down_to_sync_current_feed else Res.string.pull_down_to_sync_all_feeds)
                                        else stringResource(if (feedEntriesVM.feedId.value.isNotEmpty()) Res.string.release_to_sync_current_feed else Res.string.release_to_sync_all_feeds)
                                    }
                                }
                            })
                        }
                    },
                ) {
                    AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                        if (feedEntriesVM.discoveryMode.value == true) {
                            // Discovery wins over the entries list: articles arriving from
                            // a just-subscribed feed must not flip the page.
                            FeedDiscoveryContent(feedsVM, catalogVM, feedsState, paddingValues, onStartReading = {
                                feedEntriesVM.discoveryMode.value = false
                                topRefreshLayoutState.setRefreshState(RefreshContentState.Refreshing)
                            })
                        } else if (itemsState.isNotEmpty()) {
                            LazyColumnScrollbar(state = scrollState) {
                                LazyColumn(Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection), state = scrollState) {
                                    val rows = buildFeedListRows(
                                        itemsState,
                                        feedsMap.value,
                                        feedEntriesVM.clusterExpandedOverrides,
                                        TimeZone.currentSystemDefault(),
                                        TimeHelper.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
                                    )
                                    rows.forEach { row ->
                                        when (row) {
                                            is FeedListRow.DayHeader -> stickyHeader(key = row.key) {
                                                FeedDayHeaderRow(row, onMarkRead = {
                                                    feedEntriesVM.markRead(row.markReadIds)
                                                })
                                            }
                                            is FeedListRow.ClusterHeader -> item(key = row.key) {
                                                FeedClusterHeaderRow(row, onToggle = {
                                                    feedEntriesVM.clusterExpandedOverrides[row.clusterKey] = !row.collapsed
                                                })
                                            }
                                            is FeedListRow.Entry -> item(key = row.key) {
                                                val m = row.entry
                                                val tagIds = tagsMapState[m.id]?.map { it.tagId } ?: emptyList()
                                                FeedClusterEntryRow(
                                                    row, feedEntriesVM, tagsState.filter { tagIds.contains(it.id) },
                                                    onClick = { if (feedEntriesVM.selectMode.value) feedEntriesVM.select(m.id) else { if (!m.read) feedEntriesVM.markRead(setOf(m.id)); pagerVM.setup(itemsState.map { it.id }); navController.navigate(Routing.FeedEntry(m.id)) } },
                                                    onLongClick = { if (!feedEntriesVM.selectMode.value) feedEntriesVM.selectedItem.value = m },
                                                    onClickTag = { tag -> if (!feedEntriesVM.selectMode.value) applyFilter("", FeedEntryFilterType.DEFAULT, tag) }
                                                )
                                            }
                                            is FeedListRow.CollapsedDigest -> item(key = row.key) {
                                                FeedClusterDigestRow(row, onToggle = {
                                                    feedEntriesVM.clusterExpandedOverrides[row.clusterKey] = false
                                                })
                                            }
                                        }
                                    }
                                    item(key = "bottom") {
                                        if (!feedEntriesVM.noMore.value) {
                                            LaunchedEffect(Unit) { scope.launch(IODispatcher) { feedEntriesVM.moreAsync(tagsVM) } }
                                        }
                                        LoadMoreRefreshContent(feedEntriesVM.noMore.value)
                                        VerticalSpace(dp = paddingValues.calculateBottomPadding().value.dp)
                                    }
                                }
                            }
                        } else if (feedsState.isNotEmpty() && !feedEntriesVM.showSearchBar.value &&
                            feedEntriesVM.feedId.value.isEmpty() && feedEntriesVM.tag.value == null &&
                            feedEntriesVM.filterType.value == FeedEntryFilterType.DEFAULT
                        ) {
                            // Feeds exist but no articles in the "all" view (e.g. first sync
                            // still running): dedicated empty state, never the catalog.
                            EmptyArticlesState(Modifier.padding(bottom = paddingValues.calculateBottomPadding()))
                        } else {
                            NoDataColumn(loading = feedEntriesVM.showLoading.value, search = feedEntriesVM.showSearchBar.value)
                        }
                    }
                }
            }
        }
    }
}
