package com.ismartcoding.plain.ui.page.feeds

import com.ismartcoding.plain.i18n.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.extensions.getFinalPath
import com.ismartcoding.plain.extensions.timeAgo
import com.ismartcoding.plain.enums.ExportFileType
import com.ismartcoding.plain.enums.FeedEntryFilterType
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.events.ExportFileEvent
import com.ismartcoding.plain.events.PickFileEvent
import com.ismartcoding.plain.features.feed.FeedFetcher
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
import com.ismartcoding.plain.ui.components.CheckCircle
import com.ismartcoding.plain.ui.components.SidebarItem
import com.ismartcoding.plain.ui.components.SidebarSectionHeader
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
import com.ismartcoding.plain.ui.theme.listItemTag
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
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

    ViewFeedEntryBottomSheet(feedEntriesVM, tagsVM, tagsMapState, tagsState)
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

/**
 * Drawer content for the feed entries page: All, Today, Feeds (with a "+"
 * action to add a feed; long-press manages a feed), then Tags.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedEntriesDrawerContent(
    feedEntriesVM: FeedEntriesViewModel,
    feedsVM: FeedsViewModel,
    feedsState: List<DFeed>,
    tagsState: List<DTag>,
    drawerState: DrawerState,
    onSelect: (feedId: String, filterType: FeedEntryFilterType, tag: DTag?) -> Unit,
    onOpenCatalog: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var feedsExpanded by remember { mutableStateOf(true) }
    var tagsExpanded by remember { mutableStateOf(true) }
    val closeDrawer: () -> Unit = { scope.launch { drawerState.close() } }

    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .padding(NavigationDrawerItemDefaults.ItemPadding)
    ) {
        VerticalSpace(dp = 16.dp)

        SidebarItem(
            label = stringResource(Res.string.all),
            icon = Res.drawable.layout_grid,
            isSelected = feedEntriesVM.feedId.value.isEmpty() && feedEntriesVM.tag.value == null && feedEntriesVM.filterType.value == FeedEntryFilterType.DEFAULT,
            onClick = { onSelect("", FeedEntryFilterType.DEFAULT, null) },
            badge = feedEntriesVM.total.intValue.toString()
        )

        SidebarItem(
            label = stringResource(Res.string.today),
            icon = Res.drawable.history,
            isSelected = feedEntriesVM.feedId.value.isEmpty() && feedEntriesVM.tag.value == null && feedEntriesVM.filterType.value == FeedEntryFilterType.TODAY,
            onClick = { onSelect("", FeedEntryFilterType.TODAY, null) },
            badge = feedEntriesVM.totalToday.value.toString()
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SidebarSectionHeader(
            title = stringResource(Res.string.feeds),
            isExpanded = feedsExpanded,
            onToggle = { feedsExpanded = !feedsExpanded },
            onAction = onOpenCatalog,
            actionIcon = Res.drawable.plus
        )
        if (feedsExpanded) {
            feedsState.forEach { feed ->
                SidebarItem(
                    label = feed.name,
                    // Same identity as the list's cluster header: synced logo, or a
                    // first-letter chip on primaryContainer until one exists.
                    leading = {
                        if (feed.logo.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = feed.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                )
                            }
                        } else {
                            AsyncImage(
                                model = feed.logo.getFinalPath(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    },
                    isSelected = feedEntriesVM.feedId.value == feed.id,
                    onClick = { onSelect(feed.id, FeedEntryFilterType.DEFAULT, null) },
                    onLongClick = { feedsVM.selectedItem.value = feed },
                    badge = feed.count.toString(),
                    // Persisted sync failure: keep the broken feed visible at a glance.
                    showErrorDot = feed.hasSyncError,
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SidebarSectionHeader(
            title = stringResource(Res.string.tags),
            isExpanded = tagsExpanded,
            onToggle = { tagsExpanded = !tagsExpanded },
            onAction = { feedEntriesVM.showTagsDialog.value = true; closeDrawer() },
            actionIcon = Res.drawable.plus
        )
        if (tagsExpanded) {
            tagsState.forEach { tag ->
                SidebarItem(
                    label = tag.name,
                    icon = Res.drawable.tag,
                    isSelected = feedEntriesVM.feedId.value.isEmpty() && feedEntriesVM.tag.value?.id == tag.id,
                    onClick = { onSelect("", FeedEntryFilterType.DEFAULT, tag) },
                    badge = tag.count.toString()
                )
            }
        }
        BottomSpace()
    }
}

/**
 * Sticky day separator for the grouped feed list. The mark-read action only
 * covers the entries loaded in the current paging window.
 */
@Composable
private fun FeedDayHeaderRow(
    row: FeedListRow.DayHeader,
    onMarkRead: () -> Unit,
) {
    val label = when (row.kind) {
        DayKind.TODAY -> stringResource(Res.string.today)
        DayKind.YESTERDAY -> stringResource(Res.string.yesterday)
        DayKind.DATE -> row.dateLabel
    }
    val meta = listOf(
        stringResource(Res.string.n_articles, row.count),
        if (row.unreadCount > 0) stringResource(Res.string.n_unread, row.unreadCount) else stringResource(Res.string.all_read),
    ).joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
        HorizontalSpace(dp = 8.dp)
        Text(
            meta,
            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        if (row.unreadCount > 0) {
            TextButton(onClick = onMarkRead) {
                Text(stringResource(Res.string.mark_all_read), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * Per-feed cluster header, borderless and full-bleed: letter chip + name +
 * counts. Collapse is expressed by the rotating chevron plus child rows
 * indented to align under the feed name (16 margin + 32 chip + 8 gap = 56dp).
 */
@Composable
private fun FeedClusterHeaderRow(
    row: FeedListRow.ClusterHeader,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val logo = row.feed?.logo
        if (!logo.isNullOrEmpty()) {
            AsyncImage(
                model = logo.getFinalPath(),
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentDescription = row.feed?.name,
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = row.feed?.name?.take(1)?.uppercase() ?: "#",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }
        HorizontalSpace(dp = 8.dp)
        Text(
            text = row.feed?.name ?: "#",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        HorizontalSpace(dp = 8.dp)
        Text(
            text = stringResource(Res.string.n_articles, row.count),
            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            maxLines = 1,
        )
        if (row.unreadCount > 0) {
            HorizontalSpace(dp = 8.dp)
            Text(
                text = stringResource(Res.string.n_unread, row.unreadCount),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                ),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        HorizontalSpace(dp = 8.dp)
        Icon(
            painter = painterResource(Res.drawable.chevron_right),
            contentDescription = null,
            modifier = Modifier
                .size(20.dp)
                .rotate(if (row.collapsed) 0f else 90f),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * One entry as a full-bleed borderless row, indented under its cluster's feed
 * name. Unread state is typography only: bold dark title vs muted regular.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeedClusterEntryRow(
    row: FeedListRow.Entry,
    feedEntriesVM: FeedEntriesViewModel,
    tags: List<DTag>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClickTag: (DTag) -> Unit,
) {
    val m = row.entry
    val unread = !m.read
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 56.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
    ) {
        if (feedEntriesVM.selectMode.value) {
            CheckCircle(
                selected = feedEntriesVM.selectedIds.contains(m.id),
                onClick = { feedEntriesVM.select(m.id) },
            )
            HorizontalSpace(dp = 8.dp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = m.title,
                style = if (unread) {
                    MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                maxLines = 2,
            )
            FlowRow(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // The cluster header already names the feed; byline keeps author + time.
                Text(
                    text = arrayOf(m.author, m.publishedAt.timeAgo()).filter { it.isNotEmpty() }.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                )
                tags.forEach { tag ->
                    Text(
                        text = "#" + tag.name,
                        modifier = Modifier
                            .wrapContentHeight()
                            .align(Alignment.Bottom)
                            .clickable { onClickTag(tag) },
                        style = MaterialTheme.typography.listItemTag(),
                    )
                }
            }
        }
        if (m.image.isNotEmpty()) {
            HorizontalSpace(dp = 12.dp)
            AsyncImage(
                model = m.image.getFinalPath(),
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentDescription = m.image,
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/**
 * Collapsed cluster body: newest entry titles as plain indented text lines,
 * full-bleed and tappable to expand. Unread titles bold, read titles muted.
 */
@Composable
private fun FeedClusterDigestRow(
    row: FeedListRow.CollapsedDigest,
    onToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(start = 56.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
    ) {
        row.entries.forEachIndexed { index, entry ->
            if (index > 0) VerticalSpace(dp = 4.dp)
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (entry.read) FontWeight.Normal else FontWeight.SemiBold,
                    color = if (entry.read) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
