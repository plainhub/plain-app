package com.ismartcoding.plain.ui.images.editor.scene

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.ismartcoding.plain.ui.images.editor.EditorDocumentState
import com.ismartcoding.plain.lib.yjs.ImageLayer
import com.ismartcoding.plain.lib.yjs.MosaicLayer

@Composable
fun NativeEditorCanvas(state: EditorDocumentState, displayScale: Float, textMeasurer: TextMeasurer, modifier: Modifier) {
    val images = remember(state.sourceImage, state.images) { decodeSceneImages(state.sourceImage, state.images) }
    val bitmap = remember(state, textMeasurer) {
        renderSceneBitmap(state, textMeasurer, images)
    }
    Canvas(modifier) {
        scale(displayScale, pivot = Offset.Zero) {
                drawSceneCheckerboard(bitmap.width.toFloat(), bitmap.height.toFloat())
                drawImage(bitmap)
        }
    }
}

internal fun renderSceneBitmap(state: EditorDocumentState, textMeasurer: TextMeasurer, resources: SceneImages): ImageBitmap {
    val width = state.canvasSize.width
    val height = state.canvasSize.height
    val bitmap = ImageBitmap(width, height)
    val images = resources.layers
    val source = resources.source
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(bitmap), Size(width.toFloat(), height.toFloat())) {
        if (state.bgColor != "transparent") drawSceneBackground(state.bgColor, width.toFloat(), height.toFloat())
        if (source != null) drawImage(source, topLeft = Offset(state.imgOffset.x.toFloat(), state.imgOffset.y.toFloat()), alpha = (state.imgAlpha / 100).toFloat())
        for (layer in state.layers) {
            if (!layer.visible) continue
            when (layer) {
                is MosaicLayer -> drawSceneMosaic(bitmap, layer)
                is ImageLayer -> {
                    val image = checkNotNull(images[layer.id]) { "Missing editor image resource" }
                    rotate(layer.rotation.toFloat(), Offset(layer.x.toFloat(), layer.y.toFloat())) {
                        drawImage(image, dstOffset = androidx.compose.ui.unit.IntOffset((layer.x - layer.w / 2).toInt(), (layer.y - layer.h / 2).toInt()), dstSize = IntSize(layer.w.toInt(), layer.h.toInt()), alpha = layer.opacity.toFloat())
                    }
                }
                else -> drawSceneLayer(layer, textMeasurer)
            }
        }
    }
    return bitmap
}
