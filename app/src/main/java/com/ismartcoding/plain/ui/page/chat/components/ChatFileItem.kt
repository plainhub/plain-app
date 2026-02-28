package com.ismartcoding.plain.ui.page.chat.components

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.lib.extensions.isAudioFast
import com.ismartcoding.lib.extensions.isImageFast
import com.ismartcoding.lib.extensions.isPdfFile
import com.ismartcoding.lib.extensions.isTextFile
import com.ismartcoding.lib.extensions.isVideoFast
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.chat.DownloadQueue
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.chat.DownloadTask
import com.ismartcoding.plain.data.DPlaylistAudio
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.enums.TextFileType
import com.ismartcoding.plain.features.AudioPlayer
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberTransformItemState
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.MediaPreviewData
import com.ismartcoding.plain.ui.models.VChat
import com.ismartcoding.plain.ui.nav.navigateOtherFile
import com.ismartcoding.plain.ui.nav.navigatePdf
import com.ismartcoding.plain.ui.nav.navigateTextFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatFileItem(
    context: Context,
    items: List<VChat>,
    navController: NavHostController,
    m: VChat,
    peer: DPeer?,
    audioPlaylistVM: AudioPlaylistViewModel,
    previewerState: MediaPreviewerState,
    item: DMessageFile,
    index: Int,
    downloadProgressMap: Map<String, DownloadTask>,
    onShowAudioPlayer: () -> Unit,
) {
    val itemState = rememberTransformItemState()
    val previewPath = item.getPreviewPath(context, peer)
    val path = item.uri.getFinalPath(context)
    val fileName = item.fileName.ifEmpty { path.getFilenameFromPath() }
    val isAudio = fileName.isAudioFast()
    val currentPlayingPath = audioPlaylistVM.selectedPath
    val isCurrentlyPlaying = currentPlayingPath.value == path && isAudio
    val isPlaying by AudioPlayer.isPlayingFlow.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    val showContextMenu = remember { mutableStateOf(false) }

    val downloadTask = downloadProgressMap[item.id]
    val isDownloading = downloadTask?.isDownloading() == true
    val downloadProgress = downloadTask?.let {
        if (it.messageFile.size > 0) it.downloadedSize.toFloat() / it.messageFile.size.toFloat() else 0f
    } ?: 0f

    LaunchedEffect(item.uri) {
        if (item.isRemoteFile() && downloadTask == null && peer != null) {
            DownloadQueue.addDownloadTask(item, peer, m.id)
        }
    }

    var progress by remember(item.id) { mutableFloatStateOf(0f) }
    var duration by remember(item.id) { mutableFloatStateOf(item.duration.toFloat()) }
    var isDraggingProgress by remember(item.id) { mutableStateOf(false) }

    LaunchedEffect(path, isAudio, isCurrentlyPlaying) {
        if (isAudio && isCurrentlyPlaying && duration <= 0f) {
            val loadedDuration = withIO {
                runCatching {
                    DPlaylistAudio.fromPath(context, path).duration.toFloat()
                }.getOrDefault(0f)
            }
            if (loadedDuration > 0f) {
                duration = loadedDuration
            }
        }
    }

    LaunchedEffect(isCurrentlyPlaying, isPlaying, isDraggingProgress) {
        if (isCurrentlyPlaying) {
            while (isActive) {
                if (!isDraggingProgress) {
                    progress = AudioPlayer.playerProgress / 1000f
                }
                delay(500)
            }
        }
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (isDownloading) return@combinedClickable
                        if (fileName.isImageFast() || fileName.isVideoFast()) {
                            coMain {
                                keyboardController?.hide()
                                withIO { MediaPreviewData.setDataAsync(context, itemState, items.reversed(), item) }
                                previewerState.openTransform(
                                    index = MediaPreviewData.items.indexOfFirst { it.id == item.id },
                                    itemState = itemState,
                                )
                            }
                        } else if (isAudio) {
                            Permissions.checkNotification(context, R.string.audio_notification_prompt) {
                                AudioPlayer.play(context, DPlaylistAudio.fromPath(context, path))
                            }
                        } else if (fileName.isTextFile()) {
                            navController.navigateTextFile(path, fileName, mediaId = "", type = TextFileType.CHAT)
                        } else if (fileName.isPdfFile()) {
                            navController.navigatePdf(File(path).toUri())
                        } else {
                            navController.navigateOtherFile(path, fileName)
                        }
                    },
                    onLongClick = {
                        showContextMenu.value = true
                    },
                ),
        ) {
            PDropdownMenu(
                expanded = showContextMenu.value,
                onDismissRequest = { showContextMenu.value = false },
            ) {
                PDropdownMenuItem(
                    text = { Text(stringResource(R.string.save)) },
                    onClick = {
                        showContextMenu.value = false
                        coMain {
                            val result = withIO { FileHelper.copyFileToDownloads(path, fileName) }
                            if (result.isNotEmpty()) {
                                DialogHelper.showConfirmDialog("", LocaleHelper.getStringF(R.string.file_save_to, "path", result))
                            } else {
                                DialogHelper.showErrorMessage(result)
                            }
                        }
                    },
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (index == 0) 16.dp else 6.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChatFileInfo(
                    modifier = Modifier.weight(1f),
                    fileName = fileName,
                    size = item.size,
                    duration = item.duration,
                    summary = item.summary,
                    isCurrentlyPlaying = isCurrentlyPlaying,
                )

                ChatFileThumbnail(
                    context = context,
                    fileName = fileName,
                    previewPath = previewPath,
                    item = item,
                    itemState = itemState,
                    previewerState = previewerState,
                    isDownloading = isDownloading,
                    downloadProgress = downloadProgress,
                    downloadTask = downloadTask,
                )
            }
        }

        if (isCurrentlyPlaying) {
            ChatAudioInlineControls(
                progress = progress,
                duration = duration,
                isPlaying = isPlaying,
                onProgressChange = { newProgress ->
                    isDraggingProgress = true
                    if (duration > 0f) {
                        progress = newProgress * duration
                    }
                },
                onValueChangeFinished = {
                    if (duration > 0f) {
                        AudioPlayer.seekTo(progress.toLong())
                    }
                    isDraggingProgress = false
                },
                onShowFullPlayer = onShowAudioPlayer,
            )
        }
    }
}
