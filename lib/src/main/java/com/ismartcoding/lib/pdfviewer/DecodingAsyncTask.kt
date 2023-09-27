package com.ismartcoding.lib.pdfviewer

import android.os.AsyncTask
import com.ismartcoding.lib.pdfviewer.source.DocumentSource
import com.shockwave.pdfium.PdfiumCore
import com.shockwave.pdfium.util.Size
import java.lang.NullPointerException
import java.lang.ref.WeakReference

internal class DecodingAsyncTask(
    val docSource: DocumentSource,
    val password: String?,
    val userPages: IntArray?,
    val pdfViewReference: WeakReference<PDFView>,
    val pdfiumCore: PdfiumCore,
) : AsyncTask<Void?, Void?, Throwable?>() {
    private var cancelled: Boolean = false
    private var pdfFile: PdfFile? = null

    override fun doInBackground(vararg params: Void?): Throwable? {
        return try {
            val pdfView = pdfViewReference.get()
            if (pdfView != null) {
                val pdfDocument = docSource.createDocument(pdfView.context, pdfiumCore, password)
                pdfFile =
                    PdfFile(
                        pdfiumCore, pdfDocument, pdfView.pageFitPolicy, getViewSize(pdfView),
                        userPages, pdfView.isSwipeVertical, pdfView.spacingPx, pdfView.isAutoSpacingEnabled,
                        pdfView.isFitEachPage,
                    )
                null
            } else {
                NullPointerException("pdfView == null")
            }
        } catch (t: Throwable) {
            t
        }
    }

    private fun getViewSize(pdfView: PDFView): Size {
        return Size(pdfView.width, pdfView.height)
    }

    override fun onPostExecute(result: Throwable?) {
        val pdfView = pdfViewReference.get()
        if (pdfView != null) {
            if (result != null) {
                pdfView.loadError(result)
                return
            }
            if (!cancelled) {
                pdfView.loadComplete(pdfFile!!)
            }
        }
    }

    protected override fun onCancelled() {
        cancelled = true
    }
}
