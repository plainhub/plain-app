package com.ismartcoding.plain.ui.components

import android.webkit.JavascriptInterface

class EditorWebViewInterface(val ready: () -> Unit, val update: (String) -> Unit) {
    @JavascriptInterface
    fun updateContent(code: String) {
        update(code)
    }

    @JavascriptInterface
    fun editorReady() {
        ready()
    }
}