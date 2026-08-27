package com.ismartcoding.plain.ui.page.apps
import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.platform.PBackHandler
import com.ismartcoding.plain.preferences.PackageSortByPreference
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.base.rememberLifecycleEvent
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.ListSearchBar
import com.ismartcoding.plain.ui.components.SidebarItem
import com.ismartcoding.plain.ui.extensions.reset
import com.ismartcoding.plain.ui.models.AppsViewModel
import com.ismartcoding.plain.ui.models.enterSearchMode
import com.ismartcoding.plain.ui.models.exitSearchMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppsPage(navController: NavHostController, appsVM: AppsViewModel = viewModel { AppsViewModel() }) {
    val itemsState by appsVM.itemsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollStateMap = remember { mutableStateMapOf<Int, LazyListState>() }
    val pagerState = rememberPagerState(pageCount = { appsVM.tabs.value.size })
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = { (scrollStateMap[pagerState.currentPage]?.firstVisibleItemIndex ?: 0) > 0 })
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var isFirstTime by remember { mutableStateOf(true) }
    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launch { appsVM.loadAsync(); setRefreshState(RefreshContentState.Finished) }
    }
    val lifecycleEvent = rememberLifecycleEvent()
    LaunchedEffect(lifecycleEvent) {
        if (lifecycleEvent == Lifecycle.Event.ON_RESUME) {
            scope.launch(Dispatchers.Default) { appsVM.loadAsync() }
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        if (isFirstTime) { isFirstTime = false; return@LaunchedEffect }
        val tab = appsVM.tabs.value.getOrNull(pagerState.currentPage)
        if (tab != null) {
            appsVM.appType.value = tab.value
            scope.launch { scrollBehavior.reset(); scrollStateMap[pagerState.currentPage]?.scrollToItem(0) }
            scope.launch(Dispatchers.Default) { appsVM.loadAsync() }
        }
    }
    if (appsVM.showSortDialog.value) {
        RadioDialog(title = stringResource(Res.string.sort), options = FileSortBy.entries.map {
            RadioDialogOption(text = stringResource(it.getTextId()), selected = it == appsVM.sortBy.value) {
                scope.launch(Dispatchers.Default) { PackageSortByPreference.putAsync(it); appsVM.sortBy.value = it; appsVM.loadAsync() }
            }
        }) { appsVM.showSortDialog.value = false }
    }
    val onSearch: (String) -> Unit = { appsVM.searchActive.value = false; appsVM.showLoading.value = true; scope.launch(Dispatchers.Default) { appsVM.loadAsync() } }
    PBackHandler(enabled = appsVM.showSearchBar.value || drawerState.isOpen) {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            appsVM.showSearchBar.value && (!appsVM.searchActive.value || appsVM.queryText.value.isEmpty()) -> { appsVM.exitSearchMode(); onSearch("") }
        }
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                AppsDrawerContent(appsVM, pagerState, drawerState)
            }
        },
    ) {
    PScaffold(topBar = {
        if (appsVM.showSearchBar.value) { ListSearchBar(viewModel = appsVM, onSearch = onSearch); return@PScaffold }
        PTopAppBar(modifier = Modifier.combinedClickable(onClick = {}, onDoubleClick = { scope.launch { scrollStateMap[pagerState.currentPage]?.scrollToItem(0) } }),
            navController = navController,
            navigationIcon = { PIconButton(icon = Res.drawable.left_panel_open, contentDescription = stringResource(Res.string.apps), click = { scope.launch { if (drawerState.isOpen) drawerState.close() else drawerState.open() } }) },
            title = stringResource(Res.string.apps), scrollBehavior = scrollBehavior, actions = {
                ActionButtonSearch { appsVM.enterSearchMode() }
                PCapsuleMoreClose(onClose = { navController.navigateUp() }) { dismiss ->
                    PDropdownMenuItemSort {
                        dismiss()
                        appsVM.showSortDialog.value = true
                    }
                }
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
}

/**
 * Drawer content for the apps page: All / System / User app filters with counts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppsDrawerContent(appsVM: AppsViewModel, pagerState: PagerState, drawerState: DrawerState) {
    val scope = rememberCoroutineScope()
    val tabIcons = listOf(Res.drawable.layout_grid, Res.drawable.package2, Res.drawable.rocket)
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(NavigationDrawerItemDefaults.ItemPadding)
    ) {
        VerticalSpace(dp = 16.dp)
        appsVM.tabs.value.forEachIndexed { index, tab ->
            SidebarItem(
                label = tab.title,
                icon = tabIcons.getOrElse(index) { Res.drawable.layout_grid },
                isSelected = pagerState.currentPage == index,
                onClick = { scope.launch { drawerState.close(); pagerState.scrollToPage(index) } },
                badge = tab.count.toString()
            )
        }
        BottomSpace()
    }
}
