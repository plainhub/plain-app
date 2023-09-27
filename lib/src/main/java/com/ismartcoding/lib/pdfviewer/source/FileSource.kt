package com.ismartcoding.lib.pdfviewer.source

import android.content.Context
import android.os.ParcelFileDescriptor
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import java.io.File
import java.io.IOException

class FileSource(private val file: File) : DocumentSource {
    @Throws(IOException::class)
    override fun createDocument(
        context: Context,
        core: PdfiumCore,
        password: String?,
    ): PdfDocument {
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return core.newDocument(pfd, password)
    }
}
