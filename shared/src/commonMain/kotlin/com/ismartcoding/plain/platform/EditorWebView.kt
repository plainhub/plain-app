package com.ismartcoding.plain.platform

class EditorWebViewHandle {
    var webView: Any? = null

    fun evaluateJavascript(script: String, callback: ((String) -> Unit)? = null) {
        evaluateEditorJavascript(webView, script, callback)
    }

    fun destroy() {
        destroyEditorWebView(webView)
        webView = null
    }
}

expect fun evaluateEditorJavascript(webView: Any?, script: String, callback: ((String) -> Unit)?)

expect fun destroyEditorWebView(webView: Any?)
