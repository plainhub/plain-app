package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.ui.components.EditorData
import com.ismartcoding.plain.ui.models.TextFileViewModel
import kotlinx.coroutines.CoroutineScope

@Composable
actual fun AceEditor(textFileVM: TextFileViewModel, scope: CoroutineScope, data: EditorData) {
    // no-op - WebView not supported on iOS
}
