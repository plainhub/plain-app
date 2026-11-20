package com.ismartcoding.plain.ui.images.editor.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.lib.yjs.ArrowLayer
import com.ismartcoding.plain.lib.yjs.EditorLayer
import com.ismartcoding.plain.lib.yjs.EllipseLayer
import com.ismartcoding.plain.lib.yjs.FreehandLayer
import com.ismartcoding.plain.lib.yjs.HighlightLayer
import com.ismartcoding.plain.lib.yjs.ImageLayer
import com.ismartcoding.plain.lib.yjs.MosaicLayer
import com.ismartcoding.plain.lib.yjs.RectLayer
import com.ismartcoding.plain.lib.yjs.EditorFontStyle
import com.ismartcoding.plain.lib.yjs.EditorFontWeight
import com.ismartcoding.plain.lib.yjs.StickerLayer
import com.ismartcoding.plain.lib.yjs.TextLayer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Constraints

internal fun DrawScope.drawSceneBackground(bgColor: String, cw: Float, ch: Float) {
    val isTransparent = bgColor.isBlank() || bgColor.equals("transparent", ignoreCase = true)
    if (isTransparent) {
        drawSceneCheckerboard(cw, ch)
    } else {
        drawRect(parseScopeBgColor(bgColor), topLeft = Offset.Zero, size = Size(cw, ch))
    }
}

internal fun DrawScope.drawSceneCheckerboard(cw: Float, ch: Float, tile: Float = 32f) {
    val light = Color(0xFFF8F8F8)
    val dark = Color(0xFFE5E5E5)
    var y = 0f
    var row = 0
    while (y < ch) {
        var x = 0f
        var col = 0
        while (x < cw) {
            drawRect(
                color = if ((row + col) % 2 == 0) light else dark,
                topLeft = Offset(x, y),
                size = Size(minOf(tile, cw - x), minOf(tile, ch - y)),
            )
            x += tile
            col++
        }
        y += tile
        row++
    }
}

internal fun DrawScope.drawSceneLayer(
    layer: EditorLayer,
    textMeasurer: TextMeasurer,
) {
    val angle = when (layer) {
        is ArrowLayer -> layer.rotation.toFloat()
        is TextLayer -> layer.rotation.toFloat()
        is StickerLayer -> layer.rotation.toFloat()
        else -> 0f
    }
    val center = when (layer) {
        is ArrowLayer -> Offset(((layer.x1 + layer.x2) / 2).toFloat(), ((layer.y1 + layer.y2) / 2).toFloat())
        is TextLayer -> Offset(layer.x.toFloat(), layer.y.toFloat())
        is StickerLayer -> Offset(layer.x.toFloat(), layer.y.toFloat())
        else -> Offset.Zero
    }
    rotate(angle, center) {
    when (layer) {
        is ArrowLayer -> drawSceneArrow(layer)
        is RectLayer -> drawRect(
            color = parseScopeBgColor(layer.color),
            topLeft = Offset(layer.x.toFloat(), layer.y.toFloat()),
            size = Size(layer.w.toFloat(), layer.h.toFloat()),
            style = Stroke(width = layer.lineWidth.toFloat()),
        )
        is EllipseLayer -> drawOval(
            color = parseScopeBgColor(layer.color),
            topLeft = Offset((layer.cx - layer.rx).toFloat(), (layer.cy - layer.ry).toFloat()),
            size = Size((layer.rx * 2).toFloat(), (layer.ry * 2).toFloat()),
            style = Stroke(width = layer.lineWidth.toFloat()),
        )
        is HighlightLayer -> drawRect(
            color = parseScopeBgColor(layer.color).let { it.copy(alpha = it.alpha * .35f) },
            topLeft = Offset(layer.x.toFloat(), layer.y.toFloat()),
            size = Size(layer.w.toFloat(), layer.h.toFloat()),
        )
        is MosaicLayer, is ImageLayer -> Unit
        is TextLayer -> drawSceneTextLayer(layer, textMeasurer)
        is FreehandLayer -> drawSceneFreehand(layer)
        is StickerLayer -> drawSceneStickerLayer(layer, textMeasurer)
    }
    }
}

private fun DrawScope.drawSceneStickerLayer(layer: StickerLayer, textMeasurer: TextMeasurer) {
    val left = (layer.x - layer.w / 2).toFloat()
    val top = (layer.y - layer.h / 2).toFloat()
    drawRoundRect(parseScopeBgColor(layer.color), Offset(left, top), Size(layer.w.toFloat(), layer.h.toFloat()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f))
    if (layer.text.isEmpty()) return
    val textColor = when (layer.color) {
        "#fef08a" -> "#713f12"
        "#bbf7d0" -> "#14532d"
        "#bfdbfe" -> "#1e3a5f"
        "#fbcfe8" -> "#831843"
        "#fde68a" -> "#78350f"
        else -> "#374151"
    }
    val style = TextStyle(fontSize = layer.fontSize.toFloat().sp, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif, fontWeight = sceneFontWeight(layer.fontWeight), fontStyle = if (layer.fontStyle == EditorFontStyle.ITALIC) FontStyle.Italic else FontStyle.Normal)
    fun measure(text: String) = textMeasurer.measure(text, style, density = androidx.compose.ui.unit.Density(1f))
    val lines = wrapSceneText(layer.text, layer.w - 28) { measure(it).size.width.toDouble() }
    val lineHeight = layer.fontSize.toFloat() * 1.3f
    for ((index, line) in lines.withIndex()) {
        val y = top + (layer.h.toFloat() - lines.size * lineHeight) / 2 + index * lineHeight
        if (y + lineHeight > top + layer.h + 2) break
        drawText(measure(line), parseScopeBgColor(textColor), Offset(left + 14, y))
    }
}

private fun DrawScope.drawSceneArrow(layer: ArrowLayer) {
    val color = parseScopeBgColor(layer.color)
    val start = Offset(layer.x1.toFloat(), layer.y1.toFloat())
    val end = Offset(layer.x2.toFloat(), layer.y2.toFloat())
    val lw = layer.lineWidth.toFloat()
    drawLine(color = color, start = start, end = end, strokeWidth = lw, cap = StrokeCap.Round)
    val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
    val headLen = max(lw * 4f, 12f).toDouble()
    val headW = headLen * 0.5
    val p1 = Offset(
        (end.x - headLen * cos(angle) + headW * sin(angle)).toFloat(),
        (end.y - headLen * sin(angle) - headW * cos(angle)).toFloat(),
    )
    val p2 = Offset(
        (end.x - headLen * cos(angle) - headW * sin(angle)).toFloat(),
        (end.y - headLen * sin(angle) + headW * cos(angle)).toFloat(),
    )
    val path = Path().apply {
        moveTo(end.x, end.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        close()
    }
    drawPath(path = path, color = color)
}

private fun DrawScope.drawSceneFreehand(layer: FreehandLayer) {
    if (layer.points.size < 2) return
    val path = Path().apply {
        moveTo(layer.points[0].x.toFloat(), layer.points[0].y.toFloat())
        for (index in 1 until layer.points.lastIndex) {
            val current = layer.points[index]
            val next = layer.points[index + 1]
            quadraticBezierTo(current.x.toFloat(), current.y.toFloat(), ((current.x + next.x) / 2).toFloat(), ((current.y + next.y) / 2).toFloat())
        }
        lineTo(layer.points.last().x.toFloat(), layer.points.last().y.toFloat())
    }
    drawPath(
        path = path,
        color = parseScopeBgColor(layer.color),
        style = Stroke(width = layer.lineWidth.toFloat(), cap = StrokeCap.Round),
    )
}

internal fun parseScopeBgColor(value: String): Color {
    if (value.isBlank() || value.equals("transparent", ignoreCase = true)) return Color.Transparent
    val trimmed = value.trim()
    return runCatching {
        when {
            trimmed.startsWith("#") -> parseScopeHexColor(trimmed)
            trimmed.startsWith("rgba", ignoreCase = true) -> parseScopeRgba(trimmed)
            trimmed.startsWith("rgb", ignoreCase = true) -> parseScopeRgb(trimmed)
            else -> Color.White
        }
    }.getOrDefault(Color.Transparent)
}

private fun parseScopeHexColor(hex: String): Color {
    val n = hex.removePrefix("#")
    return when (n.length) {
        3 -> {
            val r = n[0].digitToInt(16) * 17
            val g = n[1].digitToInt(16) * 17
            val b = n[2].digitToInt(16) * 17
            Color(r, g, b)
        }
        6 -> {
            val r = n.substring(0, 2).toInt(16)
            val g = n.substring(2, 4).toInt(16)
            val b = n.substring(4, 6).toInt(16)
            Color(r, g, b)
        }
        8 -> {
            val r = n.substring(0, 2).toInt(16)
            val g = n.substring(2, 4).toInt(16)
            val b = n.substring(4, 6).toInt(16)
            val a = n.substring(6, 8).toInt(16)
            Color(r, g, b, a)
        }
        else -> Color.White
    }
}

private fun parseScopeRgba(value: String): Color {
    val nums = value.substringAfter("(").substringBefore(")")
        .split(",").map { it.trim() }
    if (nums.size != 4) return Color.White
    val r = nums[0].toInt()
    val g = nums[1].toInt()
    val b = nums[2].toInt()
    val a = (nums[3].toFloatOrNull() ?: 1f).coerceIn(0f, 1f)
    return Color(r, g, b, (a * 255).toInt())
}

private fun parseScopeRgb(value: String): Color {
    val nums = value.substringAfter("(").substringBefore(")")
        .split(",").map { it.trim() }
    if (nums.size != 3) return Color.White
    return Color(nums[0].toInt(), nums[1].toInt(), nums[2].toInt())
}

internal fun sceneFontWeight(value: EditorFontWeight): FontWeight = when (value) {
    EditorFontWeight.NORMAL -> FontWeight.Normal
    EditorFontWeight.BOLD -> FontWeight.Bold
    EditorFontWeight.SEMIBOLD -> FontWeight.SemiBold
}

private fun parseScopeTextAlign(value: String): TextAlign {
    return when (value) {
        "center" -> TextAlign.Center
        "right", "end" -> TextAlign.End
        "justify" -> TextAlign.Justify
        else -> TextAlign.Start
    }
}

internal val sceneDashPathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
