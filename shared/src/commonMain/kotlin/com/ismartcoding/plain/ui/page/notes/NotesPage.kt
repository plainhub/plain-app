package com.ismartcoding.plain.ui.page.notes

import com.ismartcoding.plain.i18n.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.ui.base.ActionButtonSearch
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.PCapsuleMoreClose
import com.ismartcoding.plain.ui.base.PDropdownMenuItemTags
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.platform.PBackHandler
import com.ismartcoding.plain.ui.base.PDraggableElement
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.PTopRightButton
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.ListSearchBar
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.enterSearchMode
import com.ismartcoding.plain.ui.models.exitSearchMode
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.isAllSelected
import com.ismartcoding.plain.ui.models.showBottomActions
import com.ismartcoding.plain.ui.models.toggleSelectAll
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.tags.TagsBottomSheet
import com.ismartcoding.plain.platform.IODispatcher
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesPage(navController: NavHostController, notesVM: NotesViewModel, tagsVM: TagsViewModel) {
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
    NotesPageEffects(notesVM, tagsVM, scrollBehavior, scrollState, scope, isFirstTime)

    val pageTitle = if (notesVM.selectMode.value) LocaleHelper.getStringF(Res.string.x_selected, "count", notesVM.selectedIds.size)
        else if (notesVM.tag.value != null) notesVM.tag.value!!.name
        else if (notesVM.trash.value) stringResource(Res.string.notes) + " - " + stringResource(Res.string.trash)
        else stringResource(Res.string.notes)
    ViewNoteBottomSheet(notesVM, tagsVM, tagsMapState, tagsState)
    if (notesVM.showTagsDialog.value) { TagsBottomSheet(tagsVM) { notesVM.showTagsDialog.value = false } }
    val onSearch: (String) -> Unit = { notesVM.searchActive.value = false; notesVM.showLoading.value = true; scope.launch { scrollState.scrollToItem(0) }; scope.launch(IODispatcher) { notesVM.loadAsync(tagsVM) } }
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
                NotesSidebarDrawer(notesVM, tagsVM, drawerState)
            }
        },
    ) {
        PScaffold(
            topBar = {
                if (notesVM.showSearchBar.value) { ListSearchBar(viewModel = notesVM, onSearch = onSearch); return@PScaffold }
                PTopAppBar(modifier = Modifier.combinedClickable(onClick = {}, onDoubleClick = { scope.launch { scrollState.scrollToItem(0) } }),
                    navController = navController, navigationIcon = {
                        if (notesVM.selectMode.value) NavigationCloseIcon { notesVM.exitSelectMode() }
                        else PIconButton(
                            icon = Res.drawable.left_panel_open,
                            contentDescription = null,
                            click = { scope.launch { if (drawerState.isOpen) drawerState.close() else drawerState.open() } }
                        )
                    },
                    title = pageTitle, scrollBehavior = scrollBehavior, actions = {
                        if (notesVM.selectMode.value) { PTopRightButton(label = stringResource(if (notesVM.isAllSelected()) Res.string.unselect_all else Res.string.select_all), click = { notesVM.toggleSelectAll() }); HorizontalSpace(dp = 8.dp) }
                        else {
                            ActionButtonSearch { notesVM.enterSearchMode() }
                            PCapsuleMoreClose(onClose = { navController.navigateUp() }) { dismiss ->
                                PDropdownMenuItemTags {
                                    dismiss()
                                    notesVM.showTagsDialog.value = true
                                }
                            }
                        }
                    })
            },
            bottomBar = { AnimatedVisibility(visible = notesVM.showBottomActions(), enter = slideInVertically { it }, exit = slideOutVertically { it }) { NotesSelectModeBottomActions(notesVM, tagsVM, tagsState) } },
            floatingActionButton = if (notesVM.selectMode.value) null else { { PDraggableElement { FloatingActionButton(onClick = { navController.navigate(Routing.NotesCreate(notesVM.tag.value?.id ?: "")) }) { Icon(painter = painterResource(Res.drawable.plus), stringResource(Res.string.add)) } } } },
        ) { paddingValues ->
            Column(Modifier.padding(top = paddingValues.calculateTopPadding())) {
                NotesPageContent(notesVM, tagsVM, itemsState, tagsState, tagsMapState, scrollState, scrollBehavior, topRefreshLayoutState, navController, paddingValues.calculateBottomPadding(), scope)
            }
        }
    }
}
