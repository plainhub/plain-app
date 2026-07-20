package com.ismartcoding.plain.ble.server

import com.ismartcoding.plain.ble.BleRequestData
import com.ismartcoding.plain.ble.BleRpcRequest
import com.ismartcoding.plain.ble.BleRpcResponse
import com.ismartcoding.plain.ble.BleUuids
import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.web.HttpRouteRegistry
import com.ismartcoding.plain.web.http.HttpMethod
import com.ismartcoding.plain.web.http.HttpStatus

/**
 * [BleServiceHandler] registered on [BleUuids.RPC_CHAR_UUID] that turns the
 * BLE RPC channel into a transport for the embedded HTTP API.
 *
 * The client sends a [BleRequestData] whose [BleRequestData.body] carries a
 * JSON-encoded [BleRpcRequest] describing the HTTP request (method, path,
 * query, body). The handler builds an in-memory [BleHttpCall] and
 * dispatches it through [HttpRouteRegistry] — the same router used by the
 * Ktor/SwiftNIO HTTP server — so `/graphql`, `/peer_graphql`, `/fs` and the
 * rest of the commonMain routes can be invoked over BLE with identical
 * semantics to a direct HTTP call.
 *
 * The captured HTTP response (status + headers + body) is wrapped in a
 * [BleRpcResponse] and returned as the BLE write response. Binary response
 * bodies (encrypted GraphQL bytes, `/fs` file bytes) are base64-encoded so
 * they survive the string-only BLE transport.
 *
 * All HTTP headers (both client identity and request-specific overrides)
 * are carried by the outer [BleRequestData.headers], populated by
 * `BleRequestData.create()` via [com.ismartcoding.plain.api.clientHeadersMap].
 */
class HTTPServiceHandler : BleServiceHandler {
    override val charUuid: String = BleUuids.RPC_CHAR_UUID

    override suspend fun handleRequest(requestData: BleRequestData, clientMac: String): String? {
        val rpcRequest = try {
            JsonHelper.jsonDecode<BleRpcRequest>(requestData.body)
        } catch (e: Exception) {
            LogCat.e("HTTPServiceHandler: invalid RPC request from $clientMac: ${e.message}")
            return errorResponse(HttpStatus.BAD_REQUEST, "invalid RPC request: ${e.message}")
        }

        if (rpcRequest.path.isBlank()) {
            return errorResponse(HttpStatus.BAD_REQUEST, "missing path")
        }

        val method = HttpMethod(rpcRequest.method.uppercase().ifBlank { "GET" })
        LogCat.d("HTTPServiceHandler: $method ${rpcRequest.path} from=$clientMac")

        val routeEntry = HttpRouteRegistry.matchRoute(method, rpcRequest.path) ?: run {
            LogCat.d("HTTPServiceHandler: no route for $method ${rpcRequest.path}")
            return errorResponse(HttpStatus.NOT_FOUND, "no route for $method ${rpcRequest.path}")
        }

        val pathParams = HttpRouteRegistry.matchPath(routeEntry.path, rpcRequest.path) ?: emptyMap()
        val call = BleHttpCall(
            request = rpcRequest,
            clientHeaders = requestData.headers,
            remoteHostValue = clientMac,
        )
        call.setPathParams(pathParams)

        try {
            routeEntry.handler(call)
        } catch (e: UnsupportedOperationException) {
            LogCat.e("HTTPServiceHandler: unsupported operation for $method ${rpcRequest.path}: ${e.message}")
            return errorResponse(HttpStatus.BAD_REQUEST, e.message ?: "unsupported over BLE")
        } catch (e: Throwable) {
            LogCat.e("HTTPServiceHandler: handler error for $method ${rpcRequest.path}: ${e.message}")
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.message ?: "internal error")
        }

        // `encodeResponse()` always returns a valid BleRpcResponse — when the
        // route handler did not call any `respond*` method, it falls back to
        // status 200 with an empty body and the headers captured so far.
        return call.encodeResponse()
    }

    private fun errorResponse(status: Int, message: String): String {
        return JsonHelper.jsonEncode(
            BleRpcResponse(
                status = status,
                headers = mapOf("Content-Type" to "text/plain"),
                body = message,
            ),
        )
    }
}
