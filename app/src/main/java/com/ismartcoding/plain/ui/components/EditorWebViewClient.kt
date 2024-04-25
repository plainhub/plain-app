package com.ismartcoding.plain.ui.components

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.JsonHelper.jsonEncode
import kotlinx.serialization.Serializable

@Serializable
data class EditorData(val language: String, val wrapContent: Boolean, val isDarkTheme: Boolean, val readOnly: Boolean, val gotoEnd: Boolean, val content: String)

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