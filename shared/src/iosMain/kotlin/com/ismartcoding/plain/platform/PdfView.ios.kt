package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun PdfView(uri: String, modifier: Modifier) {
    // no-op - PDF rendering not supported on iOS
}
