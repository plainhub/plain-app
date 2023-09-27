package com.ismartcoding.lib.pdfviewer

import android.graphics.PointF
import android.view.*
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.View.OnTouchListener
import com.ismartcoding.lib.pdfviewer.model.LinkTapEvent
import com.ismartcoding.lib.pdfviewer.scroll.ScrollHandle
import com.ismartcoding.lib.pdfviewer.util.Constants.PINCH_MAXIMUM_ZOOM
import com.ismartcoding.lib.pdfviewer.util.Constants.PINCH_MINIMUM_ZOOM
import java.lang.Integer.max
import kotlin.math.abs
import kotlin.math.min

/**
 * This Manager takes care of moving the PDFView,
 * set its zoom track user actions.
 */
internal class DragPinchManager(
    private val pdfView: PDFView,
    private val animationManager: AnimationManager,
) : GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener,
    OnScaleGestureListener,
    OnTouchListener {
    private val gestureDetector = GestureDetector(pdfView.context, this)
    private val scaleGestureDetector = ScaleGestureDetector(pdfView.context, this)
    private var scrolling = false
    private var scaling = false
    var enabled = false

    init {
        pdfView.setOnTouchListener(this)
    }

    fun disableLongPress() {
        gestureDetector.setIsLongpressEnabled(false)
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        val onTapHandled: Boolean = pdfView.callbacks.callOnTap(e)
        val linkTapped = checkLinkTapped(e.x, e.y)
        if (!onTapHandled && !linkTapped) {
            val ps: ScrollHandle? = pdfView.scrollHandle
            if (ps != null && !pdfView.documentFitsView()) {
                if (!ps.shown()) {
                    ps.show()
                } else {
                    ps.hide()
                }
            }
        }
        pdfView.performClick()
        return true
    }

    private fun checkLinkTapped(
        x: Float,
        y: Float,
    ): Boolean {
        val pdfFile = pdfView.pdfFile ?: return false
        val mappedX = -pdfView.currentXOffset + x
        val mappedY = -pdfView.currentYOffset + y
        val page = pdfFile.getPageAtOffset(if (pdfView.isSwipeVertical) mappedY else mappedX, pdfView.zoom)
        val pageSize = pdfFile.getScaledPageSize(page, pdfView.zoom)
        val pageX: Int
        val pageY: Int
        if (pdfView.isSwipeVertical) {
            pageX = pdfFile.getSecondaryPageOffset(page, pdfView.zoom).toInt()
            pageY = pdfFile.getPageOffset(page, pdfView.zoom).toInt()
        } else {
            pageY = pdfFile.getSecondaryPageOffset(page, pdfView.zoom).toInt()
            pageX = pdfFile.getPageOffset(page, pdfView.zoom).toInt()
        }
        for (link in pdfFile.getPageLinks(page)) {
            val mapped = pdfFile.mapRectToDevice(page, pageX, pageY, pageSize.width.toInt(), pageSize.height.toInt(), link.bounds)
            mapped.sort()
            if (mapped.contains(mappedX, mappedY)) {
                pdfView.callbacks.callLinkHandler(LinkTapEvent(x, y, mappedX, mappedY, mapped, link))
                return true
            }
        }
        return false
    }

    private fun startPageFling(
        downEvent: MotionEvent,
        ev: MotionEvent,
        velocityX: Float,
        velocityY: Float,
    ) {
        if (!checkDoPageFling(velocityX, velocityY)) {
            return
        }
        val direction =
            if (pdfView.isSwipeVertical) {
                if (velocityY > 0) -1 else 1
            } else {
                if (velocityX > 0) -1 else 1
            }
        // get the focused page during the down event to ensure only a single page is changed
        val delta = if (pdfView.isSwipeVertical) ev.y - downEvent.y else ev.x - downEvent.x
        val offsetX = pdfView.currentXOffset - delta * pdfView.zoom
        val offsetY = pdfView.currentYOffset - delta * pdfView.zoom
        val startingPage = pdfView.findFocusPage(offsetX, offsetY)
        val targetPage = max(0, min(pdfView.pageCount - 1, startingPage + direction))
        val edge = pdfView.findSnapEdge(targetPage)
        val offset = pdfView.snapOffsetForPage(targetPage, edge)
        animationManager.startPageFlingAnimation(-offset)
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        if (!pdfView.isDoubletapEnabled) {
            return false
        }
        if (pdfView.zoom < pdfView.midZoom) {
            pdfView.zoomWithAnimation(e.x, e.y, pdfView.midZoom)
        } else if (pdfView.zoom < pdfView.maxZoom) {
            pdfView.zoomWithAnimation(e.x, e.y, pdfView.maxZoom)
        } else {
            pdfView.resetZoomWithAnimation()
        }
        return true
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        return false
    }

    override fun onDown(e: MotionEvent): Boolean {
        animationManager.stopFling()
        return true
    }

    override fun onShowPress(e: MotionEvent) {}

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return false
    }

    override fun onScroll(
        p0: MotionEvent?,
        p1: MotionEvent,
        p2: Float,
        p3: Float,
    ): Boolean {
        scrolling = true
        if (pdfView.isZooming || pdfView.isSwipeEnabled) {
            pdfView.moveRelativeTo(-p2, -p3)
        }
        if (!scaling || pdfView.doRenderDuringScale()) {
            pdfView.loadPageByOffset()
        }
        return true
    }

    private fun onScrollEnd(event: MotionEvent) {
        pdfView.loadPages()
        hideHandle()
        if (!animationManager.isFlinging()) {
            pdfView.performPageSnap()
        }
    }

    override fun onLongPress(e: MotionEvent) {
        pdfView.callbacks.callOnLongPress(e)
    }

    override fun onFling(
        p0: MotionEvent?,
        p1: MotionEvent,
        p2: Float,
        p3: Float,
    ): Boolean {
        if (!pdfView.isSwipeEnabled) {
            return false
        }
        if (pdfView.isPageFlingEnabled) {
            if (pdfView.pageFillsScreen()) {
                onBoundedFling(p2, p3)
            } else {
                startPageFling(p0!!, p1, p2, p3)
            }
            return true
        }
        val xOffset = pdfView.currentXOffset.toInt()
        val yOffset = pdfView.currentYOffset.toInt()
        val minX: Float
        val minY: Float
        val pdfFile = pdfView.pdfFile!!
        if (pdfView.isSwipeVertical) {
            minX = -(pdfView.toCurrentScale(pdfFile.maxPageWidth) - pdfView.width)
            minY = -(pdfFile.getDocLen(pdfView.zoom) - pdfView.height)
        } else {
            minX = -(pdfFile.getDocLen(pdfView.zoom) - pdfView.width)
            minY = -(pdfView.toCurrentScale(pdfFile.maxPageHeight) - pdfView.height)
        }
        animationManager.startFlingAnimation(xOffset, yOffset, p2.toInt(), p3.toInt(), minX.toInt(), 0, minY.toInt(), 0)
        return true
    }

    private fun onBoundedFling(
        velocityX: Float,
        velocityY: Float,
    ) {
        val xOffset = pdfView.currentXOffset.toInt()
        val yOffset = pdfView.currentYOffset.toInt()
        val pdfFile = pdfView.pdfFile
        val pageStart = -pdfFile!!.getPageOffset(pdfView.currentPage, pdfView.zoom)
        val pageEnd = pageStart - pdfFile.getPageLength(pdfView.currentPage, pdfView.zoom)
        val minX: Float
        val minY: Float
        val maxX: Float
        val maxY: Float
        if (pdfView.isSwipeVertical) {
            minX = -(pdfView.toCurrentScale(pdfFile.maxPageWidth) - pdfView.width)
            minY = pageEnd + pdfView.height
            maxX = 0f
            maxY = pageStart
        } else {
            minX = pageEnd + pdfView.width
            minY = -(pdfView.toCurrentScale(pdfFile.maxPageHeight) - pdfView.height)
            maxX = pageStart
            maxY = 0f
        }
        animationManager.startFlingAnimation(
            xOffset,
            yOffset,
            velocityX.toInt(),
            velocityY.toInt(),
            minX.toInt(),
            maxX.toInt(),
            minY.toInt(),
            maxY.toInt(),
        )
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        var dr = detector.scaleFactor
        val wantedZoom = pdfView.zoom * dr
        val minZoom = min(PINCH_MINIMUM_ZOOM, pdfView.minZoom)
        val maxZoom = min(PINCH_MAXIMUM_ZOOM, pdfView.maxZoom)
        if (wantedZoom < minZoom) {
            dr = minZoom / pdfView.zoom
        } else if (wantedZoom > maxZoom) {
            dr = maxZoom / pdfView.zoom
        }
        pdfView.zoomCenteredRelativeTo(dr, PointF(detector.focusX, detector.focusY))
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        scaling = true
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        pdfView.loadPages()
        hideHandle()
        scaling = false
    }

    override fun onTouch(
        v: View,
        event: MotionEvent,
    ): Boolean {
        if (!enabled) {
            return false
        }
        var retVal = scaleGestureDetector.onTouchEvent(event)
        retVal = gestureDetector.onTouchEvent(event) || retVal
        if (event.action == MotionEvent.ACTION_UP) {
            if (scrolling) {
                scrolling = false
                onScrollEnd(event)
            }
        }
        return retVal
    }

    private fun hideHandle() {
        val scrollHandle: ScrollHandle? = pdfView.scrollHandle
        if (scrollHandle != null && scrollHandle.shown()) {
            scrollHandle.hideDelayed()
        }
    }

    private fun checkDoPageFling(
        velocityX: Float,
        velocityY: Float,
    ): Boolean {
        val absX = abs(velocityX)
        val absY = abs(velocityY)
        return if (pdfView.isSwipeVertical) absY > absX else absX > absY
    }
}
