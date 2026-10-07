package com.ismartcoding.plain.httpserver.http

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
        val OPTIONS = HttpMethod("OPTIONS")
    }
}

/**
 * Platform-agnostic HTTP request/response. The commonMain route handlers
 * operate exclusively against this interface, so they carry no transport
 * dependencies. The platform layer wraps the native call and implements
 * every member.
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
