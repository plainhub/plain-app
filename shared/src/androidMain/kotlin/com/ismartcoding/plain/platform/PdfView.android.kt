package com.ismartcoding.plain.platform

import com.ismartcoding.plain.preferences.*

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.pdfviewer.PDFView
import com.ismartcoding.plain.lib.pdfviewer.listener.OnPageErrorListener
import com.ismartcoding.plain.lib.pdfviewer.util.FitPolicy
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.preferences.LocalDarkTheme

@Composable
actual fun PdfView(
    uri: String,
    modifier: Modifier,
) {
    val parsedUri = Uri.parse(uri)
    val context = LocalContext.current
    val darkTheme = LocalDarkTheme.current
    val isDarkTheme = DarkTheme.isDarkTheme(darkTheme)
    AndroidView(
        modifier = modifier,
        factory = { factoryContext ->
            val v = PDFView(factoryContext)
            v.fromUri(parsedUri)
                .defaultPage(0)
                .enableAnnotationRendering(true)
                .scrollHandle(MinimalScrollHandle(factoryContext))
                .spacing(10)
                .onPageError(object : OnPageErrorListener {
                    override fun onPageError(
                        page: Int,
                        t: Throwable?,
                    ) {
                        LogCat.e(page.toString())
                    }
                })
                .pageFitPolicy(FitPolicy.BOTH)
                .nightMode(isDarkTheme)
                .load()
            v
        }
    )
}
