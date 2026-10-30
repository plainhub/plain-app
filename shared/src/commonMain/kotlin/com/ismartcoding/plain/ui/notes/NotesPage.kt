package com.ismartcoding.plain.ui.notes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

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
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.ui.components.ActionButtonSearch
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.components.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.components.PCapsuleMoreClose
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.base.PSheetActionRow
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
import com.ismartcoding.plain.ui.notes.components.NoteListItem
import com.ismartcoding.plain.ui.notes.NotesViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.state.enterSearchMode
import com.ismartcoding.plain.ui.state.exitSearchMode
import com.ismartcoding.plain.ui.state.exitSelectMode
import com.ismartcoding.plain.ui.state.isAllSelected
import com.ismartcoding.plain.ui.state.select
import com.ismartcoding.plain.ui.state.showBottomActions
import com.ismartcoding.plain.ui.state.toggleSelectAll
import com.ismartcoding.plain.ui.navigation.Routing
import com.ismartcoding.plain.ui.tags.TagsBottomSheet
import kotlinx.coroutines.launch
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.left_panel_open as ui_drawable_left_panel_open
import com.ismartcoding.plain.ui.resources.plus as ui_drawable_plus
import com.ismartcoding.plain.ui.resources.tags as ui_drawable_tags
import com.ismartcoding.plain.i18n.tags

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesPage(navController: NavHostController, tagsVM: TagsViewModel,
    notesVM: NotesViewModel = NotesViewModel.local,
) {
    val itemsState by notesVM.itemsFlow.collectAsState()
    val tagsState by tagsVM.itemsFlow.collectAsState()
    val tagsMapState by tagsVM.tagsMapFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = { scrollState.firstVisibleItemIndex > 0 && !notesVM.selectMode.value })
    val isFirstTime = remember { mutableStateOf(true) }
    val topRefreshLayoutState = rememberRefreshLayoutState { scope.launch { notesVM.loadAsync(tagsVM) }; setRefreshState(RefreshContentState.Finished) }

    tagsVM.dataType.value = notesVM.dataType
    NotesPageEffects(tagsVM, scrollBehavior, scrollState, scope, isFirstTime, notesVM)

    val pageTitle = if (notesVM.selectMode.value) LocaleHelper.getStringF(Res.string.x_selected, notesVM.selectedIds.size)
        else if (notesVM.tag.value != null) notesVM.tag.value!!.name
        else if (notesVM.trash.value) stringResource(Res.string.notes) + " - " + stringResource(Res.string.trash)
        else stringResource(Res.string.notes)
    ViewNoteBottomSheet(tagsVM, tagsMapState, tagsState, notesVM)
    if (notesVM.showTagsDialog.value) { TagsBottomSheet(tagsVM) { notesVM.showTagsDialog.value = false } }
    val onSearch: (String) -> Unit = { notesVM.searchActive.value = false; notesVM.showLoading.value = true; scope.launch { scrollState.scrollToItem(0) }; scope.launch(Dispatchers.IO) { notesVM.loadAsync(tagsVM) } }
    PBackHandler(enabled = notesVM.selectMode.value || notesVM.showSearchBar.value || drawerState.isOpen) {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            notesVM.selectMode.value -> notesVM.exitSelectMode()
            else -> if (notesVM.showSearchBar.value) { if (!notesVM.searchActive.value || notesVM.queryText.value.isEmpty()) { notesVM.exitSearchMode(); onSearch("") } }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                NotesSidebarDrawer(tagsVM, drawerState, notesVM)
            }
        },
    ) {
        PScaffold(
            topBar = {
                if (notesVM.showSearchBar.value) { ListSearchBar(viewModel = notesVM, onSearch = onSearch); return@PScaffold }
                PTopAppBar(modifier = Modifier.combinedClickable(onClick = {}, onDoubleClick = { scope.launch { scrollState.scrollToItem(0) } }),
                    onNavigateBack = { navController.navigateUp() }, navigationIcon = {
                        if (notesVM.selectMode.value) NavigationCloseIcon { notesVM.exitSelectMode() }
                        else PIconButton(
                            icon = UiRes.drawable.ui_drawable_left_panel_open,
                            contentDescription = null,
                            click = { scope.launch { if (drawerState.isOpen) drawerState.close() else drawerState.open() } }
                        )
                    },
                    title = pageTitle, scrollBehavior = scrollBehavior, actions = {
                        if (notesVM.selectMode.value) { PTopRightButton(label = stringResource(if (notesVM.isAllSelected()) Res.string.unselect_all else Res.string.select_all), click = { notesVM.toggleSelectAll() }); HorizontalSpace(dp = 8.dp) }
                        else {
                            ActionButtonSearch { notesVM.enterSearchMode() }
                            PCapsuleMoreClose(onClose = { navController.navigateUp() }) { dismiss ->
                                PSheetActionRow(UiRes.drawable.ui_drawable_tags, stringResource(Res.string.tags)) {
                                    dismiss()
                                    notesVM.showTagsDialog.value = true
                                }
                            }
                        }
                    })
            },
            bottomBar = { AnimatedVisibility(visible = notesVM.showBottomActions(), enter = slideInVertically { it }, exit = slideOutVertically { it }) { NotesSelectModeBottomActions(tagsVM, tagsState, notesVM) } },
            floatingActionButton = if (notesVM.selectMode.value) null else { { PDraggableElement { FloatingActionButton(onClick = { navController.navigate(Routing.NotesCreate(notesVM.tag.value?.id ?: "")) }) { Icon(painter = painterResource(UiRes.drawable.ui_drawable_plus), stringResource(Res.string.add)) } } } },
        ) { paddingValues ->
            Column(Modifier.padding(top = paddingValues.calculateTopPadding())) {
                PullToRefresh(refreshLayoutState = topRefreshLayoutState) {
                    if (itemsState.isNotEmpty()) {
                        LazyColumnScrollbar(state = scrollState) {
                            LazyColumn(Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection), state = scrollState) {
                                item { TopSpace() }
                                items(itemsState, key = { it.id }) { m ->
                                    val tagIds = tagsMapState[m.id]?.map { it.tagId } ?: emptyList()
                                    NoteListItem(m, tagsState.filter { tagIds.contains(it.id) }, notesVM = notesVM,
                                        onClick = { if (notesVM.selectMode.value) notesVM.select(m.id) else navController.navigate(Routing.NoteDetail(m.id)) },
                                        onLongClick = { if (notesVM.selectMode.value) return@NoteListItem; notesVM.selectedItem.value = m },
                                        onClickTag = { tag ->
                                            if (notesVM.selectMode.value) return@NoteListItem
                                            notesVM.trash.value = false
                                            notesVM.tag.value = tag
                                        },
                                    )
                                    VerticalSpace(dp = 8.dp)
                                }
                                item {
                                    if (itemsState.isNotEmpty() && !notesVM.noMore.value) {
                                        LaunchedEffect(Unit) { scope.launch(Dispatchers.IO) { notesVM.moreAsync(tagsVM) } }
                                    }
                                    LoadMoreRefreshContent(notesVM.noMore.value)
                                }
                                item { VerticalSpace(dp = paddingValues.calculateBottomPadding()) }
                            }
                        }
                    } else {
                        NoDataColumn(loading = notesVM.showLoading.value, search = notesVM.showSearchBar.value)
                    }
                }
            }
        }
    }
}
