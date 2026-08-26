package com.ismartcoding.plain.ui.page.feeds

import com.ismartcoding.plain.i18n.*
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.enums.ExportFileType
import com.ismartcoding.plain.enums.FeedEntryFilterType
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.events.ExportFileEvent
import com.ismartcoding.plain.events.PickFileEvent
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
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.FeedEntryListItem
import com.ismartcoding.plain.ui.components.SidebarItem
import com.ismartcoding.plain.ui.components.SidebarSectionHeader
import com.ismartcoding.plain.ui.extensions.reset
import com.ismartcoding.plain.ui.models.FeedEntriesViewModel
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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FeedEntriesPage(
    navController: NavHostController, feedId: String, tagsVM: TagsViewModel,
    feedEntriesVM: FeedEntriesViewModel = viewModel { FeedEntriesViewModel() }, feedsVM: FeedsViewModel = viewModel { FeedsViewModel() },
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
    AddFeedDialog(feedsVM); EditFeedDialog(feedsVM); ViewFeedBottomSheet(feedsVM)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                FeedEntriesDrawerContent(feedEntriesVM, feedsVM, feedsState, tagsState, drawerState, onSelectDrawerItem)
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
                            PDropdownMenuItemSettings(onClick = { dismiss(); navController.navigate(Routing.FeedSettings) })
                            PDropdownMenuItem(
                                text = { Text(stringResource(Res.string.import_opml_file)) },
                                leadingIcon = { Icon(painterResource(Res.drawable.upload), contentDescription = stringResource(Res.string.import_opml_file)) },
                                onClick = { dismiss(); sendEvent(PickFileEvent(PickFileTag.FEED, PickFileType.FILE, false)) })
                            PDropdownMenuItem(
                                text = { Text(stringResource(Res.string.export_opml_file)) },
                                leadingIcon = { Icon(painterResource(Res.drawable.download), contentDescription = stringResource(Res.string.export_opml_file)) },
                                onClick = { dismiss(); sendEvent(ExportFileEvent(ExportFileType.OPML, "feeds_" + TimeHelper.now().formatName() + ".opml")) })
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
                PullToRefresh(
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
                        if (itemsState.isNotEmpty()) {
                            LazyColumnScrollbar(state = scrollState) {
                                LazyColumn(Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection), state = scrollState) {
                                    item(key = "top") { TopSpace() }
                                    itemsIndexed(itemsState, key = { _, m -> m.id }) { idx, m ->
                                        val tagIds = tagsMapState[m.id]?.map { it.tagId } ?: emptyList()
                                        FeedEntryListItem(
                                            feedEntriesVM, idx, m, feedsMap.value[m.feedId], tagsState.filter { tagIds.contains(it.id) },
                                            onClick = { if (feedEntriesVM.selectMode.value) feedEntriesVM.select(m.id) else navController.navigate(Routing.FeedEntry(m.id)) },
                                            onLongClick = { if (!feedEntriesVM.selectMode.value) feedEntriesVM.selectedItem.value = m },
                                            onClickTag = { tag -> if (!feedEntriesVM.selectMode.value) applyFilter("", FeedEntryFilterType.DEFAULT, tag) }
                                        )
                                        VerticalSpace(dp = 8.dp)
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
            onAction = { feedsVM.showAddDialog() },
            actionIcon = Res.drawable.plus
        )
        if (feedsExpanded) {
            feedsState.forEach { feed ->
                SidebarItem(
                    label = feed.name,
                    icon = Res.drawable.rss,
                    isSelected = feedEntriesVM.feedId.value == feed.id,
                    onClick = { onSelect(feed.id, FeedEntryFilterType.DEFAULT, null) },
                    onLongClick = { feedsVM.selectedItem.value = feed },
                    badge = feed.count.toString()
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

