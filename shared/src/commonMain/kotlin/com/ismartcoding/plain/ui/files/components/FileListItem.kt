package com.ismartcoding.plain.ui.files.components

import com.ismartcoding.plain.features.media.MediaClient

import com.ismartcoding.plain.features.files.FilesClient

import com.ismartcoding.plain.features.files.extension

import com.ismartcoding.plain.models.File as FileEntry

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
import androidx.compose.foundation.shape.CornerSize
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
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.features.files.ZipBrowserHelper
import org.jetbrains.compose.resources.pluralStringResource
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.CheckCircle
import com.ismartcoding.plain.ui.media.FileEntryThumb
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.media.viewer.previewer.TransformItemState
import com.ismartcoding.plain.ui.media.fileIconData
import com.ismartcoding.plain.platform.system.formatDateTime
import com.ismartcoding.plain.platform.media.audioIsPlayingFlow
import com.ismartcoding.plain.platform.media.audioPlayerProgress
import com.ismartcoding.plain.platform.media.audioSeekTo
import com.ismartcoding.plain.platform.media.audioPause
import com.ismartcoding.plain.platform.media.audioPlay
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.audio.player.AudioPlayerPage
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    file: FileEntry, isSelected: Boolean, isSelectMode: Boolean,
    itemState: TransformItemState, previewerState: MediaPreviewerState,
    onClick: () -> Unit, onLongClick: () -> Unit, audioQueueVM: AudioQueueViewModel,
) {
    val source = com.ismartcoding.plain.ui.navigation.LocalContentSource.current
    val scope = rememberCoroutineScope()
    val isAudio = file.path.isAudioFast()
    val isCurrentlyPlaying = audioQueueVM.selectedPath.value == file.path && isAudio
    val isPlaying by audioIsPlayingFlow().collectAsState()
    var progress by remember { mutableFloatStateOf(0f) }
    var durationMs by remember { mutableFloatStateOf(0f) }
    var showAudioPlayer by remember { mutableStateOf(false) }

    LaunchedEffect(isCurrentlyPlaying) {
        if (isCurrentlyPlaying && isAudio) {
            scope.launch { withIO {
                val audio = source.media.audioInfo(file.path)
                durationMs = audio.durationMs.toFloat()
            } }
        }
    }

    var progressUpdateJob: Job? = null
    LaunchedEffect(isCurrentlyPlaying, isPlaying) {
        progressUpdateJob?.cancel()
        if (isCurrentlyPlaying && isPlaying) {
            progressUpdateJob = scope.launch {
                while (isActive) { progress = audioPlayerProgress().toFloat(); delay(500) }
            }
        }
    }

    Column {
        FileListRow(
            file = file, itemState = itemState, previewerState = previewerState,
            isSelected = isSelected, isSelectMode = isSelectMode, isCurrentlyPlaying = isCurrentlyPlaying,
            onClick = onClick, onLongClick = onLongClick,
        )

        if (isCurrentlyPlaying) {
            FileListItemPlayer(isSelected = isSelected, isPlaying = isPlaying, progress = progress, durationMs = durationMs,
                onProgressChange = { newProgress -> progress = newProgress * durationMs },
                onShowFullPlayer = { showAudioPlayer = true },
                onSeekTo = { audioSeekTo(it) },
                onTogglePlay = { if (isPlaying) audioPause() else audioPlay() })
        }
    }

    if (showAudioPlayer) {
        AudioPlayerPage(audioQueueVM, onDismissRequest = { showAudioPlayer = false })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FileListRow(
    file: FileEntry,
    itemState: TransformItemState,
    previewerState: MediaPreviewerState,
    isSelected: Boolean,
    isSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isCurrentlyPlaying: Boolean = false,
) {
    val rowShape = MaterialTheme.shapes.extraSmall
    val shape = if (isCurrentlyPlaying) rowShape.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)) else rowShape
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        .clip(shape)
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.cardBackgroundNormal),
        contentAlignment = Alignment.Center) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isSelectMode) {
                CheckCircle(selected = isSelected)
                Spacer(modifier = Modifier.width(8.dp))
            }
            FileListItemThumbnail(file.name, file.path, file.isDir, itemState, previewerState)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = file.name, style = MaterialTheme.typography.bodyLarge,
                    color = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                VerticalSpace(4.dp)
                val detail = if (file.isDir) pluralStringResource(Res.plurals.items, file.childCount, file.childCount) else file.size.formatBytes()
                Text(text = detail + ", " + file.updatedAt.formatDateTime(),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FileListItemThumbnail(
    name: String, path: String, isDir: Boolean, itemState: TransformItemState, previewerState: MediaPreviewerState,
) {
    val source = com.ismartcoding.plain.ui.navigation.LocalContentSource.current
    val isMedia = name.isImageFast() || name.isVideoFast()
    val isZipEntry = ZipBrowserHelper.isZipPath(path)

    // For zip entries that are images/videos, extract to cache asynchronously so
    // we can show a real thumbnail instead of a generic icon.
    var zipCachedPath by remember(path) { mutableStateOf<String?>(null) }
    if (isMedia && isZipEntry) {
        LaunchedEffect(path) {
            zipCachedPath = withIO { source.files.cacheZipEntry(path) }
        }
    }

    val widthPx = with(LocalDensity.current) { 48.dp.toPx() }.toInt()
    // Empty display path while a zip entry is still extracting: the shared
    // thumb falls back to the type icon until the cached path arrives.
    val displayPath = when {
        !isZipEntry -> path
        zipCachedPath != null -> zipCachedPath!!
        else -> ""
    }
    FileEntryThumb(
        name = name,
        path = displayPath,
        isDir = isDir,
        isMedia = isMedia,
        itemState = itemState,
        previewerState = previewerState,
        widthPx = widthPx,
    )
}
