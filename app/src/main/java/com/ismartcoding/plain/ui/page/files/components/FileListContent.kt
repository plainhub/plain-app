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
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.ismartcoding.lib.extensions.isAudioFast
import com.ismartcoding.lib.extensions.isImageFast
import com.ismartcoding.lib.extensions.isPdfFile
import com.ismartcoding.lib.extensions.isTextFile
import com.ismartcoding.lib.extensions.isVideoFast
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DPlaylistAudio
import com.ismartcoding.plain.features.AudioPlayer
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.components.NoDataView
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformItemState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberTransformItemState
import com.ismartcoding.plain.ui.extensions.toPreviewItem
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.FilesViewModel
import com.ismartcoding.plain.ui.models.MediaPreviewData
import com.ismartcoding.plain.ui.models.select
import com.ismartcoding.plain.ui.nav.navigatePdf
import com.ismartcoding.plain.ui.nav.navigateTextFile
import java.io.File

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
            onRefresh = {
                loadFiles(emptyList(), true)
            }
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
                            } else {
                                openFile(context, files, file, navController, previewerState, itemState, audioPlaylistVM)
                            }
                        }
                    },
                    onLongClick = {
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

fun openFile(
    context: android.content.Context,
    files: List<DFile>,
    file: DFile,
    navController: NavHostController,
    previewerState: MediaPreviewerState,
    itemState: TransformItemState,
    audioPlaylistVM: AudioPlaylistViewModel? = null
) {
    val path = file.path

    when {
        path.isImageFast() || path.isVideoFast() -> {
            coMain {
                withIO { MediaPreviewData.setDataAsync(context, itemState, files.filter { it.path.isImageFast() || it.path.isVideoFast() }.map { it.toPreviewItem() }, file.toPreviewItem()) }
                previewerState.openTransform(
                    index = MediaPreviewData.items.indexOfFirst { it.id == file.path },
                    itemState = itemState,
                )
            }
        }

        path.isAudioFast() -> {
            try {
                Permissions.checkNotification(context, R.string.audio_notification_prompt) {
                    val audio = DPlaylistAudio.fromPath(context, path)
                    if (audioPlaylistVM != null) {
                        coMain {
                            withIO { audioPlaylistVM.playlistItems.value = listOf(audio) }
                            audioPlaylistVM.selectedPath.value = path
                            AudioPlayer.play(context, audio)
                        }
                    } else {
                        AudioPlayer.play(context, audio)
                    }
                }
            } catch (ex: Exception) {
                DialogHelper.showMessage(R.string.audio_play_error)
            }
        }

        path.isTextFile() -> {
            if (file.size <= Constants.MAX_READABLE_TEXT_FILE_SIZE) {
                navController.navigateTextFile(path)
            } else {
                DialogHelper.showMessage(R.string.text_file_size_limit)
            }
        }

        path.isPdfFile() -> {
            try {
                navController.navigatePdf(File(path).toUri())
            } catch (ex: Exception) {
                DialogHelper.showMessage(R.string.pdf_open_error)
            }
        }

        else -> {
            ShareHelper.openPathWith(context, path)
        }
    }
}