package com.ismartcoding.plain.ui.files

import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.features.files.FilesType
import com.ismartcoding.plain.features.files.ZipBrowserHelper
import com.ismartcoding.plain.ui.platform.media.MediaPreviewer
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.platform.system.appDir
import com.ismartcoding.plain.platform.files.getInternalStoragePath
import com.ismartcoding.plain.platform.system.openFileExternal
import com.ismartcoding.plain.platform.system.shareFile
import com.ismartcoding.plain.ui.components.ActionButtonMoreWithMenu
import com.ismartcoding.plain.ui.components.ActionButtonSort
import com.ismartcoding.plain.ui.components.NavigationBackIcon
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.components.CheckCircle
import com.ismartcoding.plain.ui.components.FileSortDialog
import com.ismartcoding.plain.ui.media.viewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.files.components.BreadcrumbView
import com.ismartcoding.plain.ui.files.components.FileListContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.share_2 as ui_drawable_share_2

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ZipFilePage(
    navController: NavHostController,
    audioQueueVM: AudioQueueViewModel,
    path: String,
    title: String = "",
    source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.ui.navigation.LocalContentSource.current,
    filesVM: FilesViewModel = viewModel { FilesViewModel(source) },
) {
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val previewerState = rememberPreviewerState()
    val itemsState by filesVM.itemsFlow.collectAsState()
    val showHiddenFiles by Prefs.showHiddenFiles.collectAsState()
    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launch { filesVM.loadAsync(); setRefreshState(RefreshContentState.Finished) }
    }

    val zipVirtualRoot = remember(path) { ZipBrowserHelper.joinPath(path, "") }

    LaunchedEffect(path) {
        withContext(Dispatchers.Default) {
            val appDataPath = source.appPath
            val type = if (path.startsWith(appDataPath)) FilesType.APP else FilesType.INTERNAL_STORAGE
            val rootPath = when (type) { FilesType.APP -> appDataPath; else -> source.rootPath }
            filesVM.initSelectedPath(rootPath, type, zipVirtualRoot, zipVirtualRoot)
            filesVM.sortBy.value = Prefs.fileSortBy.value
            audioQueueVM.loadAsync()
            Prefs.showHiddenFiles.collect { filesVM.loadAsync() }
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
            scope.launch(Dispatchers.Default) { Prefs.set(PrefKeys.FILE_SORT_BY, it.name); filesVM.sortBy.value = it; filesVM.loadAsync() }
        }, onDismiss = { filesVM.showSortDialog.value = false })
    }

    PScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            PTopAppBar(
                onNavigateBack = { navController.navigateUp() },
                scrollBehavior = scrollBehavior,
                title = pageTitle,
                navigationIcon = { NavigationBackIcon { navController.navigateUp() } },
                actions = {
                    PIconButton(
                        icon = UiRes.drawable.ui_drawable_share_2,
                        contentDescription = stringResource(Res.string.share),
                        tint = MaterialTheme.colorScheme.onSurface,
                    ) {
                        scope.launchSafe { shareFile(source.fileForOpen(path)) }
                    }
                    ActionButtonSort { filesVM.showSortDialog.value = true }
                    ActionButtonMoreWithMenu { dismiss ->
                        PDropdownMenuItem(
                            text = { Text(stringResource(Res.string.show_hidden_files)) },
                            leadingIcon = {
                                CheckCircle(selected = showHiddenFiles)
                            },
                            onClick = {
                                dismiss()
                                scope.launch(Dispatchers.Default) {
                                    Prefs.set(PrefKeys.SHOW_HIDDEN_FILES, !showHiddenFiles)
                                }
                            })
                        PDropdownMenuItem(
                            text = { Text(stringResource(Res.string.open_with_other_app)) },
                            onClick = {
                                dismiss()
                                scope.launchSafe { openFileExternal(source.fileForOpen(path)) }
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
                    audioQueueVM = audioQueueVM
                )
            }
        }
    }
    MediaPreviewer(state = previewerState)
}
