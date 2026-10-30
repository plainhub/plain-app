package com.ismartcoding.plain.ui.apps

import com.ismartcoding.plain.ui.files.getTextId
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
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.components.*
import com.ismartcoding.plain.ui.components.rememberLifecycleEvent
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.ListSearchBar
import com.ismartcoding.plain.ui.media.extensions.reset
import com.ismartcoding.plain.ui.state.VTabData
import com.ismartcoding.plain.ui.state.enterSearchMode
import com.ismartcoding.plain.ui.state.exitSearchMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.left_panel_open as ui_drawable_left_panel_open
import com.ismartcoding.plain.ui.resources.sort as ui_drawable_sort
import com.ismartcoding.plain.i18n.sort
import com.ismartcoding.plain.ui.resources.package2
import com.ismartcoding.plain.ui.state.launchSafe

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppsPage(navController: NavHostController, source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.ui.navigation.LocalContentSource.current, appsVM: AppsViewModel = viewModel { AppsViewModel(source) }) {
    val itemsState by appsVM.itemsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = { scrollState.firstVisibleItemIndex > 0 })
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launch { appsVM.loadAsync(); setRefreshState(RefreshContentState.Finished) }
    }
    if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote) {
        com.ismartcoding.plain.ui.permissions.RemotePermissionEffects(source, com.ismartcoding.plain.platform.permissions.Permission.QUERY_ALL_PACKAGES, appsVM.hasPermission) {
            appsVM.loadAsync()
        }
    }
    val lifecycleEvent = rememberLifecycleEvent()
    LaunchedEffect(lifecycleEvent) {
        if (lifecycleEvent == Lifecycle.Event.ON_RESUME && appsVM.hasPermission.value) {
            scope.launchSafe { appsVM.loadAsync() }
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
                scope.launch(Dispatchers.Default) { Prefs.set(PrefKeys.PACKAGE_SORT_BY, it.name); appsVM.sortBy.value = it; appsVM.loadAsync() }
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
        if (appsVM.showSearchBar.value && appsVM.hasPermission.value) { ListSearchBar(viewModel = appsVM, onSearch = onSearch); return@PScaffold }
        PTopAppBar(modifier = Modifier.combinedClickable(onClick = {}, onDoubleClick = { scope.launch { scrollState.scrollToItem(0) } }),
            onNavigateBack = { navController.navigateUp() },
            navigationIcon = if (appsVM.hasPermission.value) { { PIconButton(icon = UiRes.drawable.ui_drawable_left_panel_open, contentDescription = stringResource(Res.string.apps), click = { scope.launch { if (drawerState.isOpen) drawerState.close() else drawerState.open() } }) } } else null,
            title = title, scrollBehavior = scrollBehavior, actions = if (appsVM.hasPermission.value) { {
                ActionButtonSearch { appsVM.enterSearchMode() }
                PCapsuleMoreClose(onClose = { navController.navigateUp() }) { dismiss ->
                    PSheetActionRow(UiRes.drawable.ui_drawable_sort, stringResource(Res.string.sort)) {
                        dismiss()
                        appsVM.showSortDialog.value = true
                    }
                }
            } } else null)
    }) { paddingValues ->
        Column(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
            if (!appsVM.hasPermission.value) {
                NeedPermissionColumn(UiRes.drawable.package2, com.ismartcoding.plain.platform.permissions.FeaturePermission(setOf(com.ismartcoding.plain.platform.permissions.Permission.QUERY_ALL_PACKAGES), com.ismartcoding.plain.platform.permissions.Permission.QUERY_ALL_PACKAGES))
                return@PScaffold
            }
            if (appsVM.tabs.value.isEmpty()) { NoDataColumn(loading = appsVM.showLoading.value, search = appsVM.showSearchBar.value); return@PScaffold }
            AppsPageList(navController = navController, appsVM = appsVM, items = itemsState,
                scrollState = scrollState, scrollBehavior = scrollBehavior, topRefreshLayoutState = topRefreshLayoutState, paddingValues = paddingValues)
        }
    }
    }
}

