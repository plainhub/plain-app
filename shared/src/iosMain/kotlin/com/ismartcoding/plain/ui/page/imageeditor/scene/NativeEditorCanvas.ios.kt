package com.ismartcoding.plain.ui.page.imageeditor.scene

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.rememberTextMeasurer
import com.ismartcoding.plain.yjs.EditorLayer

@Composable
actual fun NativeEditorCanvas(
    state: SceneState,
    modifier: Modifier,
    onReady: () -> Unit,
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier) {
        val cw = state.canvasSize.width.toFloat()
        val ch = state.canvasSize.height.toFloat()
        if (cw <= 0f || ch <= 0f) return@Canvas
        scale(state.viewport.scale, pivot = Offset.Zero) {
            drawSceneBackground(state.bgColor, cw, ch)
            for (layer in state.layers) {
                if (!layer.visible) continue
                if (layer.id == state.hideLayerId) continue
                drawSceneLayer(layer, textMeasurer)
            }
        }
    }
}
