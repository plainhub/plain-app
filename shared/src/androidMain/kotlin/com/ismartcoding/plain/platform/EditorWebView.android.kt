package com.ismartcoding.plain.platform

import android.webkit.WebView

actual fun evaluateEditorJavascript(webView: Any?, script: String, callback: ((String) -> Unit)?) {
    (webView as? WebView)?.evaluateJavascript(script) { result ->
        callback?.invoke(result)
    }
}

actual fun destroyEditorWebView(webView: Any?) {
    (webView as? WebView)?.destroy()
}
