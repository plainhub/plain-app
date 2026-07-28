package com.ismartcoding.plain.ui.page.files

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.enums.FilesType
import com.ismartcoding.plain.features.file.ZipBrowserHelper
import com.ismartcoding.plain.platform.MediaPreviewer
import com.ismartcoding.plain.platform.PBackHandler
import com.ismartcoding.plain.platform.appDir
import com.ismartcoding.plain.platform.getInternalStoragePath
import com.ismartcoding.plain.platform.openFileExternal
import com.ismartcoding.plain.platform.shareFile
import com.ismartcoding.plain.preferences.FileSortByPreference
import com.ismartcoding.plain.preferences.ShowHiddenFilesPreference
import com.ismartcoding.plain.ui.base.ActionButtonMoreWithMenu
import com.ismartcoding.plain.ui.base.ActionButtonSort
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.components.FileSortDialog
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.FilesViewModel
import com.ismartcoding.plain.ui.page.files.components.BreadcrumbView
import com.ismartcoding.plain.ui.page.files.components.FileListContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ZipFilePage(
    navController: NavHostController,
    audioPlaylistVM: AudioPlaylistViewModel,
    path: String,
    title: String = "",
    filesVM: FilesViewModel = viewModel(),
) {
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val previewerState = rememberPreviewerState()
    val itemsState by filesVM.itemsFlow.collectAsState()
    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launch { filesVM.loadAsync(); setRefreshState(RefreshContentState.Finished) }
    }

    val zipVirtualRoot = remember(path) { ZipBrowserHelper.joinPath(path, "") }

    LaunchedEffect(path) {
        scope.launch(Dispatchers.Default) {
            val appDataPath = appDir()
            val type = if (path.startsWith(appDataPath)) FilesType.APP else FilesType.INTERNAL_STORAGE
            val rootPath = when (type) { FilesType.APP -> appDataPath; else -> getInternalStoragePath() }
            filesVM.initSelectedPath(rootPath, type, zipVirtualRoot, zipVirtualRoot)
            filesVM.sortBy.value = FileSortByPreference.getValueAsync()
            filesVM.loadAsync()
            audioPlaylistVM.loadAsync()
        }
    }

    PBackHandler(enabled = previewerState.visible || filesVM.canNavigateBack()) {
        when {
            previewerState.visible -> scope.launch { previewerState.closeTransform() }
            filesVM.canNavigateBack() -> {
                filesVM.navigateBack()
                scope.launch(Dispatchers.Default) { filesVM.loadAsync() }
            }
        }
    }

    val pageTitle = when {
        ZipBrowserHelper.isZipPath(filesVM.selectedPath) -> {
            val isAtZipRoot = ZipBrowserHelper.getInternalPath(filesVM.selectedPath).isEmpty()
            if (isAtZipRoot && title.isNotEmpty()) title
            else ZipBrowserHelper.getDisplayName(filesVM.selectedPath)
        }
        else -> title.ifEmpty { path.substringAfterLast('/') }
    }

    if (filesVM.showSortDialog.value) {
        FileSortDialog(filesVM.sortBy, onSelected = {
            scope.launch(Dispatchers.Default) { FileSortByPreference.putAsync(it); filesVM.sortBy.value = it; filesVM.loadAsync() }
        }, onDismiss = { filesVM.showSortDialog.value = false })
    }

    PScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            PTopAppBar(
                navController = navController,
                scrollBehavior = scrollBehavior,
                title = pageTitle,
                navigationIcon = { NavigationBackIcon { navController.navigateUp() } },
                actions = {
                    PIconButton(
                        icon = Res.drawable.share_2,
                        contentDescription = stringResource(Res.string.share),
                        tint = MaterialTheme.colorScheme.onSurface,
                    ) {
                        shareFile(path)
                    }
                    ActionButtonSort { filesVM.showSortDialog.value = true }
                    ActionButtonMoreWithMenu { dismiss ->
                        var showHiddenFiles by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            showHiddenFiles = ShowHiddenFilesPreference.getAsync()
                        }
                        PDropdownMenuItem(
                            text = { Text(stringResource(Res.string.show_hidden_files)) },
                            leadingIcon = {
                                Checkbox(checked = showHiddenFiles, onCheckedChange = null)
                            },
                            onClick = {
                                dismiss()
                                scope.launch(Dispatchers.Default) {
                                    val nv = !showHiddenFiles
                                    ShowHiddenFilesPreference.putAsync(nv)
                                    showHiddenFiles = nv; filesVM.loadAsync()
                                }
                            })
                        PDropdownMenuItem(
                            text = { Text(stringResource(Res.string.open_with_other_app)) },
                            onClick = {
                                dismiss()
                                openFileExternal(path)
                            })
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            BreadcrumbView(
                breadcrumbs = filesVM.breadcrumbs,
                selectedIndex = filesVM.selectedBreadcrumbIndex.value,
                onItemClick = { filesVM.navigateToDirectory(it.path) })
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
    MediaPreviewer(state = previewerState)
}
