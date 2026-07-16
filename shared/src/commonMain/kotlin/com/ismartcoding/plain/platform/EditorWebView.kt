package com.ismartcoding.plain.platform

/**
 * Platform-agnostic handle to the underlying editor WebView used by the
 * text/markdown editor. On Android this wraps `android.webkit.WebView`; on
 * iOS it is a no-op stub (the iOS editor uses a different rendering path).
 *
 * Common code holds an instance of this handle and calls [evaluateJavascript]
 * to drive the embedded Ace editor. The handle is owned by the caller and
 * must be released via [destroy] when no longer needed.
 */
expect class EditorWebViewHandle() {
    /**
     * Evaluate [script] in the underlying web view. The optional [callback]
     * is invoked with the script result (as a JSON string) on completion.
     */
    fun evaluateJavascript(script: String, callback: ((String) -> Unit)? = null)

    /** Release the underlying web view resources. Safe to call multiple times. */
    fun destroy()
}
