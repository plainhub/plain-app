package com.ismartcoding.plain.ui.platform

import androidx.compose.runtime.Composable

// Android requires native ClipData to preserve the notification label.
@Composable
internal expect fun rememberClipboardWriter(): (label: String, text: String) -> Unit

@Composable
internal expect fun rememberClipboardReader(): suspend () -> String?
