package com.ismartcoding.plain.lib.pdfviewer.link

import com.ismartcoding.plain.lib.pdfviewer.model.LinkTapEvent

interface LinkHandler {
    /**
     * Called when link was tapped by user
     *
     * @param event current event
     */
    fun handleLinkEvent(event: LinkTapEvent)
}
