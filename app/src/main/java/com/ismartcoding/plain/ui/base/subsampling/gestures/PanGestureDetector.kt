package com.ismartcoding.plain.ui.base.subsampling.gestures

import android.graphics.PointF
import android.os.SystemClock
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastForEach
import com.ismartcoding.plain.ui.base.subsampling.ComposeSubsamplingScaleImageState
import kotlin.math.absoluteValue

/**
 * One finger pan gesture
 * */
class PanGestureDetector(
  private val density: Density,
  private val state: ComposeSubsamplingScaleImageState,
) : GestureDetector(DetectorType.Pan, state.debug) {
  private val velocityTracker = VelocityTracker()

  private val vCenterStart = PointF(0f, 0f)
  private val vTranslateStart = PointF(0f, 0f)

  private val startOffset = PointF(0f, 0f)

  private var isPanning = false
  private var animatingFling = false

  override fun onGestureStarted(pointerInputChanges: List<PointerInputChange>) {
    super.onGestureStarted(pointerInputChanges)

    val pointerInputChange = pointerInputChanges.first()

    isPanning = false
    animatingFling = false

    val offset = pointerInputChange.position
    velocityTracker.resetTracking()

    startOffset.set(offset.x, offset.y)
    vCenterStart.set(offset.x, offset.y)
    vTranslateStart.set(
      state._vTranslate.x,
      state._vTranslate.y
    )
  }

  override fun onGestureUpdated(pointerInputChanges: List<PointerInputChange>) {
    super.onGestureUpdated(pointerInputChanges)

    if (!state.isReadyForGestures) {
      return
    }

    val pointerInputChange = pointerInputChanges.first()
    val offset = pointerInputChange.position
    val dx: Float = Math.abs(offset.x - vCenterStart.x)
    val dy: Float = Math.abs(offset.y - vCenterStart.y)
    val minOffset: Float = density.density * 5

    pointerInputChanges.fastForEach { pic ->
      velocityTracker.addPointerInputChange(pic)
    }

    if (dx > minOffset || dy > minOffset || isPanning) {
      state._vTranslate.set(
        (vTranslateStart.x + (offset.x - vCenterStart.x)),
        (vTranslateStart.y + (offset.y - vCenterStart.y))
      )

      val lastX: Float = state._vTranslate.x
      val lastY: Float = state._vTranslate.y
      state.fitToBounds(true)
      val atXEdge = lastX != state._vTranslate.x
      val atYEdge = lastY != state._vTranslate.y
      val edgeXSwipe = atXEdge && dx > dy && !isPanning
      val edgeYSwipe = atYEdge && dy > dx && !isPanning
      val yPan = lastY == state._vTranslate.y && dy > minOffset * 3

      if (!edgeXSwipe && !edgeYSwipe && (!atXEdge || !atYEdge || yPan || isPanning)) {
        isPanning = true
        state.refreshRequiredTiles(load = false)
        state.requestInvalidate()
      }
    }
  }

  override fun onGestureEnded(canceled: Boolean, pointerInputChanges: List<PointerInputChange>) {
    val pointerInputChange = pointerInputChanges.first()
    val endOffset = pointerInputChange.position
    val minVelocity = state.minFlingVelocityPxPerSecond
    val minDist = state.minFlingMoveDistPx

    if (
      !canceled &&
      state.isReadyForGestures &&
      !animatingFling &&
      isPanning &&
      coroutineScope != null &&
      ((endOffset.x - startOffset.x).absoluteValue > minDist || (endOffset.y - startOffset.y).absoluteValue > minDist)
    ) {
      val velocity = velocityTracker.calculateVelocity()

      if (velocity.x.absoluteValue > minVelocity || velocity.y.absoluteValue > minVelocity) {
        if (currentGestureAnimation != null) {
          return
        }

        animatingFling = true

        initAndStartFlingAnimation(
          debug = debug,
          velocityX = velocity.x,
          velocityY = velocity.y,
          pointerInputChanges = pointerInputChanges
        )

        return
      }

      // fallthrough
    }

    super.onGestureEnded(canceled, pointerInputChanges)

    vCenterStart.set(0f, 0f)
    vTranslateStart.set(0f, 0f)
    startOffset.set(0f, 0f)
    isPanning = false
    animatingFling = false
    velocityTracker.resetTracking()

    if (state.isReadyForGestures) {
      state.refreshRequiredTiles(load = true)
      state.requestInvalidate()
    }
  }

  private fun initAndStartFlingAnimation(
    debug: Boolean,
    velocityX: Float,
    velocityY: Float,
    pointerInputChanges: List<PointerInputChange>
  ) {
    currentGestureAnimation = GestureAnimation<PanAnimationParameters>(
      debug = debug,
      detectorType = detectorType,
      state = state,
      coroutineScope = coroutineScope!!,
      durationMs = state.flingAnimationDurationMs,
      animationUpdateIntervalMs = state.animationUpdateIntervalMs.toLong(),
      animationParams = {
        val currentScale = state.currentScale

        val vTranslateEnd = PointF(
          state._vTranslate.x + (velocityX * 0.25f),
          state._vTranslate.y + (velocityY * 0.25f)
        )
        val sCenterXEnd: Float = (state.viewWidth / 2 - vTranslateEnd.x) / currentScale
        val sCenterYEnd: Float = (state.viewHeight / 2 - vTranslateEnd.y) / currentScale

        val sCenter = PointF(sCenterXEnd, sCenterYEnd)

        val vxCenter = state.viewWidth / 2f
        val vyCenter = state.viewHeight / 2f

        val vFocusStart = state.sourceToViewCoord(sCenter)
        val vFocusEnd = PointF(vxCenter, vyCenter)

        return@GestureAnimation PanAnimationParameters(
          gestureAnimationEasing = GestureAnimationEasing.EaseOutQuad,
          startTime = SystemClock.elapsedRealtime(),
          startScale = currentScale,
          endScale = currentScale,
          sCenter = sCenter,
          sCenterEnd = sCenter,
          vFocusStart = vFocusStart,
          vFocusEnd = vFocusEnd,
        )
      },
      animationFunc = { params: PanAnimationParameters, _: Float, duration: Long ->
        val startScale = params.startScale
        val endScale = params.endScale

        var timeElapsed = SystemClock.elapsedRealtime() - params.startTime
        val finished = timeElapsed > duration
        timeElapsed = Math.min(timeElapsed, duration)

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

        animatingFling = false
      }
    )

    currentGestureAnimation!!.start()
  }

  private data class PanAnimationParameters(
    val gestureAnimationEasing: GestureAnimationEasing,
    val startTime: Long,
    val startScale: Float,
    val endScale: Float,
    val sCenter: PointF,
    val sCenterEnd: PointF,
    val vFocusStart: PointF,
    val vFocusEnd: PointF,
  )

}