package com.ismartcoding.plain.discover

import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.createUnsafeHttpClient
import com.ismartcoding.plain.platform.postText
import kotlinx.coroutines.withTimeoutOrNull

/**
 * LAN transport for pairing messages: POSTs a ready-made
 * [NearbyMessageType][com.ismartcoding.plain.enums.NearbyMessageType]-prefixed
 * [body][post] to the peer's `POST /nearby` endpoint.
 *
 * Uses an unsafe (self-signed-cert-trusting) HttpClient because the in-app
 * HTTP server on the remote side presents a self-signed certificate and
 * uses a local IP as the hostname.
 */
object NearbyHttpClient {
    /** Pairing is interactive; a stale/unreachable peer must fail fast. */
    private const val REQUEST_TIMEOUT_MS = 5_000L

    private val client by lazy { createUnsafeHttpClient() }

    /**
     * POSTs [body] to `https://[targetIp]:[targetPort]/nearby`.
     * Returns true when the peer answered with a 2xx status.
     */
    suspend fun post(body: String, targetIp: String, targetPort: Int): Boolean {
        return try {
            val url = "https://$targetIp:$targetPort/nearby"
            val response = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
                client.postText(url, body, contentType = "application/json")
            } ?: run {
                LogCat.e("NearbyHttpClient: POST timed out after ${REQUEST_TIMEOUT_MS}ms")
                return false
            }
            response.use {
                val ok = it.isSuccess()
                if (!ok) {
                    LogCat.e("NearbyHttpClient: rejected ${it.status} ${it.bodyAsText()}")
                }
                ok
            }
        } catch (e: Exception) {
            LogCat.e("NearbyHttpClient: failed ${e.message}")
            false
        }
    }
}
