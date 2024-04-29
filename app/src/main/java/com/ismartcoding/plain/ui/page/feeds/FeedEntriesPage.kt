package com.ismartcoding.plain.ui.page.feeds

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.FeedEntryFilterType
import com.ismartcoding.plain.features.FeedStatusEvent
import com.ismartcoding.plain.features.feed.FeedWorkerStatus
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.ActionButtonMoreWithMenu
import com.ismartcoding.plain.ui.base.ActionButtonSearch
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PDropdownMenuItemSelect
import com.ismartcoding.plain.ui.base.PDropdownMenuItemSettings
import com.ismartcoding.plain.ui.base.PDropdownMenuItemTags
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PMiniOutlineButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.FeedEntryListItem
import com.ismartcoding.plain.ui.extensions.navigate
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FeedEntriesViewModel
import com.ismartcoding.plain.ui.models.FeedsViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.isAllSelected
import com.ismartcoding.plain.ui.models.select
import com.ismartcoding.plain.ui.models.toggleSelectAll
import com.ismartcoding.plain.ui.models.toggleSelectMode
import com.ismartcoding.plain.ui.page.RouteName
import com.ismartcoding.plain.workers.FeedFetchWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FeedEntriesPage(
    navController: NavHostController,
    feedId: String,
    viewModel: FeedEntriesViewModel = viewModel(),
    feedsViewModel: FeedsViewModel = viewModel(),
    tagsViewModel: TagsViewModel = viewModel(),
) {
    val view = LocalView.current
    val window = (view.context as Activity).window
    val itemsState by viewModel.itemsFlow.collectAsState()
    val feedsState by feedsViewModel.itemsFlow.collectAsState()
    val feedsMap = remember(feedsState) {
        derivedStateOf {
            feedsState.associateBy { it.id }
        }
    }
    val tagsState by tagsViewModel.itemsFlow.collectAsState()
    val tagsMapState by tagsViewModel.tagsMapFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val filtersScrollState = rememberScrollState()
    val events by remember { mutableStateOf<MutableList<Job>>(arrayListOf()) }
    val scrollState = rememberLazyListState()

    val topRefreshLayoutState =
        rememberRefreshLayoutState {
            scope.launch {
                viewModel.sync()
            }
        }

    LaunchedEffect(Unit) {
        tagsViewModel.dataType.value = viewModel.dataType
        viewModel.feedId.value = feedId
        scope.launch(Dispatchers.IO) {
            feedsViewModel.loadAsync()
            viewModel.loadAsync(tagsViewModel)
        }

        events.add(
            receiveEventHandler<FeedStatusEvent> { event ->
                if (event.status == FeedWorkerStatus.COMPLETED) {
                    scope.launch(Dispatchers.IO) {
                        viewModel.loadAsync(tagsViewModel)
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
                        DialogHelper.showErrorDialog(FeedFetchWorker.errorMap.values.toList().joinToString("\n"))
                    }
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            events.forEach { it.cancel() }
            events.clear()
        }
    }

    val insetsController = WindowCompat.getInsetsController(window, view)
    LaunchedEffect(viewModel.selectMode.value) {
        if (viewModel.selectMode.value) {
            insetsController.hide(WindowInsetsCompat.Type.navigationBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.navigationBars())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            insetsController.show(WindowInsetsCompat.Type.navigationBars())
        }
    }

    val feed = if (viewModel.feedId.value.isEmpty()) null else feedsMap.value[viewModel.feedId.value]
    val feedName = feed?.name ?: stringResource(id = R.string.feeds)
    val pageTitle = if (viewModel.selectMode.value) {
        LocaleHelper.getStringF(R.string.x_selected, "count", viewModel.selectedIds.size)
    } else if (viewModel.tag.value != null) {
        listOf(
            feedName,
            viewModel.tag.value!!.name,
        ).joinToString(" - ")
    } else {
        if (viewModel.filterType == FeedEntryFilterType.TODAY) {
            feedName + " - " + stringResource(id = R.string.today)
        } else {
            feedName
        }
    }

    ViewFeedEntryBottomSheet(
        viewModel,
        tagsViewModel,
        tagsMapState,
        tagsState,
    )

    BackHandler(enabled = viewModel.selectMode.value) {
        viewModel.exitSelectMode()
    }

    PScaffold(
        navController,
        topBarTitle = pageTitle,
        topBarOnDoubleClick = {
            scope.launch {
                scrollState.scrollToItem(0)
            }
        },
        navigationIcon = {
            if (viewModel.selectMode.value) {
                NavigationCloseIcon {
                    viewModel.exitSelectMode()
                }
            } else {
                NavigationBackIcon {
                    navController.popBackStack()
                }
            }
        },
        actions = {
            if (viewModel.selectMode.value) {
                PMiniOutlineButton(
                    text = stringResource(if (viewModel.isAllSelected()) R.string.unselect_all else R.string.select_all),
                    onClick = {
                        viewModel.toggleSelectAll()
                    },
                )
                HorizontalSpace(dp = 8.dp)
            } else {
                ActionButtonSearch {
                    navController.navigate("${RouteName.FEED_ENTRIES.name}/search?q=")
                }
                if (viewModel.feedId.value.isEmpty()) {
                    PIconButton(
                        icon = Icons.Outlined.RssFeed,
                        contentDescription = stringResource(R.string.subscriptions),
                        tint = MaterialTheme.colorScheme.onSurface,
                    ) {
                        navController.navigate(RouteName.FEEDS)
                    }
                }
                ActionButtonMoreWithMenu { dismiss ->
                    PDropdownMenuItemSelect(onClick = {
                        dismiss()
                        viewModel.toggleSelectMode()
                    })
                    PDropdownMenuItemTags(onClick = {
                        dismiss()
                        navController.navigate("${RouteName.TAGS.name}?dataType=${viewModel.dataType.value}")
                    })
                    if (viewModel.feedId.value.isEmpty()) {
                        PDropdownMenuItemSettings(onClick = {
                            dismiss()
                            navController.navigate(RouteName.FEED_SETTINGS)
                        })
                    }
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = viewModel.selectMode.value,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }) {
                SelectModeBottomActions(viewModel, tagsViewModel, tagsState)
            }
        },
    ) {
        if (!viewModel.selectMode.value) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .horizontalScroll(filtersScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PFilterChip(
                    selected = viewModel.filterType == FeedEntryFilterType.DEFAULT && viewModel.tag.value == null,
                    onClick = {
                        viewModel.filterType = FeedEntryFilterType.DEFAULT
                        viewModel.tag.value = null
                        scope.launch(Dispatchers.IO) {
                            viewModel.loadAsync(tagsViewModel)
                        }
                    },
                    label = { Text(stringResource(id = R.string.all) + " (${viewModel.total.value})") }
                )
                PFilterChip(
                    selected = viewModel.filterType == FeedEntryFilterType.TODAY && viewModel.tag.value == null,
                    onClick = {
                        viewModel.filterType = FeedEntryFilterType.TODAY
                        viewModel.tag.value = null
                        scope.launch(Dispatchers.IO) {
                            viewModel.loadAsync(tagsViewModel)
                        }
                    },
                    label = { Text(stringResource(id = R.string.today) + " (${viewModel.totalToday.value})") }
                )
                tagsState.forEach { tag ->
                    PFilterChip(
                        selected = viewModel.tag.value?.id == tag.id,
                        onClick = {
                            viewModel.filterType = FeedEntryFilterType.DEFAULT
                            viewModel.tag.value = tag
                            scope.launch(Dispatchers.IO) {
                                viewModel.loadAsync(tagsViewModel)
                            }
                        },
                        label = { Text(if (viewModel.feedId.value.isNotEmpty()) tag.name else "${tag.name} (${tag.count})") }
                    )
                }
            }
        }

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
                                        stringResource(if (viewModel.feedId.value.isNotEmpty()) R.string.pull_down_to_sync_current_feed else R.string.pull_down_to_sync_all_feeds)
                                    } else {
                                        stringResource(if (viewModel.feedId.value.isNotEmpty()) R.string.release_to_sync_current_feed else R.string.release_to_sync_all_feeds)
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
                    LazyColumn(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        state = scrollState,
                    ) {
                        item {
                            TopSpace()
                        }
                        items(itemsState, key = { it.id }) { m ->
                            val tagIds = tagsMapState[m.id]?.map { it.tagId } ?: emptyList()
                            FeedEntryListItem(
                                viewModel,
                                tagsViewModel,
                                m,
                                feedsMap.value[m.feedId],
                                tagsState.filter { tagIds.contains(it.id) },
                                onClick = {
                                    if (viewModel.selectMode.value) {
                                        viewModel.select(m.id)
                                    } else {
                                        navController.navigate("${RouteName.FEED_ENTRIES.name}/${m.id}")
                                    }
                                },
                                onLongClick = {
                                    if (viewModel.selectMode.value) {
                                        return@FeedEntryListItem
                                    }
                                    viewModel.selectedItem.value = m
                                }
                            )
                            VerticalSpace(dp = 8.dp)
                        }
                        item {
                            if (itemsState.isNotEmpty() && !viewModel.noMore.value) {
                                LaunchedEffect(Unit) {
                                    scope.launch(Dispatchers.IO) {
                                        withIO { viewModel.moreAsync(tagsViewModel) }
                                    }
                                }
                            }
                            LoadMoreRefreshContent(viewModel.noMore.value)
                            BottomSpace()
                        }
                    }
                } else {
                    NoDataColumn(loading = viewModel.showLoading.value)
                }
            }
        }
    }
}
