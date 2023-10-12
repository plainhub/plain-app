package com.ismartcoding.plain.ui.base.subsampling

import android.graphics.PointF

class PointfMut {
  var x: Float = 0f
  var y: Float = 0f

  constructor() {
    this.x = 0f
    this.y = 0f
  }

  constructor(x: Float, y: Float) {
    this.x = x
    this.y = y
  }

  fun set(x: Float, y: Float) {
    this.x = x
    this.y = y
  }

  fun set(pointF: PointF) {
    this.x = pointF.x
    this.y = pointF.y
  }

  fun set(pointF: PointfMut) {
    this.x = pointF.x
    this.y = pointF.y
  }

}