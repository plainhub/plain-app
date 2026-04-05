package com.ismartcoding.plain.ui.page.apps

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.preferences.PackageSortByPreference
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.ListSearchBar
import com.ismartcoding.plain.ui.extensions.reset
import com.ismartcoding.plain.ui.models.AppsViewModel
import com.ismartcoding.plain.ui.models.enterSearchMode
import com.ismartcoding.plain.ui.models.exitSearchMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppsPage(navController: NavHostController, appsVM: AppsViewModel = viewModel()) {
    val context = LocalContext.current
    val itemsState by appsVM.itemsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollStateMap = remember { mutableStateMapOf<Int, LazyListState>() }
    val pagerState = rememberPagerState(pageCount = { appsVM.tabs.value.size })
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = { (scrollStateMap[pagerState.currentPage]?.firstVisibleItemIndex ?: 0) > 0 })
    var isFirstTime by remember { mutableStateOf(true) }
    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launch { withIO { appsVM.loadAsync() }; setRefreshState(RefreshContentState.Finished) }
    }
    val once = rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (!once.value) { once.value = true; scope.launch(Dispatchers.IO) { appsVM.loadAsync() } } }
    LaunchedEffect(pagerState.currentPage) {
        if (isFirstTime) { isFirstTime = false; return@LaunchedEffect }
        val tab = appsVM.tabs.value.getOrNull(pagerState.currentPage)
        if (tab != null) {
            appsVM.appType.value = tab.value
            scope.launch { scrollBehavior.reset(); scrollStateMap[pagerState.currentPage]?.scrollToItem(0) }
            scope.launch(Dispatchers.IO) { appsVM.loadAsync() }
        }
    }
    if (appsVM.showSortDialog.value) {
        RadioDialog(title = stringResource(R.string.sort), options = FileSortBy.entries.map {
            RadioDialogOption(text = stringResource(id = it.getTextId()), selected = it == appsVM.sortBy.value) {
                scope.launch(Dispatchers.IO) { PackageSortByPreference.putAsync(context, it); appsVM.sortBy.value = it; appsVM.loadAsync() }
            }
        }) { appsVM.showSortDialog.value = false }
    }
    val onSearch: (String) -> Unit = { appsVM.searchActive.value = false; appsVM.showLoading.value = true; scope.launch(Dispatchers.IO) { appsVM.loadAsync() } }
    BackHandler(enabled = appsVM.showSearchBar.value) {
        if (appsVM.showSearchBar.value && (!appsVM.searchActive.value || appsVM.queryText.value.isEmpty())) { appsVM.exitSearchMode(); onSearch("") }
    }
    PScaffold(topBar = {
        if (appsVM.showSearchBar.value) { ListSearchBar(viewModel = appsVM, onSearch = onSearch); return@PScaffold }
        PTopAppBar(modifier = Modifier.combinedClickable(onClick = {}, onDoubleClick = { scope.launch { scrollStateMap[pagerState.currentPage]?.scrollToItem(0) } }),
            navController = navController,
            title = stringResource(id = R.string.apps), scrollBehavior = scrollBehavior, actions = {
                ActionButtonSearch { appsVM.enterSearchMode() }; ActionButtonSort { appsVM.showSortDialog.value = true }
            })
    }) { paddingValues ->
        Column(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
            if (!appsVM.showLoading.value) {
                PScrollableTabRow(selectedTabIndex = pagerState.currentPage, modifier = Modifier.fillMaxWidth()) {
                    appsVM.tabs.value.forEachIndexed { index, s ->
                        PFilterChip(modifier = Modifier.padding(start = if (index == 0) 0.dp else 8.dp), selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.scrollToPage(index) } }, label = { Text(text = s.title + " (" + s.count + ")") })
                    }
                }
            }
            if (pagerState.pageCount == 0) { NoDataColumn(loading = appsVM.showLoading.value, search = appsVM.showSearchBar.value); return@PScaffold }
            HorizontalPager(state = pagerState, userScrollEnabled = false) { index ->
                AppsPageList(navController = navController, appsVM = appsVM, items = itemsState, index = index,
                    scrollStateMap = scrollStateMap, scrollBehavior = scrollBehavior, topRefreshLayoutState = topRefreshLayoutState, paddingValues = paddingValues)
            }
        }
    }
}
