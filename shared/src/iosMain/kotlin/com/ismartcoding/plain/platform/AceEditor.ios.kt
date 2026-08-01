@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.viewinterop.UIKitView
import com.ismartcoding.plain.enums.Language
import com.ismartcoding.plain.ui.components.EditorData
import com.ismartcoding.plain.ui.models.TextFileViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
actual fun AceEditor(
    textFileVM: TextFileViewModel,
    scope: CoroutineScope,
    data: EditorData,
) {
    DisposableEffect(Unit) {
        onDispose {
            textFileVM.webView.value?.destroy()
            textFileVM.webView.value = null
        }
    }

    UIKitView(
        factory = {
            IosEditorWebView(
                data = data,
                onReady = { textFileVM.isEditorReady.value = true },
                onUpdate = { c -> textFileVM.content.value = c },
            ).also { editor ->
                val handle = EditorWebViewHandle()
                handle.webView = editor
                textFileVM.webView.value = handle
                scope.launch(Dispatchers.Default) {
                    Language.initLocaleAsync()
                }
            }.webView
        },
    )
}
