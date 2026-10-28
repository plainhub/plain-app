package com.ismartcoding.plain.rust.graphql

import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.rust.native.RustNativeBindings
import com.ismartcoding.plain.rust.native.nativeCall
import kotlinx.coroutines.withTimeoutOrNull
import io.ktor.http.Url
import kotlin.io.encoding.Base64

internal data class GraphqlHttpResponse(val status: Int, val body: ByteArray)

internal interface GraphqlHttpTransport {
    suspend fun post(url: String, clientId: String, body: ByteArray): GraphqlHttpResponse
}

internal expect fun createGraphqlHttpTransport(): GraphqlHttpTransport

internal fun peerGraphqlClient(
    peer: Peer,
    transport: GraphqlHttpTransport = createGraphqlHttpTransport(),
    peers: suspend () -> List<Peer> = { GraphqlApi.execute(Query.peers()) },
    encrypt: suspend (String, String) -> String = { token, payload -> nativeCall { RustNativeBindings.encryptHttpRequest(token, payload) } },
    decrypt: suspend (String, String, String) -> String = { token, request, response ->
        nativeCall { RustNativeBindings.decryptHttpResponse(token, "/graphql", request, response) }
    },
    clientId: () -> String = { Prefs.clientId.value },
): GraphqlClient {
    val expected = peer.copy()
    suspend fun current(): Peer {
        val current = peers().find { it.id == expected.id } ?: error("peer_removed")
        check(current.isPaired() && current.token.isNotEmpty()) { "no_permission" }
        check(current.publicKey == expected.publicKey && current.key == expected.key && current.token == expected.token) { "peer_identity_changed" }
        return current
    }
    return GraphqlClient { operation ->
        val current = current()
        val base = Url(current.address?.baseUrl ?: error("peer_address_unavailable"))
        require(base.protocol.name == "https" && base.host.isNotEmpty()) { "Invalid management address" }
        val encoded = encrypt(current.token, JsonHelper.jsonEncode(operation))
        val response = withTimeoutOrNull(30_000) {
            transport.post(base.toString().trimEnd('/') + "/graphql", clientId(), Base64.decode(encoded))
        } ?: error("connection_timeout")
        when (response.status) {
            401 -> error("unauthorized")
            403 -> error("desktop_access_disabled")
            in 200..299 -> Unit
            else -> error("HTTP ${response.status}")
        }
        val plain = decrypt(current.token, encoded, Base64.encode(response.body))
        current()
        JsonHelper.jsonDecode<GraphqlResponse>(Base64.decode(plain).decodeToString())
    }
}
