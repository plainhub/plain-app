package com.ismartcoding.plain.platform

import com.ismartcoding.plain.api.HttpApiTimeout
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.channels.ReceiveChannel

/** Read buffer used by every response/stream copy in this module. */
internal const val STREAM_BUFFER_SIZE = 64 * 1024

/**
 * Streams this channel into [write] chunk by chunk until EOF. Single
 * implementation shared by file downloads, Coil's network fetcher and the
 * in-app HTTP server proxy, so no caller has to re-implement the read loop.
 *
 * @return total number of bytes written.
 */
suspend fun ByteReadChannel.copyTo(write: suspend (buffer: ByteArray, length: Int) -> Unit): Long {
    val buffer = ByteArray(STREAM_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = readAvailable(buffer)
        if (read <= 0) break
        write(buffer, read)
        total += read
    }
    return total
}

/**
 * A request to be executed by [PlainHttpClient]. [method] is a plain HTTP verb
 * string ("GET", "POST", "SUBSCRIBE", ...) so extension verbs like the DLNA
 * SUBSCRIBE/UNSUBSCRIBE work without special-casing.
 */
class PlainRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray? = null,
    val contentType: String? = null,
)

/**
 * A single WebSocket frame received from a [PlainWebSocketSession].
 * Exactly one of [text] / [binary] is non-null.
 */
class PlainWsFrame(val text: String?, val binary: ByteArray?)

interface PlainWebSocketSession {
    /** Receives frames until the socket closes; iteration throws on failure. */
    val incoming: ReceiveChannel<PlainWsFrame>

    suspend fun sendBinary(data: ByteArray)

    suspend fun sendText(text: String)

    fun close()
}

/**
 * Platform-agnostic HTTP response. [channel] streams the body via ktor-io's
 * [io.ktor.utils.io.ByteReadChannel] so large downloads never need to be
 * buffered in memory.
 */
class PlainResponse(
    val status: HttpStatusCode,
    val url: String,
    val headers: Map<String, List<String>>,
    val channel: ByteReadChannel,
    private val onClose: (() -> Unit)? = null,
) : AutoCloseable {
    /** True only for HTTP 200 — same semantics as the `HttpResponse.isOk()` helper. */
    fun isOk(): Boolean = status == HttpStatusCode.OK

    /** True for any 2xx status — same semantics as ktor's `HttpStatusCode.isSuccess()`. */
    fun isSuccess(): Boolean = status.isSuccess

    suspend fun bodyAsBytes(): ByteArray {
        val chunks = ArrayList<ByteArray>()
        channel.copyTo { buffer, length -> chunks.add(buffer.copyOf(length)) }
        val out = ByteArray(chunks.sumOf { it.size })
        var pos = 0
        for (chunk in chunks) {
            chunk.copyInto(out, pos)
            pos += chunk.size
        }
        return out
    }

    suspend fun bodyAsText(): String = bodyAsBytes().decodeToString()

    /** Case-insensitive lookup, matching HTTP header semantics (HTTP/2 servers send lowercase names). */
    fun header(name: String): String? =
        headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.firstOrNull()

    override fun toString(): String = "$status $url"

    override fun close() {
        onClose?.invoke()
    }
}

/**
 * Minimal multiplatform HTTP client abstraction that replaces the previous
 * ktor-based HTTP client. Android is backed by OkHttp directly; iOS uses a
 * native NSURLSession implementation.
 *
 * When built from [PlainHttpClientSpec.Crypto], [cryptoKey] carries the
 * ChaCha20 key so transport-layer callers (e.g. `executeGraphQLRequest`) can
 * encrypt/decrypt bodies — replacing the previous ktor attribute approach.
 */
interface PlainHttpClient : AutoCloseable {
    val cryptoKey: ByteArray? get() = null

    suspend fun request(req: PlainRequest): PlainResponse

    suspend fun <T> webSocket(
        url: String,
        headers: Map<String, String> = emptyMap(),
        block: suspend (PlainWebSocketSession) -> T,
    ): T

    override fun close()
}

suspend fun PlainHttpClient.request(
    method: String,
    url: String,
    headers: Map<String, String> = emptyMap(),
): PlainResponse = request(PlainRequest(method, url, headers))

suspend fun PlainHttpClient.get(
    url: String,
    headers: Map<String, String> = emptyMap(),
): PlainResponse = request(PlainRequest("GET", url, headers))

suspend fun PlainHttpClient.post(
    url: String,
    body: ByteArray,
    contentType: String? = null,
    headers: Map<String, String> = emptyMap(),
): PlainResponse = request(PlainRequest("POST", url, headers, body, contentType))

suspend fun PlainHttpClient.postText(
    url: String,
    body: String,
    contentType: String? = null,
    headers: Map<String, String> = emptyMap(),
): PlainResponse = request(PlainRequest("POST", url, headers, body.encodeToByteArray(), contentType))

/** User-Agent sent by the Browser spec (equivalent to the old ktor BrowserUserAgent plugin). */
const val BROWSER_USER_AGENT =
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

sealed class PlainHttpClientSpec {
    data object Default : PlainHttpClientSpec()
    data object Browser : PlainHttpClientSpec()
    data object Unsafe : PlainHttpClientSpec()
    data object Download : PlainHttpClientSpec()
    data object PeerStatus : PlainHttpClientSpec()
    data class Crypto(
        val keyBytes: ByteArray,
        val timeoutSeconds: Int = 10,
        val connectTimeoutMs: Long = 1_000L,
    ) : PlainHttpClientSpec()
}

expect fun createPlainHttpClient(spec: PlainHttpClientSpec): PlainHttpClient

fun createHttpClient(): PlainHttpClient = createPlainHttpClient(PlainHttpClientSpec.Default)

fun createBrowserHttpClient(): PlainHttpClient = createPlainHttpClient(PlainHttpClientSpec.Browser)

fun createUnsafeHttpClient(): PlainHttpClient = createPlainHttpClient(PlainHttpClientSpec.Unsafe)

fun createDownloadClient(): PlainHttpClient = createPlainHttpClient(PlainHttpClientSpec.Download)

fun createCryptoClient(
    keyBytes: ByteArray,
    timeoutSeconds: Int = 10,
    connectTimeoutMs: Long = 1_000L,
): PlainHttpClient = createPlainHttpClient(PlainHttpClientSpec.Crypto(keyBytes, timeoutSeconds, connectTimeoutMs))

fun createPeerStatusHttpClient(): PlainHttpClient = createPlainHttpClient(PlainHttpClientSpec.PeerStatus)

/** Shared timeout constants (kept for parity with the previous ktor plugin config). */
object PlainHttpTimeouts {
    val DEFAULT_MS = HttpApiTimeout.DEFAULT_SECONDS * 1000L
    val BROWSER_MS = HttpApiTimeout.BROWSER_SECONDS * 1000L
}
