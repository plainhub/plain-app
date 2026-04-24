package com.ismartcoding.plain.ui.page.files.components

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ismartcoding.lib.extensions.dp2px
import com.ismartcoding.lib.extensions.formatBytes
import com.ismartcoding.lib.extensions.getFilenameExtension
import com.ismartcoding.lib.extensions.isAudioFast
import com.ismartcoding.lib.extensions.isImageFast
import com.ismartcoding.lib.extensions.isVideoFast
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.audio.AudioPlayer
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformImageView
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformItemState
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.extensions.formatDateTime
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isAudio = file.path.isAudioFast()
    val isCurrentlyPlaying = audioPlaylistVM.selectedPath.value == file.path && isAudio
    val isPlaying by AudioPlayer.isPlayingFlow.collectAsState()
    var progress by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    var showAudioPlayer by remember { mutableStateOf(false) }

    LaunchedEffect(isCurrentlyPlaying) {
        if (isCurrentlyPlaying && isAudio) {
            scope.launch { withIO {
                val audio = DPlaylistAudio.fromPath(context, file.path)
                duration = audio.duration.toFloat()
            } }
        }
    }

    var progressUpdateJob: Job? = null
    LaunchedEffect(isCurrentlyPlaying, isPlaying) {
        progressUpdateJob?.cancel()
        if (isCurrentlyPlaying && isPlaying) {
            progressUpdateJob = scope.launch {
                while (isActive) { progress = AudioPlayer.playerProgress / 1000f; delay(500) }
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
                FileListItemThumbnail(file, itemState, previewerState, context)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = file.name, style = MaterialTheme.typography.bodyLarge,
                        color = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    VerticalSpace(4.dp)
                    Text(text = if (file.isDir) LocaleHelper.getQuantityString(R.plurals.items, file.children) + ", " + file.updatedAt.formatDateTime()
                        else file.size.formatBytes() + ", " + file.updatedAt.formatDateTime(),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (isCurrentlyPlaying) {
            FileListItemPlayer(isSelected = isSelected, isPlaying = isPlaying, progress = progress, duration = duration,
                onProgressChange = { newProgress -> progress = newProgress * duration },
                onShowFullPlayer = { showAudioPlayer = true })
        }
    }

    if (showAudioPlayer) {
        AudioPlayerPage(audioPlaylistVM, onDismissRequest = { showAudioPlayer = false })
    }
}

@Composable
private fun FileListItemThumbnail(
    file: DFile, itemState: TransformItemState, previewerState: MediaPreviewerState, context: android.content.Context,
) {
    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        if (file.path.isImageFast() || file.path.isVideoFast()) {
            TransformImageView(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
                path = file.path, fileName = file.name, key = file.path,
                itemState = itemState, previewerState = previewerState, widthPx = context.dp2px(48))
        } else {
            AsyncImage(model = if (file.isDir) AppHelper.getFileIconPath("folder") else AppHelper.getFileIconPath(file.path.getFilenameExtension()),
                modifier = Modifier.size(48.dp), alignment = Alignment.Center, contentDescription = file.path)
        }
    }
}
