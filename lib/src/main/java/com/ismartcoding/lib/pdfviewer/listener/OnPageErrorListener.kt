package com.ismartcoding.lib.pdfviewer.listener

interface OnPageErrorListener {
    /**
     * Called if error occurred while loading PDF page
     * @param t Throwable with error
     */
    fun onPageError(
        page: Int,
        t: Throwable?,
    )
}
