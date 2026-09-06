package com.ismartcoding.plain.ui.page.notes

import com.ismartcoding.plain.i18n.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.IODispatcher
import com.ismartcoding.plain.platform.PBackHandler
import com.ismartcoding.plain.ui.base.ActionButtonSearch
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PCapsuleMoreClose
import com.ismartcoding.plain.ui.base.PDropdownMenuItemTags
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PTopRightButton
import com.ismartcoding.plain.ui.base.PDraggableElement
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.fastscroll.LazyColumnScrollbar
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.ListSearchBar
import com.ismartcoding.plain.ui.components.NoteListItem
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.enterSearchMode
import com.ismartcoding.plain.ui.models.exitSearchMode
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.isAllSelected
import com.ismartcoding.plain.ui.models.select
import com.ismartcoding.plain.ui.models.showBottomActions
import com.ismartcoding.plain.ui.models.toggleSelectAll
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.tags.TagsBottomSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesPage(navController: NavHostController, tagsVM: TagsViewModel) {
    val itemsState by NotesViewModel.itemsFlow.collectAsState()
    val tagsState by tagsVM.itemsFlow.collectAsState()
    val tagsMapState by tagsVM.tagsMapFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = { scrollState.firstVisibleItemIndex > 0 && !NotesViewModel.selectMode.value })
    val isFirstTime = remember { mutableStateOf(true) }
    val topRefreshLayoutState = rememberRefreshLayoutState { scope.launch { NotesViewModel.loadAsync(tagsVM) }; setRefreshState(RefreshContentState.Finished) }

    tagsVM.dataType.value = NotesViewModel.dataType
    NotesPageEffects(tagsVM, scrollBehavior, scrollState, scope, isFirstTime)

    val pageTitle = if (NotesViewModel.selectMode.value) LocaleHelper.getStringF(Res.string.x_selected, NotesViewModel.selectedIds.size)
        else if (NotesViewModel.tag.value != null) NotesViewModel.tag.value!!.name
        else if (NotesViewModel.trash.value) stringResource(Res.string.notes) + " - " + stringResource(Res.string.trash)
        else stringResource(Res.string.notes)
    ViewNoteBottomSheet(tagsVM, tagsMapState, tagsState)
    if (NotesViewModel.showTagsDialog.value) { TagsBottomSheet(tagsVM) { NotesViewModel.showTagsDialog.value = false } }
    val onSearch: (String) -> Unit = { NotesViewModel.searchActive.value = false; NotesViewModel.showLoading.value = true; scope.launch { scrollState.scrollToItem(0) }; scope.launch(IODispatcher) { NotesViewModel.loadAsync(tagsVM) } }
    PBackHandler(enabled = NotesViewModel.selectMode.value || NotesViewModel.showSearchBar.value || drawerState.isOpen) {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            NotesViewModel.selectMode.value -> NotesViewModel.exitSelectMode()
            else -> if (NotesViewModel.showSearchBar.value) { if (!NotesViewModel.searchActive.value || NotesViewModel.queryText.value.isEmpty()) { NotesViewModel.exitSearchMode(); onSearch("") } }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                NotesSidebarDrawer(tagsVM, drawerState)
            }
        },
    ) {
        PScaffold(
            topBar = {
                if (NotesViewModel.showSearchBar.value) { ListSearchBar(viewModel = NotesViewModel, onSearch = onSearch); return@PScaffold }
                PTopAppBar(modifier = Modifier.combinedClickable(onClick = {}, onDoubleClick = { scope.launch { scrollState.scrollToItem(0) } }),
                    navController = navController, navigationIcon = {
                        if (NotesViewModel.selectMode.value) NavigationCloseIcon { NotesViewModel.exitSelectMode() }
                        else PIconButton(
                            icon = Res.drawable.left_panel_open,
                            contentDescription = null,
                            click = { scope.launch { if (drawerState.isOpen) drawerState.close() else drawerState.open() } }
                        )
                    },
                    title = pageTitle, scrollBehavior = scrollBehavior, actions = {
                        if (NotesViewModel.selectMode.value) { PTopRightButton(label = stringResource(if (NotesViewModel.isAllSelected()) Res.string.unselect_all else Res.string.select_all), click = { NotesViewModel.toggleSelectAll() }); HorizontalSpace(dp = 8.dp) }
                        else {
                            ActionButtonSearch { NotesViewModel.enterSearchMode() }
                            PCapsuleMoreClose(onClose = { navController.navigateUp() }) { dismiss ->
                                PDropdownMenuItemTags {
                                    dismiss()
                                    NotesViewModel.showTagsDialog.value = true
                                }
                            }
                        }
                    })
            },
            bottomBar = { AnimatedVisibility(visible = NotesViewModel.showBottomActions(), enter = slideInVertically { it }, exit = slideOutVertically { it }) { NotesSelectModeBottomActions(tagsVM, tagsState) } },
            floatingActionButton = if (NotesViewModel.selectMode.value) null else { { PDraggableElement { FloatingActionButton(onClick = { navController.navigate(Routing.NotesCreate(NotesViewModel.tag.value?.id ?: "")) }) { Icon(painter = painterResource(Res.drawable.plus), stringResource(Res.string.add)) } } } },
        ) { paddingValues ->
            Column(Modifier.padding(top = paddingValues.calculateTopPadding())) {
                PullToRefresh(refreshLayoutState = topRefreshLayoutState) {
                    if (itemsState.isNotEmpty()) {
                        LazyColumnScrollbar(state = scrollState) {
                            LazyColumn(Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection), state = scrollState) {
                                item { TopSpace() }
                                items(itemsState, key = { it.id }) { m ->
                                    val tagIds = tagsMapState[m.id]?.map { it.tagId } ?: emptyList()
                                    NoteListItem(m, tagsState.filter { tagIds.contains(it.id) },
                                        onClick = { if (NotesViewModel.selectMode.value) NotesViewModel.select(m.id) else navController.navigate(Routing.NoteDetail(m.id)) },
                                        onLongClick = { if (NotesViewModel.selectMode.value) return@NoteListItem; NotesViewModel.selectedItem.value = m },
                                        onClickTag = { tag ->
                                            if (NotesViewModel.selectMode.value) return@NoteListItem
                                            NotesViewModel.trash.value = false
                                            NotesViewModel.tag.value = tag
                                        },
                                    )
                                    VerticalSpace(dp = 8.dp)
                                }
                                item {
                                    if (itemsState.isNotEmpty() && !NotesViewModel.noMore.value) {
                                        LaunchedEffect(Unit) { scope.launch(IODispatcher) { NotesViewModel.moreAsync(tagsVM) } }
                                    }
                                    LoadMoreRefreshContent(NotesViewModel.noMore.value)
                                }
                                item { VerticalSpace(dp = paddingValues.calculateBottomPadding()) }
                            }
                        }
                    } else {
                        NoDataColumn(loading = NotesViewModel.showLoading.value, search = NotesViewModel.showSearchBar.value)
                    }
                }
            }
        }
    }
}
