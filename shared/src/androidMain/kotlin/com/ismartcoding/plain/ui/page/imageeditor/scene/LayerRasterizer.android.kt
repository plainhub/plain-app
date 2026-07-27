package com.ismartcoding.plain.ui.page.imageeditor.scene

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Picture
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.ismartcoding.plain.yjs.ArrowLayer
import com.ismartcoding.plain.yjs.EditorLayer
import com.ismartcoding.plain.yjs.EllipseLayer
import com.ismartcoding.plain.yjs.FreehandLayer
import com.ismartcoding.plain.yjs.HighlightLayer
import com.ismartcoding.plain.yjs.ImageLayer
import com.ismartcoding.plain.yjs.MosaicLayer
import com.ismartcoding.plain.yjs.Point
import com.ismartcoding.plain.yjs.RectLayer
import com.ismartcoding.plain.yjs.StickerLayer
import com.ismartcoding.plain.yjs.TextLayer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

internal data class LayerCacheEntry(
    val hash: String,
    val picture: Picture,
    val width: Int,
    val height: Int,
    val offsetX: Float,
    val offsetY: Float,
    val bitmap: Bitmap? = null,
)

internal class LayerRasterizer {

    private val cache = LinkedHashMap<String, LayerCacheEntry>()

    fun getOrRasterize(
        layer: EditorLayer,
        images: Map<String, String>,
        sourceImage: Bitmap? = null,
    ): LayerCacheEntry? {
        val hash = computeHash(layer, images)
        val existing = cache[layer.id]
        if (existing != null && existing.hash == hash) return existing
        val entry = rasterize(layer, hash, images, sourceImage) ?: return null
        if (existing != null) {
            existing.bitmap?.recycle()
        }
        cache[layer.id] = entry
        return entry
    }

    fun invalidate(layerId: String) {
        cache.remove(layerId)?.bitmap?.recycle()
    }

    fun clear() {
        cache.values.forEach { it.bitmap?.recycle() }
        cache.clear()
    }

    private fun computeHash(layer: EditorLayer, images: Map<String, String>): String {
        return when (layer) {
            is ArrowLayer -> "a:${layer.x1},${layer.y1},${layer.x2},${layer.y2},${layer.color},${layer.lineWidth},${layer.visible}"
            is RectLayer -> "r:${layer.x},${layer.y},${layer.w},${layer.h},${layer.color},${layer.lineWidth},${layer.visible}"
            is EllipseLayer -> "e:${layer.cx},${layer.cy},${layer.rx},${layer.ry},${layer.color},${layer.lineWidth},${layer.visible}"
            is HighlightLayer -> "h:${layer.x},${layer.y},${layer.w},${layer.h},${layer.color},${layer.visible}"
            is MosaicLayer -> "m:${layer.x},${layer.y},${layer.w},${layer.h},${layer.blockSize},${layer.visible}"
            is TextLayer -> "t:${layer.text},${layer.fontSize},${layer.color},${layer.fontFamily},${layer.fontWeight},${layer.fontStyle},${layer.textAlign},${layer.letterSpacing},${layer.maxWidth},${layer.visible}"
            is ImageLayer -> "i:${layer.x},${layer.y},${layer.w},${layer.h},${layer.opacity},${layer.visible}"
            is FreehandLayer -> "f:${layer.points.size},${layer.color},${layer.lineWidth},${layer.visible},${layer.points.firstOrNull()?.x},${layer.points.firstOrNull()?.y},${layer.points.lastOrNull()?.x},${layer.points.lastOrNull()?.y}"
            is StickerLayer -> "s:${layer.x},${layer.y},${layer.w},${layer.h},${layer.color},${layer.text},${layer.fontSize},${layer.fontWeight},${layer.visible}"
        }
    }

    private fun rasterize(
        layer: EditorLayer,
        hash: String,
        images: Map<String, String>,
        sourceImage: Bitmap?,
    ): LayerCacheEntry? {
        if (!layer.visible) return null
        return when (layer) {
            is ArrowLayer -> rasterizeArrow(layer, hash)
            is RectLayer -> rasterizeRect(layer, hash)
            is EllipseLayer -> rasterizeEllipse(layer, hash)
            is HighlightLayer -> rasterizeHighlight(layer, hash)
            is MosaicLayer -> rasterizeMosaicPlaceholder(layer, hash)
            is TextLayer -> rasterizeText(layer, hash)
            is ImageLayer -> rasterizeImage(layer, hash, sourceImage)
            is FreehandLayer -> rasterizeFreehand(layer, hash)
            is StickerLayer -> rasterizeSticker(layer, hash)
        }
    }

    private fun rasterizeArrow(layer: ArrowLayer, hash: String): LayerCacheEntry {
        val minX = minOf(layer.x1, layer.x2).toFloat()
        val minY = minOf(layer.y1, layer.y2).toFloat()
        val maxX = maxOf(layer.x1, layer.x2).toFloat()
        val maxY = maxOf(layer.y1, layer.y2).toFloat()
        val lw = layer.lineWidth.toFloat()
        val headLen = max(lw * 4f, 12f)
        val pad = (lw + headLen + 4f)
        val w = (maxX - minX + pad * 2).toInt().coerceAtLeast(1)
        val h = (maxY - minY + pad * 2).toInt().coerceAtLeast(1)
        val offX = minX - pad
        val offY = minY - pad

        val picture = Picture()
        val canvas = picture.beginRecording(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseColor(layer.color)
            strokeWidth = lw
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val sx = (layer.x1 - offX).toFloat()
        val sy = (layer.y1 - offY).toFloat()
        val ex = (layer.x2 - offX).toFloat()
        val ey = (layer.y2 - offY).toFloat()
        canvas.drawLine(sx, sy, ex, ey, paint)
        val angle = atan2((ey - sy).toDouble(), (ex - sx).toDouble())
        val headW = headLen * 0.5
        val p1x = (ex - headLen * cos(angle) + headW * sin(angle)).toFloat()
        val p1y = (ey - headLen * sin(angle) - headW * cos(angle)).toFloat()
        val p2x = (ex - headLen * cos(angle) - headW * sin(angle)).toFloat()
        val p2y = (ey - headLen * sin(angle) + headW * cos(angle)).toFloat()
        val path = Path().apply {
            moveTo(ex, ey)
            lineTo(p1x, p1y)
            lineTo(p2x, p2y)
            close()
        }
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)
        picture.endRecording()
        return LayerCacheEntry(hash, picture, w, h, offX, offY)
    }

    private fun rasterizeRect(layer: RectLayer, hash: String): LayerCacheEntry {
        val lw = layer.lineWidth.toFloat()
        val pad = lw + 2f
        val w = (layer.w + pad * 2).toInt().coerceAtLeast(1)
        val h = (layer.h + pad * 2).toInt().coerceAtLeast(1)
        val offX = (layer.x - pad).toFloat()
        val offY = (layer.y - pad).toFloat()
        val picture = Picture()
        val canvas = picture.beginRecording(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseColor(layer.color)
            strokeWidth = lw
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawRect(pad, pad, pad + layer.w.toFloat(), pad + layer.h.toFloat(), paint)
        picture.endRecording()
        return LayerCacheEntry(hash, picture, w, h, offX, offY)
    }

    private fun rasterizeEllipse(layer: EllipseLayer, hash: String): LayerCacheEntry {
        val lw = layer.lineWidth.toFloat()
        val pad = lw + 2f
        val left = (layer.cx - layer.rx - pad).toFloat()
        val top = (layer.cy - layer.ry - pad).toFloat()
        val w = (layer.rx * 2 + pad * 2).toInt().coerceAtLeast(1)
        val h = (layer.ry * 2 + pad * 2).toInt().coerceAtLeast(1)
        val picture = Picture()
        val canvas = picture.beginRecording(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseColor(layer.color)
            strokeWidth = lw
            style = Paint.Style.STROKE
        }
        val rect = RectF(pad, pad, pad + (layer.rx * 2).toFloat(), pad + (layer.ry * 2).toFloat())
        canvas.drawOval(rect, paint)
        picture.endRecording()
        return LayerCacheEntry(hash, picture, w, h, left, top)
    }

    private fun rasterizeHighlight(layer: HighlightLayer, hash: String): LayerCacheEntry {
        val pad = 2f
        val w = (layer.w + pad * 2).toInt().coerceAtLeast(1)
        val h = (layer.h + pad * 2).toInt().coerceAtLeast(1)
        val offX = (layer.x - pad).toFloat()
        val offY = (layer.y - pad).toFloat()
        val picture = Picture()
        val canvas = picture.beginRecording(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseColor(layer.color)
            style = Paint.Style.FILL
        }
        canvas.drawRect(pad, pad, pad + layer.w.toFloat(), pad + layer.h.toFloat(), paint)
        picture.endRecording()
        return LayerCacheEntry(hash, picture, w, h, offX, offY)
    }

    private fun rasterizeMosaicPlaceholder(layer: MosaicLayer, hash: String): LayerCacheEntry {
        val pad = 2f
        val w = (layer.w + pad * 2).toInt().coerceAtLeast(1)
        val h = (layer.h + pad * 2).toInt().coerceAtLeast(1)
        val offX = (layer.x - pad).toFloat()
        val offY = (layer.y - pad).toFloat()
        val picture = Picture()
        val canvas = picture.beginRecording(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(80, 128, 128, 128)
            style = Paint.Style.FILL
        }
        canvas.drawRect(pad, pad, pad + layer.w.toFloat(), pad + layer.h.toFloat(), paint)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 80, 80, 80)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        canvas.drawRect(pad, pad, pad + layer.w.toFloat(), pad + layer.h.toFloat(), stroke)
        picture.endRecording()
        return LayerCacheEntry(hash, picture, w, h, offX, offY)
    }

    private fun rasterizeText(layer: TextLayer, hash: String): LayerCacheEntry {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseColor(layer.color)
            textSize = layer.fontSize.toFloat()
            typeface = typefaceOf(layer.fontFamily, layer.fontWeight, layer.fontStyle)
            letterSpacing = layer.letterSpacing.toFloat() / layer.fontSize.toFloat()
        }
        val maxWidthPx = if (layer.maxWidth > 0) layer.maxWidth.toInt() else Int.MAX_VALUE
        val emptyW = 80
        val emptyH = layer.fontSize.toInt().coerceAtLeast(1) + 8
        if (layer.text.isEmpty()) {
            val pad = 4f
            val w = (emptyW + pad * 2).toInt()
            val h = (emptyH + pad * 2).toInt()
            val picture = Picture()
            val canvas = picture.beginRecording(w, h)
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = (paint.color and 0x00FFFFFF) or 0x33000000.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRect(pad, pad, pad + emptyW, pad + emptyH, stroke)
            picture.endRecording()
            return LayerCacheEntry(hash, picture, w, h, (layer.x - 40 - pad).toFloat(), (layer.y - emptyH / 2f - pad).toFloat())
        }
        val layout = StaticLayout.Builder
            .obtain(layer.text, 0, layer.text.length, paint, maxWidthPx.coerceAtMost(4096))
            .setAlignment(alignOf(layer.textAlign))
            .setIncludePad(true)
            .build()
        val w = (layout.width + 16).coerceAtLeast(1)
        val h = (layout.height + 16).coerceAtLeast(1)
        val picture = Picture()
        val canvas = picture.beginRecording(w, h)
        canvas.save()
        canvas.translate(8f, 8f)
        layout.draw(canvas)
        canvas.restore()
        picture.endRecording()
        val textW = if (layer.maxWidth > 0) layer.maxWidth.toFloat() else layout.width.toFloat()
        return LayerCacheEntry(
            hash, picture, w, h,
            (layer.x - textW / 2f - 8f).toFloat(),
            (layer.y - layout.height / 2f - 8f).toFloat(),
        )
    }

    private fun rasterizeImage(
        layer: ImageLayer,
        hash: String,
        sourceImage: Bitmap?,
    ): LayerCacheEntry? {
        if (sourceImage == null) return null
        val left = (layer.x - layer.w / 2).toFloat()
        val top = (layer.y - layer.h / 2).toFloat()
        val w = layer.w.toInt().coerceAtLeast(1)
        val h = layer.h.toInt().coerceAtLeast(1)
        val scaled = if (sourceImage.width == w && sourceImage.height == h) {
            sourceImage
        } else {
            Bitmap.createScaledBitmap(sourceImage, w, h, true)
        }
        return LayerCacheEntry(hash, Picture(), w, h, left, top, bitmap = scaled)
    }

    private fun rasterizeFreehand(layer: FreehandLayer, hash: String): LayerCacheEntry? {
        if (layer.points.size < 2) return null
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        for (p in layer.points) {
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
        }
        val lw = layer.lineWidth.toFloat()
        val pad = lw + 2f
        val w = ((maxX - minX + pad * 2).toInt()).coerceAtLeast(1)
        val h = ((maxY - minY + pad * 2).toInt()).coerceAtLeast(1)
        val offX = (minX - pad).toFloat()
        val offY = (minY - pad).toFloat()
        val picture = Picture()
        val canvas = picture.beginRecording(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseColor(layer.color)
            strokeWidth = lw
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val path = Path()
        val first = layer.points[0]
        path.moveTo((first.x - offX).toFloat(), (first.y - offY).toFloat())
        for (i in 1 until layer.points.size) {
            val p = layer.points[i]
            path.lineTo((p.x - offX).toFloat(), (p.y - offY).toFloat())
        }
        canvas.drawPath(path, paint)
        picture.endRecording()
        return LayerCacheEntry(hash, picture, w, h, offX, offY)
    }

    private fun rasterizeSticker(layer: StickerLayer, hash: String): LayerCacheEntry {
        val left = (layer.x - layer.w / 2).toFloat()
        val top = (layer.y - layer.h / 2).toFloat()
        val pad = 2f
        val w = (layer.w + pad * 2).toInt().coerceAtLeast(1)
        val h = (layer.h + pad * 2).toInt().coerceAtLeast(1)
        val picture = Picture()
        val canvas = picture.beginRecording(w, h)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseColor(layer.color)
            style = Paint.Style.FILL
        }
        canvas.drawRect(pad, pad, pad + layer.w.toFloat(), pad + layer.h.toFloat(), bg)
        if (layer.text.isNotEmpty()) {
            val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = layer.fontSize.toFloat()
                typeface = typefaceOf("sans-serif", layer.fontWeight, layer.fontStyle)
            }
            val layout = StaticLayout.Builder
                .obtain(layer.text, 0, layer.text.length, tp, layer.w.toInt().coerceAtLeast(1))
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setIncludePad(false)
                .build()
            val tx = pad + (layer.w.toFloat() - layout.width) / 2f
            val ty = pad + (layer.h.toFloat() - layout.height) / 2f
            canvas.save()
            canvas.translate(tx, ty)
            layout.draw(canvas)
            canvas.restore()
        }
        picture.endRecording()
        return LayerCacheEntry(hash, picture, w, h, left - pad, top - pad)
    }

    companion object {
        internal fun parseColor(value: String): Int {
            val trimmed = value.trim()
            if (trimmed.isEmpty() || trimmed.equals("transparent", ignoreCase = true)) return Color.TRANSPARENT
            return runCatching {
                when {
                    trimmed.startsWith("#") -> parseHexColor(trimmed)
                    trimmed.startsWith("rgba", ignoreCase = true) -> parseRgba(trimmed)
                    trimmed.startsWith("rgb", ignoreCase = true) -> parseRgb(trimmed)
                    else -> Color.BLACK
                }
            }.getOrDefault(Color.TRANSPARENT)
        }

        private fun parseHexColor(hex: String): Int {
            val n = hex.removePrefix("#")
            return when (n.length) {
                3 -> {
                    val r = n[0].digitToInt(16) * 17
                    val g = n[1].digitToInt(16) * 17
                    val b = n[2].digitToInt(16) * 17
                    Color.argb(255, r, g, b)
                }
                6 -> Color.parseColor("#$n")
                8 -> Color.parseColor("#$n")
                else -> Color.BLACK
            }
        }

        private fun parseRgba(value: String): Int {
            val nums = value.substringAfter("(").substringBefore(")")
                .split(",").map { it.trim() }
            if (nums.size != 4) return Color.BLACK
            val r = nums[0].toInt()
            val g = nums[1].toInt()
            val b = nums[2].toInt()
            val a = (nums[3].toFloatOrNull() ?: 1f).coerceIn(0f, 1f)
            return Color.argb((a * 255).toInt(), r, g, b)
        }

        private fun parseRgb(value: String): Int {
            val nums = value.substringAfter("(").substringBefore(")")
                .split(",").map { it.trim() }
            if (nums.size != 3) return Color.BLACK
            return Color.argb(255, nums[0].toInt(), nums[1].toInt(), nums[2].toInt())
        }

        private fun typefaceOf(family: String, weight: String, style: String): Typeface {
            val base = when (family.lowercase()) {
                "serif" -> Typeface.SERIF
                "monospace", "monos" -> Typeface.MONOSPACE
                else -> Typeface.SANS_SERIF
            }
            val bold = weight == "bold" || weight == "700" || weight == "600" || weight == "semibold" || weight == "500" || weight == "medium"
            val italic = style == "italic"
            val styleFlag = when {
                bold && italic -> Typeface.BOLD_ITALIC
                bold -> Typeface.BOLD
                italic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
            return Typeface.create(base, styleFlag)
        }

        private fun alignOf(value: String): Layout.Alignment {
            return when (value) {
                "center" -> Layout.Alignment.ALIGN_CENTER
                "right", "end" -> Layout.Alignment.ALIGN_OPPOSITE
                else -> Layout.Alignment.ALIGN_NORMAL
            }
        }
    }
}

internal fun drawCheckerboardOnCanvas(canvas: Canvas, w: Float, h: Float, tile: Float = 32f) {
    val light = Color.parseColor("#F8F8F8")
    val dark = Color.parseColor("#E5E5E5")
    val paint = Paint()
    paint.color = light
    canvas.drawRect(0f, 0f, w, h, paint)
    paint.color = dark
    var y = 0f
    var row = 0
    while (y < h) {
        var x = 0f
        var col = 0
        while (x < w) {
            if ((row + col) % 2 == 0) {
                canvas.drawRect(x, y, x + tile, y + tile, paint)
            }
            x += tile
            col++
        }
        y += tile
        row++
    }
}

internal fun drawDarkCheckerboardOnCanvas(canvas: Canvas, w: Float, h: Float, tile: Float = 16f) {
    val c1 = Color.parseColor("#1E1E1E")
    val c2 = Color.parseColor("#2A2A2A")
    val paint = Paint()
    paint.color = c1
    canvas.drawRect(0f, 0f, w, h, paint)
    paint.color = c2
    var y = 0f
    var row = 0
    while (y < h) {
        var x = 0f
        var col = 0
        while (x < w) {
            if ((row + col) % 2 == 0) {
                canvas.drawRect(x, y, x + tile, y + tile, paint)
            }
            x += tile
            col++
        }
        y += tile
        row++
    }
}

internal fun parseSceneBgColor(value: String): Int {
    return LayerRasterizer.parseColor(value)
}

internal fun isGradientBg(value: String): Boolean {
    return value.startsWith("gradient:") || value.startsWith("linear-gradient") || value.startsWith("radial-gradient")
}

internal fun makeGradientShader(value: String, w: Float, h: Float): Shader? {
    if (!value.startsWith("gradient:") && !value.startsWith("linear-gradient")) return null
    val parts = if (value.startsWith("gradient:")) {
        value.split(":")
    } else {
        val inner = value.removePrefix("linear-gradient(").removeSuffix(")")
        listOf("linear-gradient") + inner.split(",").map { it.trim() }
    }
    val angle = if (parts.size > 2 && parts[2].endsWith("deg")) {
        parts[2].removeSuffix("deg").toFloatOrNull() ?: 0f
    } else if (parts.size > 1 && parts[1].endsWith("deg")) {
        parts[1].removeSuffix("deg").toFloatOrNull() ?: 0f
    } else 0f
    val colorIdx = if (parts[1].endsWith("deg")) 2 else 1
    val colors = parts.drop(colorIdx).mapNotNull { runCatching { LayerRasterizer.parseColor(it) }.getOrNull() }
    if (colors.isEmpty()) return null
    val rad = ((angle - 90f) * Math.PI.toFloat() / 180f)
    val cx = w / 2f
    val cy = h / 2f
    val diag = (kotlin.math.sqrt((w * w + h * h).toDouble()) / 2).toFloat()
    val dx = cos(rad.toDouble()).toFloat() * diag
    val dy = sin(rad.toDouble()).toFloat() * diag
    val positions = if (colors.size == 1) floatArrayOf(0f) else null
    return LinearGradient(
        cx - dx, cy - dy, cx + dx, cy + dy,
        colors.toIntArray(), positions, Shader.TileMode.CLAMP
    )
}
