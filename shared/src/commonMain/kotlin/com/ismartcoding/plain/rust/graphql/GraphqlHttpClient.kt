package com.ismartcoding.plain.rust.graphql

import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.models.UploadChunkInfo
import com.ismartcoding.plain.rust.native.RustNativeBindings
import com.ismartcoding.plain.rust.native.nativeCall
import kotlinx.coroutines.withTimeoutOrNull
import io.ktor.http.Url
import kotlin.io.encoding.Base64

internal class GraphqlHttpException(val status: Int) : IllegalStateException("HTTP $status")

internal data class GraphqlHttpResponse(val status: Int, val body: ByteArray)

internal interface GraphqlHttpTransport {
    suspend fun post(url: String, clientId: String, body: ByteArray, contentType: String = "application/octet-stream", tlsCertificateSha256: String? = null): GraphqlHttpResponse
}

internal expect fun createGraphqlHttpTransport(): GraphqlHttpTransport

internal class PeerHttpClient(
    peer: Peer,
    private val transport: GraphqlHttpTransport = createGraphqlHttpTransport(),
    private val peers: suspend () -> List<Peer> = { GraphqlApi.execute(Query.peers()) },
    private val encrypt: suspend (String, String) -> String = { token, payload -> nativeCall { RustNativeBindings.encryptHttpRequest(token, payload) } },
    private val decrypt: suspend (String, String, String) -> String = { token, request, response ->
        nativeCall { RustNativeBindings.decryptHttpResponse(token, "/graphql", request, response) }
    },
    private val clientId: () -> String = { Prefs.clientId.value },
    internal val encryptResourceReference: suspend (String, String) -> String = { token, path -> nativeCall { RustNativeBindings.encryptResourceReference(token, JsonHelper.jsonEncode(mapOf("clientId" to clientId(), "path" to path))) } },
) {
    private val expected = peer.copy()

    internal suspend fun current(): Peer {
        val current = peers().find { it.id == expected.id } ?: error("peer_removed")
        check(current.isPaired() && current.token.isNotEmpty()) { "no_permission" }
        check(current.publicKey == expected.publicKey && current.token == expected.token) { "peer_identity_changed" }
        return current
    }

    private suspend fun post(peer: Peer, endpoint: String, body: ByteArray, contentType: String = "application/octet-stream", tlsCertificateSha256: String? = null): GraphqlHttpResponse {
        val base = Url(peer.address?.baseUrl ?: error("peer_address_unavailable"))
        require(base.protocol.name == "https" && base.host.isNotEmpty()) { "Invalid management address" }
        val response = withTimeoutOrNull(30_000) {
            transport.post(base.toString().trimEnd('/') + endpoint, clientId(), body, contentType, tlsCertificateSha256)
        } ?: error("connection_timeout")
        when (response.status) {
            401 -> error("unauthorized")
            403 -> error("desktop_access_disabled")
            in 200..299 -> Unit
            else -> throw GraphqlHttpException(response.status)
        }
        current()
        return response
    }

    val graphql = GraphqlClient("http") { operation ->
        val peer = current()
        val encoded = encrypt(peer.token, JsonHelper.jsonEncode(operation))
        val response = post(peer, "/graphql", Base64.decode(encoded))
        val plain = decrypt(peer.token, encoded, Base64.encode(response.body))
        current()
        JsonHelper.jsonDecode<GraphqlResponse>(Base64.decode(plain).decodeToString())
    }

    suspend fun uploadChunk(id: String, index: Long, bytes: ByteArray, tlsCertificateSha256: String): String {
        val peer = current()
        val info = Base64.decode(encrypt(peer.token, JsonHelper.jsonEncode(UploadChunkInfo(id, index, bytes.size.toLong()))))
        val boundary = "plain-upload-$id-$index"
        val segments = listOf(
            "--$boundary\r\nContent-Disposition: form-data; name=\"info\"; filename=\"info\"\r\nContent-Type: application/octet-stream\r\n\r\n".encodeToByteArray(),
            info,
            "\r\n--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"chunk\"\r\nContent-Type: application/octet-stream\r\n\r\n".encodeToByteArray(),
            bytes,
            "\r\n--$boundary--\r\n".encodeToByteArray(),
        )
        val body = ByteArray(segments.sumOf { it.size })
        var offset = 0
        for (segment in segments) { segment.copyInto(body, offset); offset += segment.size }
        val response = post(peer, "/upload_chunk", body, "multipart/form-data; boundary=$boundary", tlsCertificateSha256)
        check(response.status == 201 && response.body.decodeToString() == "$index:${bytes.size}") { "Invalid upload acknowledgement" }
        return ""
    }
}
