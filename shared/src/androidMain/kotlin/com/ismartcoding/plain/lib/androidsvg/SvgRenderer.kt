package com.ismartcoding.plain.lib.androidsvg

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Picture
import android.graphics.RectF
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Android-specific renderer that converts a [SvgDocument] (parsed in commonMain)
 * into an Android [Picture].
 */
fun SvgDocument.renderToPicture(widthInPixels: Int, heightInPixels: Int): Picture {
    val picture = Picture()
    val canvas = picture.beginRecording(widthInPixels, heightInPixels)
    val (scaleX, scaleY) = computeScale(viewBox, widthInPixels, heightInPixels)
    renderGroup(root, canvas, scaleX, scaleY)
    picture.endRecording()
    return picture
}

private fun computeScale(viewBox: SvgRect?, w: Int, h: Int): Pair<Float, Float> {
    val vb = viewBox ?: return 1f to 1f
    val sx = if (vb.width() > 0) w / vb.width() else 1f
    val sy = if (vb.height() > 0) h / vb.height() else 1f
    return sx to sy
}

private fun renderGroup(group: SvgGroup, canvas: Canvas, scaleX: Float, scaleY: Float) {
    val saveCount = if (group.transform != null) canvas.save() else -1
    if (group.transform != null) {
        canvas.concat(group.transform.toMatrix())
    }
    group.children.forEach { child ->
        when (child) {
            is SvgGroup -> renderGroup(child, canvas, scaleX, scaleY)
            is SvgLeaf -> renderLeaf(child, canvas, scaleX, scaleY)
        }
    }
    if (saveCount >= 0) canvas.restoreToCount(saveCount)
}

private fun renderLeaf(leaf: SvgLeaf, canvas: Canvas, scaleX: Float, scaleY: Float) {
    val path = leaf.shape.toAndroidPath()
    val scaled = Path(path)
    scaled.transform(Matrix().apply { setScale(scaleX, scaleY) })
    leaf.style.fill?.let { color ->
        canvas.drawPath(
            scaled,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                this.color = color
            },
        )
    }
    leaf.style.stroke?.let { color ->
        canvas.drawPath(
            scaled,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.color = color
                strokeWidth = leaf.style.strokeWidth ?: 1f
            },
        )
    }
}

private fun SvgShape.toAndroidPath(): Path {
    val path = Path()
    var cx = 0f
    var cy = 0f
    var startX = 0f
    var startY = 0f
    when (this) {
        is SvgShape.Path -> {
            for (seg in segments) {
                when (seg) {
                    is SvgPathSegment.MoveTo -> {
                        path.moveTo(seg.x, seg.y)
                        cx = seg.x; cy = seg.y; startX = seg.x; startY = seg.y
                    }
                    is SvgPathSegment.LineTo -> {
                        path.lineTo(seg.x, seg.y)
                        cx = seg.x; cy = seg.y
                    }
                    is SvgPathSegment.CubicTo -> {
                        path.cubicTo(seg.x1, seg.y1, seg.x2, seg.y2, seg.x3, seg.y3)
                        cx = seg.x3; cy = seg.y3
                    }
                    is SvgPathSegment.QuadTo -> {
                        path.quadTo(seg.x1, seg.y1, seg.x2, seg.y2)
                        cx = seg.x2; cy = seg.y2
                    }
                    is SvgPathSegment.ArcTo -> {
                        arcToAndroid(path, cx, cy, seg)
                        cx = seg.x; cy = seg.y
                    }
                    is SvgPathSegment.Close -> {
                        path.close()
                        cx = startX; cy = startY
                    }
                }
            }
        }
        is SvgShape.Rect -> path.addRect(x, y, x + w, y + h, Path.Direction.CW)
        is SvgShape.Circle -> path.addCircle(cx, cy, r, Path.Direction.CW)
        is SvgShape.Ellipse -> path.addOval(
            RectF(cx - rx, cy - ry, cx + rx, cy + ry),
            Path.Direction.CW,
        )
        is SvgShape.Line -> {
            path.moveTo(x1, y1)
            path.lineTo(x2, y2)
        }
        is SvgShape.Polyline -> {
            if (points.size >= 4) {
                path.moveTo(points[0], points[1])
                var i = 2
                while (i + 1 < points.size) {
                    path.lineTo(points[i], points[i + 1])
                    i += 2
                }
                if (closed) path.close()
            }
        }
    }
    return path
}

private fun arcToAndroid(
    path: Path,
    x1: Float,
    y1: Float,
    seg: SvgPathSegment.ArcTo,
) {
    val rx = seg.rx
    val ry = seg.ry
    val angle = seg.angle
    val largeArc = seg.largeArc
    val sweep = seg.sweep
    val x2 = seg.x
    val y2 = seg.y

    if (rx == 0f || ry == 0f) {
        path.lineTo(x2, y2)
        return
    }
    val rad = Math.toRadians(angle.toDouble())
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()
    val dx = (x1 - x2) / 2f
    val dy = (y1 - y2) / 2f
    val x1p = cosA * dx + sinA * dy
    val y1p = -sinA * dx + cosA * dy
    var rxS = rx * rx
    var ryS = ry * ry
    val x1pS = x1p * x1p
    val y1pS = y1p * y1p
    val lambda = x1pS / rxS + y1pS / ryS
    if (lambda > 1) {
        val s = sqrt(lambda.toDouble()).toFloat()
        rxS *= s * s
        ryS *= s * s
    }
    val sign = if (largeArc != sweep) 1f else -1f
    val num = rxS * ryS - rxS * y1pS - ryS * x1pS
    val den = rxS * y1pS + ryS * x1pS
    val factor = if (num < 0) 0f else num / den
    val c = sign * sqrt(factor.toDouble()).toFloat()
    val cxp = c * rx * y1p / ry
    val cyp = -c * ry * x1p / rx
    val centerX = cosA * cxp - sinA * cyp + (x1 + x2) / 2f
    val centerY = sinA * cxp + cosA * cyp + (y1 + y2) / 2f
    val startAngle = angleBetween(cxp, cyp, rx, ry)
    val sweepAngle =
        angleBetween((x1p - cxp) / rx, (cyp - y1p) / ry, rx, ry) - startAngle
    val arcSweep = if (!sweep && sweepAngle > 0) sweepAngle - 360f
    else if (sweep && sweepAngle < 0) sweepAngle + 360f
    else sweepAngle
    val rect = RectF(centerX - rx, centerY - ry, centerX + rx, centerY + ry)
    val useStart = if (sweep) startAngle else startAngle + arcSweep
    val useSweep = if (sweep) arcSweep else -arcSweep
    path.arcTo(rect, useStart, useSweep)
}

private fun angleBetween(x: Float, y: Float, rx: Float, ry: Float): Float {
    val rad = atan2(y.toDouble(), x.toDouble())
    return Math.toDegrees(rad).toFloat()
}

private fun List<SvgTransformOp>.toMatrix(): Matrix {
    val matrix = Matrix()
    for (op in this) {
        when (op) {
            is SvgTransformOp.Translate -> matrix.postTranslate(op.tx, op.ty)
            is SvgTransformOp.Scale -> matrix.postScale(op.sx, op.sy)
            is SvgTransformOp.Rotate -> {
                if (op.cx != 0f || op.cy != 0f) {
                    matrix.postRotate(op.angle, op.cx, op.cy)
                } else {
                    matrix.postRotate(op.angle)
                }
            }
            is SvgTransformOp.Matrix -> {
                val m = Matrix()
                m.setValues(op.values)
                matrix.postConcat(m)
            }
        }
    }
    return matrix
}
