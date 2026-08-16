package com.ismartcoding.plain.ui.page.appfiles.components

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
import com.ismartcoding.plain.extensions.resolveAppFileRealPath
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.lib.extensions.isAudioFast
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.audioIsPlayingFlow
import com.ismartcoding.plain.platform.audioPause
import com.ismartcoding.plain.platform.audioPlay
import com.ismartcoding.plain.platform.audioPlayerProgress
import com.ismartcoding.plain.platform.audioSeekTo
import com.ismartcoding.plain.platform.formatDateTime
import com.ismartcoding.plain.platform.getExtensionFromMimeType
import com.ismartcoding.plain.platform.getFileIconPath
import com.ismartcoding.plain.platform.playlistAudioFromPath
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.platform.TransformImageView
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformItemState
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.VAppFile
import com.ismartcoding.plain.ui.page.audio.AudioPlayerPage
import com.ismartcoding.plain.ui.page.files.components.FileListItemPlayer
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
    audioPlaylistVM: AudioPlaylistViewModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val fileName = file.fileName
    val path = file.appFile.realPath.resolveAppFileRealPath()
    val extension = fileName.getFilenameExtension().ifEmpty {
        getExtensionFromMimeType(file.appFile.mimeType)
    }
    val isAudio = path.isAudioFast()
    val isCurrentlyPlaying = audioPlaylistVM.selectedPath.value == path && isAudio
    val isPlaying by audioIsPlayingFlow().collectAsState()
    var progress by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    var showAudioPlayer by remember { mutableStateOf(false) }

    LaunchedEffect(isCurrentlyPlaying) {
        if (isCurrentlyPlaying && isAudio) {
            scope.launch {
                val audio = withIO { playlistAudioFromPath(path) }
                duration = audio.duration.toFloat()
            }
        }
    }

    var progressUpdateJob: Job? = null
    LaunchedEffect(isCurrentlyPlaying, isPlaying) {
        progressUpdateJob?.cancel()
        if (isCurrentlyPlaying && isPlaying) {
            progressUpdateJob = scope.launch {
                while (isActive) {
                    progress = audioPlayerProgress() / 1000f
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
                    TransformImageView(
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
                        model = getFileIconPath(extension),
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
                duration = duration,
                onProgressChange = { newProgress -> progress = newProgress * duration },
                onShowFullPlayer = { showAudioPlayer = true },
                onSeekTo = { audioSeekTo(it) },
                onTogglePlay = { if (isPlaying) audioPause() else audioPlay() },
            )
        }
    }

    if (showAudioPlayer) {
        AudioPlayerPage(audioPlaylistVM, onDismissRequest = { showAudioPlayer = false })
    }
}