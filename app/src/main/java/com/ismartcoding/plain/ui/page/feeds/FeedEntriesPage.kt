package com.ismartcoding.plain.ui.page.feeds

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.FeedEntryFilterType
import com.ismartcoding.plain.events.FeedStatusEvent
import com.ismartcoding.plain.features.feed.FeedWorkerStatus
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.ActionButtonMoreWithMenu
import com.ismartcoding.plain.ui.base.ActionButtonSearch
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PDropdownMenuItemSettings
import com.ismartcoding.plain.ui.base.PDropdownMenuItemTags
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PScrollableTabRow
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.PTopRightButton
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.fastscroll.LazyColumnScrollbar
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.FeedEntryListItem
import com.ismartcoding.plain.ui.components.ListSearchBar
import com.ismartcoding.plain.ui.extensions.reset
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FeedEntriesViewModel
import com.ismartcoding.plain.ui.models.FeedsViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VTabData
import com.ismartcoding.plain.ui.models.enterSearchMode
import com.ismartcoding.plain.ui.models.exitSearchMode
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.isAllSelected
import com.ismartcoding.plain.ui.models.select
import com.ismartcoding.plain.ui.models.showBottomActions
import com.ismartcoding.plain.ui.models.toggleSelectAll
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.tags.TagsBottomSheet
import com.ismartcoding.plain.workers.FeedFetchWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FeedEntriesPage(
    navController: NavHostController,
    feedId: String,
    tagsVM: TagsViewModel,
    feedEntriesVM: FeedEntriesViewModel = viewModel(),
    feedsVM: FeedsViewModel = viewModel(),
) {
    val itemsState by feedEntriesVM.itemsFlow.collectAsState()
    val feedsState by feedsVM.itemsFlow.collectAsState()
    val feedsMap = remember(feedsState) {
        derivedStateOf {
            feedsState.associateBy { it.id }
        }
    }
    val tagsState by tagsVM.itemsFlow.collectAsState()
    val tagsMapState by tagsVM.tagsMapFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val sharedFlow = Channel.sharedFlow
    val scrollStateMap = remember {
        mutableStateMapOf<Int, LazyListState>()
    }
    val pagerState = rememberPagerState(pageCount = { tagsState.size + 2 })
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = {
        (scrollStateMap[pagerState.currentPage]?.firstVisibleItemIndex ?: 0) > 0 && !feedEntriesVM.selectMode.value
    })
    var isFirstTime by remember { mutableStateOf(true) }
    val tabs = remember(tagsState, feedEntriesVM.total.intValue, feedEntriesVM.total.intValue) {
        listOf(
            VTabData(LocaleHelper.getString(R.string.all), "all", feedEntriesVM.total.intValue),
            VTabData(LocaleHelper.getString(R.string.today), "today", feedEntriesVM.totalToday.value),
            *tagsState.map { VTabData(it.name, it.id, it.count) }.toTypedArray()
        )
    }

    val topRefreshLayoutState =
        rememberRefreshLayoutState {
            scope.launch {
                feedEntriesVM.sync()
            }
        }

    LaunchedEffect(Unit) {
        tagsVM.dataType.value = feedEntriesVM.dataType
        feedEntriesVM.feedId.value = feedId
        scope.launch(Dispatchers.IO) {
            feedsVM.loadAsync()
            feedEntriesVM.loadAsync(tagsVM)
        }
    }

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            when (event) {
                is FeedStatusEvent -> {
                    if (event.status == FeedWorkerStatus.COMPLETED) {
                        scope.launch(Dispatchers.IO) {
                            feedEntriesVM.loadAsync(tagsVM)
                        }
                        topRefreshLayoutState.setRefreshState(RefreshContentState.Finished)
                    } else if (event.status == FeedWorkerStatus.ERROR) {
                        topRefreshLayoutState.setRefreshState(RefreshContentState.Failed)
                        if (feedId.isNotEmpty()) {
                            when (FeedFetchWorker.statusMap[feedId]) {
                                FeedWorkerStatus.ERROR -> {
                                    DialogHelper.showErrorDialog(FeedFetchWorker.errorMap[feedId] ?: "")
                                }

                                else -> {}
                            }
                        } else {
                            DialogHelper.showErrorDialog(FeedFetchWorker.errorMap.values.joinToString("\n"))
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (isFirstTime) {
            isFirstTime = false
            return@LaunchedEffect
        }

        val tab = tabs.getOrNull(pagerState.currentPage)
        if (tab != null) {
            when (tab.value) {
                "all" -> {
                    feedEntriesVM.filterType = FeedEntryFilterType.DEFAULT
                    feedEntriesVM.tag.value = null
                }

                "today" -> {
                    feedEntriesVM.filterType = FeedEntryFilterType.TODAY
                    feedEntriesVM.tag.value = null
                }

                else -> {
                    feedEntriesVM.filterType = FeedEntryFilterType.DEFAULT
                    feedEntriesVM.tag.value = tagsState.find { it.id == tab.value }
                }
            }
        }
        scope.launch {
            scrollBehavior.reset()
            scrollStateMap[pagerState.currentPage]?.scrollToItem(0)
        }
        scope.launch(Dispatchers.IO) {
            feedEntriesVM.loadAsync(tagsVM)
        }
    }

    LaunchedEffect(feedEntriesVM.selectMode.value) {
        if (feedEntriesVM.selectMode.value) {
            scrollBehavior.reset()
        }
    }

    val feed = if (feedEntriesVM.feedId.value.isEmpty()) null else feedsMap.value[feedEntriesVM.feedId.value]
    val feedName = feed?.name ?: stringResource(id = R.string.feeds)
    val pageTitle = if (feedEntriesVM.selectMode.value) {
        LocaleHelper.getStringF(R.string.x_selected, "count", feedEntriesVM.selectedIds.size)
    } else if (feedEntriesVM.tag.value != null) {
        listOf(
            feedName,
            feedEntriesVM.tag.value!!.name,
        ).joinToString(" - ")
    } else {
        if (feedEntriesVM.filterType == FeedEntryFilterType.TODAY) {
            feedName + " - " + stringResource(id = R.string.today)
        } else {
            feedName
        }
    }

    ViewFeedEntryBottomSheet(
        feedEntriesVM,
        tagsVM,
        tagsMapState,
        tagsState,
    )

    if (feedEntriesVM.showTagsDialog.value) {
        TagsBottomSheet(tagsVM) {
            feedEntriesVM.showTagsDialog.value = false
        }
    }

    val onSearch: (String) -> Unit = {
        feedEntriesVM.searchActive.value = false
        feedEntriesVM.showLoading.value = true
        scope.launch {
            scrollStateMap[pagerState.currentPage]?.scrollToItem(0)
        }
        scope.launch(Dispatchers.IO) {
            feedEntriesVM.loadAsync(tagsVM)
        }
    }

    BackHandler(enabled = feedEntriesVM.selectMode.value || feedEntriesVM.showSearchBar.value) {
        if (feedEntriesVM.selectMode.value) {
            feedEntriesVM.exitSelectMode()
        } else if (feedEntriesVM.showSearchBar.value) {
            if (!feedEntriesVM.searchActive.value || feedEntriesVM.queryText.value.isEmpty()) {
                feedEntriesVM.exitSearchMode()
                onSearch("")
            }
        }
    }

    PScaffold(
        topBar = {
            if (feedEntriesVM.showSearchBar.value) {
                ListSearchBar(
                    viewModel = feedEntriesVM,
                    onSearch = onSearch
                )
                return@PScaffold
            }
            PTopAppBar(
                modifier = Modifier.combinedClickable(onClick = {}, onDoubleClick = {
                    scope.launch {
                        scrollStateMap[pagerState.currentPage]?.scrollToItem(0)
                    }
                }),
                navController = navController,
                navigationIcon = {
                    if (feedEntriesVM.selectMode.value) {
                        NavigationCloseIcon {
                            feedEntriesVM.exitSelectMode()
                        }
                    } else {
                        NavigationBackIcon {
                            navController.popBackStack()
                        }
                    }
                },
                title = pageTitle,
                scrollBehavior = scrollBehavior,
                actions = {
                    if (feedEntriesVM.selectMode.value) {
                        PTopRightButton(
                            label = stringResource(if (feedEntriesVM.isAllSelected()) R.string.unselect_all else R.string.select_all),
                            click = {
                                feedEntriesVM.toggleSelectAll()
                            },
                        )
                        HorizontalSpace(dp = 8.dp)
                    } else {
                        ActionButtonSearch {
                            feedEntriesVM.enterSearchMode()
                        }
                        if (feedEntriesVM.feedId.value.isEmpty()) {
                            PIconButton(
                                icon = R.drawable.rss,
                                contentDescription = stringResource(R.string.subscriptions),
                                tint = MaterialTheme.colorScheme.onSurface,
                            ) {
                                navController.navigate(Routing.Feeds)
                            }
                        }
                        ActionButtonMoreWithMenu { dismiss ->
                            PDropdownMenuItemTags(onClick = {
                                dismiss()
                                feedEntriesVM.showTagsDialog.value = true
                            })
                            if (feedEntriesVM.feedId.value.isEmpty()) {
                                PDropdownMenuItemSettings(onClick = {
                                    dismiss()
                                    navController.navigate(Routing.FeedSettings)
                                })
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = feedEntriesVM.showBottomActions(),
                enter = slideInVertically { it },
                exit = slideOutVertically { it }) {
                FeedEntriesSelectModeBottomActions(feedEntriesVM, tagsVM, tagsState)
            }
        },

        ) { paddingValues ->
        Column(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {

            if (!feedEntriesVM.selectMode.value) {
                PScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, s ->
                        PFilterChip(
                            modifier = Modifier.padding(start = if (index == 0) 0.dp else 8.dp),
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.scrollToPage(index)
                                }
                            },
                            label = {
                                if (index < 2) {
                                    Text(text = s.title + " (" + s.count + ")")
                                } else {
                                    Text(if (feedEntriesVM.feedId.value.isNotEmpty() || feedEntriesVM.queryText.value.isNotEmpty()) s.title else "${s.title} (${s.count})")
                                }
                            }
                        )
                    }
                }
            }
            if (pagerState.pageCount == 0) {
                NoDataColumn(loading = feedEntriesVM.showLoading.value, search = feedEntriesVM.showSearchBar.value)
                return@PScaffold
            }
            HorizontalPager(state = pagerState) { index ->
                PullToRefresh(
                    refreshLayoutState = topRefreshLayoutState,
                    refreshContent = remember {
                        {
                            PullToRefreshContent(
                                createText = {
                                    when (it) {
                                        RefreshContentState.Failed -> stringResource(id = R.string.sync_failed)
                                        RefreshContentState.Finished -> stringResource(id = R.string.synced)
                                        RefreshContentState.Refreshing -> stringResource(id = R.string.syncing)
                                        RefreshContentState.Dragging -> {
                                            if (abs(getRefreshContentOffset()) < getRefreshContentThreshold()) {
                                                stringResource(if (feedEntriesVM.feedId.value.isNotEmpty()) R.string.pull_down_to_sync_current_feed else R.string.pull_down_to_sync_all_feeds)
                                            } else {
                                                stringResource(if (feedEntriesVM.feedId.value.isNotEmpty()) R.string.release_to_sync_current_feed else R.string.release_to_sync_all_feeds)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    },
                ) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        if (itemsState.isNotEmpty()) {
                            val scrollState = rememberLazyListState()
                            scrollStateMap[index] = scrollState
                            LazyColumnScrollbar(
                                state = scrollState,
                            ) {
                                LazyColumn(
                                    Modifier
                                        .fillMaxSize()
                                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                                    state = scrollState,
                                ) {
                                    item(key = "top") {
                                        TopSpace()
                                    }
                                    itemsIndexed(itemsState, key = { _, m -> m.id }) { index, m ->
                                        val tagIds = tagsMapState[m.id]?.map { it.tagId } ?: emptyList()
                                        FeedEntryListItem(
                                            feedEntriesVM,
                                            index,
                                            m,
                                            feedsMap.value[m.feedId],
                                            tagsState.filter { tagIds.contains(it.id) },
                                            onClick = {
                                                if (feedEntriesVM.selectMode.value) {
                                                    feedEntriesVM.select(m.id)
                                                } else {
                                                    navController.navigate(Routing.FeedEntry(m.id))
                                                }
                                            },
                                            onLongClick = {
                                                if (feedEntriesVM.selectMode.value) {
                                                    return@FeedEntryListItem
                                                }
                                                feedEntriesVM.selectedItem.value = m
                                            },
                                            onClickTag = { tag ->
                                                if (feedEntriesVM.selectMode.value) {
                                                    return@FeedEntryListItem
                                                }
                                                val idx = tabs.indexOfFirst { it.value == tag.id }
                                                if (idx != -1) {
                                                    scope.launch {
                                                        pagerState.scrollToPage(idx)
                                                    }
                                                }
                                            }
                                        )
                                        VerticalSpace(dp = 8.dp)
                                    }
                                    item(key = "bottom") {
                                        if (itemsState.isNotEmpty() && !feedEntriesVM.noMore.value) {
                                            LaunchedEffect(Unit) {
                                                scope.launch(Dispatchers.IO) {
                                                    withIO { feedEntriesVM.moreAsync(tagsVM) }
                                                }
                                            }
                                        }
                                        LoadMoreRefreshContent(feedEntriesVM.noMore.value)
                                        VerticalSpace(dp = paddingValues.calculateBottomPadding())
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
