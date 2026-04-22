package com.ismartcoding.plain.ui.page.files

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.FilesType
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.preferences.ShowHiddenFilesPreference
import com.ismartcoding.plain.ui.base.ActionButtonFolderKanban
import com.ismartcoding.plain.ui.base.ActionButtonMoreWithMenu
import com.ismartcoding.plain.ui.base.ActionButtonSearch
import com.ismartcoding.plain.ui.base.ActionButtonSort
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.base.PDropdownMenuItemCreateFile
import com.ismartcoding.plain.ui.base.PDropdownMenuItemCreateFolder
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopRightButton
import com.ismartcoding.plain.ui.base.SearchableTopBar
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewer
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.FilesViewModel
import com.ismartcoding.plain.ui.models.enterSearchMode
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.isAllSelected
import com.ismartcoding.plain.ui.models.showBottomActions
import com.ismartcoding.plain.ui.models.toggleSelectAll
import com.ismartcoding.plain.ui.page.files.components.BreadcrumbView
import com.ismartcoding.plain.ui.page.files.components.FileListContent
import com.ismartcoding.plain.ui.page.files.components.FilePasteBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FilesPage(
    navController: NavHostController, audioPlaylistVM: AudioPlaylistViewModel,
    folderPath: String = "", filesVM: FilesViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val previewerState = rememberPreviewerState()
    val itemsState by filesVM.itemsFlow.collectAsState()
    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launch { withIO { filesVM.loadAsync(context) }; setRefreshState(RefreshContentState.Finished) }
    }

    FilesPageEffects(filesVM, context, scope, folderPath, previewerState, audioPlaylistVM)
    FilesPageDialogs(filesVM, context, scope)

    val title = when {
        filesVM.selectMode.value -> LocaleHelper.getStringF(
            R.string.x_selected,
            "count",
            filesVM.selectedIds.size
        )

        filesVM.type == FilesType.RECENTS -> stringResource(R.string.recents)
        filesVM.selectedPath != filesVM.rootPath -> filesVM.selectedPath.getFilenameFromPath()
        else -> stringResource(R.string.files)
    }
    val subtitle = if (!filesVM.selectMode.value) {
        val fc = itemsState.count { it.isDir };
        val flc = itemsState.count { !it.isDir }
        val sl = mutableListOf<String>()
        if (fc > 0) sl.add(LocaleHelper.getQuantityString(R.plurals.x_folders, fc))
        if (flc > 0) sl.add(LocaleHelper.getQuantityString(R.plurals.x_files, flc))
        sl.joinToString(", ")
    } else ""

    PScaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), topBar = {
        SearchableTopBar(
            navController = navController,
            viewModel = filesVM,
            scrollBehavior = scrollBehavior,
            title = title,
            subtitle = subtitle,
            navigationIcon = { if (filesVM.selectMode.value) NavigationCloseIcon { filesVM.exitSelectMode() } else NavigationBackIcon { navController.navigateUp() } },
            actions = {
                if (!filesVM.selectMode.value) {
                    ActionButtonSearch { filesVM.enterSearchMode() }
                    ActionButtonFolderKanban { filesVM.showFolderKanbanDialog.value = true }
                    ActionButtonSort { filesVM.showSortDialog.value = true }
                    ActionButtonMoreWithMenu { dismiss ->
                        var showHiddenFiles by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            showHiddenFiles = withContext(Dispatchers.IO) {
                                ShowHiddenFilesPreference.getAsync(context)
                            }
                        }
                        PDropdownMenuItem(
                            text = { Text(stringResource(R.string.show_hidden_files)) },
                            leadingIcon = {
                                Checkbox(
                                    checked = showHiddenFiles,
                                    onCheckedChange = null
                                )
                            },
                            onClick = {
                                dismiss()
                                scope.launch(Dispatchers.IO) {
                                    val nv = !showHiddenFiles
                                    ShowHiddenFilesPreference.putAsync(
                                        context,
                                        nv
                                    )
                                    showHiddenFiles = nv; filesVM.loadAsync(context)
                                }
                            })
                        PDropdownMenuItemCreateFolder {
                            dismiss()
                            filesVM.showCreateFolderDialog.value = true
                        }
                        PDropdownMenuItemCreateFile {
                            dismiss()
                            filesVM.showCreateFileDialog.value = true
                        }
                    }
                } else {
                    PTopRightButton(
                        label = stringResource(if (filesVM.isAllSelected()) R.string.unselect_all else R.string.select_all),
                        click = { filesVM.toggleSelectAll() }); HorizontalSpace(dp = 8.dp)
                }
            },
            onSearchAction = { q ->
                filesVM.queryText.value = q; scope.launch(Dispatchers.IO) {
                filesVM.loadAsync(
                    context
                )
            }
            })
    }, bottomBar = {
        AnimatedVisibility(
            visible = filesVM.showBottomActions(),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()) {
            FilesSelectModeBottomActions(
                filesVM = filesVM,
                onShowPasteBar = { filesVM.showPasteBar.value = it })
        }
        AnimatedVisibility(
            visible = filesVM.showPasteBar.value,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()) {
            FilePasteBar(
                filesVM = filesVM,
                coroutineScope = scope,
                onPasteComplete = { scope.launch(Dispatchers.IO) { filesVM.loadAsync(context) } })
        }
    }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (Permission.WRITE_EXTERNAL_STORAGE.can(context) && filesVM.type != FilesType.RECENTS) {
                BreadcrumbView(
                    breadcrumbs = filesVM.breadcrumbs,
                    selectedIndex = filesVM.selectedBreadcrumbIndex.value,
                    onItemClick = { filesVM.navigateToDirectory(context, it.path) })
            }
            PullToRefresh(refreshLayoutState = topRefreshLayoutState) {
                FileListContent(
                    navController = navController,
                    filesVM = filesVM,
                    files = itemsState,
                    loadFiles = { _, _ -> scope.launch(Dispatchers.IO) { filesVM.loadAsync(context) } },
                    previewerState = previewerState,
                    audioPlaylistVM = audioPlaylistVM
                )
            }
        }
    }
    MediaPreviewer(state = previewerState)
}
