package com.ismartcoding.plain.ui.page.docs

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.preference.DocSortByPreference
import com.ismartcoding.plain.ui.base.ActionButtonSearch
import com.ismartcoding.plain.ui.base.ActionButtonSort
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.NeedPermissionColumn
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PScrollableTabRow
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.PTopRightButton
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.fastscroll.LazyColumnScrollbar
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DocsPage(
    navController: NavHostController,
    docsVM: DocsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val view = LocalView.current
    val itemsState by docsVM.itemsFlow.collectAsState()
    val filteredItemsState by remember {
        derivedStateOf { itemsState.filter { docsVM.fileType.value.isEmpty() || it.extension == docsVM.fileType.value } }
    }
    val scope = rememberCoroutineScope()
    val scrollStateMap = remember {
        mutableStateMapOf<Int, LazyListState>()
    }
    val pagerState = rememberPagerState(pageCount = { docsVM.tabs.value.size })
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = {
        (scrollStateMap[pagerState.currentPage]?.firstVisibleItemIndex ?: 0) > 0 && !docsVM.selectMode.value
    })
    var isFirstTime by remember { mutableStateOf(true) }

    var hasPermission by remember {
        mutableStateOf(AppFeatureType.FILES.hasPermission(context))
    }
    val sharedFlow = Channel.sharedFlow

    val topRefreshLayoutState =
        rememberRefreshLayoutState {
            scope.launch {
                withIO { docsVM.loadAsync(context) }
                setRefreshState(RefreshContentState.Finished)
            }
        }

    LaunchedEffect(pagerState.currentPage) {
        if (isFirstTime) {
            isFirstTime = false
            return@LaunchedEffect
        }

        val tab = docsVM.tabs.value.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        docsVM.fileType.value = tab.value
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
                    docsVM.sortBy.value = DocSortByPreference.getValueAsync(context)
                    docsVM.loadAsync(context)
                }
            }
        }
    }

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            when (event) {
                is PermissionsResultEvent -> {
                    hasPermission = AppFeatureType.FILES.hasPermission(context)
                    scope.launch(Dispatchers.IO) {
                        docsVM.sortBy.value = DocSortByPreference.getValueAsync(context)
                        docsVM.loadAsync(context)
                    }
                }
            }
        }
    }

    LaunchedEffect(docsVM.selectMode.value) {
        if (docsVM.selectMode.value) {
            scrollBehavior.reset()
        }
    }

    ViewDocBottomSheet(docsVM)

    val pageTitle = if (docsVM.selectMode.value) {
        LocaleHelper.getStringF(R.string.x_selected, "count", docsVM.selectedIds.size)
    } else {
        stringResource(id = R.string.docs)
    }

    if (docsVM.showSortDialog.value) {
        FileSortDialog(docsVM.sortBy, onSelected = {
            scope.launch(Dispatchers.IO) {
                DocSortByPreference.putAsync(context, it)
                docsVM.sortBy.value = it
                docsVM.loadAsync(context)
            }
        }, onDismiss = {
            docsVM.showSortDialog.value = false
        })
    }

    val onSearch: (String) -> Unit = {
        docsVM.searchActive.value = false
        docsVM.showLoading.value = true
        scope.launch(Dispatchers.IO) {
            docsVM.loadAsync(context)
        }
    }

    BackHandler(enabled = docsVM.selectMode.value || docsVM.showSearchBar.value) {
        if (docsVM.selectMode.value) {
            docsVM.exitSelectMode()
        } else if (docsVM.showSearchBar.value) {
            if (!docsVM.searchActive.value || docsVM.queryText.value.isEmpty()) {
                docsVM.exitSearchMode()
                onSearch("")
            }
        }
    }

    PScaffold(
        topBar = {
            if (docsVM.showSearchBar.value) {
                ListSearchBar(
                    viewModel = docsVM,
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
                    if (docsVM.selectMode.value) {
                        NavigationCloseIcon {
                            docsVM.exitSelectMode()
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
                    if (docsVM.selectMode.value) {
                        PTopRightButton(
                            label = stringResource(if (docsVM.isAllSelected()) R.string.unselect_all else R.string.select_all),
                            click = {
                                docsVM.toggleSelectAll()
                            },
                        )
                        HorizontalSpace(dp = 8.dp)
                    } else {
                        ActionButtonSearch {
                            docsVM.enterSearchMode()
                        }
                        ActionButtonSort {
                            docsVM.showSortDialog.value = true
                        }
                    }
                },
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = docsVM.showBottomActions(),
                enter = slideInVertically { it },
                exit = slideOutVertically { it }) {
                DocFilesSelectModeBottomActions(docsVM)
            }
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
            if (!hasPermission) {
                NeedPermissionColumn(R.drawable.file_text, AppFeatureType.FILES.getPermission()!!)
                return@PScaffold
            }
            if (!docsVM.selectMode.value) {
                PScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    docsVM.tabs.value.forEachIndexed { index, s ->
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
                NoDataColumn(loading = docsVM.showLoading.value, search = docsVM.showSearchBar.value)
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
                                        DocItem(navController, docsVM, m)
                                        VerticalSpace(dp = 8.dp)
                                    }
                                    item {
                                        if (filteredItemsState.isNotEmpty() && !docsVM.noMore.value) {
                                            LaunchedEffect(Unit) {
                                                scope.launch(Dispatchers.IO) {
                                                    withIO { docsVM.moreAsync(context) }
                                                }
                                            }
                                        }
                                        LoadMoreRefreshContent(docsVM.noMore.value)
                                    }
                                    item {
                                        VerticalSpace(dp = paddingValues.calculateBottomPadding())
                                    }
                                }
                            }
                        } else {
                            NoDataColumn(loading = docsVM.showLoading.value, search = docsVM.showSearchBar.value)
                        }
                    }
                }
            }
        }
    }
}


