package com.ismartcoding.plain.ui.components.mediaviewer.hugeimage

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

data class RenderBlock(
    var inBound: Boolean = false,
    var inSampleSize: Int = 1,
    var renderOffset: IntOffset = IntOffset.Zero,
    var renderSize: IntSize = IntSize.Zero,
    var sliceRect: Rect = Rect(0, 0, 0, 0),
    var bitmap: Bitmap? = null,
) {
    fun release() {
        bitmap?.recycle()
        bitmap = null
    }
}