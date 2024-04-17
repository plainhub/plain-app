package com.ismartcoding.plain.ui.components

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.JsonHelper.jsonEncode
import kotlinx.serialization.Serializable


class EditorWebViewClient(
    val context: Context,
    val content: String,
    val language: String,
    val wrapContent: Boolean,
    val isDarkTheme: Boolean,
    val readOnly: Boolean,
) : WebViewClient() {
    override fun onPageFinished(view: WebView, url: String?) {
        coMain {
            val json = withIO { jsonEncode(EditorData(language, wrapContent, isDarkTheme, readOnly, content)) }
            view.evaluateJavascript("loadEditor(${json})") {}
        }
    }

    @Serializable
    data class EditorData(val language: String, val wrapContent: Boolean, val isDarkTheme: Boolean, val readOnly: Boolean, val content: String)
}