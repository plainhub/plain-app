package com.ismartcoding.plain.web.http

import kotlin.jvm.JvmInline

/**
 * HTTP method used by [HttpRouter]. Stored as a string to allow arbitrary
 * custom methods (e.g. DLNA's `NOTIFY`) in addition to the common verbs.
 */
@JvmInline
value class HttpMethod(val name: String) {
    companion object {
        val GET = HttpMethod("GET")
        val POST = HttpMethod("POST")
        val PUT = HttpMethod("PUT")
        val DELETE = HttpMethod("DELETE")
        val HEAD = HttpMethod("HEAD")
        val OPTIONS = HttpMethod("OPTIONS")
    }
}

/**
 * A single multipart part produced by [HttpCall.handleMultipart].
 *
 * [readBytes] is intended for small parts (e.g. encrypted JSON metadata),
 * while [copyTo] streams large parts to a [StreamSink] (e.g. a file sink
 * created by `platform.createFileSink`) without holding the whole payload
 * in memory.
 */
interface HttpMultipartPart {
    val name: String?
    val originalFileName: String?
    val contentType: String?

    /** Read the entire part payload into memory. */
    suspend fun readBytes(): ByteArray

    /** Stream the part payload to [sink]. Closes [sink] on completion. */
    suspend fun copyTo(sink: StreamSink)
}

/**
 * Platform-agnostic HTTP request/response. The commonMain route handlers
 * operate exclusively against this interface, so they carry no Ktor or
 * SwiftNIO dependencies. The platform layer (e.g. `KtorHttpCall`) wraps the
 * native call and implements every member.
 */
interface HttpCall {
    val method: HttpMethod
    val path: String
    val remoteHost: String

    fun queryParam(name: String): String?
    fun queryParamStrings(): Map<String, List<String>>
    fun pathParam(name: String): String?
    fun header(name: String): String?

    suspend fun receiveBody(): ByteArray
    suspend fun receiveText(): String

    /**
     * Iterate multipart parts. Each [HttpMultipartPart] is only valid inside
     * the [handler] callback and must not escape it — platforms dispose of
     * the underlying resource once [handler] returns.
     */
    suspend fun handleMultipart(handler: suspend (HttpMultipartPart) -> Unit)

    // --- response side ---

    fun responseHeader(name: String, value: String)
    fun responseStatus(status: Int)

    suspend fun respond(bytes: ByteArray, contentType: String? = null)
    suspend fun respondText(body: String, contentType: String? = null, status: Int = HttpStatus.OK)
    suspend fun respondNoBody(status: Int)

    /**
     * Stream a response body. The platform allocates an output channel and
     * invokes [writer] with a [StreamSink] that writes through to the client.
     */
    suspend fun respondStream(
        contentType: String? = null,
        status: Int = HttpStatus.OK,
        headers: Map<String, String> = emptyMap(),
        writer: suspend (StreamSink) -> Unit,
    )

    /** Respond with a file located at [path] using native range support. */
    suspend fun respondFile(
        path: String,
        contentType: String? = null,
        contentDisposition: String? = null,
    )

    /**
     * Proxy a remote [url] to this response: makes the request, copies the
     * upstream status code and headers (except `Transfer-Encoding` and
     * `Connection`), and streams the response body through. Used by the
     * `/proxyfs` route to forward peer-to-peer downloads.
     *
     * @return `true` on success, `false` if the upstream request failed.
     */
    suspend fun proxyUrl(url: String): Boolean

    /**
     * Respond with a file using DLNA-specific headers and HTTP 206 status.
     * Sets `realTimeInfo.dlna.org`, `contentFeatures.dlna.org`,
     * `transferMode.dlna.org`, `Connection`, `Server`, `ETag`, and
     * `Last-Modified` headers as appropriate for DLNA compliance, then serves
     * the file with native range support. Used by the `/media/{id}` route.
     *
     * @return `true` on success, `false` if the file does not exist.
     */
    suspend fun respondDlnaFile(path: String): Boolean
}
