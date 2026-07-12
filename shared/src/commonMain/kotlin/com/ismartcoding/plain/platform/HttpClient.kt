package com.ismartcoding.plain.platform

import com.ismartcoding.plain.api.HttpApiTimeout
import com.ismartcoding.plain.api.httpLogSink
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.headers

sealed class HttpClientSpec {
    data object Default : HttpClientSpec()
    data object Browser : HttpClientSpec()
    data object Unsafe : HttpClientSpec()
    data object Download : HttpClientSpec()
    data object PeerStatus : HttpClientSpec()
    data class Crypto(
        val keyBytes: ByteArray,
        val timeoutSeconds: Int = 10,
        val connectTimeoutMs: Long = 1_000L,
    ) : HttpClientSpec()
}

expect fun createHttpEngine(spec: HttpClientSpec): HttpClientEngine

fun createPlatformHttpClient(spec: HttpClientSpec): HttpClient {
    val engine = createHttpEngine(spec)
    return HttpClient(engine) {
        when (spec) {
            HttpClientSpec.Default -> {
                install(HttpTimeout) {
                    requestTimeoutMillis = HttpApiTimeout.DEFAULT_SECONDS * 1000L
                    connectTimeoutMillis = HttpApiTimeout.DEFAULT_SECONDS * 1000L
                }
                install(WebSockets)
            }

            HttpClientSpec.Browser -> {
                BrowserUserAgent()
                install(Logging) {
                    logger =
                        object : Logger {
                            override fun log(message: String) {
                                httpLogSink.log(message)
                            }
                        }
                    level = LogLevel.HEADERS
                }
                install(HttpCookies)
                install(HttpTimeout) {
                    requestTimeoutMillis = HttpApiTimeout.BROWSER_SECONDS * 1000L
                }
                headers {
                    set("accept", "*/*")
                }
            }

            HttpClientSpec.Unsafe -> {
                install(WebSockets)
            }

            HttpClientSpec.Download -> {
                install(HttpTimeout) {
                    requestTimeoutMillis = 120_000L
                    connectTimeoutMillis = 30_000L
                }
            }

            HttpClientSpec.PeerStatus -> {
                install(WebSockets)
            }

            is HttpClientSpec.Crypto -> {
                install(WebSockets)
            }
        }
    }
}

fun createHttpClient(): HttpClient = createPlatformHttpClient(HttpClientSpec.Default)

internal fun platformBrowserClient(): HttpClient = createPlatformHttpClient(HttpClientSpec.Browser)

fun createUnsafeHttpClient(): HttpClient = createPlatformHttpClient(HttpClientSpec.Unsafe)

fun createDownloadClient(): HttpClient = createPlatformHttpClient(HttpClientSpec.Download)

fun createCryptoClient(
    keyBytes: ByteArray,
    timeoutSeconds: Int = 10,
    connectTimeoutMs: Long = 1_000L,
): HttpClient = createPlatformHttpClient(
    HttpClientSpec.Crypto(keyBytes, timeoutSeconds, connectTimeoutMs),
)

fun createPeerStatusHttpClient(): HttpClient = createPlatformHttpClient(HttpClientSpec.PeerStatus)

object KtorClientFactory {
    fun httpClient(): HttpClient = createHttpClient()

    fun browserClient(): HttpClient = platformBrowserClient()
}
