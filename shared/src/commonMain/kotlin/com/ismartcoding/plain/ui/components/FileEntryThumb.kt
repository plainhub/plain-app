package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.platform.TransformImageView
import com.ismartcoding.plain.platform.getFileIconPath
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformItemState

/**
 * Thumbnail block for a file-row entry, shared by local (FilesPage) and
 * remote (shared-folder) lists:
 *
 * - local media with [itemState]/[previewerState] renders via
 *   [TransformImageView] so taps zoom into the shared previewer;
 * - remote media resolves [remoteThumb] asynchronously to a locally cached
 *   thumbnail path (the caller downloads it through the app's own HTTP stack
 *   — LAN guest endpoints are self-signed, which Coil's network fetcher
 *   cannot load);
 * - everything else falls back to the extension type icon.
 */
@Composable
fun FileEntryThumb(
    name: String,
    path: String,
    isDir: Boolean,
    isMedia: Boolean,
    itemState: TransformItemState? = null,
    previewerState: MediaPreviewerState? = null,
    widthPx: Int = 0,
    remoteThumb: (suspend () -> String?)? = null,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(40.dp), contentAlignment = Alignment.Center) {
        val localMedia = isMedia && path.isNotEmpty() && itemState != null && previewerState != null
        when {
            localMedia -> TransformImageView(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
                path = path,
                fileName = name,
                key = path,
                itemState = itemState,
                previewerState = previewerState,
                widthPx = widthPx,
            )

            remoteThumb != null -> RemoteThumbImage(remoteThumb, name, isDir)

            else -> FileTypeIcon(name, isDir)
        }
    }
}

@Composable
private fun RemoteThumbImage(
    remoteThumb: suspend () -> String?,
    name: String,
    isDir: Boolean,
) {
    var thumbPath by remember(remoteThumb) { mutableStateOf<String?>(null) }
    var failed by remember(remoteThumb) { mutableStateOf(false) }
    LaunchedEffect(remoteThumb) {
        thumbPath = remoteThumb()
        if (thumbPath == null) failed = true
    }
    when {
        thumbPath != null -> AsyncImage(
            model = thumbPath,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)),
        )

        failed -> FileTypeIcon(name, isDir)

        else -> Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.fillMaxSize(), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun FileTypeIcon(name: String, isDir: Boolean) {
    AsyncImage(
        model = if (isDir) getFileIconPath("folder") else getFileIconPath(name.getFilenameExtension()),
        modifier = Modifier.size(48.dp).padding(0.dp),
        alignment = Alignment.Center,
        contentDescription = name,
    )
}
