@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.web.http.HttpCall
import com.ismartcoding.plain.web.http.HttpMethod
import com.ismartcoding.plain.web.http.HttpMultipartPart
import com.ismartcoding.plain.web.http.StreamSink
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.readBytes
import io.ktor.http.HttpHeaders
import platform.Foundation.NSFileManager

/**
 * Bridges an [IosRequestContext] (populated by Swift) to the commonMain
 * [HttpCall] interface so [com.ismartcoding.plain.web.HttpRouteRegistry] route
 * handlers can run on iOS without touching SwiftNIO types.
 *
 * The call buffers the full response body in memory before returning. For
 * [respondFile] the file path is forwarded back to Swift via
 * [IosRequestContext.setResponseFilePath] so SwiftNIO can use
 * `nonBlockingFileIO` to stream the file directly — avoiding loading large
 * media files into RAM.
 */
class NioHttpCall(
    private val ctx: IosRequestContext,
) : HttpCall {

    private var pathParams: Map<String, String> = emptyMap()

    fun setPathParams(params: Map<String, String>) {
        pathParams = params
    }

    override val method: HttpMethod
        get() = HttpMethod(ctx.method)

    override val path: String
        get() = ctx.path

    override val remoteHost: String
        get() = ctx.remoteHost

    override fun queryParam(name: String): String? =
        ctx.getQueryParams()[name]?.firstOrNull()

    override fun queryParamStrings(): Map<String, List<String>> =
        ctx.getQueryParams()

    override fun pathParam(name: String): String? = pathParams[name]

    override fun header(name: String): String? = ctx.getRequestHeader(name)

    override suspend fun receiveBody(): ByteArray = ctx.getRequestBody()

    override suspend fun receiveText(): String = ctx.getRequestBody().decodeToString()

    override suspend fun handleMultipart(handler: suspend (HttpMultipartPart) -> Unit) {
        val body = ctx.getRequestBody()
        val contentType = ctx.getRequestHeader("Content-Type") ?: return
        if (!contentType.startsWith("multipart/form-data")) return
        val boundary = extractBoundary(contentType) ?: return
        parseMultipart(body, boundary, handler)
    }

    override fun responseHeader(name: String, value: String) {
        ctx.setResponseHeader(name, value)
    }

    override fun responseStatus(status: Int) {
        ctx.responseStatus = status
    }

    override suspend fun respond(bytes: ByteArray, contentType: String?) {
        contentType?.let { ctx.setResponseHeader("Content-Type", it) }
        ctx.setResponseBody(bytes)
    }

    override suspend fun respondText(body: String, contentType: String?, status: Int) {
        ctx.responseStatus = status
        contentType?.let { ctx.setResponseHeader("Content-Type", it) }
        ctx.setResponseBody(body.encodeToByteArray())
    }

    override suspend fun respondNoBody(status: Int) {
        ctx.responseStatus = status
        ctx.setResponseBody(ByteArray(0))
    }

    override suspend fun respondStream(
        contentType: String?,
        status: Int,
        headers: Map<String, String>,
        writer: suspend (StreamSink) -> Unit,
    ) {
        ctx.responseStatus = status
        contentType?.let { ctx.setResponseHeader("Content-Type", it) }
        headers.forEach { (k, v) -> ctx.setResponseHeader(k, v) }
        // Buffer the stream output in memory; iOS uses this for DLNA and zip
        // downloads where the body is produced by commonMain code.
        val chunks = mutableListOf<ByteArray>()
        val sink = object : StreamSink {
            override suspend fun write(bytes: ByteArray) { chunks.add(bytes) }
            override suspend fun write(bytes: ByteArray, offset: Int, length: Int) {
                chunks.add(bytes.copyOfRange(offset, offset + length))
            }
            override suspend fun flush() {}
            override suspend fun close() {}
        }
        writer(sink)
        val total = chunks.sumOf { it.size }
        val result = ByteArray(total)
        var pos = 0
        for (chunk in chunks) {
            chunk.copyInto(result, pos)
            pos += chunk.size
        }
        ctx.setResponseBody(result)
    }

    override suspend fun respondFile(
        path: String,
        contentType: String?,
        contentDisposition: String?,
    ) {
        ctx.setResponseFilePath(path, contentType, contentDisposition)
    }

    override suspend fun proxyUrl(url: String): Boolean {
        return try {
            val client = createUnsafeHttpClient()
            val response = withIO { client.get(url) }
            ctx.responseStatus = response.status.value
            // Copy selected headers through (skip hop-by-hop ones).
            response.headers.entries().forEach { (name, values) ->
                if (!name.equals(HttpHeaders.TransferEncoding, true) &&
                    !name.equals(HttpHeaders.Connection, true)
                ) {
                    values.forEach { ctx.setResponseHeader(name, it) }
                }
            }
            ctx.setResponseBody(withIO { response.readBytes() })
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun respondDlnaFile(path: String): Boolean {
        if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return false
        ctx.setResponseHeader("realTimeInfo.dlna.org", "DLNA.ORG_TLAG=*")
        ctx.setResponseHeader("contentFeatures.dlna.org", "")
        ctx.setResponseHeader("transferMode.dlna.org", "Streaming")
        ctx.setResponseHeader("Connection", "keep-alive")
        ctx.setResponseHeader("Server", "DLNADOC/1.50 UPnP/1.0 Plain/1.0 iOS")
        ctx.responseStatus = 206
        ctx.setResponseFilePath(path, null, null)
        return true
    }

    // --- multipart parsing (pure Kotlin, no platform deps) ---

    private fun extractBoundary(contentType: String): String? {
        val idx = contentType.indexOf("boundary=")
        if (idx < 0) return null
        return contentType.substring(idx + "boundary=".length).trim('"')
    }

    private suspend fun parseMultipart(
        body: ByteArray,
        boundary: String,
        handler: suspend (HttpMultipartPart) -> Unit,
    ) {
        val delimiter = "--$boundary".encodeToByteArray()
        val parts = splitMultipartParts(body, delimiter)
        for (partBytes in parts) {
            val part = parseMultipartPart(partBytes) ?: continue
            handler(part)
        }
    }

    private fun splitMultipartParts(body: ByteArray, delimiter: ByteArray): List<ByteArray> {
        val parts = mutableListOf<ByteArray>()
        var start = indexOf(body, delimiter, 0)
        if (start < 0) return parts
        start += delimiter.size
        // Skip optional \r\n after first boundary
        if (start + 2 <= body.size && body[start] == '\r'.code.toByte() && body[start + 1] == '\n'.code.toByte()) {
            start += 2
        }
        while (true) {
            val end = indexOf(body, delimiter, start)
            if (end < 0) break
            val partEnd = if (end >= 2 && body[end - 2] == '\r'.code.toByte() && body[end - 1] == '\n'.code.toByte()) {
                end - 2
            } else {
                end
            }
            parts.add(body.copyOfRange(start, partEnd))
            start = end + delimiter.size
            if (start + 2 <= body.size && body[start] == '\r'.code.toByte() && body[start + 1] == '\n'.code.toByte()) {
                start += 2
            }
        }
        return parts
    }

    private fun parseMultipartPart(partBytes: ByteArray): HttpMultipartPart? {
        val sep = byteArrayOf('\r'.code.toByte(), '\n'.code.toByte(), '\r'.code.toByte(), '\n'.code.toByte())
        val headerEnd = indexOf(partBytes, sep, 0)
        if (headerEnd < 0) return null
        val headerStr = partBytes.copyOfRange(0, headerEnd).decodeToString()
        val bodyStart = headerEnd + sep.size
        val bodyBytes = partBytes.copyOfRange(bodyStart, partBytes.size)

        var name: String? = null
        var fileName: String? = null
        var contentType: String? = null
        for (line in headerStr.split("\r\n")) {
            if (line.startsWith("Content-Disposition:", ignoreCase = true)) {
                name = extractParam(line, "name")
                fileName = extractParam(line, "filename")
            } else if (line.startsWith("Content-Type:", ignoreCase = true)) {
                contentType = line.substringAfter(":").trim()
            }
        }
        return object : HttpMultipartPart {
            override val name: String? get() = name
            override val originalFileName: String? get() = fileName
            override val contentType: String? get() = contentType
            override suspend fun readBytes(): ByteArray = bodyBytes
            override suspend fun copyTo(sink: StreamSink) {
                sink.write(bodyBytes)
                sink.flush()
                sink.close()
            }
        }
    }

    private fun extractParam(headerLine: String, param: String): String? {
        val regex = """$param="([^"]*)"""".toRegex(RegexOption.IGNORE_CASE)
        return regex.find(headerLine)?.groupValues?.getOrNull(1)
    }

    private fun indexOf(haystack: ByteArray, needle: ByteArray, from: Int): Int {
        if (needle.isEmpty()) return -1
        outer@ for (i in from..haystack.size - needle.size) {
            for (j in needle.indices) {
                if (haystack[i + j] != needle[j]) continue@outer
            }
            return i
        }
        return -1
    }
}
