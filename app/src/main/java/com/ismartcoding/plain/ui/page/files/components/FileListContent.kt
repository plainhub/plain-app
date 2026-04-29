package com.ismartcoding.plain.ui.page.files.components

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.ZipBrowserHelper
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.components.NoDataView
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberTransformItemState
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.FilesViewModel
import com.ismartcoding.plain.ui.models.select

@Composable
fun FileListContent(
    navController: NavHostController,
    filesVM: FilesViewModel,
    files: List<DFile>,
    loadFiles: (List<DFile>, Boolean) -> Unit,
    previewerState: MediaPreviewerState,
    audioPlaylistVM: AudioPlaylistViewModel
) {
    val context = LocalContext.current

    if (filesVM.isLoading.value) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (files.isEmpty()) {
        NoDataView(
            iconResId = R.drawable.package_open,
            message = stringResource(R.string.no_data),
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
                                filesVM.navigateToDirectory(context, file.path)
                            } else if (!ZipBrowserHelper.isZipPath(file.path) && file.extension.lowercase() == "zip") {
                                // Enter the zip as a virtual directory
                                filesVM.navigateToDirectory(context, ZipBrowserHelper.joinPath(file.path, ""))
                            } else {
                                openFile(context, files, file, navController, previewerState, itemState, audioPlaylistVM)
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
                    audioPlaylistVM = audioPlaylistVM
                )
            }
            item {
                BottomSpace()
            }
        }
    }
}