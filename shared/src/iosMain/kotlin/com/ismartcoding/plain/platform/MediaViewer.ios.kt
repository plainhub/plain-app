package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.ui.components.mediaviewer.RawGesture
import com.ismartcoding.plain.ui.components.mediaviewer.SizeChangeContent

/**
 * iOS has no huge-image tiled decoder; this is never reached because
 * [getModel] always returns a [PreviewItem] on iOS.
 */
@Composable
actual fun MediaHugeImageContent(
    model: Any?,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    rotation: Float,
    gesture: RawGesture,
    onSizeChange: suspend (SizeChangeContent) -> Unit,
    onMounted: () -> Unit,
    boundClip: Boolean,
) {
}
