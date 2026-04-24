package com.ismartcoding.plain.ui.page.chat.components

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.lib.extensions.isAudioFast
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.chat.download.DownloadQueue
import com.ismartcoding.plain.chat.download.DownloadTask
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.audio.AudioPlayer
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberTransformItemState
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.VChat
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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
        ChatFileItemContent(
            context = context,
            items = items,
            navController = navController,
            item = item,
            itemState = itemState,
            previewerState = previewerState,
            fileName = fileName,
            path = path,
            previewPath = previewPath,
            isDownloading = isDownloading,
            downloadProgress = downloadProgress,
            downloadTask = downloadTask,
            isCurrentlyPlaying = isCurrentlyPlaying,
            showContextMenu = showContextMenu,
            index = index,
        )

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
