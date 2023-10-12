package com.ismartcoding.plain.ui.base.subsampling.gestures

import android.graphics.PointF
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.unit.Density
import com.ismartcoding.plain.ui.base.subsampling.ComposeSubsamplingScaleImageState
import com.ismartcoding.plain.ui.base.subsampling.helpers.distance

/**
 * Two finger zoom/pan gesture
 * */
class MultiTouchGestureDetector(
  private val density: Density,
  private val state: ComposeSubsamplingScaleImageState,
) : GestureDetector(DetectorType.MultiTouch, state.debug) {

  private var scaleStart = 1f
  private var vDistStart = 0f
  private var isPanningOrZooming = false

  private val vTranslateStart = PointF(0f, 0f)
  private val vCenterStart = PointF(0f, 0f)

  override fun onGestureStarted(pointerInputChanges: List<PointerInputChange>) {
    super.onGestureStarted(pointerInputChanges)

    val firstPointerChange = pointerInputChanges[0]
    val secondPointerChange = pointerInputChanges[1]

    val distance = distance(
      x0 = firstPointerChange.position.x,
      x1 = secondPointerChange.position.x,
      y0 = firstPointerChange.position.y,
      y1 = secondPointerChange.position.y
    )

    isPanningOrZooming = false
    scaleStart = state.currentScale
    vDistStart = distance
    vTranslateStart.set(state._vTranslate.x, state._vTranslate.y)
    vCenterStart.set(
      (firstPointerChange.position.x + secondPointerChange.position.x) / 2,
      (firstPointerChange.position.y + secondPointerChange.position.y) / 2
    )
  }

  override fun onGestureUpdated(pointerInputChanges: List<PointerInputChange>) {
    super.onGestureUpdated(pointerInputChanges)

    if (!state.isReadyForGestures) {
      return
    }

    val firstPointerChange = pointerInputChanges[0]
    val secondPointerChange = pointerInputChanges[1]

    val vDistEnd = distance(
      x0 = firstPointerChange.position.x,
      x1 = secondPointerChange.position.x,
      y0 = firstPointerChange.position.y,
      y1 = secondPointerChange.position.y
    )

    val vCenterEndX = (firstPointerChange.position.x + secondPointerChange.position.x) / 2
    val vCenterEndY = (firstPointerChange.position.y + secondPointerChange.position.y) / 2

    val distanceOk = (distance(vCenterStart.x, vCenterEndX, vCenterStart.y, vCenterEndY) > 5) ||
      (Math.abs(vDistEnd - vDistStart) > 5)

    if (distanceOk || isPanningOrZooming) {
      isPanningOrZooming = true

      val previousScale = state.currentScale.toDouble()
      state.currentScale = Math.min(state.maxScale, vDistEnd / vDistStart * scaleStart)

      if (state.currentScale <= state.minScale) {
        vDistStart = vDistEnd
        scaleStart = state.minScale
        vCenterStart.set(vCenterEndX, vCenterEndX)
        vTranslateStart.set(state._vTranslate.x, state._vTranslate.y)
      } else {
        val vLeftStart = vCenterStart.x - vTranslateStart.x
        val vTopStart = vCenterStart.y - vTranslateStart.y
        val vLeftNow: Float = vLeftStart * (state.currentScale / scaleStart)
        val vTopNow: Float = vTopStart * (state.currentScale / scaleStart)

        state._vTranslate.x = vCenterEndX - vLeftNow
        state._vTranslate.y = vCenterEndY - vTopNow

        if (
          (previousScale * state.sHeight < state.viewHeight && state.currentScale * state.sHeight >= state.viewHeight) ||
          (previousScale * state.sWidth < state.viewWidth && state.currentScale * state.sWidth >= state.viewWidth)
        ) {
          state.fitToBounds(true)
          vCenterStart.set(vCenterEndX, vCenterEndY)
          vTranslateStart.set(state._vTranslate.x, state._vTranslate.y)
          scaleStart = state.currentScale
          vDistStart = vDistEnd
        }
      }

      state.fitToBounds(true)
      state.refreshRequiredTiles(load = true)
      state.requestInvalidate()
    }
  }

  override fun onGestureEnded(canceled: Boolean, pointerInputChanges: List<PointerInputChange>) {
    super.onGestureEnded(canceled, pointerInputChanges)

    isPanningOrZooming = false

    scaleStart = 1f
    vDistStart = 0f
    vCenterStart.set(0f, 0f)
    vTranslateStart.set(state._vTranslate.x, state._vTranslate.y)

    if (state.isReadyForGestures) {
      state.refreshRequiredTiles(load = true)
      state.requestInvalidate()
    }
  }
}