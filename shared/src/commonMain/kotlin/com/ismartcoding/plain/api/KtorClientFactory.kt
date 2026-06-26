package com.ismartcoding.plain.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets

object KtorClientFactory {
    fun httpClient() =
        HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = HttpApiTimeout.DEFAULT_SECONDS * 1000L
                connectTimeoutMillis = HttpApiTimeout.DEFAULT_SECONDS * 1000L
            }
            install(WebSockets)
        }

    fun browserClient(): HttpClient = platformBrowserClient()
}

internal expect fun platformBrowserClient(): HttpClient