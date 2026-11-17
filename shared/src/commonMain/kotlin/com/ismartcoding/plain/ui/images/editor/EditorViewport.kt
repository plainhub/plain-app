package com.ismartcoding.plain.ui.images.editor

import androidx.compose.ui.geometry.Offset
import com.ismartcoding.plain.lib.yjs.Point

internal data class EditorViewport(val scale: Float, val origin: Offset) {
    fun canvas(point: Offset) = Point(((point.x - origin.x) / scale).toDouble(), ((point.y - origin.y) / scale).toDouble())
    fun screen(point: Point) = origin + Offset((point.x * scale).toFloat(), (point.y * scale).toFloat())
    fun zoomed(ratio: Float, previousAnchor: Offset, anchor: Offset) = EditorViewport(scale * ratio, anchor + (origin - previousAnchor) * ratio)
}
