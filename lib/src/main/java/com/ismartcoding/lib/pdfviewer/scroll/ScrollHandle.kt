package com.ismartcoding.lib.pdfviewer.scroll

import com.ismartcoding.lib.pdfviewer.PDFView

interface ScrollHandle {
    /**
     * Used to move the handle, called internally by PDFView
     *
     * @param position current scroll ratio between 0 and 1
     */
    fun setScroll(position: Float)

    /**
     * Method called by PDFView after setting scroll handle.
     * Do not call this method manually.
     * For usage sample see [DefaultScrollHandle]
     *
     * @param pdfView PDFView instance
     */
    fun setupLayout(pdfView: PDFView)

    /**
     * Method called by PDFView when handle should be removed from layout
     * Do not call this method manually.
     */
    fun destroyLayout()

    /**
     * Set page number displayed on handle
     *
     * @param pageNum page number
     */
    fun setPageNum(pageNum: Int)

    /**
     * Get handle visibility
     *
     * @return true if handle is visible, false otherwise
     */
    fun shown(): Boolean

    /**
     * Show handle
     */
    fun show()

    /**
     * Hide handle immediately
     */
    fun hide()

    /**
     * Hide handle after some time (defined by implementation)
     */
    fun hideDelayed()
}
