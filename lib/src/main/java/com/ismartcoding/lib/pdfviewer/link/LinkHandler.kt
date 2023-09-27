package com.ismartcoding.lib.pdfviewer.link

import com.ismartcoding.lib.pdfviewer.model.LinkTapEvent

interface LinkHandler {
    /**
     * Called when link was tapped by user
     *
     * @param event current event
     */
    fun handleLinkEvent(event: LinkTapEvent)
}
