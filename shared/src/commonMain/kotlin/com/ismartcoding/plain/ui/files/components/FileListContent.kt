package com.ismartcoding.plain.ui.files.components

import com.ismartcoding.plain.features.files.extension

import com.ismartcoding.plain.models.File as FileEntry

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.features.files.ZipBrowserHelper
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.components.NoDataView
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.media.viewer.previewer.rememberTransformItemState
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.files.FilesViewModel
import com.ismartcoding.plain.ui.navigation.navigateZipFile
import com.ismartcoding.plain.ui.state.exitSelectMode
import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.ui.state.select

@Composable
fun FileListContent(
    navController: NavHostController,
    filesVM: FilesViewModel,
    files: List<FileEntry>,
    previewerState: MediaPreviewerState,
    audioQueueVM: AudioQueueViewModel,
    systemCount: Int = 0,
    systemExpanded: Boolean = false,
    onToggleSystem: () -> Unit = {},
    showingTrash: Boolean = false,
    loading: Boolean = filesVM.isLoading.value,
    onRefresh: suspend () -> Unit = filesVM::loadAsync,
) {
    val scope = rememberCoroutineScope()
    val refreshState = rememberRefreshLayoutState {
        scope.launchSafe(onError = {
            setRefreshState(RefreshContentState.Failed)
            DialogHelper.showMessage(it)
        }) {
            onRefresh()
            setRefreshState(RefreshContentState.Finished)
        }
    }
    val fileRow: @Composable (FileEntry) -> Unit = { file ->
        val itemState = rememberTransformItemState()
        val toggleSelection = {
            filesVM.select(file.path)
            if (showingTrash && filesVM.selectedIds.isEmpty()) filesVM.exitSelectMode()
        }
        FileListItem(
            file = file,
            isSelected = filesVM.selectedIds.contains(file.path),
            isSelectMode = filesVM.selectMode.value,
            itemState = itemState,
            previewerState = previewerState,
            onClick = {
                when {
                    filesVM.selectMode.value -> toggleSelection()
                    file.isDir && showingTrash -> filesVM.selectedFile.value = file
                    file.isDir -> filesVM.navigateToDirectory(file.path)
                    !ZipBrowserHelper.isZipPath(file.path) && file.extension.equals("zip", ignoreCase = true) -> {
                        if (showingTrash) navController.navigateZipFile(file.path, file.name)
                        else filesVM.navigateToDirectory(ZipBrowserHelper.joinPath(file.path, ""))
                    }
                    else -> openFile(files, file, navController, previewerState, itemState, audioQueueVM, filesVM.source)
                }
            },
            onLongClick = {
                // Disable select-mode for entries inside a zip archive (read-only)
                if (ZipBrowserHelper.isZipPath(file.path)) return@FileListItem
                if (!filesVM.selectMode.value) {
                    filesVM.selectedFile.value = file
                } else {
                    toggleSelection()
                }
            },
            audioQueueVM = audioQueueVM
        )
    }
    PullToRefresh(refreshLayoutState = refreshState, userEnable = !loading) {
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@PullToRefresh
        }
        val lazyListState = rememberLazyListState()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (files.isEmpty() && systemCount == 0) {
                item {
                    NoDataView(
                        modifier = Modifier.fillParentMaxSize(),
                        icon = null,
                        message = stringResource(Res.string.no_data),
                        showRefreshButton = !showingTrash,
                        onRefresh = { refreshState.setRefreshState(RefreshContentState.Refreshing) },
                    )
                }
            } else {
                if (systemCount > 0) item(key = "system-header") {
                    SystemFilesCard(systemCount, systemExpanded, onToggleSystem) {
                        if (systemExpanded) files.take(systemCount).forEach { file -> key(file.path) { fileRow(file) } }
                    }
                }
                items(if (systemExpanded) files.drop(systemCount) else files, key = { it.path }) { file ->
                    fileRow(file)
                }
                item { BottomSpace() }
            }
        }
    }
}
