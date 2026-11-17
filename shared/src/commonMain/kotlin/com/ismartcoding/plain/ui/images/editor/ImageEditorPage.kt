package com.ismartcoding.plain.ui.images.editor

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.lib.generateId
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.components.NoDataView
import com.ismartcoding.plain.ui.components.NavigationBackIcon
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.components.TextFieldDialog
import com.ismartcoding.plain.ui.images.editor.ImageEditorEditViewModel
import com.ismartcoding.plain.ui.images.editor.ImageEditorTool
import com.ismartcoding.plain.ui.images.editor.ResizeHandle
import com.ismartcoding.plain.ui.images.editor.scene.NativeEditorCanvas
import com.ismartcoding.plain.ui.images.editor.scene.SceneViewport
import com.ismartcoding.plain.ui.images.editor.scene.parseScopeBgColor
import com.ismartcoding.plain.lib.yjs.ArrowLayer
import com.ismartcoding.plain.lib.yjs.EditorLayer
import com.ismartcoding.plain.lib.yjs.EllipseLayer
import com.ismartcoding.plain.lib.yjs.FreehandLayer
import com.ismartcoding.plain.lib.yjs.HighlightLayer
import com.ismartcoding.plain.lib.yjs.ImageLayer
import com.ismartcoding.plain.lib.yjs.MosaicLayer
import com.ismartcoding.plain.lib.yjs.Point
import com.ismartcoding.plain.lib.yjs.RectLayer
import com.ismartcoding.plain.lib.yjs.StickerLayer
import com.ismartcoding.plain.lib.yjs.TextLayer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.redo as ui_drawable_redo
import com.ismartcoding.plain.ui.resources.trash_2 as ui_drawable_trash_2
import com.ismartcoding.plain.ui.resources.undo as ui_drawable_undo
import com.ismartcoding.plain.i18n.undo
import com.ismartcoding.plain.i18n.redo
import kotlin.math.abs

private enum class DragMode { NONE, MOVE, PAN, RESIZE }

private val PRESET_COLORS = listOf(
    "#ff0000", "#00cc00", "#0066ff", "#ffcc00",
    "#ff8800", "#ff00ff", "#00cccc", "#000000",
)
private val PRESET_WIDTHS = listOf(2.0, 4.0, 8.0, 16.0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorPage(
    navController: NavHostController,
    projectId: String,
    source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.ui.navigation.LocalContentSource.current,
    vm: ImageEditorEditViewModel = viewModel { ImageEditorEditViewModel(com.ismartcoding.plain.features.images.editor.ImageEditorProjectsClient(source.api)) },
) {
    val document by vm.document.collectAsState()
    val layers = document.layers

    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val imageActions = rememberEditorImageActions(vm, textMeasurer)
    var showCanvasSettings by remember { mutableStateOf(false) }
    var previewImage by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var showProperties by remember { mutableStateOf(false) }
    var cropRect by remember { mutableStateOf<RectLayer?>(null) }
    var drawingLayer by remember { mutableStateOf<EditorLayer?>(null) }
    var showTextDialog by remember { mutableStateOf(false) }
    var textInsertPoint by remember { mutableStateOf<Point?>(null) }
    var zoom by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(projectId) {
        vm.loadAsync(projectId)
        zoom = 1f
        pan = Offset.Zero
    }

    fun handleBack() {
        if (vm.dirty.value) {
            vm.saveAsync(onDone = { navController.navigateUp() })
        } else {
            navController.navigateUp()
        }
    }

    if (showTextDialog) {
        TextFieldDialog(
            title = stringResource(Res.string.image_editor_tool_text),
            value = "",
            singleLine = false,
            placeholder = stringResource(Res.string.image_editor_tool_text),
            onDismissRequest = { showTextDialog = false; textInsertPoint = null },
            onConfirm = { text ->
                val p = textInsertPoint ?: Point(50.0, 50.0)
                vm.addLayer(
                    TextLayer(
                        id = generateId(),
                        x = p.x, y = p.y,
                        text = text,
                        fontSize = 48.0,
                        color = vm.color.value,
                        maxWidth = 200.0,
                    ),
                )
                showTextDialog = false
                textInsertPoint = null
            },
        )
    }

    if (vm.showLayerPanel.value) {
        ImageEditorLayerPanel(vm = vm, onDismiss = { vm.showLayerPanel.value = false })
    }

    val selectedLayer = layers.find { it.id == vm.selectedLayerId.value }
    if (showProperties && selectedLayer != null) ImageEditorProperties(selectedLayer, vm::updateLayer, replaceImage = { imageActions.pick(EditorImageTarget.Replacement(selectedLayer.id)) }, dismiss = { showProperties = false })

    if (showCanvasSettings) ImageEditorCanvasSettings(document, vm, { imageActions.pick(EditorImageTarget.Background) }, { showCanvasSettings = false })
    previewImage?.let { ImageEditorPreview(it) { previewImage = null } }

    PScaffold(
        topBar = {
            PTopAppBar(
                onNavigateBack = { navController.navigateUp() },
                navigationIcon = { NavigationBackIcon { handleBack() } },
                title = stringResource(Res.string.image_editor),
                actions = {
                    PIconButton(
                        icon = UiRes.drawable.ui_drawable_undo,
                        contentDescription = stringResource(Res.string.undo),
                        tint = MaterialTheme.colorScheme.onSurface,
                        enabled = vm.canUndo.value,
                    ) { vm.undo() }
                    PIconButton(
                        icon = UiRes.drawable.ui_drawable_redo,
                        contentDescription = stringResource(Res.string.redo),
                        tint = MaterialTheme.colorScheme.onSurface,
                        enabled = vm.canRedo.value,
                    ) { vm.redo() }
                    if (selectedLayer != null) com.ismartcoding.plain.ui.base.PTextButton(stringResource(Res.string.image_editor_properties), { showProperties = true })
                    ImageEditorMenu(vm.loadState.value == EditorLoadState.Ready, { showCanvasSettings = true }, { previewImage = imageActions.render() }, imageActions::export)
                    val sel = vm.selectedLayerId.value
                    if (sel != null) {
                        PIconButton(
                            icon = UiRes.drawable.ui_drawable_trash_2,
                            contentDescription = stringResource(Res.string.image_editor_delete_layer),
                            tint = MaterialTheme.colorScheme.onSurface,
                        ) { vm.deleteLayer(sel) }
                    }
                },
            )
        },
        bottomBar = {
            if (vm.loadState.value == EditorLoadState.Ready) Column {
                if (vm.currentTool.value == ImageEditorTool.CROP) Row {
                    com.ismartcoding.plain.ui.base.PTextButton(stringResource(Res.string.image_editor_apply_crop), {
                        cropRect?.let { crop ->
                            val image = cropEditorBitmap(imageActions.render(), crop)
                            vm.applyCrop(embeddedEditorPng(image), image.width, image.height)
                            cropRect = null
                        }
                    }, enabled = cropRect?.let { it.w >= 2 && it.h >= 2 } == true)
                    com.ismartcoding.plain.ui.base.PTextButton(stringResource(Res.string.cancel), { cropRect = null; vm.currentTool.value = ImageEditorTool.SELECT })
                }
                if (vm.currentTool.value != ImageEditorTool.SELECT &&
                    vm.currentTool.value != ImageEditorTool.TEXT
                ) {
                    StylePickerRow(
                        currentColor = vm.color.value,
                        currentWidth = vm.lineWidth.value,
                        onColorSelected = { vm.color.value = it },
                        onWidthSelected = { vm.lineWidth.value = it },
                    )
                }
                ImageEditorBottomAppBar(
                    currentTool = vm.currentTool.value,
                    onToolSelected = {
                        cropRect = null
                        vm.currentTool.value = it
                        vm.selectLayer(null)
                    },
                    onAddImage = { imageActions.pick(EditorImageTarget.Layer) },
                    onClear = { vm.clearLayers() },
                    onLayerPanel = { vm.showLayerPanel.value = true },
                )
            }
        },
    ) { paddingValues ->
        when (val loading = vm.loadState.value) {
            EditorLoadState.Loading -> { NoDataColumn(loading = true, modifier = Modifier.padding(paddingValues)); return@PScaffold }
            is EditorLoadState.Failed -> { NoDataView(modifier = Modifier.padding(paddingValues), message = loading.message, icon = null); return@PScaffold }
            EditorLoadState.Ready -> Unit
        }
        val density = LocalDensity.current
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
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

            val toCanvas: (Offset) -> Point = { offset ->
                Point((offset.x / renderScale).toDouble(), (offset.y / renderScale).toDouble())
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawDarkCheckerboard(size.width, size.height)
            }

            Box(
                modifier = Modifier
                    .size(
                        with(density) { displayW.toDp() },
                        with(density) { displayH.toDp() },
                    )
                    .align(Alignment.Center)
                    .offset { IntOffset(pan.x.roundToInt(), pan.y.roundToInt()) }
                    .border(1.dp, Color(0xFF888888))
                    .pointerInput(vm.currentTool.value, layers) {
                        val tool = vm.currentTool.value
                        awaitEachGesture {
                            val firstDown = awaitFirstDown(requireUnconsumed = false)
                            val firstPos = firstDown.position
                            firstDown.consume()

                            var isMultiTouch = false
                            var lastPinchDist = 0f
                            var lastCentroid = firstPos

                            var mode = DragMode.NONE
                            var activeHandleLocal: ResizeHandle? = null
                            var startPoint = Point(0.0, 0.0)
                            var previewId: String? = null
                            val strokePoints = mutableListOf<Point>()

                            when {
                                tool == ImageEditorTool.SELECT -> {
                                    val cp = toCanvas(firstPos)
                                    val selId = vm.selectedLayerId.value
                                    val selLayer = if (selId != null) layers.find { it.id == selId } else null
                                    val handleThreshold = 24.0 / renderScale.toDouble()
                                    val handle = if (selLayer != null) hitTestHandle(selLayer, cp, handleThreshold) else null
                                    when {
                                        handle != null && selLayer != null -> {
                                            mode = DragMode.RESIZE
                                            activeHandleLocal = handle
                                            vm.beginResize()
                                        }
                                        else -> {
                                            val hit = layers.lastOrNull { hitTest(it, cp) }
                                            vm.selectLayer(hit?.id)
                                            if (hit != null) {
                                                mode = DragMode.MOVE
                                                vm.beginMove()
                                            } else {
                                                mode = DragMode.PAN
                                            }
                                        }
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
                                    // No preview; action happens on gesture end
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
                                        activeHandleLocal = null
                                        lastPinchDist = 0f
                                    }
                                    val p0 = active[0].position
                                    val p1 = active[1].position
                                    val dist = (p0 - p1).getDistance()
                                    val cent = (p0 + p1) / 2f
                                    if (lastPinchDist > 0) {
                                        val zoomChange = dist / lastPinchDist
                                        val panChange = cent - lastCentroid
                                        val newZoom = (zoom * zoomChange).coerceIn(0.1f, 5f)
                                        zoom = newZoom
                                        pan += panChange
                                    }
                                    lastPinchDist = dist
                                    lastCentroid = cent
                                    active.forEach { it.consume() }
                                } else if (active.size == 1 && !isMultiTouch) {
                                    val change = active[0]
                                    when (tool) {
                                        ImageEditorTool.SELECT -> {
                                            val dx = (change.positionChange().x / renderScale).toDouble()
                                            val dy = (change.positionChange().y / renderScale).toDouble()
                                            when (mode) {
                                                DragMode.RESIZE -> {
                                                    val selId = vm.selectedLayerId.value
                                                    if (selId != null && activeHandleLocal != null) {
                                                        vm.resizeLayer(selId, activeHandleLocal, dx, dy)
                                                    }
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
                                                DragMode.NONE -> {}
                                            }
                                        }
                                        ImageEditorTool.FREEHAND -> {
                                            strokePoints.add(toCanvas(change.position))
                                            drawingLayer = FreehandLayer(
                                                id = previewId ?: generateId(),
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
                                                id = previewId ?: generateId(),
                                                start = startPoint,
                                                end = cur,
                                                color = vm.color.value,
                                                lineWidth = vm.lineWidth.value,
                                            )
                                        }
                                    }
                                    change.consume()
                                }
                            }

                            if (!isMultiTouch) {
                                when {
                                    tool == ImageEditorTool.CROP -> {
                                        cropRect = (drawingLayer as? RectLayer)?.let { constrainCrop(it, document.canvasSize) }
                                        drawingLayer = null
                                        previewId = null
                                    }
                                    tool == ImageEditorTool.TEXT -> {
                                        textInsertPoint = toCanvas(firstPos)
                                        showTextDialog = true
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
                            activeHandleLocal = null
                        }
                    },
            ) {
                val renderList = if (drawingLayer != null && vm.currentTool.value != ImageEditorTool.CROP) layers + drawingLayer!! else layers
                NativeEditorCanvas(
                    state = document.copy(layers = renderList),
                    viewport = SceneViewport(renderScale, 0f, 0f),
                    modifier = Modifier.fillMaxSize(),
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    scale(renderScale, pivot = Offset.Zero) {
                        if (vm.currentTool.value == ImageEditorTool.CROP) ((drawingLayer as? RectLayer) ?: cropRect)?.let { drawEditorCrop(constrainCrop(it, document.canvasSize), renderScale) }
                        val selId = vm.selectedLayerId.value
                        if (selId != null) {
                            val selLayer = layers.find { it.id == selId }
                            if (selLayer != null) {
                                drawSelection(selLayer, renderScale)
                                drawResizeHandles(selLayer, renderScale)
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
private fun StylePickerRow(
    currentColor: String,
    currentWidth: Double,
    onColorSelected: (String) -> Unit,
    onWidthSelected: (Double) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PRESET_COLORS.forEach { color ->
                val selected = color.equals(currentColor, ignoreCase = true)
                val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(parseScopeBgColor(color))
                        .border(2.dp, borderColor, CircleShape)
                        .clickable { onColorSelected(color) },
                )
            }
            Box(
                modifier = Modifier
                    .size(1.dp, 24.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            PRESET_WIDTHS.forEach { w ->
                val selected = w == currentWidth
                val containerColor = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                } else {
                    Color.Transparent
                }
                val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Box(
                    modifier = Modifier
                        .size(40.dp, 26.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(containerColor)
                        .clickable { onWidthSelected(w) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = w.toInt().toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                    )
                }
            }
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
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.6f),
    ) {
        Box(
            modifier = Modifier
                .clickable { onReset() }
                .padding(horizontal = 10.dp, vertical = 6.dp),
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

private fun DrawScope.drawDarkCheckerboard(w: Float, h: Float, tile: Float = 16f) {
    val c1 = Color(0xFF1E1E1E)
    val c2 = Color(0xFF2A2A2A)
    var y = 0f
    var row = 0
    while (y < h) {
        var x = 0f
        var col = 0
        while (x < w) {
            drawRect(
                color = if ((row + col) % 2 == 0) c1 else c2,
                topLeft = Offset(x, y),
                size = Size(tile, tile),
            )
            x += tile
            col++
        }
        y += tile
        row++
    }
}

private fun createShape(
    tool: ImageEditorTool,
    id: String,
    start: Point,
    end: Point,
    color: String,
    lineWidth: Double,
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
            id = id, x = x1, y = y1, w = w, h = h,
        )
        else -> ArrowLayer(
            id = id, x1 = start.x, y1 = start.y, x2 = end.x, y2 = end.y,
            color = color, lineWidth = lineWidth,
        )
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

private fun DrawScope.drawSelection(layer: EditorLayer, renderScale: Float) {
    val bounds = layerBounds(layer) ?: return
    val dashW = 2f / renderScale
    val dashLen = 8f / renderScale
    val gapLen = 6f / renderScale
    drawRect(
        color = Color(0xFF4A90E2),
        topLeft = Offset(bounds.first.x.toFloat(), bounds.first.y.toFloat()),
        size = Size(bounds.second.x.toFloat(), bounds.second.y.toFloat()),
        style = Stroke(width = dashW, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLen, gapLen))),
    )
}

private fun DrawScope.drawResizeHandles(layer: EditorLayer, renderScale: Float) {
    val bounds = layerBounds(layer) ?: return
    val (origin, size) = bounds
    val hs = 12f / renderScale
    val corners = listOf(
        Offset(origin.x.toFloat(), origin.y.toFloat()),
        Offset((origin.x + size.x).toFloat(), origin.y.toFloat()),
        Offset(origin.x.toFloat(), (origin.y + size.y).toFloat()),
        Offset((origin.x + size.x).toFloat(), (origin.y + size.y).toFloat()),
    )
    for (corner in corners) {
        drawRect(
            color = Color.White,
            topLeft = Offset(corner.x - hs / 2, corner.y - hs / 2),
            size = Size(hs, hs),
        )
        drawRect(
            color = Color(0xFF4A90E2),
            topLeft = Offset(corner.x - hs / 2, corner.y - hs / 2),
            size = Size(hs, hs),
            style = Stroke(width = 2f / renderScale),
        )
    }
}
