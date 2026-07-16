package com.ismartcoding.plain.platform

import android.webkit.JavascriptInterface

class EditorWebViewInterface(val ready: () -> Unit, val update: (String) -> Unit) {
    @JavascriptInterface
    fun updateContent(content: String) {
        update(content)
    }

    @JavascriptInterface
    fun editorReady() {
        ready()
    }
}
