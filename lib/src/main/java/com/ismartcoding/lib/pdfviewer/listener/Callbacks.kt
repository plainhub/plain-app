package com.ismartcoding.lib.pdfviewer.listener

import android.view.MotionEvent
import com.ismartcoding.lib.pdfviewer.link.LinkHandler
import com.ismartcoding.lib.pdfviewer.model.LinkTapEvent

class Callbacks {
    /**
     * Call back object to call when the PDF is loaded
     */
    var onLoadComplete: OnLoadCompleteListener? = null

    /**
     * Call back object to call when document loading error occurs
     */
    var onError: OnErrorListener? = null

    /**
     * Call back object to call when the page load error occurs
     */
    var onPageError: OnPageErrorListener? = null

    /**
     * Call back object to call when the document is initially rendered
     */
    var onRender: OnRenderListener? = null

    /**
     * Call back object to call when the page has changed
     */
    var onPageChange: OnPageChangeListener? = null

    /**
     * Call back object to call when the page is scrolled
     */
    var onPageScroll: OnPageScrollListener? = null

    /**
     * Call back object to call when the above layer is to drawn
     */
    var onDraw: OnDrawListener? = null
    var onDrawAll: OnDrawListener? = null

    /**
     * Call back object to call when the user does a tap gesture
     */
    var onTap: OnTapListener? = null

    /**
     * Call back object to call when the user does a long tap gesture
     */
    var onLongPress: OnLongPressListener? = null

    /**
     * Call back object to call when clicking link
     */
    var linkHandler: LinkHandler? = null

    fun callOnLoadComplete(pagesCount: Int) {
        onLoadComplete?.loadComplete(pagesCount)
    }

    fun callOnPageError(
        page: Int,
        error: Throwable?,
    ): Boolean {
        if (onPageError != null) {
            onPageError?.onPageError(page, error)
            return true
        }
        return false
    }

    fun callOnRender(pagesCount: Int) {
        onRender?.onInitiallyRendered(pagesCount)
    }

    fun callOnPageChange(
        page: Int,
        pagesCount: Int,
    ) {
        onPageChange?.onPageChanged(page, pagesCount)
    }

    fun callOnPageScroll(
        currentPage: Int,
        offset: Float,
    ) {
        onPageScroll?.onPageScrolled(currentPage, offset)
    }

    fun callOnTap(event: MotionEvent): Boolean {
        return onTap?.onTap(event) == true
    }

    fun callOnLongPress(event: MotionEvent) {
        onLongPress?.onLongPress(event)
    }

    fun callLinkHandler(event: LinkTapEvent) {
        linkHandler?.handleLinkEvent(event)
    }
}
