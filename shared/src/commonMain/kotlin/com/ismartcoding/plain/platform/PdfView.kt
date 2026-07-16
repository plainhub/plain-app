package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific PDF viewer composable.
 * Android: PDFView from lib.pdfviewer; iOS: not supported (no-op).
 *
 * @param uri URI string of the PDF document (e.g. content://... or file://...).
 */
@Composable
expect fun PdfView(uri: String, modifier: Modifier = Modifier)
