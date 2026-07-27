package com.ismartcoding.plain.yjs

import kotlinx.serialization.Serializable

@Serializable
data class Point(val x: Double, val y: Double)

@Serializable
data class TextStroke(val width: Double = 0.0, val color: String = "#000000")

@Serializable
data class TextShadow(
    val offsetX: Double = 0.0,
    val offsetY: Double = 0.0,
    val blur: Double = 0.0,
    val color: String = "rgba(0,0,0,0.7)",
)

@Serializable
data class CanvasSize(val width: Int = 1920, val height: Int = 1080)

@Serializable
sealed class EditorLayer {
    abstract val id: String
    abstract val type: String
    abstract val visible: Boolean
    abstract val name: String
}

@Serializable
data class ArrowLayer(
    override val id: String,
    override val visible: Boolean = true,
    override val name: String = "Arrow",
    val x1: Double = 0.0,
    val y1: Double = 0.0,
    val x2: Double = 0.0,
    val y2: Double = 0.0,
    val color: String = "#ff0000",
    val lineWidth: Double = 4.0,
    val rotation: Double = 0.0,
) : EditorLayer() {
    override val type: String = "arrow"
}

@Serializable
data class RectLayer(
    override val id: String,
    override val visible: Boolean = true,
    override val name: String = "Rect",
    val x: Double = 0.0,
    val y: Double = 0.0,
    val w: Double = 0.0,
    val h: Double = 0.0,
    val color: String = "#ff0000",
    val lineWidth: Double = 4.0,
) : EditorLayer() {
    override val type: String = "rect"
}

@Serializable
data class EllipseLayer(
    override val id: String,
    override val visible: Boolean = true,
    override val name: String = "Ellipse",
    val cx: Double = 0.0,
    val cy: Double = 0.0,
    val rx: Double = 0.0,
    val ry: Double = 0.0,
    val color: String = "#ff0000",
    val lineWidth: Double = 4.0,
) : EditorLayer() {
    override val type: String = "ellipse"
}

@Serializable
data class HighlightLayer(
    override val id: String,
    override val visible: Boolean = true,
    override val name: String = "Highlight",
    val x: Double = 0.0,
    val y: Double = 0.0,
    val w: Double = 0.0,
    val h: Double = 0.0,
    val color: String = "rgba(255, 255, 0, 0.4)",
) : EditorLayer() {
    override val type: String = "highlight"
}

@Serializable
data class MosaicLayer(
    override val id: String,
    override val visible: Boolean = true,
    override val name: String = "Mosaic",
    val x: Double = 0.0,
    val y: Double = 0.0,
    val w: Double = 0.0,
    val h: Double = 0.0,
    val blockSize: Double = 10.0,
) : EditorLayer() {
    override val type: String = "mosaic"
}

@Serializable
data class TextLayer(
    override val id: String,
    override val visible: Boolean = true,
    override val name: String = "Text",
    val x: Double = 0.0,
    val y: Double = 0.0,
    val text: String = "",
    val fontSize: Double = 48.0,
    val color: String = "#000000",
    val fontFamily: String = "sans-serif",
    val fontWeight: String = "normal",
    val fontStyle: String = "normal",
    val textAlign: String = "left",
    val letterSpacing: Double = 0.0,
    val textDecoration: String = "none",
    val textStroke: TextStroke = TextStroke(),
    val textShadow: TextShadow = TextShadow(),
    val glow: Boolean = false,
    val rotation: Double = 0.0,
    val maxWidth: Double = 0.0,
) : EditorLayer() {
    override val type: String = "text"
}

@Serializable
data class ImageLayer(
    override val id: String,
    override val visible: Boolean = true,
    override val name: String = "Image",
    val x: Double = 0.0,
    val y: Double = 0.0,
    val w: Double = 0.0,
    val h: Double = 0.0,
    val opacity: Double = 1.0,
    val rotation: Double = 0.0,
) : EditorLayer() {
    override val type: String = "image"
}

@Serializable
data class FreehandLayer(
    override val id: String,
    override val visible: Boolean = true,
    override val name: String = "Brush",
    val points: List<Point> = emptyList(),
    val color: String = "#ff0000",
    val lineWidth: Double = 4.0,
) : EditorLayer() {
    override val type: String = "freehand"
}

@Serializable
data class StickerLayer(
    override val id: String,
    override val visible: Boolean = true,
    override val name: String = "Sticker",
    val x: Double = 0.0,
    val y: Double = 0.0,
    val w: Double = 0.0,
    val h: Double = 0.0,
    val color: String = "#ffeb3b",
    val text: String = "",
    val fontSize: Double = 48.0,
    val fontWeight: String = "normal",
    val fontStyle: String = "normal",
    val rotation: Double = 0.0,
) : EditorLayer() {
    override val type: String = "sticker"
}
