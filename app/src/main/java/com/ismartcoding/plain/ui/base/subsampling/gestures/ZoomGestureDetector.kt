package com.ismartcoding.plain.ui.base.subsampling.gestures

import android.graphics.PointF
import android.os.SystemClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.subsampling.ComposeSubsamplingScaleImageState
import com.ismartcoding.plain.ui.base.subsampling.PointfMut
import com.ismartcoding.plain.ui.base.subsampling.ScaleAndTranslate

/**
 * One finger quick scale gesture or double-tap zoom gesture
 * */
class ZoomGestureDetector(
  private val density: Density,
  private val state: ComposeSubsamplingScaleImageState,
) : GestureDetector(DetectorType.Zoom, state.debug) {
  private val quickScaleThreshold = with(density) { 20.dp.toPx() }

  private val vCenterStart = PointF(0f, 0f)
  private val vTranslateStart = PointF(0f, 0f)

  private val quickScaleSCenter = PointF(0f, 0f)
  private val quickScaleVStart = PointF(0f, 0f)
  private val quickScaleVLastPoint = PointF(0f, 0f)

  private val sCenterStart = PointF(0f, 0f)
  private val sCenterEnd = PointF(0f, 0f)
  private val sCenterEndRequested = PointF(0f, 0f)

  private var scaleStart = 0f
  private var isQuickScaling = false
  private var quickScaleMoved = false
  private var quickScaleLastDistance = 0f
  private var animatingQuickZoom = false

  override fun onGestureStarted(pointerInputChanges: List<PointerInputChange>) {
    super.onGestureStarted(pointerInputChanges)

    val pointerInputChange = pointerInputChanges.first()
    val offset = pointerInputChange.position
    val currentScale = state.currentScale
    val screenTranslateX = state._vTranslate.x
    val screenTranslateY = state._vTranslate.y

    vCenterStart.set(offset.x, offset.y)
    vTranslateStart.set(screenTranslateX, screenTranslateY)
    scaleStart = currentScale
    quickScaleLastDistance = -1f
    quickScaleSCenter.set(state.viewToSourceCoord(vCenterStart))
    quickScaleVStart.set(offset.x, offset.y)
    quickScaleVLastPoint.set(quickScaleSCenter.x, quickScaleSCenter.y)
  }

  override fun onGestureEnded(canceled: Boolean, pointerInputChanges: List<PointerInputChange>) {
    if (
      !canceled &&
      state.isReadyForGestures &&
      !animatingQuickZoom &&
      !quickScaleMoved &&
      coroutineScope != null
    ) {
      if (currentGestureAnimation != null) {
        return
      }

      animatingQuickZoom = true
      initAndStartQuickZoomAnimation(debug, pointerInputChanges)

      return
    }

    super.onGestureEnded(canceled, pointerInputChanges)

    vCenterStart.set(0f, 0f)
    vTranslateStart.set(0f, 0f)
    quickScaleSCenter.set(0f, 0f)
    quickScaleVStart.set(0f, 0f)
    quickScaleVLastPoint.set(0f, 0f)

    sCenterStart.set(0f, 0f)
    sCenterEnd.set(0f, 0f)
    sCenterEndRequested.set(0f, 0f)

    scaleStart = 0f
    isQuickScaling = false
    animatingQuickZoom = false
    quickScaleMoved = false
    quickScaleLastDistance = 0f
    currentGestureAnimation = null

    if (state.isReadyForGestures) {
      state.refreshRequiredTiles(load = true)
      state.requestInvalidate()
    }
  }

  override fun onGestureUpdated(pointerInputChanges: List<PointerInputChange>) {
    super.onGestureUpdated(pointerInputChanges)

    if (!state.isReadyForGestures) {
      return
    }

    val pointerInputChange = pointerInputChanges.first()
    val offset = pointerInputChange.position
    var dist = Math.abs(quickScaleVStart.y - offset.y) * 2 + quickScaleThreshold

    if (quickScaleLastDistance == -1f) {
      quickScaleLastDistance = dist
    }

    val isUpwards: Boolean = offset.y > quickScaleVLastPoint.y
    quickScaleVLastPoint.set(0f, offset.y)
    val spanDiff = Math.abs(1 - (dist / quickScaleLastDistance)) * 0.5f

    if (spanDiff > 0.03f || quickScaleMoved) {
      quickScaleMoved = true

      var multiplier = 1f
      if (quickScaleLastDistance > 0) {
        multiplier = if (isUpwards) (1 + spanDiff) else (1 - spanDiff)
      }

      val previousScale = state.currentScale.toDouble()
      val newScale = Math.max(
        state.calculateMinScale(),
        Math.min(state.maxScale, previousScale.toFloat() * multiplier)
      )
      state.currentScale = newScale

      val vLeftStart: Float = vCenterStart.x - vTranslateStart.x
      val vTopStart: Float = vCenterStart.y - vTranslateStart.y
      val vLeftNow: Float = vLeftStart * (newScale / scaleStart)
      val vTopNow: Float = vTopStart * (newScale / scaleStart)

      state._vTranslate.set((vCenterStart.x - vLeftNow), (vCenterStart.y - vTopNow))

      if (
        (previousScale * state.sHeight < state.viewHeight && newScale * state.sHeight >= state.viewHeight) ||
        (previousScale * state.sWidth < state.viewWidth && newScale * state.sWidth >= state.viewWidth)
      ) {
        state.fitToBounds(true)
        vCenterStart.set(state.sourceToViewCoord(quickScaleSCenter))

        vTranslateStart.set(state._vTranslate.x, state._vTranslate.y)
        scaleStart = newScale
        dist = 0f
      }
    }

    quickScaleLastDistance = dist
    state.fitToBounds(true)
    state.refreshRequiredTiles(load = true)
    state.requestInvalidate()
  }

  fun debugDraw(drawScope: DrawScope) {
    val style = Stroke(width = 8f)

    with(drawScope) {
      if (animatingQuickZoom) {
        val vCenterStart: PointF = state.sourceToViewCoord(sCenterStart)
        val vCenterEndRequested: PointF = state.sourceToViewCoord(sCenterEndRequested)
        val vCenterEnd: PointF = state.sourceToViewCoord(sCenterEnd)

        drawCircle(
          color = Color.Magenta,
          radius = 10.dp.toPx(),
          style = style,
          center = Offset(vCenterStart.x, vCenterStart.y)
        )

        drawCircle(
          color = Color.Red,
          radius = 20.dp.toPx(),
          style = style,
          center = Offset(vCenterEndRequested.x, vCenterEndRequested.y)
        )

        drawCircle(
          color = Color.Blue,
          radius = 25.dp.toPx(),
          style = style,
          center = Offset(vCenterEnd.x, vCenterEnd.y)
        )

        drawCircle(
          color = Color.Cyan,
          radius = 30.dp.toPx(),
          style = style,
          center = Offset(state.viewWidth / 2f, state.viewHeight / 2f)
        )
      }

      if (vCenterStart.x > 0f || vCenterStart.y > 0f) {
        drawCircle(
          color = Color.Green,
          radius = 20.dp.toPx(),
          style = style,
          center = Offset(vCenterStart.x, vCenterStart.y)
        )
      }

      if (quickScaleSCenter.x > 0 || quickScaleSCenter.y > 0) {
        drawCircle(
          color = Color.Yellow,
          radius = 35.dp.toPx(),
          style = style,
          center = Offset(
            state.sourceToViewX(quickScaleSCenter.x),
            state.sourceToViewY(quickScaleSCenter.y),
          )
        )
      }

      if ((quickScaleVStart.x > 0 || quickScaleVStart.y > 0) && isQuickScaling) {
        drawCircle(
          color = Color.White,
          radius = 30.dp.toPx(),
          style = style,
          center = Offset(quickScaleVStart.x, quickScaleVStart.y)
        )
      }
    }
  }

  private fun initAndStartQuickZoomAnimation(debug: Boolean, pointerInputChanges: List<PointerInputChange>) {
    val pointerInputChange = pointerInputChanges.first()
    val offset = pointerInputChange.position
    val vTranslateX = state._vTranslate.x
    val vTranslateY = state._vTranslate.y

    vCenterStart.set(offset.x, offset.y)
    vTranslateStart.set(vTranslateX, vTranslateY)
    scaleStart = state.currentScale
    isQuickScaling = false
    quickScaleLastDistance = -1f
    quickScaleSCenter.set(state.viewToSourceCoord(vCenterStart))
    quickScaleVStart.set(offset.x, offset.y)
    quickScaleVLastPoint.set(quickScaleSCenter.x, quickScaleSCenter.y)

    currentGestureAnimation = GestureAnimation<GestureAnimationParameters>(
      debug = debug,
      detectorType = detectorType,
      state = state,
      coroutineScope = coroutineScope!!,
      durationMs = state.zoomAnimationDurationMs,
      animationUpdateIntervalMs = state.animationUpdateIntervalMs.toLong(),
      animationParams = {
        val currentScale = state.currentScale
        val minScale = state.minScale

        val doubleTapZoomScale = Math.min(state.maxScale, state.doubleTapZoomScale)
        val zoomIn = (currentScale <= doubleTapZoomScale * 0.9) || (currentScale == minScale)
        val endScale = if (zoomIn) doubleTapZoomScale else state.calculateMinScale()
        val targetScale = state.limitedScale(endScale)

        val sCenter = quickScaleSCenter
        val vFocus = if (zoomIn) {
          vCenterStart
        } else {
          null
        }

        val targetSCenter = state.limitedSCenter(
          sCenterX = sCenter.x,
          sCenterY = sCenter.y,
          scale = targetScale,
          sTarget = PointF()
        )

        val vFocusStart = state.sourceToViewCoord(targetSCenter)

        val sCenterEnd = targetSCenter
        val sCenterStart = state.getCenter()

        val vFocusEnd = if (vFocus != null) {
          val vTranslateXEnd: Float = vFocus.x - targetScale * sCenterStart.x
          val vTranslateYEnd: Float = vFocus.y - targetScale * sCenterStart.y

          val satEnd = ScaleAndTranslate(
            scale = targetScale,
            vTranslate = PointfMut(vTranslateXEnd, vTranslateYEnd)
          )
          state.fitToBounds(true, satEnd)

          PointF(
            vFocus.x + (satEnd.vTranslate.x - vTranslateXEnd),
            vFocus.y + (satEnd.vTranslate.y - vTranslateYEnd)
          )
        } else {
          val vxCenter = state.viewWidth / 2f
          val vyCenter = state.viewHeight / 2f

          PointF(vxCenter, vyCenter)
        }

        this.sCenterStart.set(sCenterStart)
        this.sCenterEnd.set(targetSCenter)
        this.sCenterEndRequested.set(targetSCenter)

        return@GestureAnimation GestureAnimationParameters(
          gestureAnimationEasing = GestureAnimationEasing.EaseInOutQuad,
          startTime = SystemClock.elapsedRealtime(),
          startScale = currentScale,
          endScale = targetScale,
          vFocusStart = vFocusStart,
          vFocusEnd = vFocusEnd,
          sCenterEnd = sCenterEnd
        )
      },
      animationFunc = { params: GestureAnimationParameters, _: Float, duration: Long ->
        val startScale = params.startScale
        val endScale = params.endScale

        var timeElapsed = SystemClock.elapsedRealtime() - params.startTime
        val finished = timeElapsed > duration
        timeElapsed = Math.min(timeElapsed, duration)

        state.currentScale = state.ease(
          gestureAnimationEasing = params.gestureAnimationEasing,
          time = timeElapsed,
          from = startScale,
          change = endScale - startScale,
          duration = duration
        )

        val vFocusNowX = state.ease(
          gestureAnimationEasing = params.gestureAnimationEasing,
          time = timeElapsed,
          from = params.vFocusStart.x,
          change = params.vFocusEnd.x - params.vFocusStart.x,
          duration = duration
        )

        val vFocusNowY = state.ease(
          gestureAnimationEasing = params.gestureAnimationEasing,
          time = timeElapsed,
          from = params.vFocusStart.y,
          change = params.vFocusEnd.y - params.vFocusStart.y,
          duration = duration
        )

        state._vTranslate.x -= (state.sourceToViewX(params.sCenterEnd.x) - vFocusNowX).toInt()
        state._vTranslate.y -= (state.sourceToViewY(params.sCenterEnd.y) - vFocusNowY).toInt()

        state.fitToBounds(finished || startScale == endScale)
        state.refreshRequiredTiles(finished)
        state.requestInvalidate()
      },
      onAnimationEnd = { canceled ->
        onGestureEnded(
          canceled = canceled,
          pointerInputChanges = pointerInputChanges
        )

        animatingQuickZoom = false
      }
    )

    currentGestureAnimation!!.start()
  }

  private data class GestureAnimationParameters(
    val gestureAnimationEasing: GestureAnimationEasing,
    val startTime: Long,
    val startScale: Float,
    val endScale: Float,

    val vFocusStart: PointF,
    val vFocusEnd: PointF,

    val sCenterEnd: PointF
  )

}