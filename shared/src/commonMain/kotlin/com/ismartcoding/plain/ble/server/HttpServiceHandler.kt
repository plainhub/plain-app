package com.ismartcoding.plain.ble.server

import com.ismartcoding.plain.ble.BleRequestData
import com.ismartcoding.plain.ble.BleHttpRequest
import com.ismartcoding.plain.ble.BleHttpResponse
import com.ismartcoding.plain.ble.BleUuids
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.httpserver.HttpRouteRegistry
import com.ismartcoding.plain.httpserver.http.HttpMethod
import com.ismartcoding.plain.httpserver.http.HttpStatus

/**
 * [BleServiceHandler] registered on [BleUuids.HTTP_CHAR_UUID] that turns the
 * BLE RPC channel into a transport for the embedded HTTP API.
 *
 * The client sends a [BleRequestData] whose [BleRequestData.body] carries a
 * JSON-encoded [BleHttpRequest] describing the HTTP request (method, path,
 * query, body). The handler builds an in-memory [BleHttpCall] and
 * dispatches it through [HttpRouteRegistry] — the same router used by the
 * Ktor/SwiftNIO HTTP server — so `/graphql`, `/peer_graphql`, `/fs` and the
 * rest of the commonMain routes can be invoked over BLE with identical
 * semantics to a direct HTTP call.
 *
 * The captured HTTP response (status + headers + body) is wrapped in a
 * [BleHttpResponse] and returned as the BLE write response. Binary response
 * bodies (encrypted GraphQL bytes, `/fs` file bytes) are base64-encoded so
 * they survive the string-only BLE transport.
 *
 * All HTTP headers (both client identity and request-specific overrides)
 * are carried by the outer [BleRequestData.headers], populated by
 * `BleRequestData.create()` via [com.ismartcoding.plain.api.clientHeadersMap].
 */
class HttpServiceHandler : BleServiceHandler {
    override val charUuid: String = BleUuids.HTTP_CHAR_UUID

    override suspend fun handleRequest(requestData: BleRequestData, clientMac: String): String? {
        val httpRequest = try {
            JsonHelper.jsonDecode<BleHttpRequest>(requestData.body)
        } catch (e: Exception) {
            LogCat.e("HTTPServiceHandler: invalid RPC request from $clientMac: ${e.message}")
            return errorResponse(HttpStatus.BAD_REQUEST, "invalid RPC request: ${e.message}")
        }

        if (httpRequest.path.isBlank()) {
            return errorResponse(HttpStatus.BAD_REQUEST, "missing path")
        }

        val method = HttpMethod(httpRequest.method.uppercase().ifBlank { "GET" })
        LogCat.d("HTTPServiceHandler: $method ${httpRequest.path} from=$clientMac")

        val routeEntry = HttpRouteRegistry.matchRoute(method, httpRequest.path) ?: run {
            LogCat.d("HTTPServiceHandler: no route for $method ${httpRequest.path}")
            return errorResponse(HttpStatus.NOT_FOUND, "no route for $method ${httpRequest.path}")
        }

        val pathParams = HttpRouteRegistry.matchPath(routeEntry.path, httpRequest.path) ?: emptyMap()
        val call = BleHttpCall(
            request = httpRequest,
            clientHeaders = requestData.headers,
            remoteHostValue = clientMac,
        )
        call.setPathParams(pathParams)

        try {
            routeEntry.handler(call)
        } catch (e: UnsupportedOperationException) {
            LogCat.e("HTTPServiceHandler: unsupported operation for $method ${httpRequest.path}: ${e.message}")
            return errorResponse(HttpStatus.BAD_REQUEST, e.message ?: "unsupported over BLE")
        } catch (e: Throwable) {
            LogCat.e("HTTPServiceHandler: handler error for $method ${httpRequest.path}: ${e.message}")
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.message ?: "internal error")
        }

        // `encodeResponse()` always returns a valid BleRpcResponse — when the
        // route handler did not call any `respond*` method, it falls back to
        // status 200 with an empty body and the headers captured so far.
        return call.encodeResponse()
    }

    private fun errorResponse(status: Int, message: String): String {
        return JsonHelper.jsonEncode(
            BleHttpResponse(
                status = status,
                headers = mapOf("Content-Type" to "text/plain"),
                body = message,
            ),
        )
    }
}
