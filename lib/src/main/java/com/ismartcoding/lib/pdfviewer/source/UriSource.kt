package com.ismartcoding.lib.pdfviewer.source

import android.content.Context
import android.net.Uri
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import java.io.IOException

class UriSource(private val uri: Uri) : DocumentSource {
    @Throws(IOException::class)
    override fun createDocument(
        context: Context,
        core: PdfiumCore,
        password: String?,
    ): PdfDocument {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
        return core.newDocument(pfd, password)
    }
}
