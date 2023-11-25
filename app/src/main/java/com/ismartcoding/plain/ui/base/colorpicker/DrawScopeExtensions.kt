package com.ismartcoding.plain.ui.base.colorpicker

import android.graphics.PointF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

private const val SELECTOR_RADIUS: Float = 50f
private const val BORDER_WIDTH: Float = 10f

public fun DrawScope.drawColorIndicator(pos: PointF, color: Color) {
  drawCircle(color, SELECTOR_RADIUS, Offset(pos.x, pos.y))
  drawCircle(
    Color.White,
    SELECTOR_RADIUS - (BORDER_WIDTH / 2),
    Offset(pos.x, pos.y),
    style = Stroke(width = BORDER_WIDTH),
  )
  drawCircle(
    Color.LightGray,
    SELECTOR_RADIUS,
    Offset(pos.x, pos.y),
    style = Stroke(width = 1f),
  )
}
