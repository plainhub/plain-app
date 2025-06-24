package com.ismartcoding.plain.ui.base

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.lib.pdfviewer.PDFView
import com.ismartcoding.lib.pdfviewer.listener.OnPageErrorListener
import com.ismartcoding.lib.pdfviewer.scroll.DefaultScrollHandle
import com.ismartcoding.lib.pdfviewer.util.FitPolicy
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.preferences.LocalDarkTheme

@Composable
fun PdfView(
    uri: Uri,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val darkTheme = LocalDarkTheme.current
    val isDarkTheme = DarkTheme.isDarkTheme(darkTheme)
    AndroidView(
        modifier = modifier,
        factory = { factoryContext ->
            val v = PDFView(factoryContext)
            v.fromUri(uri)
                .defaultPage(0)
                .enableAnnotationRendering(true)
                .scrollHandle(DefaultScrollHandle(context))
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
