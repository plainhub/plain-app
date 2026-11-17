package com.ismartcoding.plain.ui.images.editor

import com.ismartcoding.plain.lib.yjs.CanvasSize
import com.ismartcoding.plain.lib.yjs.EditorLayer
import com.ismartcoding.plain.lib.yjs.Point

data class EditorDocumentState(
    val canvasSize: CanvasSize = CanvasSize(1920, 1080),
    val bgColor: String = "transparent",
    val sourceImage: String? = null,
    val imgOffset: Point = Point(0.0, 0.0),
    val imgAlpha: Double = 100.0,
    val layers: List<EditorLayer> = emptyList(),
    val images: Map<String, String> = emptyMap(),
)

sealed interface EditorLoadState {
    data object Loading : EditorLoadState
    data object Ready : EditorLoadState
    data class Failed(val message: String) : EditorLoadState
}
