package com.ismartcoding.plain.ui.images.editor.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.lib.yjs.EditorFontStyle
import com.ismartcoding.plain.lib.yjs.EditorTextAlign
import com.ismartcoding.plain.lib.yjs.EditorTextDecoration
import com.ismartcoding.plain.lib.yjs.TextLayer

internal data class SceneTextLayout(val lines: List<TextLayoutResult>, val lineHeight: Float)

internal fun measureSceneText(layer: TextLayer, measurer: TextMeasurer): SceneTextLayout {
    val style = TextStyle(
        fontSize = layer.fontSize.toFloat().sp,
        fontFamily = sceneFontFamily(layer.fontFamily),
        fontStyle = if (layer.fontStyle == EditorFontStyle.ITALIC) FontStyle.Italic else FontStyle.Normal,
        fontWeight = sceneFontWeight(layer.fontWeight),
        letterSpacing = layer.letterSpacing.toFloat().sp,
    )
    fun measure(text: String) = measurer.measure(text, style, density = Density(1f))
    val lines = wrapSceneText(layer.text, layer.maxWidth) { measure(it).size.width.toDouble() }.map(::measure)
    return SceneTextLayout(lines, layer.fontSize.toFloat() * 1.15f)
}

internal fun DrawScope.drawSceneTextLayer(layer: TextLayer, measurer: TextMeasurer) {
    if (layer.text.isEmpty()) return
    val (lines, lineHeight) = measureSceneText(layer, measurer)
    val shadow = when {
        layer.glow -> Shadow(parseScopeBgColor(layer.color), blurRadius = if (layer.textShadow.blur > 0) layer.textShadow.blur.toFloat() else 20f)
        layer.textShadow.blur > 0 || layer.textShadow.offsetX != 0.0 || layer.textShadow.offsetY != 0.0 -> Shadow(parseScopeBgColor(layer.textShadow.color), Offset(layer.textShadow.offsetX.toFloat(), layer.textShadow.offsetY.toFloat()), layer.textShadow.blur.toFloat())
        else -> Shadow.None
    }
    for ((index, measured) in lines.withIndex()) {
        val width = measured.size.width.toFloat()
        val left = when (layer.textAlign) {
            EditorTextAlign.LEFT -> (layer.x - layer.maxWidth / 2).toFloat()
            EditorTextAlign.RIGHT -> (layer.x + layer.maxWidth / 2).toFloat() - width
            EditorTextAlign.CENTER -> layer.x.toFloat() - width / 2
        }
        val centerY = layer.y.toFloat() - lines.size * lineHeight / 2 + (index + .5f) * lineHeight
        val position = Offset(left, centerY - measured.size.height / 2f)
        if (layer.textStroke.width > 0) drawText(measured, parseScopeBgColor(layer.textStroke.color), position, shadow = shadow, drawStyle = Stroke(layer.textStroke.width.toFloat(), join = StrokeJoin.Round))
        drawText(measured, parseScopeBgColor(layer.color), position, shadow = shadow, drawStyle = Fill)
        if (layer.textDecoration == EditorTextDecoration.UNDERLINE) drawLine(parseScopeBgColor(layer.color), Offset(left, centerY + layer.fontSize.toFloat() * .5f), Offset(left + width, centerY + layer.fontSize.toFloat() * .5f), strokeWidth = (layer.fontSize / 16).toFloat().coerceAtLeast(1f))
    }
}

internal fun sceneFontFamily(css: String): FontFamily = when (css.split(',').map { it.trim().trim('\'', '"') }.last()) {
    "serif" -> FontFamily.Serif
    "monospace" -> FontFamily.Monospace
    "cursive" -> FontFamily.Cursive
    else -> FontFamily.SansSerif
}

internal fun wrapSceneText(text: String, maxWidth: Double, width: (String) -> Double): List<String> {
    val lines = mutableListOf<String>()
    for (paragraph in text.split('\n')) {
        if (paragraph.isEmpty()) { lines.add(""); continue }
        var current = ""
        for (word in paragraph.split(' ')) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (width(candidate) > maxWidth && current.isNotEmpty()) { lines.add(current); current = word } else current = candidate
            while (current.length > 1 && width(current) > maxWidth) {
                var split = current.length - 1
                while (split > 1 && width(current.take(split)) > maxWidth) split--
                lines.add(current.take(split)); current = current.drop(split)
            }
        }
        if (current.isNotEmpty()) lines.add(current)
    }
    return lines
}
