package com.ismartcoding.plain.tests

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.json.JSONTokener

/** Uses the shipped browser client and its Rust WASM, never a second password protocol. */
internal class PasswordLoginFixture(private val baseUrl: String) {
    private lateinit var view: WebView
    private var closed = false

    suspend fun start(clientId: String, password: String) {
        val loaded = CompletableDeferred<Unit>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            view = WebView(InstrumentationRegistry.getInstrumentation().targetContext)
            view.settings.javaScriptEnabled = true
            view.settings.domStorageEnabled = true
            view.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    loaded.complete(Unit)
                }
            }
            view.loadDataWithBaseURL(baseUrl, "<html><body></body></html>", "text/html", "UTF-8", null)
        }
        withTimeout(10_000) { loaded.await() }
        evaluate("localStorage.setItem('client_id', ${JSONObject.quote(clientId)}); localStorage.removeItem('auth_token'); location.replace(${JSONObject.quote(baseUrl)}); true")
        withTimeout(30_000) {
            while (evaluate("!!document.querySelector('input[type=password]')") != "true") delay(100)
        }
        evaluate("""(() => {
            const input = document.querySelector('input[type=password]');
            const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
            setter.call(input, ${JSONObject.quote(password)});
            input.dispatchEvent(new Event('input', {bubbles: true}));
            input.closest('form').dispatchEvent(new Event('submit', {bubbles: true, cancelable: true}));
            return true;
        })()""")
    }

    suspend fun sessionToken(): String = withTimeout(30_000) {
        while (true) {
            val raw = evaluate("localStorage.getItem('auth_token')")
            if (raw != "null") {
                val value = JSONTokener(raw).nextValue() as? String
                if (!value.isNullOrEmpty()) return@withTimeout value
            }
            delay(100)
        }
        @Suppress("UNREACHABLE_CODE") ""
    }

    private suspend fun evaluate(script: String): String {
        val result = CompletableDeferred<String>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            view.evaluateJavascript(script) { result.complete(it) }
        }
        return result.await()
    }

    fun close() {
        if (!::view.isInitialized || closed) return
        closed = true
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            view.stopLoading()
            view.destroy()
        }
    }
}
