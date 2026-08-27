package com.ismartcoding.plain.ui.page.apps
import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import org.jetbrains.compose.resources.stringResource
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
import com.ismartcoding.plain.ui.extensions.reset
import com.ismartcoding.plain.ui.models.AppsViewModel
import com.ismartcoding.plain.ui.models.VTabData
import com.ismartcoding.plain.ui.models.enterSearchMode
import com.ismartcoding.plain.ui.models.exitSearchMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppsPage(navController: NavHostController, appsVM: AppsViewModel = viewModel { AppsViewModel() }) {
    val itemsState by appsVM.itemsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = { scrollState.firstVisibleItemIndex > 0 })
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launch { appsVM.loadAsync(); setRefreshState(RefreshContentState.Finished) }
    }
    val lifecycleEvent = rememberLifecycleEvent()
    LaunchedEffect(lifecycleEvent) {
        if (lifecycleEvent == Lifecycle.Event.ON_RESUME) {
            scope.launch(Dispatchers.Default) { appsVM.loadAsync() }
        }
    }
    val onSelectTab: (VTabData) -> Unit = { tab ->
        appsVM.appType.value = tab.value
        appsVM.showLoading.value = true
        scope.launch { drawerState.close(); scrollBehavior.reset(); scrollState.scrollToItem(0) }
        scope.launch(Dispatchers.Default) { appsVM.loadAsync() }
    }
    if (appsVM.showSortDialog.value) {
        RadioDialog(title = stringResource(Res.string.sort), options = FileSortBy.entries.map {
            RadioDialogOption(text = stringResource(it.getTextId()), selected = it == appsVM.sortBy.value) {
                scope.launch(Dispatchers.Default) { PackageSortByPreference.putAsync(it); appsVM.sortBy.value = it; appsVM.loadAsync() }
            }
        }) { appsVM.showSortDialog.value = false }
    }
    val onSearch: (String) -> Unit = { appsVM.searchActive.value = false; appsVM.showLoading.value = true; scope.launch(Dispatchers.Default) { appsVM.loadAsync() } }
    // Like the media pages, the title reflects the filter picked in the drawer
    val title = if (appsVM.appType.value.isEmpty()) stringResource(Res.string.apps)
    else appsVM.tabs.value.firstOrNull { it.value == appsVM.appType.value }?.title ?: stringResource(Res.string.apps)
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
                AppsDrawerContent(appsVM, onSelectTab)
            }
        },
    ) {
    PScaffold(topBar = {
        if (appsVM.showSearchBar.value) { ListSearchBar(viewModel = appsVM, onSearch = onSearch); return@PScaffold }
        PTopAppBar(modifier = Modifier.combinedClickable(onClick = {}, onDoubleClick = { scope.launch { scrollState.scrollToItem(0) } }),
            navController = navController,
            navigationIcon = { PIconButton(icon = Res.drawable.left_panel_open, contentDescription = stringResource(Res.string.apps), click = { scope.launch { if (drawerState.isOpen) drawerState.close() else drawerState.open() } }) },
            title = title, scrollBehavior = scrollBehavior, actions = {
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
            if (appsVM.tabs.value.isEmpty()) { NoDataColumn(loading = appsVM.showLoading.value, search = appsVM.showSearchBar.value); return@PScaffold }
            AppsPageList(navController = navController, appsVM = appsVM, items = itemsState,
                scrollState = scrollState, scrollBehavior = scrollBehavior, topRefreshLayoutState = topRefreshLayoutState, paddingValues = paddingValues)
        }
    }
    }
}

