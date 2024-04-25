package com.ismartcoding.plain.ui.base

import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.viewinterop.AndroidView
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.enums.Language
import com.ismartcoding.plain.ui.components.EditorData
import com.ismartcoding.plain.ui.components.EditorWebViewClient
import com.ismartcoding.plain.ui.components.EditorWebViewInterface
import com.ismartcoding.plain.ui.models.TextFileViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun AceEditor(
    viewModel: TextFileViewModel,
    scope: CoroutineScope,
    data: EditorData,
) {
    DisposableEffect(Unit) {
        onDispose {
            viewModel.webView.value?.destroy()
            viewModel.webView.value = null
        }
    }

    AndroidView(factory = {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        val webView = WebView(it)
        webView.apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            webViewClient = EditorWebViewClient(
                context,
                data
            )
            addJavascriptInterface(
                EditorWebViewInterface(
                    ready = {
                        viewModel.isEditorReady.value = true
                    },
                    update = { c ->
                        viewModel.content.value = c
                    }), "AndroidApp"
            )
            settings.domStorageEnabled = true
            loadUrl("file:///android_asset/editor/index.html")
            scope.launch(Dispatchers.IO) {
                Language.initLocaleAsync(context)
            }
        }
        viewModel.webView.value = webView
        webView
    })
}