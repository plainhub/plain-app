package com.ismartcoding.plain.ble

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request envelope carried inside [BleRequestData.body] when an RPC client
 * wants to invoke an HTTP API (e.g. `/graphql`, `/peer_graphql`, `/fs`) over
 * the BLE RPC characteristic ([BleUuids.RPC_CHAR_UUID]).
 *
 * The format mirrors a normal HTTP request so the server can route it
 * through the same [com.ismartcoding.plain.web.http.HttpRouter] used by the
 * embedded HTTP server:
 *
 * - [method] + [path] select the route handler.
 * - [query] supplies query-string parameters (e.g. `id` for `/fs`).
 * - [body] is the HTTP request body, base64-encoded when it carries binary
 *   data (e.g. encrypted GraphQL payloads). The flag [bodyBase64] tells the
 *   server whether to base64-decode [body] before passing it to the route
 *   handler.
 *
 * All HTTP headers (client identity AND request-specific overrides like
 * `c-cid`, `authorization`) live in the outer [BleRequestData.headers],
 * populated by `BleRequestData.create()` via
 * [com.ismartcoding.plain.api.clientHeadersMap]. The server's
 * [com.ismartcoding.plain.ble.server.BleHttpCall] reads them from there
 * directly — no header merging needed.
 */
@Serializable
data class BleRpcRequest(
    @SerialName("m") val method: String = "POST",
    @SerialName("p") val path: String,
    @SerialName("q") val query: Map<String, List<String>> = emptyMap(),
    @SerialName("b") val body: String = "",
    @SerialName("bb") val bodyBase64: Boolean = false,
)

/**
 * Response returned by [com.ismartcoding.plain.ble.server.HTTPServiceHandler]
 * and JSON-encoded as the BLE write response.
 *
 * - [status] is the HTTP status code produced by the route handler.
 * - [headers] carries the response headers set by the route handler.
 * - [body] is the raw response body, base64-encoded so binary payloads
 *   (encrypted GraphQL bytes, `/fs` file bytes, etc.) survive the
 *   string-only BLE transport.
 */
@Serializable
data class BleRpcResponse(
    @SerialName("s") val status: Int = 200,
    @SerialName("h") val headers: Map<String, String> = emptyMap(),
    @SerialName("b") val body: String = "",
)
