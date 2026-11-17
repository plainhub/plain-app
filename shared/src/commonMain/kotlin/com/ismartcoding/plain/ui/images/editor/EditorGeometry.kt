package com.ismartcoding.plain.ui.images.editor

import com.ismartcoding.plain.lib.yjs.*
import kotlin.math.*

internal data class EditorBounds(val x: Double, val y: Double, val width: Double, val height: Double) {
    fun contains(point: Point) = point.x in x..x + width && point.y in y..y + height
    fun padded(padding: Double) = EditorBounds(x - padding, y - padding, width + padding * 2, height + padding * 2)
}

internal enum class EditorWidthHandle(val sign: Double) { LEFT(-1.0), RIGHT(1.0) }

internal data class EditorRotation(val center: Point, val degrees: Double)

internal fun layerRotation(layer: EditorLayer): EditorRotation? = when (layer) {
    is ArrowLayer -> EditorRotation(Point((layer.x1 + layer.x2) / 2, (layer.y1 + layer.y2) / 2), layer.rotation)
    is TextLayer -> EditorRotation(Point(layer.x, layer.y), layer.rotation)
    is ImageLayer -> EditorRotation(Point(layer.x, layer.y), layer.rotation)
    is StickerLayer -> EditorRotation(Point(layer.x, layer.y), layer.rotation)
    else -> null
}

internal fun rotateEditorPoint(point: Point, rotation: EditorRotation): Point {
    val radians = rotation.degrees * PI / 180
    val x = point.x - rotation.center.x; val y = point.y - rotation.center.y
    return Point(rotation.center.x + x * cos(radians) - y * sin(radians), rotation.center.y + x * sin(radians) + y * cos(radians))
}

internal class EditorGeometry(private val textHeight: (TextLayer) -> Double) {
    fun bounds(layer: EditorLayer): EditorBounds? = when (layer) {
        is ArrowLayer -> EditorBounds(min(layer.x1, layer.x2), min(layer.y1, layer.y2), abs(layer.x2 - layer.x1), abs(layer.y2 - layer.y1))
        is RectLayer -> EditorBounds(layer.x, layer.y, layer.w, layer.h)
        is EllipseLayer -> EditorBounds(layer.cx - layer.rx, layer.cy - layer.ry, layer.rx * 2, layer.ry * 2)
        is HighlightLayer -> EditorBounds(layer.x, layer.y, layer.w, layer.h)
        is MosaicLayer -> EditorBounds(layer.x, layer.y, layer.w, layer.h)
        is TextLayer -> textHeight(layer).let { height -> EditorBounds(layer.x - layer.maxWidth / 2, layer.y - height / 2, layer.maxWidth, height) }
        is ImageLayer -> EditorBounds(layer.x - layer.w / 2, layer.y - layer.h / 2, layer.w, layer.h)
        is StickerLayer -> EditorBounds(layer.x - layer.w / 2, layer.y - layer.h / 2, layer.w, layer.h)
        is FreehandLayer -> layer.points.takeIf { it.isNotEmpty() }?.let { points ->
            val left = points.minOf { it.x }; val top = points.minOf { it.y }
            EditorBounds(left, top, points.maxOf { it.x } - left, points.maxOf { it.y } - top)
        }
    }

    private fun localPoint(layer: EditorLayer, point: Point): Point = layerRotation(layer)?.let { rotateEditorPoint(point, it.copy(degrees = -it.degrees)) } ?: point

    fun hit(layer: EditorLayer, point: Point, threshold: Double): Boolean {
        if (!layer.visible) return false
        val local = localPoint(layer, point)
        return when (layer) {
            is ArrowLayer -> distanceToSegment(local, Point(layer.x1, layer.y1), Point(layer.x2, layer.y2)) <= threshold
            is EllipseLayer -> {
                val x = (local.x - layer.cx) / layer.rx; val y = (local.y - layer.cy) / layer.ry
                x * x + y * y <= 1
            }
            is FreehandLayer -> layer.points.zipWithNext().any { (a, b) -> distanceToSegment(local, a, b) <= threshold }
            else -> bounds(layer)?.contains(local) == true
        }
    }

    fun handle(layer: EditorLayer, point: Point, threshold: Double, padding: Double): ResizeHandle? {
        if (!layer.visible) return null
        val rect = bounds(layer)?.padded(padding) ?: return null
        val local = localPoint(layer, point)
        return ResizeHandle.entries.firstOrNull { handle -> corner(rect, handle).let { hypot(local.x - it.x, local.y - it.y) <= threshold } }
    }

    fun corner(rect: EditorBounds, handle: ResizeHandle): Point = when (handle) {
        ResizeHandle.TOP_LEFT -> Point(rect.x, rect.y)
        ResizeHandle.TOP_RIGHT -> Point(rect.x + rect.width, rect.y)
        ResizeHandle.BOTTOM_LEFT -> Point(rect.x, rect.y + rect.height)
        ResizeHandle.BOTTOM_RIGHT -> Point(rect.x + rect.width, rect.y + rect.height)
    }

    fun widthHandle(layer: EditorLayer, point: Point, threshold: Double, padding: Double): EditorWidthHandle? {
        if (!layer.visible || layer !is TextLayer && layer !is ImageLayer) return null
        val rect = checkNotNull(bounds(layer)).padded(padding)
        val local = localPoint(layer, point)
        return EditorWidthHandle.entries.firstOrNull { handle ->
            val x = if (handle == EditorWidthHandle.LEFT) rect.x else rect.x + rect.width
            hypot(local.x - x, local.y - rect.y - rect.height / 2) <= threshold
        }
    }

    fun rotationHandle(layer: EditorLayer, padding: Double, stem: Double): Point? {
        val rotation = layerRotation(layer) ?: return null
        val rect = bounds(layer)?.padded(padding) ?: return null
        return rotateEditorPoint(Point(rect.x + rect.width / 2, rect.y - stem), rotation)
    }
}

private fun distanceToSegment(point: Point, a: Point, b: Point): Double {
    val x = b.x - a.x; val y = b.y - a.y
    val length = x * x + y * y
    if (length == 0.0) return hypot(point.x - a.x, point.y - a.y)
    val fraction = (((point.x - a.x) * x + (point.y - a.y) * y) / length).coerceIn(0.0, 1.0)
    return hypot(point.x - a.x - fraction * x, point.y - a.y - fraction * y)
}
