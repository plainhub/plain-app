package com.ismartcoding.plain.ui.media

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.time.Instant
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.media.viewer.previewer.TransformImageViewWithUri
import com.ismartcoding.plain.ui.media.viewer.previewer.TransformItemState

@Composable
internal fun MediaGridThumbnail(
    uri: String,
    path: String,
    key: String,
    updatedAt: Instant,
    itemState: TransformItemState,
    previewerState: MediaPreviewerState,
) {
    BoxWithConstraints(Modifier.aspectRatio(1f)) {
        TransformImageViewWithUri(
            uri = uri,
            path = path,
            fileName = path.getFilenameFromPath(),
            key = key,
            version = updatedAt,
            itemState = itemState,
            previewerState = previewerState,
            widthPx = constraints.maxWidth,
        )
    }
}
