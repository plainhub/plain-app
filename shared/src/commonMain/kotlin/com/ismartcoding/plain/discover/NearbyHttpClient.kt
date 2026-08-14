package com.ismartcoding.plain.discover

import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.createUnsafeHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.withTimeoutOrNull

/**
 * LAN transport for pairing messages: POSTs a ready-made
 * [NearbyMessageType][com.ismartcoding.plain.enums.NearbyMessageType]-prefixed
 * [body][post] (built by [PairingMessenger]) to the peer's `POST /nearby`
 * endpoint.
 *
 * Uses an unsafe (self-signed-cert-trusting) HttpClient because the in-app
 * HTTP server on the remote side presents a self-signed certificate and
 * uses a local IP as the hostname.
 */
object NearbyHttpClient {
    /** Pairing is interactive; a stale/unreachable peer must fail fast. */
    private const val REQUEST_TIMEOUT_MS = 5_000L

    private val client: HttpClient by lazy { createUnsafeHttpClient() }

    /**
     * POSTs [body] to `https://[targetIp]:[targetPort]/nearby`.
     * Returns true when the peer answered with a 2xx status.
     */
    suspend fun post(body: String, targetIp: String, targetPort: Int): Boolean {
        return try {
            val url = "https://$targetIp:$targetPort/nearby"
            val response = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
                client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            } ?: run {
                LogCat.e("NearbyHttpClient: POST timed out after ${REQUEST_TIMEOUT_MS}ms")
                return false
            }
            val ok = response.status.value in 200..299
            if (!ok) {
                LogCat.e("NearbyHttpClient: rejected ${response.status} ${response.bodyAsText()}")
            }
            ok
        } catch (e: Exception) {
            LogCat.e("NearbyHttpClient: failed ${e.message}")
            false
        }
    }
}
