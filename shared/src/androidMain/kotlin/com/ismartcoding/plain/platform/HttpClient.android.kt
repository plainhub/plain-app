package com.ismartcoding.plain.platform

import com.ismartcoding.plain.api.OkHttpClientFactory
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.Protocol
import java.util.concurrent.TimeUnit

actual fun createHttpEngine(spec: HttpClientSpec): HttpClientEngine =
    when (spec) {
        HttpClientSpec.Default -> OkHttp.create()

        HttpClientSpec.Browser -> OkHttp.create()

        HttpClientSpec.Unsafe ->
            OkHttp.create {
                preconfigured = OkHttpClientFactory.createUnsafeOkHttpClient()
            }

        HttpClientSpec.Download ->
            OkHttp.create {
                preconfigured =
                    OkHttpClientFactory.createUnsafeOkHttpClient()
                        .newBuilder()
                        .protocols(listOf(Protocol.HTTP_1_1))
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(120, TimeUnit.SECONDS)
                        .writeTimeout(120, TimeUnit.SECONDS)
                        .retryOnConnectionFailure(true)
                        .build()
            }

        HttpClientSpec.PeerStatus ->
            OkHttp.create {
                preconfigured =
                    OkHttpClientFactory.createUnsafeOkHttpClient()
                        .newBuilder()
                        .connectTimeout(500, TimeUnit.MILLISECONDS)
                        .pingInterval(15, TimeUnit.SECONDS)
                        .build()
            }

        is HttpClientSpec.Crypto ->
            OkHttp.create {
                preconfigured =
                    OkHttpClientFactory.createCryptoHttpClient(
                        keyBytes = spec.keyBytes,
                        timeout = spec.timeoutSeconds,
                        connectTimeoutMs = spec.connectTimeoutMs,
                    )
            }
    }
