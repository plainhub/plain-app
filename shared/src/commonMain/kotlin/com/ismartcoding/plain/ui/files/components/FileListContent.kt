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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.features.files.ZipBrowserHelper
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.components.NoDataView
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.media.viewer.previewer.rememberTransformItemState
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.files.FilesViewModel
import com.ismartcoding.plain.ui.state.select

@Composable
fun FileListContent(
    navController: NavHostController,
    filesVM: FilesViewModel,
    files: List<FileEntry>,
    loadFiles: (List<FileEntry>, Boolean) -> Unit,
    previewerState: MediaPreviewerState,
    audioQueueVM: AudioQueueViewModel
) {
    if (filesVM.isLoading.value) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (files.isEmpty()) {
        NoDataView(
            icon = null,
            message = stringResource(Res.string.no_data),
            showRefreshButton = true,
            onRefresh = { loadFiles(emptyList(), true) }
        )
    } else {
        val lazyListState = rememberLazyListState()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(files) { file ->
                val itemState = rememberTransformItemState()
                FileListItem(
                    file = file,
                    isSelected = filesVM.selectedIds.contains(file.path),
                    isSelectMode = filesVM.selectMode.value,
                    itemState = itemState,
                    previewerState = previewerState,
                    onClick = {
                        if (filesVM.selectMode.value) {
                            filesVM.select(file.path)
                        } else {
                            if (file.isDir) {
                                filesVM.navigateToDirectory(file.path)
                            } else if (!ZipBrowserHelper.isZipPath(file.path) && file.extension.lowercase() == "zip") {
                                // Enter the zip as a virtual directory
                                filesVM.navigateToDirectory(ZipBrowserHelper.joinPath(file.path, ""))
                            } else {
                                openFile(files, file, navController, previewerState, itemState, audioQueueVM, filesVM.source)
                            }
                        }
                    },
                    onLongClick = {
                        // Disable select-mode for entries inside a zip archive (read-only)
                        if (ZipBrowserHelper.isZipPath(file.path)) return@FileListItem
                        if (!filesVM.selectMode.value) {
                            filesVM.selectedFile.value = file
                        } else {
                            filesVM.select(file.path)
                        }
                    },
                    audioQueueVM = audioQueueVM
                )
            }
            item {
                BottomSpace()
            }
        }
    }
}
