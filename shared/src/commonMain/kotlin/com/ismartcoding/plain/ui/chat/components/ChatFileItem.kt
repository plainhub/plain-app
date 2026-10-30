package com.ismartcoding.plain.ui.chat.components

import com.ismartcoding.plain.features.media.MediaClient

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import com.ismartcoding.plain.features.chat.download.DownloadQueue
import com.ismartcoding.plain.features.chat.download.DownloadTask
import com.ismartcoding.plain.features.chat.models.MessageFile
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.features.chat.models.getPreviewPath
import com.ismartcoding.plain.ui.extensions.rememberFilePath
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.lib.extensions.isAudioFast
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.media.audioIsPlayingFlow
import com.ismartcoding.plain.platform.media.audioPause
import com.ismartcoding.plain.platform.media.audioPlay
import com.ismartcoding.plain.platform.media.audioPlayerProgress
import com.ismartcoding.plain.platform.media.audioSeekTo
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.media.viewer.previewer.rememberTransformItemState
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.chat.VChat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun ChatFileItem(
    items: List<VChat>,
    navController: NavHostController,
    m: VChat,
    peer: Peer?,
    audioQueueVM: AudioQueueViewModel,
    previewerState: MediaPreviewerState,
    item: MessageFile,
    index: Int,
    downloadProgressMap: Map<String, DownloadTask>,
    onShowAudioPlayer: () -> Unit,
) {
    val source = com.ismartcoding.plain.ui.navigation.LocalContentSource.current
    val downloads = com.ismartcoding.plain.ui.chat.LocalChatDownloads.current
    val itemState = rememberTransformItemState()
    val previewPath by produceState("", source, item.uri, peer?.id, peer?.address, peer?.ip, peer?.port, peer?.key, peer?.publicKey, peer?.status) {
        value = try {
            if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote) { if (item.isRemoteFile()) "" else item.uri }
            else item.getPreviewPath(peer)
        }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { "" }
    }
    val path = if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote) item.uri else rememberFilePath(item.uri)
    val fileName = item.fileName.ifEmpty { path.getFilenameFromPath() }
    val isAudio = fileName.isAudioFast()
    val currentPlayingPath = audioQueueVM.selectedPath
    val output by com.ismartcoding.plain.features.audio.AudioCommands.output.collectAsState()
    val isCurrentlyPlaying = output.source.key == source.key && path.isNotEmpty() && currentPlayingPath.value == path && isAudio
    val isPlaying by audioIsPlayingFlow().collectAsState()

    val showContextMenu = remember { mutableStateOf(false) }

    val downloadTask = downloadProgressMap[item.id]
    val taskActive = downloadTask?.isActive() == true
    val downloadProgress = downloadTask?.let {
        if (it.messageFile.size > 0) it.downloadedSize.toFloat() / it.messageFile.size.toFloat() else 0f
    } ?: 0f

    LaunchedEffect(item.uri) {
        if (item.isRemoteFile() && downloadTask == null && peer != null) {
            downloads.addDownloadTask(item, peer, m.id)
        }
    }

    var progressMs by remember(item.id) { mutableFloatStateOf(0f) }
    var durationMs by remember(item.id) { mutableFloatStateOf(item.durationMs.toFloat()) }
    var isDraggingProgress by remember(item.id) { mutableStateOf(false) }

    LaunchedEffect(path, isAudio, isCurrentlyPlaying) {
        if (isAudio && isCurrentlyPlaying && durationMs <= 0f) {
            val loadedDurationMs = withIO {
                runCatching {
                    source.media.durationMs(path).toFloat()
                }.getOrDefault(0f)
            }
            if (loadedDurationMs > 0f) {
                durationMs = loadedDurationMs
            }
        }
    }

    LaunchedEffect(isCurrentlyPlaying, isPlaying, isDraggingProgress) {
        if (isCurrentlyPlaying) {
            while (isActive) {
                if (!isDraggingProgress) {
                    progressMs = audioPlayerProgress().toFloat()
                }
                delay(500)
            }
        }
    }

    Column {
        ChatFileItemContent(
            items = items,
            navController = navController,
            item = item,
            itemState = itemState,
            previewerState = previewerState,
            fileName = fileName,
            path = path,
            previewPath = previewPath,
            isActive = taskActive,
            downloadProgress = downloadProgress,
            downloadTask = downloadTask,
            isCurrentlyPlaying = isCurrentlyPlaying,
            showContextMenu = showContextMenu,
            index = index,
        )

        if (isCurrentlyPlaying) {
            ChatAudioInlineControls(
                progressMs = progressMs,
                durationMs = durationMs,
                isPlaying = isPlaying,
                onProgressChange = { newProgress ->
                    isDraggingProgress = true
                    if (durationMs > 0f) {
                        progressMs = newProgress * durationMs
                    }
                },
                onValueChangeFinished = { normalizedProgress ->
                    if (durationMs > 0f) {
                        audioSeekTo((normalizedProgress * durationMs).toLong())
                    }
                    isDraggingProgress = false
                },
                onShowFullPlayer = onShowAudioPlayer,
                onPause = { audioPause() },
                onPlay = { audioPlay() },
            )
        }
    }
}
