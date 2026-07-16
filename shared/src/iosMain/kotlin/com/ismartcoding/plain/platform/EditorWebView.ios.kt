package com.ismartcoding.plain.platform

/**
 * iOS stub implementation of [EditorWebViewHandle]. The iOS text editor uses a
 * different rendering path (no WebView), so all operations are no-ops and
 * [evaluateJavascript] callbacks are never invoked.
 */
actual class EditorWebViewHandle actual constructor() {
    actual fun evaluateJavascript(script: String, callback: ((String) -> Unit)?) {
        // No-op on iOS: editor rendering does not use a WebView.
    }

    actual fun destroy() {
        // No-op on iOS.
    }
}
