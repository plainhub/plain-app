package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformImageViewWithUri
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformItemState

/** expect/actual bridge: delegates to [TransformImageViewWithUri] with `uri = null`. */
@Composable
actual fun TransformImageView(
    modifier: Modifier,
    path: String,
    fileName: String,
    key: String,
    itemState: TransformItemState,
    previewerState: MediaPreviewerState,
    widthPx: Int,
    forceVideoDecoder: Boolean,
) {
    TransformImageViewWithUri(
        modifier = modifier,
        path = path,
        fileName = fileName,
        key = key,
        uri = null,
        itemState = itemState,
        previewerState = previewerState,
        widthPx = widthPx,
        forceVideoDecoder = forceVideoDecoder,
    )
}
