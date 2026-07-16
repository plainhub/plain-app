package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.ui.components.EditorData
import com.ismartcoding.plain.ui.models.TextFileViewModel
import kotlinx.coroutines.CoroutineScope

/**
 * Platform-specific Ace editor composable.
 * Android: WebView wrapping the Ace editor assets; iOS: not supported (no-op).
 */
@Composable
expect fun AceEditor(textFileVM: TextFileViewModel, scope: CoroutineScope, data: EditorData)
