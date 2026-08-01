package com.ismartcoding.plain.lib.androidsvg

class SvgParseException(message: String) : Exception(message)

data class SvgRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun width() = right - left
    fun height() = bottom - top
}

data class SvgStyle(
    val fill: Int?,
    val stroke: Int?,
    val strokeWidth: Float?,
) {
    companion object {
        fun inherit(fill: String?, stroke: String?, strokeWidth: String?): SvgStyle {
            val fp = if (fill != null && fill != "none") parseColor(fill) else null
            val sp = if (stroke != null && stroke != "none") parseColor(stroke) else null
            val sw = strokeWidth?.toFloatOrNull()
            return SvgStyle(fp, sp, sw)
        }

        internal fun fromAttrs(
            fillAttr: String?,
            strokeAttr: String?,
            swAttr: String?,
            parent: SvgStyle,
        ): SvgStyle {
            val fill = if (fillAttr != null) {
                if (fillAttr == "none") null else parseColor(fillAttr)
            } else parent.fill
            val stroke = if (strokeAttr != null) {
                if (strokeAttr == "none") null else parseColor(strokeAttr)
            } else parent.stroke
            val strokeWidth = if (swAttr != null) swAttr.toFloatOrNull() else parent.strokeWidth
            return SvgStyle(fill, stroke, strokeWidth)
        }

        private fun parseColor(value: String): Int {
            if (value.startsWith("#")) {
                return if (value.length == 7) {
                    value.substring(1).toLong(16).toInt()
                } else if (value.length == 4) {
                    val r = value[1]
                    val g = value[2]
                    val b = value[3]
                    "$r$r$g$g$b$b".toLong(16).toInt()
                } else {
                    0xFF000000.toInt()
                }
            }
            return when (value.lowercase()) {
                "black" -> 0xFF000000.toInt()
                "white" -> 0xFFFFFFFF.toInt()
                "red" -> 0xFFFF0000.toInt()
                "green" -> 0xFF00FF00.toInt()
                "blue" -> 0xFF0000FF.toInt()
                "transparent" -> 0x00000000
                else -> 0xFF000000.toInt()
            }
        }
    }
}

sealed class SvgPathSegment {
    data class MoveTo(val x: Float, val y: Float) : SvgPathSegment()
    data class LineTo(val x: Float, val y: Float) : SvgPathSegment()
    data class CubicTo(
        val x1: Float, val y1: Float,
        val x2: Float, val y2: Float,
        val x3: Float, val y3: Float,
    ) : SvgPathSegment()

    data class QuadTo(
        val x1: Float, val y1: Float,
        val x2: Float, val y2: Float,
    ) : SvgPathSegment()

    data class ArcTo(
        val rx: Float, val ry: Float,
        val angle: Float,
        val largeArc: Boolean,
        val sweep: Boolean,
        val x: Float, val y: Float,
    ) : SvgPathSegment()

    object Close : SvgPathSegment()
}

sealed class SvgShape {
    data class Path(val segments: List<SvgPathSegment>) : SvgShape()
    data class Rect(val x: Float, val y: Float, val w: Float, val h: Float) : SvgShape()
    data class Circle(val cx: Float, val cy: Float, val r: Float) : SvgShape()
    data class Ellipse(val cx: Float, val cy: Float, val rx: Float, val ry: Float) : SvgShape()
    data class Line(val x1: Float, val y1: Float, val x2: Float, val y2: Float) : SvgShape()
    data class Polyline(val points: List<Float>, val closed: Boolean) : SvgShape()
}

sealed class SvgTransformOp {
    data class Translate(val tx: Float, val ty: Float) : SvgTransformOp()
    data class Scale(val sx: Float, val sy: Float) : SvgTransformOp()
    data class Rotate(val angle: Float, val cx: Float = 0f, val cy: Float = 0f) : SvgTransformOp()
    data class Matrix(val values: FloatArray) : SvgTransformOp()
}

sealed interface SvgNode {
    val viewBox: SvgRect?
    val dimensions: Pair<Float, Float>
}

data class SvgGroup(
    val isRoot: Boolean,
    val width: Float,
    val height: Float,
    override val viewBox: SvgRect?,
    val transform: List<SvgTransformOp>?,
    val children: List<SvgNode>,
) : SvgNode {
    override val dimensions: Pair<Float, Float>
        get() = if (isRoot && width > 0 && height > 0) width to height
        else if (viewBox != null) viewBox.width() to viewBox.height()
        else 0f to 0f
}

data class SvgLeaf(
    val shape: SvgShape,
    val style: SvgStyle,
) : SvgNode {
    override val viewBox: SvgRect? = null
    override val dimensions: Pair<Float, Float> = 0f to 0f
}

data class SvgDocument(
    val documentWidth: Float,
    val documentHeight: Float,
    val viewBox: SvgRect?,
    val root: SvgGroup,
)
