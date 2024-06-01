package com.ismartcoding.plain.ui.page.docs

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
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
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.features.PermissionsResultEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.preference.DocSortByPreference
import com.ismartcoding.plain.ui.base.ActionButtonMoreWithMenu
import com.ismartcoding.plain.ui.base.ActionButtonSearch
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.NeedPermissionColumn
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PDropdownMenuItemSelect
import com.ismartcoding.plain.ui.base.PDropdownMenuItemSort
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PMiniOutlineButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.fastscroll.LazyColumnScrollbar
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.tabs.PScrollableTabRow
import com.ismartcoding.plain.ui.components.DocItem
import com.ismartcoding.plain.ui.components.FileSortDialog
import com.ismartcoding.plain.ui.components.ListSearchBar
import com.ismartcoding.plain.ui.extensions.reset
import com.ismartcoding.plain.ui.models.DocsViewModel
import com.ismartcoding.plain.ui.models.enterSearchMode
import com.ismartcoding.plain.ui.models.exitSearchMode
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.isAllSelected
import com.ismartcoding.plain.ui.models.showBottomActions
import com.ismartcoding.plain.ui.models.toggleSelectAll
import com.ismartcoding.plain.ui.models.toggleSelectMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DocsPage(
    navController: NavHostController,
    viewModel: DocsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val view = LocalView.current
    val window = (view.context as Activity).window
    val itemsState by viewModel.itemsFlow.collectAsState()
    val filteredItemsState by remember {
        derivedStateOf { itemsState.filter { viewModel.fileType.value.isEmpty() || it.extension == viewModel.fileType.value } }
    }
    val scope = rememberCoroutineScope()
    val scrollStateMap = remember {
        mutableStateMapOf<Int, LazyListState>()
    }
    val pagerState = rememberPagerState(pageCount = { viewModel.tabs.value.size })
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = {
        (scrollStateMap[pagerState.currentPage]?.firstVisibleItemIndex ?: 0) > 0 && !viewModel.selectMode.value
    })
    var isFirstTime by remember { mutableStateOf(true) }

    var hasPermission by remember {
        mutableStateOf(AppFeatureType.FILES.hasPermission(context))
    }
    val events by remember { mutableStateOf<MutableList<Job>>(arrayListOf()) }

    val topRefreshLayoutState =
        rememberRefreshLayoutState {
            scope.launch {
                withIO { viewModel.loadAsync(context) }
                setRefreshState(RefreshContentState.Finished)
            }
        }

    LaunchedEffect(pagerState.currentPage) {
        if (isFirstTime) {
            isFirstTime = false
            return@LaunchedEffect
        }

        val tab = viewModel.tabs.value.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        viewModel.fileType.value = tab.value
        scope.launch {
            scrollBehavior.reset()
            scrollStateMap[pagerState.currentPage]?.scrollToItem(0)
        }
    }

    val once = rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!once.value) {
            once.value = true
            if (hasPermission) {
                scope.launch(Dispatchers.IO) {
                    viewModel.sortBy.value = DocSortByPreference.getValueAsync(context)
                    viewModel.loadAsync(context)
                }
            }
        }
        events.add(
            receiveEventHandler<PermissionsResultEvent> {
                hasPermission = AppFeatureType.FILES.hasPermission(context)
                scope.launch(Dispatchers.IO) {
                    viewModel.sortBy.value = DocSortByPreference.getValueAsync(context)
                    viewModel.loadAsync(context)
                }
            })
    }

    val insetsController = WindowCompat.getInsetsController(window, view)
    LaunchedEffect(viewModel.selectMode.value) {
        if (viewModel.selectMode.value) {
            scrollBehavior.reset()
            insetsController.hide(WindowInsetsCompat.Type.navigationBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.navigationBars())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            insetsController.show(WindowInsetsCompat.Type.navigationBars())
            events.forEach { it.cancel() }
            events.clear()
        }
    }

    ViewDocBottomSheet(viewModel)

    val pageTitle = if (viewModel.selectMode.value) {
        LocaleHelper.getStringF(R.string.x_selected, "count", viewModel.selectedIds.size)
    } else {
        stringResource(id = R.string.docs)
    }

    if (viewModel.showSortDialog.value) {
        FileSortDialog(viewModel.sortBy, onSelected = {
            scope.launch(Dispatchers.IO) {
                DocSortByPreference.putAsync(context, it)
                viewModel.sortBy.value = it
                viewModel.loadAsync(context)
            }
        }, onDismiss = {
            viewModel.showSortDialog.value = false
        })
    }

    val onSearch: (String) -> Unit = {
        viewModel.searchActive.value = false
        viewModel.showLoading.value = true
        scope.launch(Dispatchers.IO) {
            viewModel.loadAsync(context)
        }
    }

    BackHandler(enabled = viewModel.selectMode.value || viewModel.showSearchBar.value) {
        if (viewModel.selectMode.value) {
            viewModel.exitSelectMode()
        } else if (viewModel.showSearchBar.value) {
            if (!viewModel.searchActive.value || viewModel.queryText.value.isEmpty()) {
                viewModel.exitSearchMode()
                onSearch("")
            }
        }
    }

    PScaffold(
        topBar = {
            if (viewModel.showSearchBar.value) {
                ListSearchBar(
                    viewModel = viewModel,
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
                title = pageTitle,
                scrollBehavior = scrollBehavior,
                actions = {
                    if (!hasPermission) {
                        return@PTopAppBar
                    }
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
                            viewModel.enterSearchMode()
                        }
                        ActionButtonMoreWithMenu { dismiss ->
                            PDropdownMenuItemSelect(onClick = {
                                dismiss()
                                viewModel.toggleSelectMode()
                            })
                            PDropdownMenuItemSort(onClick = {
                                dismiss()
                                viewModel.showSortDialog.value = true
                            })
                        }
                    }

                },
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = viewModel.showBottomActions(),
                enter = slideInVertically { it },
                exit = slideOutVertically { it }) {
                FilesSelectModeBottomActions(viewModel)
            }
        },
    ) { paddingValues ->
        if (!hasPermission) {
            NeedPermissionColumn(AppFeatureType.FILES.getPermission()!!)
            return@PScaffold
        }
        if (!viewModel.selectMode.value) {
            PScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                viewModel.tabs.value.forEachIndexed { index, s ->
                    PFilterChip(
                        modifier = Modifier.padding(start = if (index == 0) 0.dp else 8.dp),
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.scrollToPage(index)
                            }
                        },
                        label = {
                            Text(text = s.title + " (" + s.count + ")")
                        }
                    )
                }
            }
        }
        if (pagerState.pageCount == 0) {
            NoDataColumn(loading = viewModel.showLoading.value, search = viewModel.showSearchBar.value)
            return@PScaffold
        }
        HorizontalPager(state = pagerState) { index ->
            PullToRefresh(
                refreshLayoutState = topRefreshLayoutState,
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (filteredItemsState.isNotEmpty()) {
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
                                item {
                                    TopSpace()
                                }
                                items(filteredItemsState) { m ->
                                    DocItem(navController, viewModel, m)
                                    VerticalSpace(dp = 8.dp)
                                }
                                item {
                                    if (filteredItemsState.isNotEmpty() && !viewModel.noMore.value) {
                                        LaunchedEffect(Unit) {
                                            scope.launch(Dispatchers.IO) {
                                                withIO { viewModel.moreAsync(context) }
                                            }
                                        }
                                    }
                                    LoadMoreRefreshContent(viewModel.noMore.value)
                                }
                                item {
                                    VerticalSpace(dp = paddingValues.calculateBottomPadding())
                                }
                            }
                        }
                    } else {
                        NoDataColumn(loading = viewModel.showLoading.value, search = viewModel.showSearchBar.value)
                    }
                }
            }
        }
    }
}


