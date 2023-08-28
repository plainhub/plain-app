package com.ismartcoding.plain.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.extensions.getFileName
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.lib.pdfviewer.listener.OnPageErrorListener
import com.ismartcoding.lib.pdfviewer.scroll.DefaultScrollHandle
import com.ismartcoding.lib.pdfviewer.util.FitPolicy
import com.ismartcoding.plain.databinding.DialogPdfViewerBinding
import com.ismartcoding.plain.ui.extensions.onBack
import kotlinx.coroutines.launch

class PdfViewerDialog(val uri: Uri) : BaseDialog<DialogPdfViewerBinding>(),
    OnPageErrorListener {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topAppBar.run {
            title = uri.getFileName(requireContext())
            onBack {
                dismiss()
            }
        }

        lifecycleScope.launch {
            binding.pdfView.fromUri(uri)
                .defaultPage(0)
                .enableAnnotationRendering(true)
                .scrollHandle(DefaultScrollHandle(requireContext()))
                .spacing(10)
                .onPageError(this@PdfViewerDialog)
                .pageFitPolicy(FitPolicy.BOTH)
                .load()
        }
    }

    override fun onPageError(page: Int, t: Throwable?) {
        LogCat.e(page.toString())
    }
}