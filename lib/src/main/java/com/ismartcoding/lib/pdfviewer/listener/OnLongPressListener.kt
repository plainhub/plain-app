package com.ismartcoding.lib.pdfviewer.listener

import android.view.MotionEvent

/**
 * Implement this interface to receive events from PDFView
 * when view has been long pressed
 */
interface OnLongPressListener {
    /**
     * Called when the user has a long tap gesture, before processing scroll handle toggling
     *
     * @param e MotionEvent that registered as a confirmed long press
     */
    fun onLongPress(e: MotionEvent)
}
