package com.ismartcoding.plain.platform

actual fun evaluateEditorJavascript(webView: Any?, script: String, callback: ((String) -> Unit)?) {
    // No-op on iOS: editor rendering does not use a WebView.
}

actual fun destroyEditorWebView(webView: Any?) {
    // No-op on iOS.
}
