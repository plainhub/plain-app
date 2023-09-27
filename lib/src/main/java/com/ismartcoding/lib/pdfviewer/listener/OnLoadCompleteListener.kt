package com.ismartcoding.lib.pdfviewer.listener

/**
 * Implement this interface to receive events from PDFView
 * when loading is complete.
 */
interface OnLoadCompleteListener {
    /**
     * Called when the PDF is loaded
     * @param nbPages the number of pages in this PDF file
     */
    fun loadComplete(nbPages: Int)
}
