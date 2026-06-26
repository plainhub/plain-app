package com.ismartcoding.plain.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.http.headers

internal actual fun platformBrowserClient() =
    HttpClient {
        BrowserUserAgent()
        install(HttpCookies)
        install(HttpTimeout) {
            requestTimeoutMillis = HttpApiTimeout.BROWSER_SECONDS * 1000L
        }
        headers {
            set("accept", "*/*")
        }
    }