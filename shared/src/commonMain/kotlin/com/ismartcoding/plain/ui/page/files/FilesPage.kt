package com.ismartcoding.plain.ui.page.files

import com.ismartcoding.plain.i18n.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.enums.FilesType
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.isGranted
import com.ismartcoding.plain.preferences.ShowHiddenFilesPreference
import com.ismartcoding.plain.ui.base.ActionButtonSearch
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.PCapsuleMoreClose
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.base.PDropdownMenuItemCreateFile
import com.ismartcoding.plain.ui.base.PDropdownMenuItemCreateFolder
import com.ismartcoding.plain.ui.base.PDropdownMenuItemSort
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopRightButton
import com.ismartcoding.plain.ui.base.SearchableTopBar
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.platform.MediaPreviewer
import com.ismartcoding.plain.events.FolderKanbanSelectEvent
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.preferences.FavoriteFoldersPreference
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.SidebarItem
import com.ismartcoding.plain.ui.components.buildFolderOptions
import com.ismartcoding.plain.ui.models.FolderOption
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.FilesViewModel
import com.ismartcoding.plain.ui.models.enterSearchMode
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.isAllSelected
import com.ismartcoding.plain.ui.models.showBottomActions
import com.ismartcoding.plain.ui.models.toggleSelectAll
import com.ismartcoding.plain.ui.page.files.components.BreadcrumbView
import com.ismartcoding.plain.ui.page.files.components.FileListContent
import com.ismartcoding.plain.ui.page.files.components.FilePasteBar
import com.ismartcoding.plain.features.file.ZipBrowserHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FilesPage(
    navController: NavHostController, audioPlaylistVM: AudioPlaylistViewModel,
    chatVM: ChatViewModel, folderPath: String = "", filesVM: FilesViewModel = viewModel { FilesViewModel() },
) {
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val previewerState = rememberPreviewerState()
    val itemsState by filesVM.itemsFlow.collectAsState()
    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launch { filesVM.loadAsync(); setRefreshState(RefreshContentState.Finished) }
    }

    FilesPageEffects(filesVM, scope, folderPath, previewerState, audioPlaylistVM)
    FilesPageDialogs(filesVM, scope, navController, chatVM)

    val title = when {
        filesVM.selectMode.value -> stringRes(Res.string.x_selected, "count" to filesVM.selectedIds.size)

        filesVM.type == FilesType.RECENTS -> stringResource(Res.string.recents)
        ZipBrowserHelper.isZipPath(filesVM.selectedPath) -> ZipBrowserHelper.getDisplayName(filesVM.selectedPath)
        filesVM.selectedPath != filesVM.rootPath -> filesVM.selectedPath.getFilenameFromPath()
        else -> stringResource(Res.string.files)
    }
    val subtitle = if (!filesVM.selectMode.value) {
        val fc = itemsState.count { it.isDir };
        val flc = itemsState.count { !it.isDir }
        val sl = mutableListOf<String>()
        if (fc > 0) sl.add(pluralStringResource(Res.plurals.x_folders, fc, fc))
        if (flc > 0) sl.add(pluralStringResource(Res.plurals.x_files, flc, flc))
        sl.joinToString(", ")
    } else ""

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                FilesDrawerContent(
                    filesVM = filesVM,
                    onSelect = { scope.launch { drawerState.close() } },
                    refreshSignal = filesVM.favoriteFoldersVersion.value,
                    onNavigateShares = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Routing.Shares)
                    },
                )
            }
        }) {
    PScaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), topBar = {
        SearchableTopBar(
            navController = navController,
            viewModel = filesVM,
            scrollBehavior = scrollBehavior,
            title = title,
            subtitle = subtitle,
            navigationIcon = { if (filesVM.selectMode.value) NavigationCloseIcon { filesVM.exitSelectMode() } else PIconButton(icon = Res.drawable.left_panel_open, contentDescription = stringResource(Res.string.folders), click = { scope.launch { if (drawerState.isOpen) drawerState.close() else drawerState.open() } }) },
            actions = {
                if (!filesVM.selectMode.value) {
                    ActionButtonSearch { filesVM.enterSearchMode() }
                    PCapsuleMoreClose(
                        onClose = { navController.navigateUp() },
                    ) { dismiss ->
                        PDropdownMenuItemSort {
                            dismiss()
                            filesVM.showSortDialog.value = true
                        }
                        var showHiddenFiles by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            showHiddenFiles = ShowHiddenFilesPreference.getAsync()
                        }
                        PDropdownMenuItem(
                            text = { Text(stringResource(Res.string.show_hidden_files)) },
                            leadingIcon = {
                                Checkbox(
                                    checked = showHiddenFiles,
                                    onCheckedChange = null
                                )
                            },
                            onClick = {
                                dismiss()
                                scope.launch(Dispatchers.Default) {
                                    val nv = !showHiddenFiles
                                    ShowHiddenFilesPreference.putAsync(nv)
                                    showHiddenFiles = nv; filesVM.loadAsync()
                                }
                            })
                        if (!ZipBrowserHelper.isZipPath(filesVM.selectedPath)) {
                            PDropdownMenuItemCreateFolder {
                                dismiss()
                                filesVM.showCreateFolderDialog.value = true
                            }
                            PDropdownMenuItemCreateFile {
                                dismiss()
                                filesVM.showCreateFileDialog.value = true
                            }
                        }
                    }
                } else {
                    PTopRightButton(
                        label = stringResource(if (filesVM.isAllSelected()) Res.string.unselect_all else Res.string.select_all),
                        click = { filesVM.toggleSelectAll() }); HorizontalSpace(dp = 8.dp)
                }
            },
            onSearchAction = { q ->
                filesVM.queryText.value = q; scope.launch(Dispatchers.Default) {
                filesVM.loadAsync()
            }
            })
    }, bottomBar = {
        AnimatedVisibility(
            visible = filesVM.showBottomActions() && !ZipBrowserHelper.isZipPath(filesVM.selectedPath),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()) {
            FilesSelectModeBottomActions(
                filesVM = filesVM,
                onShowPasteBar = { filesVM.showPasteBar.value = it })
        }
        AnimatedVisibility(
            visible = filesVM.showPasteBar.value && !ZipBrowserHelper.isZipPath(filesVM.selectedPath),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()) {
            FilePasteBar(
                filesVM = filesVM,
                coroutineScope = scope,
                onPasteComplete = { scope.launch(Dispatchers.Default) { filesVM.loadAsync() } })
        }
    }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (Permission.WRITE_EXTERNAL_STORAGE.isGranted() && filesVM.type != FilesType.RECENTS) {
                BreadcrumbView(
                    breadcrumbs = filesVM.breadcrumbs,
                    selectedIndex = filesVM.selectedBreadcrumbIndex.value,
                    onItemClick = { filesVM.navigateToDirectory(it.path) })
            }
            PullToRefresh(refreshLayoutState = topRefreshLayoutState) {
                FileListContent(
                    navController = navController,
                    filesVM = filesVM,
                    files = itemsState,
                    loadFiles = { _, _ -> scope.launch(Dispatchers.Default) { filesVM.loadAsync() } },
                    previewerState = previewerState,
                    audioPlaylistVM = audioPlaylistVM
                )
            }
        }
    }
    }
    MediaPreviewer(state = previewerState)
}

/**
 * Drawer content for the Files page: folder picker options as SidebarItems,
 * with a "Share links" entry at the top-level.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilesDrawerContent(
    filesVM: FilesViewModel,
    onSelect: (FolderOption) -> Unit,
    refreshSignal: Int,
    onNavigateShares: () -> Unit,
) {
    val recentsText = stringResource(Res.string.recents)
    val internalStorageText = stringResource(Res.string.internal_storage)
    val sdcardText = stringResource(Res.string.sdcard)
    val usbStorageText = stringResource(Res.string.usb_storage)
    val fileTransferAssistantText = stringResource(Res.string.app_data)
    val scope = rememberCoroutineScope()
    val options = remember { mutableStateListOf<FolderOption>() }

    LaunchedEffect(refreshSignal, filesVM.selectedPathVersion.value) {
        val items = buildFolderOptions(filesVM, recentsText, internalStorageText, sdcardText, usbStorageText, fileTransferAssistantText)
        options.clear()
        options.addAll(items)
    }

    Column(
        modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .verticalScroll(rememberScrollState())
    ) {
        VerticalSpace(dp = 16.dp)
        options.forEach { item ->
            SidebarItem(
                label = item.title + if (item.isFavoriteFolder) " ⭐" else "",
                icon = if (item.type == FilesType.RECENTS) Res.drawable.history else Res.drawable.folder,
                isSelected = item.isChecked,
                onLongClick = if (item.isFavoriteFolder) {
                    {
                        scope.launch {
                            FavoriteFoldersPreference.removeAsync(item.fullPath)
                            filesVM.favoriteFoldersVersion.value++
                        }
                    }
                } else null,
                onClick = { sendEvent(FolderKanbanSelectEvent(item)); onSelect(item) }
            )
        }
        VerticalSpace(dp = 16.dp)
        HorizontalDivider()
        VerticalSpace(dp = 8.dp)
        SidebarItem(
            label = stringResource(Res.string.share_links),
            subtitle = stringResource(Res.string.share_links_desc),
            icon = Res.drawable.link,
            onClick = onNavigateShares
        )
        VerticalSpace(dp = 16.dp)
    }
}
