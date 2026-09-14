package com.ismartcoding.plain.features.share

import com.ismartcoding.plain.chat.peer.transport.GraphQLResponseParser
import com.ismartcoding.plain.httpserver.ShareFileParams
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.lib.JsonHelper.jsonEncode
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.extensions.urlEncode
import com.ismartcoding.plain.lib.generateId
import com.ismartcoding.plain.lib.kgraphql.GraphqlRequest
import com.ismartcoding.plain.platform.chaCha20Decrypt
import com.ismartcoding.plain.platform.chaCha20Encrypt
import com.ismartcoding.plain.platform.createDownloadClient
import com.ismartcoding.plain.platform.createUnsafeHttpClient
import com.ismartcoding.plain.platform.get
import com.ismartcoding.plain.platform.post
import io.ktor.utils.io.readAvailable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Instant

/** A parsed `/s/<shared_id>#<token>` share link. */
data class SharedLink(
    val host: String,
    val port: Int,
    val sharedId: String,
    /** Base64UrlSafe-encoded 32-byte shared token from the link fragment. */
    val token: String,
) {
    @OptIn(ExperimentalEncodingApi::class)
    fun tokenBytes(): ByteArray = Base64.UrlSafe.decode(token)

    fun apiUrl(path: String): String = UrlHelper.buildUrl("https", host, port, path)
}

@Serializable
data class SharedFileDto(
    val name: String = "",
    val virtualPath: String = "",
    val isDir: Boolean = false,
    val size: Long = 0,
    val mimeType: String = "",
    val hasThumb: Boolean = false,
)

@Serializable
data class SharedInfoDto(
    val name: String = "",
    val readOnly: Boolean = true,
    val requiresPassword: Boolean = false,
    /** Server wire value is epoch millis (existing guest GraphQL contract). */
    val expiresAt: Long? = null,
    val urlToken: String = "",
    val entries: List<SharedFileDto> = emptyList(),
) {
    /** [expiresAt] converted to [Instant] at the parse boundary. */
    val expiresAtInstant: Instant?
        get() = expiresAt?.let { Instant.fromEpochMilliseconds(it) }
}

@Serializable
private data class SharedInfoWrapper(val sharedInfo: SharedInfoDto)

/**
 * Client for the guest share API (`/guest_graphql`, `/fs?sid=`, `/zip/dir?sid=`).
 * Requests are encrypted with the shared token carried in the link fragment —
 * the same secret the sharing device derives server-side.
 */
object SharedLinkClient {
    private val json = Json { ignoreUnknownKeys = true }

    /** Builds a [SharedLink] from the fields carried in a share card message. */
    fun linkOf(shareId: String, urlToken: String, ip: String, port: Int): SharedLink =
        SharedLink(host = ip, port = port, sharedId = shareId, token = urlToken)

    /** The `/s/<id>#<token>` page URL for browsers. */
    fun pageUrl(link: SharedLink): String =
        UrlHelper.buildUrl("https", link.host, link.port, "/s/${link.sharedId}#${link.token}")

    /** Fetches share metadata and the entries of [virtualPath] (null/empty = root). */
    suspend fun fetchSharedInfo(link: SharedLink, virtualPath: String?): SharedInfoDto {
        val query = $$"query($virtualPath: String) { sharedInfo(virtualPath: $virtualPath) { name readOnly requiresPassword expiresAt urlToken entries { name virtualPath isDir size mimeType hasThumb } } }"
        val variables = buildJsonObject {
            put("virtualPath", virtualPath)
        }
        val requestJson = jsonEncode(GraphqlRequest(query = query, variables = variables))

        val client = createUnsafeHttpClient()
        client.use {
            val response = it.post(
                link.apiUrl("/guest_graphql"),
                body = encryptBody(link, requestJson),
                contentType = "application/octet-stream",
                headers = mapOf("c-id" to link.sharedId),
            )
            response.use { r ->
                val encrypted = r.bodyAsBytes()
                if (!r.isSuccess()) {
                    throw Exception("HTTP ${r.status.value}")
                }
                val decrypted = chaCha20Decrypt(link.tokenBytes(), encrypted)?.decodeToString()
                    ?: encrypted.decodeToString()
                val parsed = GraphQLResponseParser.parse(decrypted)
                parsed.errors?.firstOrNull()?.let { throw Exception(it.message) }
                val data = parsed.data ?: throw Exception("Empty response")
                return json.decodeFromString(SharedInfoWrapper.serializer(), data).sharedInfo
            }
        }
        throw Exception("Unreachable")
    }

    /** Builds the encrypted `id` query param for `/fs` / `/zip/dir`. */
    @OptIn(ExperimentalEncodingApi::class)
    fun buildFileIdParam(link: SharedLink, urlToken: String, virtualPath: String): String {
        val key = Base64.decode(urlToken)
        val params = ShareFileParams(sharedId = link.sharedId, virtualPath = virtualPath)
        return UrlHelper.encrypt(jsonEncode(params), key)
    }

    fun fileUrl(link: SharedLink, urlToken: String, virtualPath: String): String {
        val id = buildFileIdParam(link, urlToken, virtualPath)
        return "${link.apiUrl("/fs")}?sid=${link.sharedId.urlEncode()}&id=${id.urlEncode()}"
    }

    fun zipDirUrl(link: SharedLink, urlToken: String, virtualPath: String): String {
        val id = buildFileIdParam(link, urlToken, virtualPath)
        return "${link.apiUrl("/zip/dir")}?sid=${link.sharedId.urlEncode()}&id=${id.urlEncode()}"
    }

    /**
     * Downloads [url], streaming each chunk into [write] and reporting
     * progress as (downloaded, total). Total is -1 when Content-Length is
     * absent.
     */
    suspend fun downloadTo(
        url: String,
        write: (buffer: ByteArray, length: Int) -> Unit,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> },
    ) {
        val client = createDownloadClient()
        client.use {
            val response = it.get(url)
            response.use { r ->
                if (!r.isSuccess()) {
                    throw Exception("HTTP ${r.status.value}")
                }
                val total = r.header("Content-Length")?.toLongOrNull() ?: -1L
                val channel = r.channel
                val buffer = ByteArray(64 * 1024)
                var downloaded = 0L
                var lastReport = 0L
                while (true) {
                    val read = channel.readAvailable(buffer)
                    if (read == -1) break
                    if (read > 0) {
                        write(buffer, read)
                        downloaded += read
                    }
                    val now = TimeHelper.nowMillis()
                    if (now - lastReport > 300) {
                        onProgress(downloaded, total)
                        lastReport = now
                    }
                }
                onProgress(downloaded, total)
            }
        }
    }

    /** Wrap into the ReplayGuard envelope `ts|nonce|body`, then encrypt. */
    private fun encryptBody(link: SharedLink, body: String): ByteArray {
        val envelope = "${TimeHelper.nowMillis()}|${generateId()}|$body"
        return chaCha20Encrypt(link.tokenBytes(), envelope)
    }
}
