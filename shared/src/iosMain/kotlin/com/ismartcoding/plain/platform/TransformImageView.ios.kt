package com.ismartcoding.plain.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformItemState

/**
 * iOS stub. The transform image view relies on Coil + Android-specific image
 * decoding; on iOS it renders an empty container so chat UI compiles.
 */
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
    Box(modifier = modifier.fillMaxSize())
}
