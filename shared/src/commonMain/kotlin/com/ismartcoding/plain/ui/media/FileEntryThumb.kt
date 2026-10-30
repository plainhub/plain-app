package com.ismartcoding.plain.ui.media

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
import com.ismartcoding.plain.ui.media.viewer.previewer.TransformImageViewWithUri
import com.ismartcoding.plain.ui.media.fileIconData
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.media.viewer.previewer.TransformItemState

@Composable
fun FileEntryThumb(
    name: String,
    path: String,
    isDir: Boolean,
    isMedia: Boolean,
    itemState: TransformItemState? = null,
    previewerState: MediaPreviewerState? = null,
    widthPx: Int = 0,
    remoteThumb: (suspend () -> Any?)? = null,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(40.dp), contentAlignment = Alignment.Center) {
        val localMedia = isMedia && path.isNotEmpty() && itemState != null && previewerState != null
        when {
            localMedia -> TransformImageViewWithUri(
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
    remoteThumb: suspend () -> Any?,
    name: String,
    isDir: Boolean,
) {
    var thumbModel by remember(remoteThumb) { mutableStateOf<Any?>(null) }
    var failed by remember(remoteThumb) { mutableStateOf(false) }
    LaunchedEffect(remoteThumb) {
        thumbModel = remoteThumb()
        if (thumbModel == null) failed = true
    }
    when {
        thumbModel != null -> AsyncImage(
            model = thumbModel,
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
        model = if (isDir) fileIconData("folder") else fileIconData(name.getFilenameExtension()),
        modifier = Modifier.size(48.dp).padding(0.dp),
        alignment = Alignment.Center,
        contentDescription = name,
    )
}
