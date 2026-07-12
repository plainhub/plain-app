package com.ismartcoding.plain.ui.components.mediaviewer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.unit.IntSize

class RawGesture(
    val onTap: (Offset) -> Unit = {},
    val onDoubleTap: (Offset) -> Unit = {},
    val onLongPress: (Offset) -> Unit = {},
    val gestureStart: () -> Unit = {},
    val gestureEnd: (transformOnly: Boolean) -> Unit = {},
    val onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float, event: PointerEvent) -> Boolean = { _, _, _, _, _ -> true },
)

data class SizeChangeContent(
    val defaultSize: IntSize,
    val containerSize: IntSize,
    val maxScale: Float,
)
