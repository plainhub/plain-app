package com.ismartcoding.plain.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.webkit.WebSettings.*
import androidx.core.view.isVisible
import com.ismartcoding.lib.extensions.*
import com.ismartcoding.plain.R
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.databinding.DialogWebBinding
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.extensions.initMenu
import com.ismartcoding.plain.ui.extensions.onBack
import com.ismartcoding.plain.ui.extensions.onMenuItemClick
import com.ismartcoding.plain.ui.helpers.DialogHelper

class WebDialog(val url: String) : BaseDialog<DialogWebBinding>() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        binding.topAppBar.toolbar.run {
            subtitle = url
            initMenu(R.menu.web)

            onBack {
                onBackPressed()
            }

            onMenuItemClick {
                when (itemId) {
                    R.id.open -> {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (ex: java.lang.Exception) {
                            DialogHelper.showMessage(R.string.no_browser_error)
                        }
                    }
                    R.id.refresh -> {
                        binding.topAppBar.progressBar.isVisible = true
                        binding.topAppBar.progressBar.progress = 0
                        binding.webView.reload()
                    }
                    R.id.copy_link -> {
                        val clip = ClipData.newPlainText(LocaleHelper.getString(R.string.link), url)
                        clipboardManager.setPrimaryClip(clip)
                        DialogHelper.showTextCopiedMessage(url)
                    }
                }
            }
        }
        binding.webView.apply {
            webChromeClient = CustomWebChromeClient()
            webViewClient = CustomWebViewClient()
            settings.setSupportZoom(false)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mixedContentMode = MIXED_CONTENT_ALWAYS_ALLOW
            settings.cacheMode = LOAD_DEFAULT
            loadUrl(this@WebDialog.url)
        }
    }

    internal inner class CustomWebChromeClient : WebChromeClient() {
        override fun onProgressChanged(
            view: WebView,
            progress: Int,
        ) {
            if (this@WebDialog.isActive) {
                if (progress < 100) {
                    binding.topAppBar.progressBar.isVisible = true
                    binding.topAppBar.progressBar.progress = progress
                } else if (progress == 100) {
                    binding.topAppBar.progressBar.isVisible = false
                }
            }
        }
    }

    internal inner class CustomWebViewClient : WebViewClient() {
        override fun onReceivedSslError(
            view: WebView,
            handler: SslErrorHandler,
            error: SslError,
        ) {
        }

        override fun onPageFinished(
            view: WebView,
            url: String?,
        ) {
            if (this@WebDialog.isActive) {
                binding.topAppBar.toolbar.title = view.title
            }
        }
    }
}
