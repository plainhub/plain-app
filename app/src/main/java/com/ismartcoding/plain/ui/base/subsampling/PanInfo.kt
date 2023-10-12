package com.ismartcoding.plain.ui.base.subsampling

import kotlin.math.absoluteValue

data class PanInfo(
  val top: Float,
  val left: Float,
  val bottom: Float,
  val right: Float,
  val horizontalTolerance: Float,
  val verticalTolerance: Float
) {

  fun touchesLeft(): Boolean {
    return left.absoluteValue < horizontalTolerance
  }

  fun touchesRight(): Boolean {
    return right.absoluteValue < horizontalTolerance
  }

  fun touchesTop(): Boolean {
    return top.absoluteValue < verticalTolerance
  }

  fun touchesBottom(): Boolean {
    return bottom.absoluteValue < verticalTolerance
  }

  fun touchesLeftAndRight(): Boolean {
    return touchesLeft() && touchesRight()
  }

  fun touchesTopAndBottom(): Boolean {
    return touchesTop() && touchesBottom()
  }

  fun touchesAllSides(): Boolean {
    return touchesLeftAndRight() && touchesTopAndBottom()
  }

  companion object {
    const val DEFAULT_TOLERANCE = 3f
  }

}