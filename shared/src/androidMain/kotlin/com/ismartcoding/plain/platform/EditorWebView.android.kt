package com.ismartcoding.plain.platform

import android.webkit.WebView

/**
 * Android implementation of [EditorWebViewHandle] wrapping `android.webkit.WebView`.
 *
 * The underlying WebView is created and configured by the platform-specific
 * `AceEditor` Composable (which needs the parent Compose context for theming)
 * and then assigned to this handle via [webView]. Common code drives the
 * embedded Ace editor via [evaluateJavascript] without touching the Android
 * WebView type directly.
 */
actual class EditorWebViewHandle actual constructor() {
    /** The underlying Android WebView, assigned by the platform AceEditor Composable. */
    var webView: WebView? = null

    actual fun evaluateJavascript(script: String, callback: ((String) -> Unit)?) {
        val view = webView ?: return
        view.evaluateJavascript(script) { result ->
            callback?.invoke(result)
        }
    }

    actual fun destroy() {
        webView?.destroy()
        webView = null
    }
}
