package com.ismartcoding.lib.pdfviewer.link

import android.content.Intent
import android.net.Uri
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.lib.pdfviewer.PDFView
import com.ismartcoding.lib.pdfviewer.model.LinkTapEvent

class DefaultLinkHandler(private val pdfView: PDFView) : LinkHandler {
    override fun handleLinkEvent(event: LinkTapEvent) {
        val uri = event.link.uri
        val page = event.link.destPageIdx
        if (uri != null && uri.isNotEmpty()) {
            handleUri(uri)
        } else {
            page?.let { handlePage(it) }
        }
    }

    private fun handleUri(uri: String) {
        val parsedUri = Uri.parse(uri)
        val intent = Intent(Intent.ACTION_VIEW, parsedUri)
        val context = pdfView.context
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            LogCat.e("No activity found for URI: $uri")
        }
    }

    private fun handlePage(page: Int) {
        pdfView.jumpTo(page)
    }
}
