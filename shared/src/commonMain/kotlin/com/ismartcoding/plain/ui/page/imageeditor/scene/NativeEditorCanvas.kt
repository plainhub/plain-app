package com.ismartcoding.plain.ui.page.imageeditor.scene

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.yjs.CanvasSize
import com.ismartcoding.plain.yjs.EditorLayer

data class SceneViewport(
    val scale: Float,
    val panX: Float,
    val panY: Float,
)

data class SceneState(
    val canvasSize: CanvasSize,
    val bgColor: String,
    val layers: List<EditorLayer>,
    val viewport: SceneViewport,
    val sourceImagePath: String? = null,
    val imgOffsetX: Double = 0.0,
    val imgOffsetY: Double = 0.0,
    val imgAlpha: Double = 100.0,
    val images: Map<String, String> = emptyMap(),
    val hideLayerId: String? = null,
)

@Composable
expect fun NativeEditorCanvas(
    state: SceneState,
    modifier: Modifier,
    onReady: () -> Unit = {},
)
