package com.ismartcoding.plain.ui.base.colorpicker

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * PointMapper calculates correct coordinates corresponding to bitmap ratio and size.
 */
internal object PointMapper {

  internal fun getColorPoint(controller: ColorPickerController, point: Offset): Offset {
    val size = controller.canvasSize.value
    val center = Offset(size.width / 2f, size.height / 2f)
    return if (controller.isHsvColorPalette) {
      getHuePoint(controller, point)
    } else {
      approximatedPoint(controller, point, center)
    }
  }

  private fun approximatedPoint(
    controller: ColorPickerController,
    start: Offset,
    end: Offset,
  ): Offset {
    if (getDistance(start, end) <= 3) return end
    val center: Offset = getCenterPoint(start, end)
    val color: Color = controller.extractPixelColor(center.x, center.y)
    return if (color == Color.Transparent) {
      approximatedPoint(controller, center, end)
    } else {
      approximatedPoint(controller, start, center)
    }
  }

  private fun getHuePoint(controller: ColorPickerController, point: Offset): Offset {
    val size = controller.canvasSize.value
    val centerX: Float = size.width * 0.5f
    val centerY: Float = size.height * 0.5f
    var x = point.x - centerX
    var y = point.y - centerY
    val radius = centerX.coerceAtMost(centerY)
    val r = sqrt((x * x + y * y).toDouble())
    if (r > radius) {
      x *= (radius / r).toFloat()
      y *= (radius / r).toFloat()
    }
    return Offset(x + centerX, y + centerY)
  }

  private fun getCenterPoint(start: Offset, end: Offset): Offset {
    return Offset((end.x + start.x) / 2, (end.y + start.y) / 2)
  }

  private fun getDistance(start: Offset, end: Offset): Int {
    return sqrt(
      (
        abs(end.x - start.x) * abs(end.x - start.x) +
          abs(end.y - start.y) * abs(end.y - start.y)
        ).toDouble(),
    ).toInt()
  }
}
