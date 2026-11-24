package com.ismartcoding.plain.ui.files

import com.ismartcoding.plain.ui.navigation.LocalContentSource
import com.ismartcoding.plain.rust.graphql.ContentSource
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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.features.files.FilesType
import com.ismartcoding.plain.ui.components.ActionButtonSearch
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.components.NavigationCloseIcon
import com.ismartcoding.plain.ui.components.NeedPermissionColumn
import com.ismartcoding.plain.ui.components.PCapsuleMoreClose
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.PTopRightButton
import com.ismartcoding.plain.ui.components.SearchableTopBar
import com.ismartcoding.plain.ui.platform.media.MediaPreviewer
import com.ismartcoding.plain.ui.media.viewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.ui.state.exitSearchMode
import com.ismartcoding.plain.ui.state.enterSearchMode
import com.ismartcoding.plain.ui.state.exitSelectMode
import com.ismartcoding.plain.ui.state.showBottomActions
import com.ismartcoding.plain.ui.files.components.BreadcrumbView
import com.ismartcoding.plain.ui.files.components.FileListContent
import com.ismartcoding.plain.ui.files.components.FilePasteBar
import com.ismartcoding.plain.features.files.ZipBrowserHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.folder as ui_drawable_folder
import com.ismartcoding.plain.ui.resources.left_panel_open as ui_drawable_left_panel_open
import com.ismartcoding.plain.i18n.folder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FilesPage(
    navController: NavHostController, audioQueueVM: AudioQueueViewModel,
    folderPath: String = "",
    source: ContentSource = LocalContentSource.current,
    filesVM: FilesViewModel = viewModel { FilesViewModel(source) },
) {
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var showTrash by rememberSaveable(source) { mutableStateOf(false) }
    val previewerState = rememberPreviewerState()
    val itemsState by filesVM.itemsFlow.collectAsState()
    var systemExpanded by remember(filesVM.selectedPath, filesVM.queryText.value) { mutableStateOf(false) }
    val groupSystem = !showTrash && !(filesVM.showSearchBar.value && filesVM.getQuery().isNotEmpty()) && filesVM.type != FilesType.RECENTS && !ZipBrowserHelper.isZipPath(filesVM.selectedPath)
    val systemFiles = if (groupSystem) itemsState.filter { it.isSystem } else emptyList()
    val canListFiles = filesVM.hasPermission.value || filesVM.source is ContentSource.Local && filesVM.type == FilesType.APP
    val trash = if (showTrash && canListFiles) rememberFilesTrash(filesVM) else null
    val regularFiles = when {
        showTrash -> trash?.files.orEmpty()
        groupSystem -> itemsState.filterNot { it.isSystem }
        else -> itemsState
    }
    val visibleFiles = if (systemExpanded) systemFiles + regularFiles else regularFiles
    var showMoreSheet by remember { mutableStateOf(false) }
    val canBrowse = filesVM.source is ContentSource.Local || filesVM.hasPermission.value
    LaunchedEffect(canBrowse) {
        if (!canBrowse) {
            showMoreSheet = false
            drawerState.close()
        }
    }
    FilesPageEffects(filesVM, scope, folderPath, previewerState, audioQueueVM, showTrash)
    if (canBrowse && !showTrash) FilesPageDialogs(filesVM, scope)
    if (canListFiles) FileInfoBottomSheet(filesVM, showingTrash = showTrash)

    if (canBrowse && !showTrash && showMoreSheet) {
        FilesMoreActionsSheet(filesVM = filesVM, onDismiss = { showMoreSheet = false })
    }

    PBackHandler(enabled = showTrash && !drawerState.isOpen && !filesVM.selectMode.value && !previewerState.visible) {
        showTrash = false
        scope.launch { filesVM.loadAsync() }
    }

    val drawerFolderTitle = filesVM.currentFolderTitleOverride()
    val title = when {
        filesVM.selectMode.value -> stringResource(Res.string.x_selected, filesVM.selectedIds.size)

        showTrash -> stringResource(Res.string.trash)
        drawerFolderTitle != null -> drawerFolderTitle
        filesVM.type == FilesType.RECENTS -> stringResource(Res.string.recents)
        ZipBrowserHelper.isZipPath(filesVM.selectedPath) -> ZipBrowserHelper.getDisplayName(filesVM.selectedPath)
        filesVM.selectedPath != filesVM.rootPath -> filesVM.selectedPath.getFilenameFromPath()
        else -> stringResource(Res.string.files)
    }
    val subtitle = if (!filesVM.selectMode.value) {
        val fc = regularFiles.count { it.isDir };
        val flc = regularFiles.count { !it.isDir }
        val sl = mutableListOf<String>()
        if (fc > 0) sl.add(pluralStringResource(Res.plurals.x_folders, fc, fc))
        if (flc > 0) sl.add(pluralStringResource(Res.plurals.x_files, flc, flc))
        sl.joinToString(", ")
    } else ""

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = canBrowse,
        drawerContent = {
            if (canBrowse) ModalDrawerSheet {
                FilesDrawerContent(
                    filesVM = filesVM,
                    drawerState = drawerState,
                    navController = navController,
                    showingTrash = showTrash,
                    onTrash = {
                        filesVM.exitSelectMode()
                        filesVM.exitSearchMode()
                        showTrash = true
                        scope.launch { drawerState.close() }
                    },
                    onSelect = { folder ->
                        filesVM.exitSelectMode()
                        showTrash = false
                        filesVM.offset = 0
                        filesVM.drawerFolderTitle.value = if (folder.isFavoriteFolder) folder.fullPath to folder.title else null
                        filesVM.initSelectedPath(folder.rootPath, folder.type, folder.fullPath, folder.fullPath)
                        scope.launch(Dispatchers.Default) { filesVM.loadAsync() }
                        scope.launch { drawerState.close() }
                    },
                )
            }
        }) {
    PScaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), topBar = toolbar@ {
        if (!canBrowse) {
            PTopAppBar(onNavigateBack = { navController.navigateUp() }, title = title)
            return@toolbar
        }
        SearchableTopBar(
            navController = navController,
            viewModel = filesVM,
            scrollBehavior = scrollBehavior,
            title = title,
            subtitle = subtitle,
            navigationIcon = { if (filesVM.selectMode.value) NavigationCloseIcon { filesVM.exitSelectMode() } else PIconButton(icon = UiRes.drawable.ui_drawable_left_panel_open, contentDescription = stringResource(Res.string.folders), click = { scope.launch { if (drawerState.isOpen) drawerState.close() else drawerState.open() } }) },
            actions = {
                if (!showTrash && !filesVM.selectMode.value) {
                    ActionButtonSearch { filesVM.enterSearchMode() }
                    PCapsuleMoreClose(
                        onClose = { navController.navigateUp() },
                        onMore = { showMoreSheet = true },
                    )
                } else if (filesVM.selectMode.value) {
                    PTopRightButton(
                        label = stringResource(if (visibleFiles.all { it.id in filesVM.selectedIds }) Res.string.unselect_all else Res.string.select_all),
                        click = {
                            val allSelected = visibleFiles.all { it.id in filesVM.selectedIds }
                            if (allSelected && showTrash) {
                                filesVM.exitSelectMode()
                            } else {
                                filesVM.selectedIds.clear()
                                if (!allSelected) filesVM.selectedIds.addAll(visibleFiles.map { it.id })
                            }
                        }); HorizontalSpace(dp = 8.dp)
                }
            },
            onSearchAction = { q ->
                filesVM.queryText.value = q; scope.launch(Dispatchers.Default) {
                filesVM.loadAsync()
            }
            })
    }, bottomBar = {
        if (showTrash) {
            if (canBrowse && filesVM.showBottomActions()) FilesTrashActions(source, filesVM.selectedIds.toSet())
        } else AnimatedVisibility(
            visible = canBrowse && filesVM.showBottomActions() && !ZipBrowserHelper.isZipPath(filesVM.selectedPath),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()) {
            FilesSelectModeBottomActions(
                filesVM = filesVM,
                onShowPasteBar = { filesVM.showPasteBar.value = it })
        }
        AnimatedVisibility(
            visible = !showTrash && canBrowse && filesVM.showPasteBar.value && !ZipBrowserHelper.isZipPath(filesVM.selectedPath),
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
            if (!canListFiles) {
                NeedPermissionColumn(UiRes.drawable.ui_drawable_folder, AppFeatureType.FILES.getPermission()!!)
                return@Column
            }
            if (!showTrash && filesVM.type != FilesType.RECENTS) {
                BreadcrumbView(
                    breadcrumbs = filesVM.breadcrumbs,
                    selectedIndex = filesVM.selectedBreadcrumbIndex.value,
                    onItemClick = { filesVM.navigateToDirectory(it.path) })
            }
            key(showTrash) {
                FileListContent(
                    navController = navController,
                    filesVM = filesVM,
                    files = visibleFiles,
                    systemCount = systemFiles.size,
                    systemExpanded = groupSystem && systemExpanded,
                    onToggleSystem = { systemExpanded = !systemExpanded; filesVM.selectedIds.clear() },
                    previewerState = previewerState,
                    audioQueueVM = audioQueueVM,
                    showingTrash = showTrash,
                    loading = if (showTrash) trash?.files == null else filesVM.isLoading.value,
                    onRefresh = trash?.let { it::refresh } ?: filesVM::loadAsync,
                )
            }
        }
    }
    }
    MediaPreviewer(state = previewerState)
}
