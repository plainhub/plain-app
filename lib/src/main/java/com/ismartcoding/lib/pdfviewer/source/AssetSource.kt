package com.ismartcoding.lib.pdfviewer.source

import android.content.Context
import android.os.ParcelFileDescriptor
import com.ismartcoding.lib.pdfviewer.util.FileUtils
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import java.io.IOException

class AssetSource(private val assetName: String) : DocumentSource {
    @Throws(IOException::class)
    override fun createDocument(
        context: Context,
        core: PdfiumCore,
        password: String?,
    ): PdfDocument {
        val f = FileUtils.fileFromAsset(context, assetName)
        val pfd = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
        return core.newDocument(pfd, password)
    }
}
