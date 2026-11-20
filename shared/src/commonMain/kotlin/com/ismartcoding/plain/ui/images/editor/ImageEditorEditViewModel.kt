package com.ismartcoding.plain.ui.images.editor

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.features.images.editor.ImageEditorProjectsClient
import com.ismartcoding.plain.helpers.Base64Lenient
import com.ismartcoding.plain.lib.generateId
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.yjs.*
import com.ismartcoding.plain.ui.state.launchSafe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max
import kotlin.math.min


enum class ImageEditorTool { SELECT, CROP, FREEHAND, ARROW, RECT, ELLIPSE, HIGHLIGHT, MOSAIC, TEXT, STICKER }
enum class ResizeHandle { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

class ImageEditorEditViewModel(private val projects: ImageEditorProjectsClient, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) : ViewModel() {
    private val state = MutableStateFlow(EditorDocumentState())
    val document = state.asStateFlow()
    val layers: List<EditorLayer> get() = state.value.layers
    val id = mutableStateOf("")
    val loadState = mutableStateOf<EditorLoadState>(EditorLoadState.Loading)
    val dirty = mutableStateOf(false)
    val currentTool = mutableStateOf(ImageEditorTool.SELECT)
    val selectedLayerId = mutableStateOf<String?>(null)
    val color = mutableStateOf("#ef4444")
    val lineWidth = mutableStateOf(4.0)
    val mosaicBlockSize = mutableStateOf(10.0)
    val showLayerPanel = mutableStateOf(false)
    val canUndo = mutableStateOf(false)
    val canRedo = mutableStateOf(false)
    private val history = mutableListOf<EditorDocumentState>()
    private val redoStack = mutableListOf<EditorDocumentState>()
    private var autoSaveJob: Job? = null
    private val saveMutex = Mutex()
    internal val imageActions = EditorImageActions(this)

    suspend fun loadAsync(projectId: String) {
        if (loadState.value == EditorLoadState.Ready) return
        loadState.value = EditorLoadState.Loading
        id.value = projectId
        history.clear(); redoStack.clear(); refreshUndoFlags()
        try {
            val decoded = if (projectId.isEmpty()) EditorDocumentState(bgColor = "#ffffff") else withIO {
                val project = checkNotNull(projects.getByIdAsync(projectId)) { "Image editor project not found" }
                val doc = YjsDoc(Base64Lenient.decode(project.stateB64))
                require((doc.getMetaValue("formatVersion") as Number).toDouble() == YjsDocEncoder.FORMAT_VERSION.toDouble()) { "Unsupported image editor document format" }
                EditorDocumentState(doc.getCanvasSize(), doc.getBgColor(), doc.getSourceImage(), doc.getImgOffset(), doc.getImgAlpha(), doc.getLayers(), doc.getImages())
            }
            com.ismartcoding.plain.ui.images.editor.scene.decodeSceneImages(decoded.sourceImage, decoded.images)
            for (layer in decoded.layers.filterIsInstance<ImageLayer>()) require(layer.id in decoded.images) { "Missing editor image resource" }
            state.value = decoded
            dirty.value = false
            loadState.value = EditorLoadState.Ready
        } catch (canceled: kotlinx.coroutines.CancellationException) { throw canceled }
        catch (error: Exception) { loadState.value = EditorLoadState.Failed(error.message.orEmpty()) }
    }

    fun selectLayer(layerId: String?) { selectedLayerId.value = layerId }
    private fun pushHistory() { history.add(state.value); redoStack.clear(); refreshUndoFlags() }
    private fun refreshUndoFlags() { canUndo.value = history.isNotEmpty(); canRedo.value = redoStack.isNotEmpty() }
    private fun markDirty() {
        dirty.value = true
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launchSafe { delay(2000); saveAsync() }
    }
    fun undo() {
        if (history.isEmpty()) return
        redoStack.add(state.value); state.value = history.removeAt(history.lastIndex)
        selectedLayerId.value = null; markDirty(); refreshUndoFlags()
    }
    fun redo() {
        if (redoStack.isEmpty()) return
        history.add(state.value); state.value = redoStack.removeAt(redoStack.lastIndex)
        selectedLayerId.value = null; markDirty(); refreshUndoFlags()
    }
    fun clearLayers() {
        if (layers.isEmpty()) return
        pushHistory(); state.update { it.copy(layers = emptyList(), images = emptyMap()) }
        selectedLayerId.value = null; markDirty()
    }
    fun addLayer(layer: EditorLayer) {
        pushHistory(); state.update { it.copy(layers = it.layers + layer) }
        selectedLayerId.value = layer.id; markDirty()
    }
    fun addStickerAt(x: Double, y: Double) = addLayer(StickerLayer(id = generateId(), x = x, y = y, w = 240.0, h = 120.0, text = "", color = "#fef08a", fontSize = 24.0))
    fun updateLayer(layer: EditorLayer) {
        if (layers.find { it.id == layer.id } == layer) return
        pushHistory(); state.update { it.copy(layers = it.layers.map { existing -> if (existing.id == layer.id) layer else existing }) }; markDirty()
    }
    fun deleteLayer(layerId: String) {
        pushHistory(); state.update { it.copy(layers = it.layers.filter { layer -> layer.id != layerId }, images = it.images - layerId) }
        if (selectedLayerId.value == layerId) selectedLayerId.value = null
        markDirty()
    }
    fun moveLayer(layerId: String, dx: Double, dy: Double) {
        if (dx == 0.0 && dy == 0.0) return
        state.update { it.copy(layers = it.layers.map { layer -> if (layer.id == layerId) moveLayerBy(layer, dx, dy) else layer }) }; markDirty()
    }
    fun beginTransform() = pushHistory()
    fun rotateLayer(id: String, degrees: Double) {
        state.update { doc -> doc.copy(layers = doc.layers.map { layer ->
            if (layer.id != id) layer else when (layer) {
                is ArrowLayer -> layer.copy(rotation = degrees)
                is ImageLayer -> layer.copy(rotation = degrees)
                is TextLayer -> layer.copy(rotation = degrees)
                is StickerLayer -> layer.copy(rotation = degrees)
                else -> error("This layer cannot rotate")
            }
        }) }
        markDirty()
    }
    internal fun scaleLayer(original: EditorLayer, ratio: Double, bounds: EditorBounds) {
        val center = Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2)
        fun coordinate(value: Double, pivot: Double) = pivot + (value - pivot) * ratio
        val scaled = when (original) {
            is ArrowLayer -> original.copy(x1 = coordinate(original.x1, center.x), y1 = coordinate(original.y1, center.y), x2 = coordinate(original.x2, center.x), y2 = coordinate(original.y2, center.y))
            is RectLayer -> original.copy(x = center.x - max(4.0, original.w * ratio) / 2, y = center.y - max(4.0, original.h * ratio) / 2, w = max(4.0, original.w * ratio), h = max(4.0, original.h * ratio))
            is EllipseLayer -> original.copy(rx = max(2.0, original.rx * ratio), ry = max(2.0, original.ry * ratio))
            is HighlightLayer -> original.copy(x = center.x - max(4.0, original.w * ratio) / 2, y = center.y - max(4.0, original.h * ratio) / 2, w = max(4.0, original.w * ratio), h = max(4.0, original.h * ratio))
            is MosaicLayer -> original.copy(x = center.x - max(4.0, original.w * ratio) / 2, y = center.y - max(4.0, original.h * ratio) / 2, w = max(4.0, original.w * ratio), h = max(4.0, original.h * ratio))
            is ImageLayer -> original.copy(w = max(20.0, original.w * ratio), h = max(20.0, original.h * ratio))
            is StickerLayer -> original.copy(w = max(4.0, original.w * ratio), h = max(4.0, original.h * ratio))
            is TextLayer -> original.copy(fontSize = (original.fontSize * ratio).coerceIn(10.0, 500.0), maxWidth = max(50.0, original.maxWidth * ratio))
            is FreehandLayer -> original.copy(points = original.points.map { Point(coordinate(it.x, center.x), coordinate(it.y, center.y)) })
        }
        state.update { it.copy(layers = it.layers.map { layer -> if (layer.id == original.id) scaled else layer }) }
        markDirty()
    }
    internal fun resizeLayerWidth(original: EditorLayer, delta: Double) {
        val resized = when (original) {
            is TextLayer -> original.copy(maxWidth = max(50.0, original.maxWidth + delta))
            is ImageLayer -> max(20.0, original.w + delta).let { width -> original.copy(w = width, h = max(20.0, width * original.h / original.w)) }
            else -> error("This layer has no width handle")
        }
        state.update { it.copy(layers = it.layers.map { layer -> if (layer.id == original.id) resized else layer }) }
        markDirty()
    }
    fun toggleVisibility(layerId: String) {
        val layer = layers.first { it.id == layerId }; updateLayer(setLayerVisible(layer, !layer.visible))
    }
    fun duplicateLayer(layerId: String) {
        val layer = layers.first { it.id == layerId }
        val copy = offsetLayer(layer, 16.0, 16.0, generateId())
        pushHistory(); state.update { doc -> doc.copy(layers = doc.layers + copy, images = doc.images[layerId]?.let { doc.images + (copy.id to it) } ?: doc.images) }
        selectedLayerId.value = copy.id; markDirty()
    }
    fun bringForward(layerId: String) = reorder(layerId, 1)
    fun sendBackward(layerId: String) = reorder(layerId, -1)
    private fun reorder(layerId: String, delta: Int) {
        val index = layers.indexOfFirst { it.id == layerId }; val target = index + delta
        if (index < 0 || target !in layers.indices) return
        pushHistory(); val reordered = layers.toMutableList(); reordered.add(target, reordered.removeAt(index)); state.update { it.copy(layers = reordered) }; markDirty()
    }
    fun setBgColor(color: String) { pushHistory(); state.update { it.copy(bgColor = color) }; markDirty() }
    fun setCanvasSize(width: Int, height: Int) {
        require(width in 1..8000 && height in 1..8000)
        pushHistory(); state.update { it.copy(canvasSize = CanvasSize(width, height)) }; markDirty()
    }
    fun setBackgroundImage(png: String?) {
        pushHistory(); state.update { it.copy(sourceImage = png, imgOffset = Point(0.0, 0.0)) }; markDirty()
    }
    fun setBackgroundOffset(offset: Point) { pushHistory(); state.update { it.copy(imgOffset = offset) }; markDirty() }
    fun setBackgroundAlpha(alpha: Double) { require(alpha in 0.0..100.0); pushHistory(); state.update { it.copy(imgAlpha = alpha) }; markDirty() }
    fun addImage(png: String, width: Int, height: Int) {
        val size = state.value.canvasSize
        val fit = min(1.0, min(size.width.toDouble() / width, size.height.toDouble() / height))
        val layer = ImageLayer(id = generateId(), x = size.width / 2.0, y = size.height / 2.0, w = width * fit, h = height * fit)
        pushHistory(); state.update { it.copy(layers = it.layers + layer, images = it.images + (layer.id to png)) }
        selectedLayerId.value = layer.id; markDirty()
    }
    fun replaceImage(id: String, png: String) { pushHistory(); state.update { it.copy(images = it.images + (id to png)) }; markDirty() }
    fun applyCrop(png: String, width: Int, height: Int) {
        pushHistory(); state.value = EditorDocumentState(CanvasSize(width, height), "transparent", png); selectedLayerId.value = null; currentTool.value = ImageEditorTool.SELECT; markDirty()
    }
    fun saveAsync(onDone: () -> Unit = {}) {
        check(loadState.value == EditorLoadState.Ready) { "Image editor document is not loaded" }
        autoSaveJob?.cancel()
        val saved = state.value
        if (id.value.isEmpty()) id.value = generateId()
        val projectId = id.value
        viewModelScope.launchSafe {
            saveMutex.withLock {
                withContext(ioDispatcher) {
                    val bytes = YjsDocEncoder.encode(saved.canvasSize, saved.bgColor, saved.sourceImage, saved.imgOffset, saved.imgAlpha, saved.layers, saved.images)
                    projects.addOrUpdateAsync(projectId) {
                        stateB64 = Base64Lenient.encode(bytes); canvasWidth = saved.canvasSize.width; canvasHeight = saved.canvasSize.height
                    }
                }
                if (state.value == saved) dirty.value = false
            }
            onDone()
        }
    }
    override fun onCleared() { autoSaveJob?.cancel(); super.onCleared() }
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
