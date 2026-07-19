package com.ismartcoding.plain.api

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.platform.getAppVersion
import com.ismartcoding.plain.platform.getDeviceName
import com.ismartcoding.plain.platform.getPlatformName
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Returns the common client identification headers (c-id, c-platform, c-name, c-version).
 *
 * Used by [addClientHeaders] for Ktor requests and by non-HTTP transports (e.g. BLE)
 * that need to forward the same metadata to the peer's HTTP router.
 */
@OptIn(ExperimentalEncodingApi::class)
fun clientHeadersMap(): Map<String, String> = mapOf(
    "c-id" to TempData.clientId,
    "c-platform" to getPlatformName(),
    "c-name" to Base64.encode(getDeviceName().encodeToByteArray()),
    "c-version" to getAppVersion(),
)

/**
 * Adds the common client identification headers to this Ktor request builder.
 *
 * Note: the import must be [io.ktor.client.request.headers] (the extension on
 * [HttpMessageBuilder] that mutates the builder's own [HeadersBuilder]). Importing
 * [io.ktor.http.headers] instead resolves to a top-level factory function that
 * returns a detached [io.ktor.http.Headers] instance, silently dropping the headers.
 */
@OptIn(ExperimentalEncodingApi::class)
fun HttpRequestBuilder.addClientHeaders() {
    val map = clientHeadersMap()
    headers {
        map.forEach { (key, value) -> append(key, value) }
    }
}
