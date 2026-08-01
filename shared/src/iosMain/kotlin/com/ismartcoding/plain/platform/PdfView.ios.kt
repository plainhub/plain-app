@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.PDFKit.PDFDocument
import platform.PDFKit.PDFView
import platform.PDFKit.kPDFDisplayDirectionVertical
import platform.PDFKit.kPDFDisplaySinglePageContinuous

@Composable
actual fun PdfView(uri: String, modifier: Modifier) {
    UIKitView(
        modifier = modifier,
        factory = {
            PDFView().apply {
                setAutoScales(true)
                setDisplayMode(kPDFDisplaySinglePageContinuous)
                setDisplayDirection(kPDFDisplayDirectionVertical)
                setDocument(PDFDocument(NSURL.fileURLWithPath(uri)))
            }
        },
    )
}
