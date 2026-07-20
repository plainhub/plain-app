package com.ismartcoding.plain.chat.peer.transport

import com.ismartcoding.plain.ble.BleRequestData
import com.ismartcoding.plain.ble.BleResult
import com.ismartcoding.plain.ble.BleRpcRequest
import com.ismartcoding.plain.ble.BleRpcResponse
import com.ismartcoding.plain.ble.BleServices
import com.ismartcoding.plain.ble.client.BleDeviceApi
import com.ismartcoding.plain.ble.client.BleGattClient
import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.helpers.Base64Lenient
import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.bleTransport
import com.ismartcoding.plain.platform.chaCha20Decrypt
import com.ismartcoding.plain.platform.chaCha20Encrypt
import com.ismartcoding.plain.platform.isBleReady
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * [PeerTransport] that routes `/peer_graphql` requests over the BLE RPC
 * characteristic ([com.ismartcoding.plain.ble.BleUuids.RPC_CHAR_UUID]) when
 * the LAN transport is unavailable.
 *
 * Peers are identified by their clientId (TempData.clientId, a 13-char short
 * UUID). Only an 8-byte truncated SHA256 hash of the clientId (the "shortId")
 * is broadcast in the BLE scan response serviceData; the scanner matches it
 * via [com.ismartcoding.plain.ble.BleServiceData.shortIdOf]. The peer's
 * `peer.id` is the full clientId (stored on the DPeer record), so callers
 * pass it to [com.ismartcoding.plain.ble.client.BleScanner.findOne] /
 * [com.ismartcoding.plain.ble.client.BleScanner.createClient], which
 * internally compute the shortId for matching. Android's BLE MAC randomization
 * no longer matters because the BLE layer never exposes the MAC as the peer id.
 *
 * When [send] is invoked the transport:
 *
 * 1. Skips itself when BLE isn't ready (throws [TransportUnavailable] so
 *    the router can fall through).
 * 2. Connects to the peer's BLE device: first checks for an already-discovered
 *    client via [com.ismartcoding.plain.ble.client.BleScanner.createClient]
 *    (matched by shortId), then falls back to a fresh BLE scan via
 *    [com.ismartcoding.plain.ble.client.BleScanner.findOne] keyed on the
 *    shortId computed from the peer's full clientId.
 * 3. Encrypts the signed request body with the peer's shared ChaCha20 key —
 *    mirroring what the OkHttp crypto interceptor does for [LanTransport].
 * 4. Wraps the encrypted bytes in a [BleRpcRequest] (base64 body) and sends
 *    it via [BleServices.rpc]. The server-side
 *    [com.ismartcoding.plain.ble.server.HTTPServiceHandler] dispatches the
 *    request through the shared [com.ismartcoding.plain.web.HttpRouteRegistry]
 *    to [com.ismartcoding.plain.web.graphql.PeerGraphQLService], which
 *    decrypts, executes, and re-encrypts the response.
 * 5. Base64-decodes and ChaCha20-decrypts the [BleRpcResponse] body, then
 *    parses it as a GraphQL JSON response via [GraphQLResponseParser].
 *
 * File download is not supported over BLE (throughput is too low for media
 * files); [downloadFile] always throws [TransportUnavailable] so the router
 * surfaces the failure to the caller instead of hanging on a multi-MB
 * transfer.
 */
@OptIn(ExperimentalEncodingApi::class)
object BleTransport : PeerTransport {
    override val id: String = "ble"

    /** Scan timeout for the case where createClient(clientId) returns null. */
    private const val SCAN_TIMEOUT_MS = 10_000L

    override suspend fun send(peer: DPeer, request: SignedRequest, keyBytes: ByteArray): GraphQLResponse {
        if (!isBleReady()) {
            throw TransportUnavailable(id, peer.id, IllegalStateException("BLE not ready"))
        }

        val clientId = peer.id
        val scanner = bleTransport().createScanner()
        val client = scanner.createClient(clientId)
            ?: withTimeoutOrNull(SCAN_TIMEOUT_MS) { scanner.findOne(clientId) }
        if (client == null) {
            throw TransportUnavailable(id, peer.id, IllegalStateException("BLE device not found"))
        }

        val api = BleDeviceApi(client)
        try {
            if (!api.ensureConnected()) {
                throw TransportUnavailable(id, peer.id, IllegalStateException("BLE connect failed"))
            }

            // Encrypt the signed request body with the peer's shared key, the
            // same way the OkHttp crypto client would for LanTransport. The
            // server's PeerGraphQLService decrypts with the same key.
            val encryptedBody = chaCha20Encrypt(keyBytes, request.body)

            val rpcRequest = BleRpcRequest(
                method = "POST",
                path = "/peer_graphql",
                body = Base64Lenient.encode(encryptedBody),
                bodyBase64 = true,
            )

            // All headers (client identity + request-specific c-cid) live in
            // the outer BleRequestData.headers — BleRpcRequest no longer has
            // its own headers field.
            val baseData = BleRequestData.create()
            val requestData = baseData.copy(
                body = JsonHelper.jsonEncode(rpcRequest),
                headers = if (request.channelId.isNotEmpty()) {
                    baseData.headers + ("c-cid" to request.channelId)
                } else {
                    baseData.headers
                },
            )

            val result = api.requestAsync(BleServices.rpc, requestData)
            if (!result.isSuccess()) {
                throw TransportUnavailable(
                    id,
                    peer.id,
                    IllegalStateException("BLE rpc failed: ${result.status}"),
                )
            }

            val responseJson = result.value as? String
            if (responseJson.isNullOrEmpty()) {
                throw TransportUnavailable(
                    id,
                    peer.id,
                    IllegalStateException("BLE rpc empty response"),
                )
            }

            val rpcResponse = JsonHelper.jsonDecode<BleRpcResponse>(responseJson)
            if (rpcResponse.status != 200) {
                LogCat.e("BleTransport: /peer_graphql status=${rpcResponse.status} body=${rpcResponse.body.take(200)}")
                throw TransportUnavailable(
                    id,
                    peer.id,
                    IllegalStateException("peer_graphql status ${rpcResponse.status}"),
                )
            }

            if (rpcResponse.body.isEmpty()) {
                LogCat.e("BleTransport: empty response body from ${peer.id}")
                return GraphQLResponse(
                    null,
                    null,
                    IllegalStateException("empty response body from peer"),
                )
            }

            val encryptedResponse = Base64Lenient.decode(rpcResponse.body)
            val decrypted = chaCha20Decrypt(keyBytes, encryptedResponse)
                ?: return GraphQLResponse(
                    null,
                    null,
                    IllegalStateException("failed to decrypt response from peer"),
                )

            return GraphQLResponseParser.parse(decrypted.decodeToString())
        } finally {
            scanner.teardownConnection(client)
        }
    }

    override suspend fun downloadFile(peer: DPeer, fileId: String): DownloadedResponse {
        // BLE throughput is too low for media file downloads. Surface as
        // TransportUnavailable so the router reports "all transports
        // exhausted" instead of stalling on a multi-MB transfer.
        throw TransportUnavailable(
            id,
            peer.id,
            UnsupportedOperationException("file download not supported over BLE"),
        )
    }
}
