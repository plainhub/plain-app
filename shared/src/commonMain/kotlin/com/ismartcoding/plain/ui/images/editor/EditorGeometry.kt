package com.ismartcoding.plain.ui.images.editor

import com.ismartcoding.plain.lib.yjs.*
import kotlin.math.*

internal fun layerBounds(layer: EditorLayer): Pair<Point, Point>? {
    return when (layer) {
        is ArrowLayer -> {
            val x1 = min(layer.x1, layer.x2)
            val y1 = min(layer.y1, layer.y2)
            val x2 = max(layer.x1, layer.x2)
            val y2 = max(layer.y1, layer.y2)
            Pair(Point(x1, y1), Point(x2 - x1, y2 - y1))
        }
        is RectLayer -> Pair(Point(layer.x, layer.y), Point(layer.w, layer.h))
        is EllipseLayer -> Pair(Point(layer.cx - layer.rx, layer.cy - layer.ry), Point(layer.rx * 2, layer.ry * 2))
        is HighlightLayer -> Pair(Point(layer.x, layer.y), Point(layer.w, layer.h))
        is MosaicLayer -> Pair(Point(layer.x, layer.y), Point(layer.w, layer.h))
        is TextLayer -> {
            val mw = maxWidthForText(layer).toDouble()
            Pair(Point(layer.x - mw / 2, layer.y - layer.fontSize / 2), Point(mw, layer.fontSize))
        }
        is ImageLayer -> Pair(Point(layer.x - layer.w / 2, layer.y - layer.h / 2), Point(layer.w, layer.h))
        is FreehandLayer -> {
            if (layer.points.isEmpty()) return null
            var minX = layer.points[0].x; var minY = layer.points[0].y
            var maxX = layer.points[0].x; var maxY = layer.points[0].y
            for (p in layer.points) {
                if (p.x < minX) minX = p.x; if (p.y < minY) minY = p.y
                if (p.x > maxX) maxX = p.x; if (p.y > maxY) maxY = p.y
            }
            Pair(Point(minX, minY), Point(maxX - minX, maxY - minY))
        }
        is StickerLayer -> Pair(Point(layer.x - layer.w / 2, layer.y - layer.h / 2), Point(layer.w, layer.h))
    }
}

private fun maxWidthForText(layer: TextLayer): Float {
    return if (layer.maxWidth > 0) layer.maxWidth.toFloat() else 200f
}

internal fun hitTest(layer: EditorLayer, p: Point, threshold: Double = 12.0): Boolean {
    return when (layer) {
        is ArrowLayer -> distanceToSegment(p, Point(layer.x1, layer.y1), Point(layer.x2, layer.y2)) < threshold
        is RectLayer -> p.x >= layer.x && p.x <= layer.x + layer.w && p.y >= layer.y && p.y <= layer.y + layer.h
        is EllipseLayer -> {
            val dx = if (layer.rx > 0) (p.x - layer.cx) / layer.rx else 0.0
            val dy = if (layer.ry > 0) (p.y - layer.cy) / layer.ry else 0.0
            dx * dx + dy * dy <= 1.0
        }
        is HighlightLayer -> p.x >= layer.x && p.x <= layer.x + layer.w && p.y >= layer.y && p.y <= layer.y + layer.h
        is MosaicLayer -> p.x >= layer.x && p.x <= layer.x + layer.w && p.y >= layer.y && p.y <= layer.y + layer.h
        is TextLayer -> {
            val mw = maxWidthForText(layer).toDouble()
            p.x >= layer.x - mw / 2 && p.x <= layer.x + mw / 2 &&
                p.y >= layer.y - layer.fontSize / 2 && p.y <= layer.y + layer.fontSize / 2
        }
        is ImageLayer -> p.x >= layer.x - layer.w / 2 && p.x <= layer.x + layer.w / 2 &&
            p.y >= layer.y - layer.h / 2 && p.y <= layer.y + layer.h / 2
        is FreehandLayer -> {
            if (layer.points.size < 2) return false
            for (i in 0 until layer.points.size - 1) {
                if (distanceToSegment(p, layer.points[i], layer.points[i + 1]) < threshold) return true
            }
            false
        }
        is StickerLayer -> p.x >= layer.x - layer.w / 2 && p.x <= layer.x + layer.w / 2 &&
            p.y >= layer.y - layer.h / 2 && p.y <= layer.y + layer.h / 2
    }
}

internal fun hitTestHandle(layer: EditorLayer, p: Point, threshold: Double): ResizeHandle? {
    val bounds = layerBounds(layer) ?: return null
    val (origin, size) = bounds
    val corners = mapOf(
        ResizeHandle.TOP_LEFT to Point(origin.x, origin.y),
        ResizeHandle.TOP_RIGHT to Point(origin.x + size.x, origin.y),
        ResizeHandle.BOTTOM_LEFT to Point(origin.x, origin.y + size.y),
        ResizeHandle.BOTTOM_RIGHT to Point(origin.x + size.x, origin.y + size.y),
    )
    val tSq = threshold * threshold
    for ((handle, corner) in corners) {
        val dx = p.x - corner.x
        val dy = p.y - corner.y
        if (dx * dx + dy * dy < tSq) return handle
    }
    return null
}

private fun distanceToSegment(p: Point, a: Point, b: Point): Double {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val lenSq = dx * dx + dy * dy
    if (lenSq == 0.0) return sqrt((p.x - a.x) * (p.x - a.x) + (p.y - a.y) * (p.y - a.y))
    val t = ((p.x - a.x) * dx + (p.y - a.y) * dy) / lenSq
    val tc = t.coerceIn(0.0, 1.0)
    val projX = a.x + tc * dx
    val projY = a.y + tc * dy
    return sqrt((p.x - projX) * (p.x - projX) + (p.y - projY) * (p.y - projY))
}
