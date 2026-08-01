@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import com.ismartcoding.plain.helpers.JsonHelper.jsonEncode
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.ui.components.EditorData
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIColor
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserScriptInjectionTime
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

/**
 * WKWebView-backed Ace editor wrapper used by [AceEditor] on iOS.
 *
 * Loads the bundled `editor/index.html` (same asset tree as Android's
 * `app/src/main/assets/editor/`) and bridges the JS `AndroidApp` interface
 * expected by `index.html` to Kotlin callbacks via `WKScriptMessageHandler`.
 *
 * Lifecycle: created in [AceEditor]'s `UIKitView.factory`, exposed to
 * [TextFileViewModel] through [EditorWebViewHandle]; released via
 * [destroyEditorWebView].
 */
class IosEditorWebView(
    private val data: EditorData,
    private val onReady: () -> Unit,
    private val onUpdate: (String) -> Unit,
) : NSObject(), WKNavigationDelegateProtocol, WKScriptMessageHandlerProtocol {

    val webView: WKWebView
    private val contentController: WKUserContentController
    private val bridgeName = "AndroidApp"

    init {
        contentController = WKUserContentController()
        contentController.addScriptMessageHandler(this, bridgeName)

        // Inject a shim before the page scripts run so `window.AndroidApp`
        // already exists when `index.html` calls `AndroidApp.editorReady()`
        // and `AndroidApp.updateContent(...)`. This keeps `index.html`
        // identical to the Android build.
        val shim = """
            (function() {
              window.AndroidApp = {
                editorReady: function() {
                  webkit.messageHandlers.${bridgeName}.postMessage({type:'editorReady'});
                },
                updateContent: function(content) {
                  webkit.messageHandlers.${bridgeName}.postMessage({type:'updateContent', content: content});
                }
              };
            })();
        """.trimIndent()
        val userScript = WKUserScript(
            source = shim,
            injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentStart,
            forMainFrameOnly = true,
        )
        contentController.addUserScript(userScript)

        val config = WKWebViewConfiguration()
        config.userContentController = contentController
        config.suppressesIncrementalRendering = false

        webView = WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = config)
        webView.setOpaque(false)
        webView.setBackgroundColor(UIColor.clearColor)
        webView.scrollView.setScrollEnabled(false)
        webView.navigationDelegate = this

        val editorRoot = NSBundle.mainBundle.resourcePath + "/editor"
        val indexUrl = NSURL.fileURLWithPath("$editorRoot/index.html")
        // loadFileURL:allowingReadAccessToURL: lets the page load `./ace/ace.js`
        // and other relative assets from the bundle directory.
        webView.loadFileURL(indexUrl, allowingReadAccessToURL = NSURL.fileURLWithPath(editorRoot))
    }

    override fun webView(
        webView: WKWebView,
        didFinishNavigation: WKNavigation?,
    ) {
        val json = jsonEncode(data)
        webView.evaluateJavaScript("loadEditor($json)") { _, error ->
            if (error != null) {
                LogCat.e("IosEditorWebView loadEditor failed: ${error.localizedDescription}")
            }
        }
    }

    override fun userContentController(
        userContentController: WKUserContentController,
        didReceiveScriptMessage: WKScriptMessage,
    ) {
        val message = didReceiveScriptMessage
        val body = message.body as? Map<Any?, *> ?: return
        val type = body["type"] as? String ?: return
        when (type) {
            "editorReady" -> onReady()
            "updateContent" -> (body["content"] as? String)?.let(onUpdate)
        }
    }

    fun evaluateJavascript(script: String, callback: ((String) -> Unit)?) {
        webView.evaluateJavaScript(script) { result, _ ->
            callback?.invoke(result?.toString() ?: "")
        }
    }

    fun destroy() {
        webView.stopLoading()
        webView.navigationDelegate = null
        contentController.removeScriptMessageHandlerForName(bridgeName)
        webView.loadHTMLString("", null)
    }
}
