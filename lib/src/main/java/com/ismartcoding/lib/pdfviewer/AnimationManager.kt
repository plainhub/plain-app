package com.ismartcoding.lib.pdfviewer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.graphics.PointF
import android.view.animation.DecelerateInterpolator
import android.widget.OverScroller

/**
 * This manager is used by the PDFView to launch animations.
 * It uses the ValueAnimator appeared in API 11 to start
 * an animation, and call moveTo() on the PDFView as a result
 * of each animation update.
 */
internal class AnimationManager(private val pdfView: PDFView) {
    private var animation: ValueAnimator? = null
    private val scroller: OverScroller = OverScroller(pdfView.context)
    private var flinging = false
    private var pageFlinging = false

    fun startXAnimation(
        xFrom: Float,
        xTo: Float,
    ) {
        stopAll()
        val xAnimation = XAnimation()
        animation =
            ValueAnimator.ofFloat(xFrom, xTo).apply {
                interpolator = DecelerateInterpolator()
                addListener(xAnimation)
                addUpdateListener(xAnimation)
                duration = 400
                start()
            }
    }

    fun startYAnimation(
        yFrom: Float,
        yTo: Float,
    ) {
        stopAll()
        val yAnimation = YAnimation()
        animation =
            ValueAnimator.ofFloat(yFrom, yTo).apply {
                interpolator = DecelerateInterpolator()
                addUpdateListener(yAnimation)
                addListener(yAnimation)
                duration = 400
                start()
            }
    }

    fun startZoomAnimation(
        centerX: Float,
        centerY: Float,
        zoomFrom: Float,
        zoomTo: Float,
    ) {
        stopAll()
        val zoomAnim = ZoomAnimation(centerX, centerY)
        animation =
            ValueAnimator.ofFloat(zoomFrom, zoomTo).apply {
                interpolator = DecelerateInterpolator()
                addUpdateListener(zoomAnim)
                addListener(zoomAnim)
                duration = 400
                start()
            }
    }

    fun startFlingAnimation(
        startX: Int,
        startY: Int,
        velocityX: Int,
        velocityY: Int,
        minX: Int,
        maxX: Int,
        minY: Int,
        maxY: Int,
    ) {
        stopAll()
        flinging = true
        scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY)
    }

    fun startPageFlingAnimation(targetOffset: Float) {
        if (pdfView.isSwipeVertical) {
            startYAnimation(pdfView.currentYOffset, targetOffset)
        } else {
            startXAnimation(pdfView.currentXOffset, targetOffset)
        }
        pageFlinging = true
    }

    fun computeFling() {
        if (scroller.computeScrollOffset()) {
            pdfView.moveTo(scroller.currX.toFloat(), scroller.currY.toFloat())
            pdfView.loadPageByOffset()
        } else if (flinging) { // fling finished
            flinging = false
            pdfView.loadPages()
            hideHandle()
            pdfView.performPageSnap()
        }
    }

    fun stopAll() {
        animation?.cancel()
        animation = null
        stopFling()
    }

    fun stopFling() {
        flinging = false
        scroller.forceFinished(true)
    }

    fun isFlinging(): Boolean {
        return flinging || pageFlinging
    }

    internal inner class XAnimation : AnimatorListenerAdapter(), AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val offset = animation.animatedValue as Float
            pdfView.moveTo(offset, pdfView.currentYOffset)
            pdfView.loadPageByOffset()
        }

        override fun onAnimationCancel(animation: Animator) {
            pdfView.loadPages()
            pageFlinging = false
            hideHandle()
        }

        override fun onAnimationEnd(animation: Animator) {
            pdfView.loadPages()
            pageFlinging = false
            hideHandle()
        }
    }

    internal inner class YAnimation : AnimatorListenerAdapter(), AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val offset = animation.animatedValue as Float
            pdfView.moveTo(pdfView.currentXOffset, offset)
            pdfView.loadPageByOffset()
        }

        override fun onAnimationCancel(animation: Animator) {
            pdfView.loadPages()
            pageFlinging = false
            hideHandle()
        }

        override fun onAnimationEnd(animation: Animator) {
            pdfView.loadPages()
            pageFlinging = false
            hideHandle()
        }
    }

    internal inner class ZoomAnimation(private val centerX: Float, private val centerY: Float) : AnimatorUpdateListener, Animator.AnimatorListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val zoom = animation.animatedValue as Float
            pdfView.zoomCenteredTo(zoom, PointF(centerX, centerY))
        }

        override fun onAnimationCancel(animation: Animator) {
            pdfView.loadPages()
            hideHandle()
        }

        override fun onAnimationEnd(animation: Animator) {
            pdfView.loadPages()
            pdfView.performPageSnap()
            hideHandle()
        }

        override fun onAnimationRepeat(animation: Animator) {}

        override fun onAnimationStart(animation: Animator) {}
    }

    private fun hideHandle() {
        pdfView.scrollHandle?.hideDelayed()
    }
}
