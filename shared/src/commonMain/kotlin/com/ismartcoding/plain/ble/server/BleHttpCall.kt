package com.ismartcoding.plain.ble.server

import com.ismartcoding.plain.ble.BleRpcRequest
import com.ismartcoding.plain.helpers.Base64Lenient
import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.streamFileTo
import com.ismartcoding.plain.web.http.HttpCall
import com.ismartcoding.plain.web.http.HttpMethod
import com.ismartcoding.plain.web.http.HttpMultipartPart
import com.ismartcoding.plain.web.http.HttpStatus
import com.ismartcoding.plain.web.http.StreamSink
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * In-memory [HttpCall] that wraps a [BleRpcRequest] received over the BLE
 * RPC characteristic ([com.ismartcoding.plain.ble.BleUuids.RPC_CHAR_UUID])
 * and captures the response produced by the matched route handler.
 *
 * The captured [responseBody] (along with [responseStatus] and
 * [responseHeaders]) is then JSON-encoded as a
 * [com.ismartcoding.plain.ble.BleRpcResponse] and sent back to the BLE
 * client by [HTTPServiceHandler].
 *
 * The merged [headers] map combines the outer [com.ismartcoding.plain.ble.BleRequestData]
 * headers (client identity: `c-id`, `c-platform`, `c-name`, `c-version`)
 * with the inner [BleRpcRequest.headers] (request-specific: `authorization`,
 * `c-cid`, etc.); the inner entries win on conflict.
 *
 * `respondFile`, `respondStream`, and `respond` paths all buffer into
 * [responseBody] so binary content (encrypted GraphQL bytes, `/fs` file
 * bytes) survives the string-only BLE transport. `proxyUrl`,
 * `respondDlnaFile`, and `handleMultipart` are not supported over BLE and
 * throw [UnsupportedOperationException] — callers should route those
 * through the HTTP server instead.
 */
class BleHttpCall(
    request: BleRpcRequest,
    clientHeaders: Map<String, String>,
    /** MAC/UUID of the BLE peer, exposed as [remoteHost] for handlers that read it. */
    private val remoteHostValue: String,
) : HttpCall {

    override val method: HttpMethod = HttpMethod(request.method.uppercase())
    override val path: String = request.path
    override val remoteHost: String = remoteHostValue

    private val queryParams: Map<String, List<String>> = request.query
    private val pathParams: MutableMap<String, String> = mutableMapOf()

    /**
     * Effective HTTP headers seen by the route handler. The inner request
     * headers override the outer client headers on conflict.
     */
    val headers: Map<String, String> = clientHeaders + request.headers

    private val requestBody: ByteArray =
        if (request.bodyBase64 && request.body.isNotEmpty()) {
            runCatching { Base64Lenient.decode(request.body) }.getOrElse {
                LogCat.e("BleHttpCall: invalid base64 request body, falling back to UTF-8")
                request.body.encodeToByteArray()
            }
        } else {
            request.body.encodeToByteArray()
        }

    // Captured response state.
    var responseStatus: Int = HttpStatus.OK
        private set
    private val responseHeaders = mutableMapOf<String, String>()
    var responseBody: ByteArray = ByteArray(0)
        private set

    fun setPathParams(params: Map<String, String>) {
        pathParams.clear()
        pathParams.putAll(params)
    }

    override fun queryParam(name: String): String? = queryParams[name]?.firstOrNull()

    override fun queryParamStrings(): Map<String, List<String>> = queryParams

    override fun pathParam(name: String): String? = pathParams[name]

    override fun header(name: String): String? = headers[name]

    override suspend fun receiveBody(): ByteArray = requestBody

    override suspend fun receiveText(): String = requestBody.decodeToString()

    override suspend fun handleMultipart(handler: suspend (HttpMultipartPart) -> Unit) {
        throw UnsupportedOperationException("multipart upload is not supported over BLE")
    }

    override fun responseHeader(name: String, value: String) {
        responseHeaders[name] = value
    }

    override fun responseStatus(status: Int) {
        responseStatus = status
    }

    override suspend fun respond(bytes: ByteArray, contentType: String?) {
        if (contentType != null) responseHeaders["Content-Type"] = contentType
        responseBody = bytes
    }

    override suspend fun respondText(body: String, contentType: String?, status: Int) {
        responseStatus = status
        if (contentType != null) responseHeaders["Content-Type"] = contentType
        responseBody = body.encodeToByteArray()
    }

    override suspend fun respondNoBody(status: Int) {
        responseStatus = status
        responseBody = ByteArray(0)
    }

    override suspend fun respondStream(
        contentType: String?,
        status: Int,
        headers: Map<String, String>,
        writer: suspend (StreamSink) -> Unit,
    ) {
        responseStatus = status
        if (contentType != null) responseHeaders["Content-Type"] = contentType
        headers.forEach { (k, v) -> responseHeaders[k] = v }
        val sink = ByteArrayStreamSink()
        try {
            writer(sink)
        } finally {
            sink.flush()
        }
        responseBody = sink.toByteArray()
    }

    override suspend fun respondFile(
        path: String,
        contentType: String?,
        contentDisposition: String?,
    ) {
        if (contentType != null) responseHeaders["Content-Type"] = contentType
        if (contentDisposition != null) responseHeaders["Content-Disposition"] = contentDisposition
        val sink = ByteArrayStreamSink()
        val ok = streamFileTo(path, sink)
        if (ok) {
            responseBody = sink.toByteArray()
            responseStatus = HttpStatus.OK
        } else {
            responseBody = ByteArray(0)
            responseStatus = HttpStatus.NOT_FOUND
        }
    }

    override suspend fun proxyUrl(url: String): Boolean {
        throw UnsupportedOperationException("proxyUrl is not supported over BLE")
    }

    override suspend fun respondDlnaFile(path: String): Boolean {
        throw UnsupportedOperationException("respondDlnaFile is not supported over BLE")
    }

    /** Snapshot of the captured response headers for [BleRpcResponse] encoding. */
    fun snapshotHeaders(): Map<String, String> = responseHeaders.toMap()

    /**
     * In-memory [StreamSink] that accumulates bytes for [respondStream] and
     * [respondFile]. Implemented with a growable [ByteArray] + write cursor
     * so it works in commonMain without JVM/AWT dependencies.
     */
    private class ByteArrayStreamSink : StreamSink {
        private var buffer: ByteArray = ByteArray(INITIAL_CAPACITY)
        private var size: Int = 0

        override suspend fun write(bytes: ByteArray) {
            append(bytes, 0, bytes.size)
        }

        override suspend fun write(bytes: ByteArray, offset: Int, length: Int) {
            append(bytes, offset, length)
        }

        override suspend fun flush() {}

        override suspend fun close() {}

        private fun append(bytes: ByteArray, offset: Int, length: Int) {
            ensureCapacity(size + length)
            bytes.copyInto(buffer, destinationOffset = size, startIndex = offset, endIndex = offset + length)
            size += length
        }

        private fun ensureCapacity(required: Int) {
            if (required <= buffer.size) return
            var newCap = buffer.size
            while (newCap < required) newCap *= 2
            val grown = ByteArray(newCap)
            buffer.copyInto(grown, endIndex = size)
            buffer = grown
        }

        fun toByteArray(): ByteArray = buffer.copyOf(size)

        companion object {
            private const val INITIAL_CAPACITY = 8 * 1024
        }
    }
}

/**
 * Encodes the [BleHttpCall]'s captured response into a JSON
 * [com.ismartcoding.plain.ble.BleRpcResponse] string suitable for returning
 * from [BleServiceHandler.handleRequest]. The response body is base64-encoded
 * so binary payloads (encrypted GraphQL bytes, `/fs` file bytes) survive the
 * string-only BLE transport.
 */
@OptIn(ExperimentalEncodingApi::class)
internal fun BleHttpCall.encodeResponse(): String {
    val response = com.ismartcoding.plain.ble.BleRpcResponse(
        status = responseStatus,
        headers = snapshotHeaders(),
        body = if (responseBody.isNotEmpty()) {
            Base64Lenient.encode(responseBody)
        } else {
            ""
        },
    )
    return JsonHelper.jsonEncode(response)
}
