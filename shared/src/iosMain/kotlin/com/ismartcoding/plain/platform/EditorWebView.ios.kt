package com.ismartcoding.plain.platform

actual fun evaluateEditorJavascript(webView: Any?, script: String, callback: ((String) -> Unit)?) {
    (webView as? IosEditorWebView)?.evaluateJavascript(script, callback)
}

actual fun destroyEditorWebView(webView: Any?) {
    (webView as? IosEditorWebView)?.destroy()
}
