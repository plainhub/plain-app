package com.ismartcoding.plain.platform

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ismartcoding.plain.helpers.coMain
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.helpers.JsonHelper.jsonEncode
import com.ismartcoding.plain.ui.components.EditorData

class EditorWebViewClient(
    val context: Context,
    val data: EditorData,
) : WebViewClient() {
    override fun onPageFinished(view: WebView, url: String?) {
        coMain {
            val json = withIO { jsonEncode(data) }
            view.evaluateJavascript("loadEditor(${json})") {}
        }
    }
}
