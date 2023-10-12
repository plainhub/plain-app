package com.ismartcoding.plain.ui.base.subsampling

import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

data class RectMut(
  var left: Int,
  var top: Int,
  var right: Int,
  var bottom: Int
) {
  var topLeft: Offset = Offset.Zero
    private set
  var size: Size = Size.Zero
    private set

  val width: Int
    get() = size.width.toInt()
  val height: Int
    get() = size.height.toInt()


  init {
    onUpdated()
  }

  fun set(left: Int, top: Int, right: Int, bottom: Int) {
    this.left = left
    this.top = top
    this.right = right
    this.bottom = bottom

    onUpdated()
  }

  fun set(other: RectMut) {
    left = other.left
    top = other.top
    right = other.right
    bottom = other.bottom

    onUpdated()
  }

  fun toRect(): Rect {
    return Rect(Offset(left.toFloat(), top.toFloat()), Offset(right.toFloat(), bottom.toFloat()))
  }

  fun toAndroidRect(tempRect: android.graphics.Rect? = null): android.graphics.Rect {
    if (tempRect != null) {
      tempRect.set(left, top, right, bottom)
      return tempRect
    }

    return android.graphics.Rect(left, top, right, bottom)
  }

  fun toAndroidRectF(tempRectF: RectF? = null): RectF {
    if (tempRectF != null) {
      tempRectF.set(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
      return tempRectF
    }

    return RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
  }

  fun intersects(otherLeft: Int, otherTop: Int, otherRight: Int, otherBottom: Int): Boolean {
    return this.left < otherRight &&
      otherLeft < this.right &&
      this.top < otherBottom &&
      otherTop < this.bottom
  }

  fun intersects(other: RectMut): Boolean {
    return this.left < other.right &&
      other.left < this.right &&
      this.top < other.bottom &&
      other.top < this.bottom
  }

  private fun onUpdated() {
    topLeft = Offset(left.toFloat(), top.toFloat())
    size = Size((right - left).toFloat(), (bottom - top).toFloat())
  }

  override fun toString(): String {
    return "RectMut($left, $top, $right, $bottom)"
  }

}