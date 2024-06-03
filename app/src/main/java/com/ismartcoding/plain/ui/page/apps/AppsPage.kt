package com.ismartcoding.plain.ui.page.apps

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.preference.PackageSortByPreference
import com.ismartcoding.plain.ui.base.ActionButtonSearch
import com.ismartcoding.plain.ui.base.ActionButtonSort
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.RadioDialog
import com.ismartcoding.plain.ui.base.RadioDialogOption
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.fastscroll.LazyColumnScrollbar
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.tabs.PScrollableTabRow
import com.ismartcoding.plain.ui.components.ListSearchBar
import com.ismartcoding.plain.ui.components.PackageListItem
import com.ismartcoding.plain.ui.extensions.reset
import com.ismartcoding.plain.ui.models.AppsViewModel
import com.ismartcoding.plain.ui.models.enterSearchMode
import com.ismartcoding.plain.ui.models.exitSearchMode
import com.ismartcoding.plain.ui.nav.RouteName
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppsPage(
    navController: NavHostController,
    viewModel: AppsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val itemsState by viewModel.itemsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollStateMap = remember {
        mutableStateMapOf<Int, LazyListState>()
    }
    val pagerState = rememberPagerState(pageCount = { viewModel.tabs.value.size })
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = {
        (scrollStateMap[pagerState.currentPage]?.firstVisibleItemIndex ?: 0) > 0
    })
    var isFirstTime by remember { mutableStateOf(true) }

    val topRefreshLayoutState =
        rememberRefreshLayoutState {
            scope.launch {
                withIO { viewModel.loadAsync() }
                setRefreshState(RefreshContentState.Finished)
            }
        }

    val once = rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!once.value) {
            once.value = true
            scope.launch(Dispatchers.IO) {
                viewModel.loadAsync()
            }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (isFirstTime) {
            isFirstTime = false
            return@LaunchedEffect
        }
        val tab = viewModel.tabs.value.getOrNull(pagerState.currentPage)
        if (tab != null) {
            viewModel.appType.value = tab.value
            scope.launch {
                scrollBehavior.reset()
                scrollStateMap[pagerState.currentPage]?.scrollToItem(0)
            }
            scope.launch(Dispatchers.IO) {
                viewModel.loadAsync()
            }
        }
    }


    if (viewModel.showSortDialog.value) {
        RadioDialog(
            title = stringResource(R.string.sort),
            options =
            FileSortBy.entries.map {
                RadioDialogOption(
                    text = stringResource(id = it.getTextId()),
                    selected = it == viewModel.sortBy.value,
                ) {
                    scope.launch(Dispatchers.IO) {
                        PackageSortByPreference.putAsync(context, it)
                        viewModel.sortBy.value = it
                        viewModel.loadAsync()
                    }
                }
            },
        ) {
            viewModel.showSortDialog.value = false
        }
    }

    val onSearch: (String) -> Unit = {
        viewModel.searchActive.value = false
        viewModel.showLoading.value = true
        scope.launch(Dispatchers.IO) {
            viewModel.loadAsync()
        }
    }

    BackHandler(enabled = viewModel.showSearchBar.value) {
        if (viewModel.showSearchBar.value) {
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
            PTopAppBar(modifier = Modifier.combinedClickable(onClick = {}, onDoubleClick = {
                scope.launch {
                    scrollStateMap[pagerState.currentPage]?.scrollToItem(0)
                }
            }), navController = navController,
                title = stringResource(id = R.string.apps),
                scrollBehavior = scrollBehavior,
                actions = {
                    ActionButtonSearch {
                        viewModel.enterSearchMode()
                    }
                    ActionButtonSort {
                        viewModel.showSortDialog.value = true
                    }
                })
        }) {
        if (!viewModel.showLoading.value) {
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
                                item {
                                    TopSpace()
                                }
                                items(itemsState, key = {
                                    it.id
                                }) { m ->
                                    PackageListItem(
                                        item = m,
                                        modifier = PlainTheme.getCardModifier(),
                                        onClick = {
                                            navController.navigate("${RouteName.APPS.name}/${m.id}")
                                        }
                                    )
                                    VerticalSpace(dp = 8.dp)
                                }
                                item {
                                    if (itemsState.isNotEmpty() && !viewModel.noMore.value) {
                                        LaunchedEffect(Unit) {
                                            scope.launch(Dispatchers.IO) {
                                                withIO { viewModel.moreAsync() }
                                            }
                                        }
                                    }
                                    LoadMoreRefreshContent(viewModel.noMore.value)
                                    BottomSpace()
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


