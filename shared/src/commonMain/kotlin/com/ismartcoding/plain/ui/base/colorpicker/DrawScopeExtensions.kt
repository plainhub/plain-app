package com.ismartcoding.plain.ui.base.colorpicker

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

private const val SELECTOR_RADIUS: Float = 50f
private const val BORDER_WIDTH: Float = 10f

public fun DrawScope.drawColorIndicator(pos: Offset, color: Color) {
  drawCircle(color, SELECTOR_RADIUS, pos)
  drawCircle(
    Color.White,
    SELECTOR_RADIUS - (BORDER_WIDTH / 2),
    pos,
    style = Stroke(width = BORDER_WIDTH),
  )
  drawCircle(
    Color.LightGray,
    SELECTOR_RADIUS,
    pos,
    style = Stroke(width = 1f),
  )
}
