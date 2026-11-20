package com.ismartcoding.plain.ui.images.editor

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.lib.generateId
import com.ismartcoding.plain.ui.images.editor.ImageEditorEditViewModel
import com.ismartcoding.plain.ui.images.editor.ImageEditorTool
import com.ismartcoding.plain.ui.images.editor.ResizeHandle
import com.ismartcoding.plain.ui.images.editor.scene.NativeEditorCanvas
import com.ismartcoding.plain.lib.yjs.ArrowLayer
import com.ismartcoding.plain.lib.yjs.ImageLayer
import com.ismartcoding.plain.lib.yjs.TextLayer
import com.ismartcoding.plain.lib.yjs.EditorLayer
import com.ismartcoding.plain.lib.yjs.EllipseLayer
import com.ismartcoding.plain.lib.yjs.FreehandLayer
import com.ismartcoding.plain.lib.yjs.HighlightLayer
import com.ismartcoding.plain.lib.yjs.MosaicLayer
import com.ismartcoding.plain.lib.yjs.Point
import com.ismartcoding.plain.lib.yjs.RectLayer
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.abs

private enum class DragMode { NONE, MOVE, PAN, RESIZE, WIDTH, ROTATE, CROP_MOVE, CROP_RESIZE }

@Composable
internal fun ImageEditorCanvas(
    vm: ImageEditorEditViewModel,
    document: EditorDocumentState,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    geometry: EditorGeometry,
    cropRect: RectLayer?,
    onCropChange: (RectLayer?) -> Unit,
    onInsertText: (Point) -> Unit,
    modifier: Modifier,
) {
    val layers = document.layers
    val currentCrop by rememberUpdatedState(cropRect)
    var drawingLayer by remember { mutableStateOf<EditorLayer?>(null) }
    var zoom by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
        val density = LocalDensity.current
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .clipToBounds(),
        ) {
            val availW = with(density) { maxWidth.toPx() }
            val availH = with(density) { maxHeight.toPx() }
            val cw = document.canvasSize.width.toFloat()
            val ch = document.canvasSize.height.toFloat()
            val fitScale = if (cw > 0f && ch > 0f) min(availW / cw, availH / ch) * 0.9f else 1f
            val renderScale = fitScale * zoom
            val displayW = cw * renderScale
            val displayH = ch * renderScale

            val currentViewport by rememberUpdatedState(EditorViewport(renderScale, Offset((availW - displayW) / 2 + pan.x, (availH - displayH) / 2 + pan.y)))

            val canvasLabel = stringResource(Res.string.image_editor_canvas)
            Box(Modifier.fillMaxSize().semantics { contentDescription = canvasLabel }
                    .pointerInput(vm.currentTool.value) {
                        val tool = vm.currentTool.value
                        awaitEachGesture {
                            val layers = vm.layers
                            val viewport = currentViewport
                            val gestureScale = viewport.scale
                            fun toCanvas(offset: Offset) = viewport.canvas(offset)
                            var transformStarted = false
                            var rotationStart = 0.0
                            var rotationAngle = 0.0
                            val firstDown = awaitFirstDown(requireUnconsumed = true)
                            val firstPos = firstDown.position
                            firstDown.consume()

                            var isMultiTouch = false
                            var lastPinchDist = 0f
                            var lastCentroid = firstPos

                            var mode = DragMode.NONE
                            var cropOriginal: RectLayer? = null
                            var cropHandle: ResizeHandle? = null
                            var widthHandle: EditorWidthHandle? = null
                            var widthStart = 0.0
                            var scaleOriginal: EditorLayer? = null
                            var scaleBounds: EditorBounds? = null
                            var scaleDistance = 0.0
                            var startPoint = Point(0.0, 0.0)
                            var previewId: String? = null
                            val strokePoints = mutableListOf<Point>()

                            when {
                                tool == ImageEditorTool.SELECT -> {
                                    val cp = toCanvas(firstPos)
                                    val selId = vm.selectedLayerId.value
                                    val selLayer = if (selId != null) layers.find { it.id == selId } else null
                                    val handleThreshold = 12.0 * density.density / gestureScale
                                    val handle = if (selLayer != null) geometry.handle(selLayer, cp, handleThreshold, 8.0 * density.density / gestureScale) else null
                                    widthHandle = selLayer?.let { geometry.widthHandle(it, cp, handleThreshold, 8.0 * density.density / gestureScale) }
                                    val rotateHandle = selLayer?.let { geometry.rotationHandle(it, 8.0 * density.density / gestureScale, 36.0 * density.density / gestureScale) }
                                    when {
                                        rotateHandle != null && kotlin.math.hypot(cp.x - rotateHandle.x, cp.y - rotateHandle.y) <= 16.0 * density.density / gestureScale -> {
                                            val rotation = checkNotNull(layerRotation(checkNotNull(selLayer)))
                                            rotationStart = rotation.degrees
                                            rotationAngle = kotlin.math.atan2(cp.y - rotation.center.y, cp.x - rotation.center.x)
                                            mode = DragMode.ROTATE
                                        }
                                        widthHandle != null && selLayer != null -> {
                                            mode = DragMode.WIDTH
                                            scaleOriginal = selLayer
                                            val rotation = checkNotNull(layerRotation(selLayer))
                                            widthStart = rotateEditorPoint(cp, rotation.copy(degrees = -rotation.degrees)).x
                                        }
                                        handle != null && selLayer != null -> {
                                            mode = DragMode.RESIZE
                                            scaleOriginal = selLayer
                                            scaleBounds = checkNotNull(geometry.bounds(selLayer))
                                            val bounds = checkNotNull(scaleBounds)
                                            scaleDistance = kotlin.math.hypot(cp.x - bounds.x - bounds.width / 2, cp.y - bounds.y - bounds.height / 2)
                                        }
                                        else -> {
                                            val hit = layers.lastOrNull { geometry.hit(it, cp, 12.0 * density.density / gestureScale) }
                                            vm.selectLayer(hit?.id)
                                            if (hit != null) {
                                                mode = DragMode.MOVE
                                            } else {
                                                mode = DragMode.PAN
                                            }
                                        }
                                    }
                                }
                                tool == ImageEditorTool.CROP -> {
                                    startPoint = toCanvas(firstPos)
                                    val crop = currentCrop
                                    cropOriginal = crop
                                    if (crop != null) {
                                        cropHandle = geometry.handle(crop, startPoint, 12.0 * density.density / gestureScale, 0.0)
                                        mode = when {
                                            cropHandle != null -> DragMode.CROP_RESIZE
                                            geometry.bounds(crop)?.contains(startPoint) == true -> DragMode.CROP_MOVE
                                            else -> DragMode.NONE
                                        }
                                    }
                                    if (mode == DragMode.NONE) {
                                        previewId = generateId()
                                        drawingLayer = createShape(tool, previewId, startPoint, startPoint, vm.color.value, vm.lineWidth.value, vm.mosaicBlockSize.value)
                                    }
                                }
                                tool == ImageEditorTool.FREEHAND -> {
                                    strokePoints.clear()
                                    strokePoints.add(toCanvas(firstPos))
                                    previewId = generateId()
                                    drawingLayer = FreehandLayer(
                                        id = previewId,
                                        points = strokePoints.toList(),
                                        color = vm.color.value,
                                        lineWidth = vm.lineWidth.value,
                                    )
                                }
                                tool == ImageEditorTool.TEXT || tool == ImageEditorTool.STICKER -> {
                                }
                                else -> {
                                    startPoint = toCanvas(firstPos)
                                    previewId = generateId()
                                    drawingLayer = createShape(
                                        tool = tool,
                                        id = previewId,
                                        start = startPoint,
                                        end = startPoint,
                                        color = vm.color.value,
                                        lineWidth = vm.lineWidth.value,
                                        mosaicBlockSize = vm.mosaicBlockSize.value,
                                    )
                                }
                            }

                            while (true) {
                                val event = awaitPointerEvent()
                                val active = event.changes.filter { it.pressed }
                                if (active.isEmpty()) break

                                if (active.size >= 2) {
                                    if (!isMultiTouch) {
                                        isMultiTouch = true
                                        drawingLayer = null
                                        previewId = null
                                        strokePoints.clear()
                                        mode = DragMode.NONE
                                        lastPinchDist = 0f
                                    }
                                    val p0 = active[0].position
                                    val p1 = active[1].position
                                    val dist = (p0 - p1).getDistance()
                                    val cent = (p0 + p1) / 2f
                                    if (lastPinchDist > 0) {
                                        val newZoom = (zoom * (dist / lastPinchDist)).coerceIn(0.1f, 5f)
                                        val oldScale = fitScale * zoom
                                        val oldViewport = EditorViewport(oldScale, Offset((availW - cw * oldScale) / 2 + pan.x, (availH - ch * oldScale) / 2 + pan.y))
                                        val next = oldViewport.zoomed(newZoom / zoom, lastCentroid, cent)
                                        zoom = newZoom
                                        pan = next.origin - Offset((availW - cw * next.scale) / 2, (availH - ch * next.scale) / 2)
                                    }
                                    lastPinchDist = dist
                                    lastCentroid = cent
                                    active.forEach { it.consume() }
                                } else if (active.size == 1 && !isMultiTouch) {
                                    val change = active[0]
                                    when (tool) {
                                        ImageEditorTool.SELECT -> {
                                            val dx = (change.positionChange().x / gestureScale).toDouble()
                                            val dy = (change.positionChange().y / gestureScale).toDouble()
                                            if (mode in listOf(DragMode.MOVE, DragMode.RESIZE, DragMode.WIDTH, DragMode.ROTATE) && (dx != 0.0 || dy != 0.0) && !transformStarted) {
                                                vm.beginTransform()
                                                transformStarted = true
                                            }
                                            when (mode) {
                                                DragMode.ROTATE -> {
                                                    val layer = layers.first { it.id == vm.selectedLayerId.value }
                                                    val rotation = checkNotNull(layerRotation(layer))
                                                    val point = toCanvas(change.position)
                                                    val angle = kotlin.math.atan2(point.y - rotation.center.y, point.x - rotation.center.x)
                                                    vm.rotateLayer(layer.id, rotationStart + (angle - rotationAngle) * 180 / kotlin.math.PI)
                                                }
                                                DragMode.WIDTH -> {
                                                    val original = checkNotNull(scaleOriginal)
                                                    val rotation = checkNotNull(layerRotation(original))
                                                    val local = rotateEditorPoint(toCanvas(change.position), rotation.copy(degrees = -rotation.degrees))
                                                    vm.resizeLayerWidth(original, (local.x - widthStart) * checkNotNull(widthHandle).sign)
                                                }
                                                DragMode.RESIZE -> {
                                                    val bounds = checkNotNull(scaleBounds)
                                                    val point = toCanvas(change.position)
                                                    val distance = kotlin.math.hypot(point.x - bounds.x - bounds.width / 2, point.y - bounds.y - bounds.height / 2)
                                                    vm.scaleLayer(checkNotNull(scaleOriginal), distance / scaleDistance, bounds)
                                                }
                                                DragMode.MOVE -> {
                                                    val selId = vm.selectedLayerId.value
                                                    if (selId != null) {
                                                        vm.moveLayer(selId, dx, dy)
                                                    }
                                                }
                                                DragMode.PAN -> {
                                                    pan += change.positionChange()
                                                }
                                                DragMode.NONE, DragMode.CROP_MOVE, DragMode.CROP_RESIZE -> {}
                                            }
                                        }
                                        ImageEditorTool.CROP -> {
                                            val point = toCanvas(change.position)
                                            when (mode) {
                                                DragMode.CROP_MOVE -> onCropChange(moveEditorCrop(checkNotNull(cropOriginal), point.x - startPoint.x, point.y - startPoint.y, vm.document.value.canvasSize))
                                                DragMode.CROP_RESIZE -> onCropChange(resizeEditorCrop(checkNotNull(cropOriginal), checkNotNull(cropHandle), point, vm.document.value.canvasSize))
                                                else -> drawingLayer = createShape(tool, checkNotNull(previewId), startPoint, point, vm.color.value, vm.lineWidth.value, vm.mosaicBlockSize.value)
                                            }
                                        }
                                        ImageEditorTool.FREEHAND -> {
                                            strokePoints.add(toCanvas(change.position))
                                            drawingLayer = FreehandLayer(
                                                id = checkNotNull(previewId),
                                                points = strokePoints.toList(),
                                                color = vm.color.value,
                                                lineWidth = vm.lineWidth.value,
                                            )
                                        }
                                        ImageEditorTool.TEXT, ImageEditorTool.STICKER -> {
                                        }
                                        else -> {
                                            val cur = toCanvas(change.position)
                                            drawingLayer = createShape(
                                                tool = tool,
                                                id = checkNotNull(previewId),
                                                start = startPoint,
                                                end = cur,
                                                color = vm.color.value,
                                                lineWidth = vm.lineWidth.value,
                                                mosaicBlockSize = vm.mosaicBlockSize.value,
                                            )
                                        }
                                    }
                                    change.consume()
                                }
                            }

                            if (!isMultiTouch) {
                                when {
                                    tool == ImageEditorTool.CROP -> {
                                        if (mode == DragMode.NONE) onCropChange((drawingLayer as? RectLayer)?.let { constrainCrop(it, vm.document.value.canvasSize) })
                                        drawingLayer = null
                                        previewId = null
                                    }
                                    tool == ImageEditorTool.TEXT -> {
                                        onInsertText(toCanvas(firstPos))
                                        drawingLayer = null
                                        previewId = null
                                    }
                                    tool == ImageEditorTool.STICKER -> {
                                        val p = toCanvas(firstPos)
                                        vm.addStickerAt(p.x, p.y)
                                        drawingLayer = null
                                        previewId = null
                                    }
                                    tool == ImageEditorTool.FREEHAND -> {
                                        if (strokePoints.size >= 2 && drawingLayer != null) {
                                            vm.addLayer(drawingLayer!!)
                                        }
                                        drawingLayer = null
                                        strokePoints.clear()
                                        previewId = null
                                    }
                                    else -> {
                                        val result = drawingLayer
                                        if (result != null && hasMinSize(result)) {
                                            vm.addLayer(result)
                                        }
                                        drawingLayer = null
                                        previewId = null
                                    }
                                }
                            }
                            mode = DragMode.NONE
                        }
                    }
            ) {
            val colors = MaterialTheme.colorScheme
            Box(Modifier.fillMaxSize().background(lerp(colors.surface, colors.onSurface, 0.12f)))

            Box(
                modifier = Modifier
                    .size(
                        with(density) { displayW.toDp() },
                        with(density) { displayH.toDp() },
                    )
                    .align(Alignment.Center)
                    .offset { IntOffset(pan.x.roundToInt(), pan.y.roundToInt()) }
                    .border(1.dp, colors.outlineVariant),
            ) {
                val renderList = if (drawingLayer != null && vm.currentTool.value != ImageEditorTool.CROP) layers + drawingLayer!! else layers
                NativeEditorCanvas(
                    state = document.copy(layers = renderList),
                    displayScale = renderScale,
                    textMeasurer = textMeasurer,
                    modifier = Modifier.fillMaxSize(),
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    scale(renderScale, pivot = Offset.Zero) {
                        if (vm.currentTool.value == ImageEditorTool.CROP) ((drawingLayer as? RectLayer) ?: cropRect)?.let { drawEditorCrop(constrainCrop(it, vm.document.value.canvasSize), document.canvasSize, renderScale) }
                        val selId = vm.selectedLayerId.value
                        if (selId != null) {
                            val selLayer = layers.find { it.id == selId }
                            if (selLayer != null) {
                                drawSelection(selLayer, geometry, renderScale, density.density)
                                drawResizeHandles(selLayer, geometry, renderScale, density.density)
                            }
                        }
                    }
                }
            }

            ZoomIndicator(
                zoom = zoom,
                onReset = { zoom = 1f; pan = Offset.Zero },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
            }
}

@Composable
private fun ZoomIndicator(
    zoom: Float,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = Color.Black.copy(alpha = 0.6f),
    ) {
        Box(
            modifier = Modifier
                .clickable { onReset() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${(zoom * 100).roundToInt()}%",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

internal fun createShape(
    tool: ImageEditorTool,
    id: String,
    start: Point,
    end: Point,
    color: String,
    lineWidth: Double,
    mosaicBlockSize: Double,
): EditorLayer {
    val x1 = min(start.x, end.x)
    val y1 = min(start.y, end.y)
    val w = abs(end.x - start.x)
    val h = abs(end.y - start.y)
    return when (tool) {
        ImageEditorTool.ARROW -> ArrowLayer(
            id = id, x1 = start.x, y1 = start.y, x2 = end.x, y2 = end.y,
            color = color, lineWidth = lineWidth,
        )
        ImageEditorTool.RECT, ImageEditorTool.CROP -> RectLayer(
            id = id, x = x1, y = y1, w = w, h = h, color = color, lineWidth = lineWidth,
        )
        ImageEditorTool.ELLIPSE -> EllipseLayer(
            id = id, cx = (start.x + end.x) / 2.0, cy = (start.y + end.y) / 2.0,
            rx = w / 2.0, ry = h / 2.0, color = color, lineWidth = lineWidth,
        )
        ImageEditorTool.HIGHLIGHT -> HighlightLayer(
            id = id, x = x1, y = y1, w = w, h = h, color = color,
        )
        ImageEditorTool.MOSAIC -> MosaicLayer(
            id = id, x = x1, y = y1, w = w, h = h, blockSize = mosaicBlockSize,
        )
        else -> error("The selected tool does not create shapes")
    }
}

private fun hasMinSize(layer: EditorLayer): Boolean {
    val threshold = 3.0
    return when (layer) {
        is ArrowLayer -> abs(layer.x2 - layer.x1) > threshold || abs(layer.y2 - layer.y1) > threshold
        is RectLayer -> layer.w > threshold && layer.h > threshold
        is EllipseLayer -> layer.rx > threshold && layer.ry > threshold
        is HighlightLayer -> layer.w > threshold && layer.h > threshold
        is MosaicLayer -> layer.w > threshold && layer.h > threshold
        else -> true
    }
}

private fun DrawScope.drawSelection(layer: EditorLayer, geometry: EditorGeometry, scale: Float, density: Float) {
    if (!layer.visible) return
    val rect = geometry.bounds(layer)?.padded(8.0 * density / scale) ?: return
    val rotation = layerRotation(layer)
    rotate(rotation?.degrees?.toFloat() ?: 0f, rotation?.center?.let { Offset(it.x.toFloat(), it.y.toFloat()) } ?: Offset.Zero) {
        drawRect(Color(0xFF3B82F6), Offset(rect.x.toFloat(), rect.y.toFloat()), Size(rect.width.toFloat(), rect.height.toFloat()),
            style = Stroke(2f * density / scale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f * density / scale, 4f * density / scale))))
    }
}

private fun DrawScope.drawResizeHandles(layer: EditorLayer, geometry: EditorGeometry, scale: Float, density: Float) {
    if (!layer.visible) return
    val rect = geometry.bounds(layer)?.padded(8.0 * density / scale) ?: return
    val rotation = layerRotation(layer)
    val side = 12f * density / scale
    rotate(rotation?.degrees?.toFloat() ?: 0f, rotation?.center?.let { Offset(it.x.toFloat(), it.y.toFloat()) } ?: Offset.Zero) {
        for (handle in ResizeHandle.entries) {
            val point = geometry.corner(rect, handle)
            val origin = Offset(point.x.toFloat() - side / 2, point.y.toFloat() - side / 2)
            drawRect(Color.White, origin, Size(side, side))
            drawRect(Color(0xFF3B82F6), origin, Size(side, side), style = Stroke(2f * density / scale))
        }
        if (layer is TextLayer || layer is ImageLayer) {
            for (x in listOf(rect.x, rect.x + rect.width)) {
                val center = Offset(x.toFloat(), (rect.y + rect.height / 2).toFloat())
                drawCircle(Color.White, side / 2, center)
                drawCircle(Color(0xFF3B82F6), side / 2, center, style = Stroke(2f * density / scale))
            }
        }
        if (rotation != null) {
            val center = Offset((rect.x + rect.width / 2).toFloat(), (rect.y - 36 * density / scale).toFloat())
            drawLine(Color(0xFF3B82F6), center, Offset(center.x, rect.y.toFloat()), strokeWidth = 2f * density / scale)
            drawCircle(Color.White, 12f * density / scale, center)
            drawCircle(Color(0xFF3B82F6), 12f * density / scale, center, style = Stroke(2f * density / scale))
            val radius = 6f * density / scale
            drawArc(Color(0xFF3B82F6), 30f, 280f, false, center - Offset(radius, radius), Size(radius * 2, radius * 2), style = Stroke(2f * density / scale))
        }
    }
}
