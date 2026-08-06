package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.features.ImageEditorProjectHelper
import com.ismartcoding.plain.helpers.Base64Lenient
import com.ismartcoding.plain.helpers.StringHelper
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.yjs.ArrowLayer
import com.ismartcoding.plain.lib.yjs.CanvasSize
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
import com.ismartcoding.plain.lib.yjs.YjsDoc
import com.ismartcoding.plain.lib.yjs.YjsDocEncoder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class ImageEditorTool {
    SELECT, ARROW, RECT, ELLIPSE, HIGHLIGHT, MOSAIC, TEXT, FREEHAND, STICKER
}

enum class ResizeHandle { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

class ImageEditorEditViewModel : ViewModel() {
    private val _layersFlow = MutableStateFlow<List<EditorLayer>>(emptyList())
    val layersFlow: StateFlow<List<EditorLayer>> = _layersFlow

    var id = mutableStateOf("")
    var canvasSize = mutableStateOf(CanvasSize(1920, 1080))
    var bgColor = mutableStateOf("transparent")
    var sourceImage = mutableStateOf<String?>(null)
    var imgOffset = mutableStateOf(Point(0.0, 0.0))
    var imgAlpha = mutableStateOf(100.0)
    var images = mutableStateOf<Map<String, String>>(emptyMap())

    var currentTool = mutableStateOf(ImageEditorTool.SELECT)
    var selectedLayerId = mutableStateOf<String?>(null)
    var color = mutableStateOf("#ff0000")
    var lineWidth = mutableStateOf(4.0)
    var showLoading = mutableStateOf(true)
    var dirty = mutableStateOf(false)
    var level = mutableStateOf(0)
    var showLayerPanel = mutableStateOf(false)
    var showColorPicker = mutableStateOf(false)

    private val history = mutableListOf<List<EditorLayer>>()
    private val redoStack = mutableListOf<List<EditorLayer>>()
    var canUndo = mutableStateOf(false)
    var canRedo = mutableStateOf(false)

    private var stickerColorIdx = 0

    private var autoSaveJob: Job? = null

    val layers: List<EditorLayer> get() = _layersFlow.value

    suspend fun loadAsync(projectId: String) = withIO {
        id.value = projectId
        history.clear()
        redoStack.clear()
        refreshUndoFlags()
        if (projectId.isEmpty()) {
            showLoading.value = false
            return@withIO
        }
        val project = ImageEditorProjectHelper.getByIdAsync(projectId)
        if (project == null) {
            showLoading.value = false
            return@withIO
        }
        if (project.stateB64.isNotEmpty()) {
            runCatching {
                val bytes = Base64Lenient.decode(project.stateB64)
                val doc = YjsDoc(bytes)
                canvasSize.value = doc.getCanvasSize()
                bgColor.value = doc.getBgColor()
                sourceImage.value = doc.getSourceImage()
                imgOffset.value = doc.getImgOffset()
                imgAlpha.value = doc.getImgAlpha()
                _layersFlow.value = doc.getLayers()
            }
        }
        dirty.value = false
        showLoading.value = false
    }

    fun toggleLevel() {
        level.value = if (level.value == 0) 1 else 0
    }

    fun selectLayer(layerId: String?) {
        selectedLayerId.value = layerId
    }

    private fun pushHistory() {
        history.add(_layersFlow.value)
        redoStack.clear()
        refreshUndoFlags()
    }

    private fun refreshUndoFlags() {
        canUndo.value = history.isNotEmpty()
        canRedo.value = redoStack.isNotEmpty()
    }

    private fun markDirty() {
        dirty.value = true
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launchSafe {
            delay(2000)
            saveAsync()
        }
    }

    fun undo() {
        if (history.isEmpty()) return
        redoStack.add(_layersFlow.value)
        _layersFlow.value = history.removeAt(history.lastIndex)
        markDirty()
        refreshUndoFlags()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        history.add(_layersFlow.value)
        _layersFlow.value = redoStack.removeAt(redoStack.lastIndex)
        markDirty()
        refreshUndoFlags()
    }

    fun clearLayers() {
        if (_layersFlow.value.isEmpty()) return
        pushHistory()
        _layersFlow.value = emptyList()
        selectedLayerId.value = null
        markDirty()
    }

    fun addLayer(layer: EditorLayer) {
        pushHistory()
        _layersFlow.update { it + layer }
        selectedLayerId.value = layer.id
        markDirty()
    }

    fun addStickerAt(x: Double, y: Double) {
        val colors = listOf("#fef08a", "#bbf7d0", "#bfdbfe", "#fbcfe8", "#fde68a")
        val color = colors[stickerColorIdx % colors.size]
        stickerColorIdx++
        val cw = canvasSize.value.width.toDouble()
        val layer = StickerLayer(
            id = StringHelper.shortUUID(),
            x = x,
            y = y,
            w = cw * 0.18,
            h = cw * 0.06,
            color = color,
            text = "Note",
            fontSize = (cw * 0.02).toInt().toDouble(),
            fontWeight = "600",
        )
        addLayer(layer)
    }

    fun updateLayer(layer: EditorLayer) {
        pushHistory()
        _layersFlow.update { list ->
            list.map { if (it.id == layer.id) layer else it }
        }
        markDirty()
    }

    fun deleteLayer(layerId: String) {
        pushHistory()
        _layersFlow.update { list -> list.filter { it.id != layerId } }
        if (selectedLayerId.value == layerId) {
            selectedLayerId.value = null
        }
        markDirty()
    }

    fun moveLayer(layerId: String, dx: Double, dy: Double) {
        _layersFlow.update { list ->
            list.map { layer ->
                if (layer.id == layerId) moveLayerBy(layer, dx, dy) else layer
            }
        }
        markDirty()
    }

    fun beginMove() {
        pushHistory()
    }

    fun beginResize() {
        pushHistory()
    }

    fun resizeLayer(layerId: String, handle: ResizeHandle, dx: Double, dy: Double) {
        _layersFlow.update { list ->
            list.map { layer ->
                if (layer.id == layerId) resizeLayerBy(layer, handle, dx, dy) else layer
            }
        }
        markDirty()
    }

    fun toggleVisibility(layerId: String) {
        pushHistory()
        _layersFlow.update { list ->
            list.map { layer ->
                if (layer.id == layerId) setLayerVisible(layer, !layer.visible) else layer
            }
        }
        markDirty()
    }

    fun duplicateLayer(layerId: String) {
        val layer = _layersFlow.value.find { it.id == layerId } ?: return
        pushHistory()
        val copy = offsetLayer(layer, 20.0, 20.0, StringHelper.shortUUID())
        _layersFlow.update { it + copy }
        selectedLayerId.value = copy.id
        markDirty()
    }

    fun bringForward(layerId: String) {
        val list = _layersFlow.value.toMutableList()
        val idx = list.indexOfFirst { it.id == layerId }
        if (idx < 0 || idx >= list.size - 1) return
        pushHistory()
        val tmp = list[idx]
        list[idx] = list[idx + 1]
        list[idx + 1] = tmp
        _layersFlow.value = list
        markDirty()
    }

    fun sendBackward(layerId: String) {
        val list = _layersFlow.value.toMutableList()
        val idx = list.indexOfFirst { it.id == layerId }
        if (idx <= 0) return
        pushHistory()
        val tmp = list[idx]
        list[idx] = list[idx - 1]
        list[idx - 1] = tmp
        _layersFlow.value = list
        markDirty()
    }

    fun setBgColor(color: String) {
        bgColor.value = color
        markDirty()
    }

    fun setCanvasSize(width: Int, height: Int) {
        canvasSize.value = CanvasSize(width, height)
        markDirty()
    }

    fun saveAsync(onDone: () -> Unit = {}) {
        autoSaveJob?.cancel()
        viewModelScope.launchSafe {
            withIO {
                val stateBytes = YjsDocEncoder.encode(
                    canvasSize = canvasSize.value,
                    bgColor = bgColor.value,
                    sourceImage = sourceImage.value,
                    imgOffset = imgOffset.value,
                    imgAlpha = imgAlpha.value,
                    layers = _layersFlow.value,
                    images = images.value,
                )
                val stateB64 = Base64Lenient.encode(stateBytes)
                val projectId = if (id.value.isNotEmpty()) id.value else StringHelper.shortUUID()
                val updated = ImageEditorProjectHelper.addOrUpdateAsync(projectId) {
                    this.stateB64 = stateB64
                    this.canvasWidth = canvasSize.value.width
                    this.canvasHeight = canvasSize.value.height
                    this.layerCount = _layersFlow.value.size
                }
                if (updated != null) {
                    id.value = updated.id
                    dirty.value = false
                }
            }
            onDone()
        }
    }

    override fun onCleared() {
        autoSaveJob?.cancel()
        super.onCleared()
    }

    private fun moveLayerBy(layer: EditorLayer, dx: Double, dy: Double): EditorLayer {
        return when (layer) {
            is ArrowLayer -> layer.copy(x1 = layer.x1 + dx, y1 = layer.y1 + dy, x2 = layer.x2 + dx, y2 = layer.y2 + dy)
            is RectLayer -> layer.copy(x = layer.x + dx, y = layer.y + dy)
            is EllipseLayer -> layer.copy(cx = layer.cx + dx, cy = layer.cy + dy)
            is HighlightLayer -> layer.copy(x = layer.x + dx, y = layer.y + dy)
            is MosaicLayer -> layer.copy(x = layer.x + dx, y = layer.y + dy)
            is TextLayer -> layer.copy(x = layer.x + dx, y = layer.y + dy)
            is ImageLayer -> layer.copy(x = layer.x + dx, y = layer.y + dy)
            is FreehandLayer -> layer.copy(
                points = layer.points.map { Point(it.x + dx, it.y + dy) }
            )
            is StickerLayer -> layer.copy(x = layer.x + dx, y = layer.y + dy)
        }
    }

    private fun offsetLayer(layer: EditorLayer, dx: Double, dy: Double, newId: String): EditorLayer {
        val moved = moveLayerBy(layer, dx, dy)
        return setLayerId(moved, newId)
    }

    private fun resizeLayerBy(layer: EditorLayer, handle: ResizeHandle, dx: Double, dy: Double): EditorLayer {
        val minSize = 4.0
        return when (layer) {
            is RectLayer -> {
                var nx = layer.x; var ny = layer.y; var nw = layer.w; var nh = layer.h
                when (handle) {
                    ResizeHandle.TOP_LEFT -> { nx += dx; ny += dy; nw -= dx; nh -= dy }
                    ResizeHandle.TOP_RIGHT -> { nw += dx; ny += dy; nh -= dy }
                    ResizeHandle.BOTTOM_LEFT -> { nx += dx; nw -= dx; nh += dy }
                    ResizeHandle.BOTTOM_RIGHT -> { nw += dx; nh += dy }
                }
                if (nw < minSize) { if (handle == ResizeHandle.TOP_LEFT || handle == ResizeHandle.BOTTOM_LEFT) nx -= (minSize - nw); nw = minSize }
                if (nh < minSize) { if (handle == ResizeHandle.TOP_LEFT || handle == ResizeHandle.TOP_RIGHT) ny -= (minSize - nh); nh = minSize }
                layer.copy(x = nx, y = ny, w = nw, h = nh)
            }
            is EllipseLayer -> {
                var left = layer.cx - layer.rx
                var top = layer.cy - layer.ry
                var right = layer.cx + layer.rx
                var bottom = layer.cy + layer.ry
                when (handle) {
                    ResizeHandle.TOP_LEFT -> { left += dx; top += dy }
                    ResizeHandle.TOP_RIGHT -> { right += dx; top += dy }
                    ResizeHandle.BOTTOM_LEFT -> { left += dx; bottom += dy }
                    ResizeHandle.BOTTOM_RIGHT -> { right += dx; bottom += dy }
                }
                val w = (right - left).coerceAtLeast(minSize)
                val h = (bottom - top).coerceAtLeast(minSize)
                layer.copy(cx = (left + right) / 2.0, cy = (top + bottom) / 2.0, rx = w / 2.0, ry = h / 2.0)
            }
            is HighlightLayer -> {
                var nx = layer.x; var ny = layer.y; var nw = layer.w; var nh = layer.h
                when (handle) {
                    ResizeHandle.TOP_LEFT -> { nx += dx; ny += dy; nw -= dx; nh -= dy }
                    ResizeHandle.TOP_RIGHT -> { nw += dx; ny += dy; nh -= dy }
                    ResizeHandle.BOTTOM_LEFT -> { nx += dx; nw -= dx; nh += dy }
                    ResizeHandle.BOTTOM_RIGHT -> { nw += dx; nh += dy }
                }
                if (nw < minSize) { if (handle == ResizeHandle.TOP_LEFT || handle == ResizeHandle.BOTTOM_LEFT) nx -= (minSize - nw); nw = minSize }
                if (nh < minSize) { if (handle == ResizeHandle.TOP_LEFT || handle == ResizeHandle.TOP_RIGHT) ny -= (minSize - nh); nh = minSize }
                layer.copy(x = nx, y = ny, w = nw, h = nh)
            }
            is MosaicLayer -> {
                var nx = layer.x; var ny = layer.y; var nw = layer.w; var nh = layer.h
                when (handle) {
                    ResizeHandle.TOP_LEFT -> { nx += dx; ny += dy; nw -= dx; nh -= dy }
                    ResizeHandle.TOP_RIGHT -> { nw += dx; ny += dy; nh -= dy }
                    ResizeHandle.BOTTOM_LEFT -> { nx += dx; nw -= dx; nh += dy }
                    ResizeHandle.BOTTOM_RIGHT -> { nw += dx; nh += dy }
                }
                if (nw < minSize) nw = minSize
                if (nh < minSize) nh = minSize
                layer.copy(x = nx, y = ny, w = nw, h = nh)
            }
            is ImageLayer -> {
                var left = layer.x - layer.w / 2
                var top = layer.y - layer.h / 2
                var right = layer.x + layer.w / 2
                var bottom = layer.y + layer.h / 2
                when (handle) {
                    ResizeHandle.TOP_LEFT -> { left += dx; top += dy }
                    ResizeHandle.TOP_RIGHT -> { right += dx; top += dy }
                    ResizeHandle.BOTTOM_LEFT -> { left += dx; bottom += dy }
                    ResizeHandle.BOTTOM_RIGHT -> { right += dx; bottom += dy }
                }
                val w = (right - left).coerceAtLeast(minSize)
                val h = (bottom - top).coerceAtLeast(minSize)
                layer.copy(x = (left + right) / 2.0, y = (top + bottom) / 2.0, w = w, h = h)
            }
            is StickerLayer -> {
                var left = layer.x - layer.w / 2
                var top = layer.y - layer.h / 2
                var right = layer.x + layer.w / 2
                var bottom = layer.y + layer.h / 2
                when (handle) {
                    ResizeHandle.TOP_LEFT -> { left += dx; top += dy }
                    ResizeHandle.TOP_RIGHT -> { right += dx; top += dy }
                    ResizeHandle.BOTTOM_LEFT -> { left += dx; bottom += dy }
                    ResizeHandle.BOTTOM_RIGHT -> { right += dx; bottom += dy }
                }
                val w = (right - left).coerceAtLeast(minSize)
                val h = (bottom - top).coerceAtLeast(minSize)
                layer.copy(x = (left + right) / 2.0, y = (top + bottom) / 2.0, w = w, h = h)
            }
            else -> layer
        }
    }

    private fun setLayerVisible(layer: EditorLayer, visible: Boolean): EditorLayer {
        return when (layer) {
            is ArrowLayer -> layer.copy(visible = visible)
            is RectLayer -> layer.copy(visible = visible)
            is EllipseLayer -> layer.copy(visible = visible)
            is HighlightLayer -> layer.copy(visible = visible)
            is MosaicLayer -> layer.copy(visible = visible)
            is TextLayer -> layer.copy(visible = visible)
            is ImageLayer -> layer.copy(visible = visible)
            is FreehandLayer -> layer.copy(visible = visible)
            is StickerLayer -> layer.copy(visible = visible)
        }
    }

    private fun setLayerId(layer: EditorLayer, newId: String): EditorLayer {
        return when (layer) {
            is ArrowLayer -> layer.copy(id = newId)
            is RectLayer -> layer.copy(id = newId)
            is EllipseLayer -> layer.copy(id = newId)
            is HighlightLayer -> layer.copy(id = newId)
            is MosaicLayer -> layer.copy(id = newId)
            is TextLayer -> layer.copy(id = newId)
            is ImageLayer -> layer.copy(id = newId)
            is FreehandLayer -> layer.copy(id = newId)
            is StickerLayer -> layer.copy(id = newId)
        }
    }
}
