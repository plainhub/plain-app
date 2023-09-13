package com.ismartcoding.plain.ui.views

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.AttributeSet
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.helpers.WebHelper
import java.io.File
import java.io.IOException


class HtmlView(context: Context, attrs: AttributeSet? = null) : WebView(context, attrs) {

    private val TEXT_HTML = "text/html"
    private val HTML_IMG_REGEX = "(?i)<[/]?[ ]?img(.|\n)*?>"
    private val BACKGROUND_COLOR = colorString(R.color.canvas)
    private val QUOTE_BACKGROUND_COLOR = colorString(R.color.secondary)
    private val QUOTE_LEFT_COLOR = colorString(R.color.secondary)
    private val TEXT_COLOR = colorString(R.color.primary)
    private val CSS = "<head><style type='text/css'> " +
            "body {max-width: 100%; margin: 0.3cm; font-family: sans-serif-light; color: " + TEXT_COLOR + "; background-color:" + BACKGROUND_COLOR + "; line-height: 150%} " +
            "* {max-width: 100%; word-break: break-word}" +
            "h1, h2 {font-weight: normal; line-height: 130%} " +
            "h1 {font-size: 170%; margin-bottom: 0.1em} " +
            "h2 {font-size: 140%} " +
            "a {color: #0099CC}" +
            "h1 a {color: inherit; text-decoration: none}" +
            "img {height: auto} " +
            "pre {white-space: pre-wrap; direction: ltr;} " +
            "blockquote {border-left: thick solid " + QUOTE_LEFT_COLOR + "; background-color:" + QUOTE_BACKGROUND_COLOR + "; margin: 0.5em 0 0.5em 0em; padding: 0.5em} " +
            "p {margin: 0.8em 0 0.8em 0} " +
            "ul, ol {margin: 0 0 0.8em 0.6em; padding: 0 0 0 1em} " +
            "ul li, ol li {margin: 0 0 0.8em 0; padding: 0} " +
            "</style><meta name='viewport' content='width=device-width'/></head>"
    private val BODY_START = "<body>"
    private val BODY_END = "</body>"

    init {

        // For scrolling
        isHorizontalScrollBarEnabled = false
        settings.useWideViewPort = false
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.allowFileAccess = true

        @SuppressLint("SetJavaScriptEnabled")
        settings.javaScriptEnabled = true

        // For color
        setBackgroundColor(Color.parseColor(BACKGROUND_COLOR))

        webViewClient = object : WebViewClient() {

            @Suppress("OverridingDeprecatedMember")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                try {
                    if (url.startsWith("file://")) {
                        val file = File(url.replace("file://", ""))
                        val contentUri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(contentUri, "image/jpeg")
                        context.startActivity(intent)
                    } else {
                        WebHelper.open(context, url)
                    }
                } catch (e: ActivityNotFoundException) {
//                    Toast.makeText(context, R.string.cant_open_link, Toast.LENGTH_SHORT).show()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                return true
            }
        }
    }

    fun setEntry(content: String) {
        if (settings.blockNetworkImage) {
            // setBlockNetworkImage(false) calls postSync, which takes time, so we clean up the html first and change the value afterwards
            loadData("", TEXT_HTML, "UTF-8")
            settings.blockNetworkImage = false
        }

        val html = StringBuilder(CSS)
            .append(BODY_START)
            .append(content)
            .append(BODY_END)
            .toString()

        // do not put 'null' to the base url...
        loadDataWithBaseURL("", html, TEXT_HTML, "UTF-8", null)

        // display top of article
        ObjectAnimator.ofInt(this@HtmlView, "scrollY", scrollY, 0).setDuration(500).start()
    }

    private fun colorString(resourceInt: Int): String {
        val color = context.getColor(resourceInt)
        return String.format("#%06X", 0xFFFFFF and color)
    }
}