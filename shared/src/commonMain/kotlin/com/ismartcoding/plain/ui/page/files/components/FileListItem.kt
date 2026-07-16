package com.ismartcoding.plain.ui.page.files.components

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.lib.extensions.isAudioFast
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.ZipBrowserHelper
import org.jetbrains.compose.resources.pluralStringResource
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.platform.TransformImageView
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformItemState
import com.ismartcoding.plain.platform.getFileIconPath
import com.ismartcoding.plain.platform.formatDateTime
import com.ismartcoding.plain.platform.audioIsPlayingFlow
import com.ismartcoding.plain.platform.audioPlayerProgress
import com.ismartcoding.plain.platform.audioSeekTo
import com.ismartcoding.plain.platform.audioPause
import com.ismartcoding.plain.platform.audioPlay
import com.ismartcoding.plain.platform.extractZipEntryToCache
import com.ismartcoding.plain.platform.playlistAudioFromPath
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.page.audio.AudioPlayerPage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    file: DFile, isSelected: Boolean, isSelectMode: Boolean,
    itemState: TransformItemState, previewerState: MediaPreviewerState,
    onClick: () -> Unit, onLongClick: () -> Unit, audioPlaylistVM: AudioPlaylistViewModel,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val isAudio = file.path.isAudioFast()
    val isCurrentlyPlaying = audioPlaylistVM.selectedPath.value == file.path && isAudio
    val isPlaying by audioIsPlayingFlow().collectAsState()
    var progress by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    var showAudioPlayer by remember { mutableStateOf(false) }

    LaunchedEffect(isCurrentlyPlaying) {
        if (isCurrentlyPlaying && isAudio) {
            scope.launch { withIO {
                val audio = playlistAudioFromPath(file.path)
                duration = audio.duration.toFloat()
            } }
        }
    }

    var progressUpdateJob: Job? = null
    LaunchedEffect(isCurrentlyPlaying, isPlaying) {
        progressUpdateJob?.cancel()
        if (isCurrentlyPlaying && isPlaying) {
            progressUpdateJob = scope.launch {
                while (isActive) { progress = audioPlayerProgress() / 1000f; delay(500) }
            }
        }
    }

    Column {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp,
                bottomStart = if (isCurrentlyPlaying) 0.dp else 8.dp,
                bottomEnd = if (isCurrentlyPlaying) 0.dp else 8.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isSelectMode) {
                    Checkbox(checked = isSelected, onCheckedChange = null)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                FileListItemThumbnail(file, itemState, previewerState, density)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = file.name, style = MaterialTheme.typography.bodyLarge,
                        color = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    VerticalSpace(4.dp)
                    Text(text = if (file.isDir) pluralStringResource(Res.plurals.items, file.children, file.children) + ", " + file.updatedAt.formatDateTime()
                        else file.size.formatBytes() + ", " + file.updatedAt.formatDateTime(),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (isCurrentlyPlaying) {
            FileListItemPlayer(isSelected = isSelected, isPlaying = isPlaying, progress = progress, duration = duration,
                onProgressChange = { newProgress -> progress = newProgress * duration },
                onShowFullPlayer = { showAudioPlayer = true },
                onSeekTo = { audioSeekTo(it) },
                onTogglePlay = { if (isPlaying) audioPause() else audioPlay() })
        }
    }

    if (showAudioPlayer) {
        AudioPlayerPage(audioPlaylistVM, onDismissRequest = { showAudioPlayer = false })
    }
}

@Composable
private fun FileListItemThumbnail(
    file: DFile, itemState: TransformItemState, previewerState: MediaPreviewerState, density: androidx.compose.ui.unit.Density,
) {
    val isMedia = file.path.isImageFast() || file.path.isVideoFast()
    val isZipEntry = ZipBrowserHelper.isZipPath(file.path)

    // For zip entries that are images/videos, extract to cache asynchronously so
    // we can show a real thumbnail instead of a generic icon.
    var zipCachedPath by remember(file.path) { mutableStateOf<String?>(null) }
    if (isMedia && isZipEntry) {
        LaunchedEffect(file.path) {
            zipCachedPath = withIO { extractZipEntryToCache(file.path) }
        }
    }

    val widthPx = with(density) { 48.dp.toPx() }.toInt()
    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        when {
            isMedia && !isZipEntry -> {
                // Regular file — use TransformImageView for the zoom-into-preview animation.
                TransformImageView(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
                    path = file.path, fileName = file.name, key = file.path,
                    itemState = itemState, previewerState = previewerState, widthPx = widthPx)
            }
            isMedia && zipCachedPath != null -> {
                // Zip entry extracted — use TransformImageView so itemState is registered
                // and openTransform() can animate from the thumbnail position.
                TransformImageView(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
                    path = zipCachedPath!!, fileName = file.name, key = file.path,
                    itemState = itemState, previewerState = previewerState, widthPx = widthPx)
            }
            else -> {
                AsyncImage(
                    model = if (file.isDir) getFileIconPath("folder") else getFileIconPath(file.path.getFilenameExtension()),
                    modifier = Modifier.size(48.dp), alignment = Alignment.Center, contentDescription = file.path,
                )
            }
        }
    }
}
