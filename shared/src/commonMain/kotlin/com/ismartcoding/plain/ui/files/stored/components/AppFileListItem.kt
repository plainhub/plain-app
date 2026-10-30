package com.ismartcoding.plain.ui.files.stored.components

import com.ismartcoding.plain.features.media.MediaClient

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.lib.extensions.isAudioFast
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.media.audioIsPlayingFlow
import com.ismartcoding.plain.platform.media.audioPause
import com.ismartcoding.plain.platform.media.audioPlay
import com.ismartcoding.plain.platform.media.audioPlayerProgress
import com.ismartcoding.plain.platform.media.audioSeekTo
import com.ismartcoding.plain.platform.system.formatDateTime
import com.ismartcoding.plain.platform.media.getExtensionFromMimeType
import com.ismartcoding.plain.ui.media.fileIconData
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.media.viewer.previewer.TransformImageViewWithUri
import com.ismartcoding.plain.ui.media.viewer.previewer.TransformItemState
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.files.stored.VAppFile
import com.ismartcoding.plain.ui.audio.player.AudioPlayerPage
import com.ismartcoding.plain.ui.files.components.FileListItemPlayer
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun AppFileListItem(
    file: VAppFile,
    itemState: TransformItemState,
    previewerState: MediaPreviewerState,
    audioQueueVM: AudioQueueViewModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    val source = com.ismartcoding.plain.ui.navigation.LocalContentSource.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val fileName = file.fileName
    val path = file.appFile.realPath
    val extension = fileName.getFilenameExtension().ifEmpty {
        getExtensionFromMimeType(file.appFile.mimeType)
    }
    val isAudio = path.isAudioFast()
    val isCurrentlyPlaying = audioQueueVM.selectedPath.value == path && isAudio
    val isPlaying by audioIsPlayingFlow().collectAsState()
    var progress by remember { mutableFloatStateOf(0f) }
    var durationMs by remember { mutableFloatStateOf(0f) }
    var showAudioPlayer by remember { mutableStateOf(false) }

    LaunchedEffect(isCurrentlyPlaying) {
        if (isCurrentlyPlaying && isAudio) {
            scope.launch {
                val audio = withIO { source.media.audioInfo(path) }
                durationMs = audio.durationMs.toFloat()
            }
        }
    }

    var progressUpdateJob: Job? = null
    LaunchedEffect(isCurrentlyPlaying, isPlaying) {
        progressUpdateJob?.cancel()
        if (isCurrentlyPlaying && isPlaying) {
            progressUpdateJob = scope.launch {
                while (isActive) {
                    progress = audioPlayerProgress().toFloat()
                    delay(500)
                }
            }
        }
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 8.dp, topEnd = 8.dp,
                        bottomStart = if (isCurrentlyPlaying) 0.dp else 8.dp,
                        bottomEnd = if (isCurrentlyPlaying) 0.dp else 8.dp,
                    )
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .background(MaterialTheme.colorScheme.cardBackgroundNormal),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val isImage = fileName.isImageFast() || file.appFile.mimeType.startsWith("image/")
                val isVideo = fileName.isVideoFast() || file.appFile.mimeType.startsWith("video/")

                if (isImage || isVideo) {
                    val widthPx = with(density) { 40.dp.toPx() }.toInt()
                    TransformImageViewWithUri(
                        modifier = Modifier.size(40.dp),
                        path = path,
                        fileName = fileName,
                        key = file.appFile.id,
                        itemState = itemState,
                        previewerState = previewerState,
                        widthPx = widthPx,
                        forceVideoDecoder = isVideo,
                    )
                } else {
                    AsyncImage(
                        model = fileIconData(extension),
                        modifier = Modifier.size(40.dp),
                        contentDescription = fileName,
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                    VerticalSpace(2.dp)
                    Text(
                        text = file.appFile.size.formatBytes() + "  ·  " + file.appFile.createdAt.formatDateTime(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (isCurrentlyPlaying) {
            FileListItemPlayer(
                isSelected = false,
                isPlaying = isPlaying,
                progress = progress,
                durationMs = durationMs,
                onProgressChange = { newProgress -> progress = newProgress * durationMs },
                onShowFullPlayer = { showAudioPlayer = true },
                onSeekTo = { audioSeekTo(it) },
                onTogglePlay = { if (isPlaying) audioPause() else audioPlay() },
            )
        }
    }

    if (showAudioPlayer) {
        AudioPlayerPage(audioQueueVM, onDismissRequest = { showAudioPlayer = false })
    }
}
