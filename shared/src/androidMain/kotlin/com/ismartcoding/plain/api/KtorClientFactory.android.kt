package com.ismartcoding.plain.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.headers

internal actual fun platformBrowserClient() =
    HttpClient {
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