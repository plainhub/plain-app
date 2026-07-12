package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformItemState

/**
 * Image/video thumbnail that participates in the shared-element transform into
 * [MediaPreviewer]. The Android implementation uses Coil + ForceVideoDecoder;
 * iOS is a no-op placeholder.
 */
@Composable
expect fun TransformImageView(
    modifier: Modifier = Modifier,
    path: String,
    fileName: String,
    key: String,
    itemState: TransformItemState,
    previewerState: MediaPreviewerState,
    widthPx: Int,
    forceVideoDecoder: Boolean = false,
)
