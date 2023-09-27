package com.ismartcoding.lib.pdfviewer.source

import android.content.Context
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import java.io.IOException

interface DocumentSource {
    @Throws(IOException::class)
    fun createDocument(
        context: Context,
        core: PdfiumCore,
        password: String?,
    ): PdfDocument
}
