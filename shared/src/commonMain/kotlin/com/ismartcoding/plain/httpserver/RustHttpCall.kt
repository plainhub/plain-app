package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.httpserver.http.*
import com.ismartcoding.plain.platform.*
import com.ismartcoding.plain.lib.extensions.getContentType
import io.ktor.http.Url
import kotlinx.serialization.json.*

internal class RustHttpCall(
    private val socket: PlainWebSocketSession,
    metadata: JsonObject,
) : HttpCall {
    override val method = HttpMethod(metadata.getValue("method").jsonPrimitive.content)
    private val uri = metadata.getValue("uri").jsonPrimitive.content
    private val url = Url(if (uri.startsWith("/")) "http://localhost$uri" else uri)
    override val path = url.encodedPath
    override val remoteHost = metadata.getValue("remoteHost").jsonPrimitive.content
    private val query = url.parameters
    private val headers = metadata.getValue("headers").jsonObject
    private val outputHeaders = mutableMapOf<String, String>()
    private var status = HttpStatus.OK
    var responded = false
        private set
    val scheme = metadata["scheme"]?.jsonPrimitive?.content ?: "http"
    val webSocket = metadata.getValue("webSocket").jsonPrimitive.boolean
    private val pathParams = HttpRouteRegistry.matchRoute(method, path)?.let {
        HttpRouteRegistry.matchPath(it.path, path)
    }.orEmpty()

    override fun queryParam(name: String) = query[name]
    override fun queryParamStrings() = query.entries().associate { it.key to it.value }
    override fun pathParam(name: String) = pathParams[name]
    override fun header(name: String) = headers.entries.firstOrNull { it.key.equals(name, true) }?.value?.jsonPrimitive?.content
    override fun responseHeader(name: String, value: String) { outputHeaders[name] = value }
    override fun responseStatus(status: Int) { this.status = status }

    internal suspend fun receiveFrame(): PlainWsFrame = socket.incoming.receiveCatching().getOrNull()
        ?: throw IllegalStateException("HTTP stream disconnected")

    internal suspend fun consume(endKind: String, write: suspend (ByteArray) -> Unit) {
        while (true) {
            val frame = receiveFrame()
            frame.binary?.let { write(it); return@let }
            val text = frame.text ?: continue
            val packet = Json.parseToJsonElement(text).jsonObject
            when (packet["kind"]?.jsonPrimitive?.content) {
                endKind -> return
                "error" -> error(packet["message"]?.jsonPrimitive?.content ?: "Invalid HTTP body")
                else -> error("Unexpected HTTP stream packet")
            }
        }
    }

    override suspend fun receiveBody(): ByteArray {
        val chunks = ArrayList<ByteArray>()
        var length = 0
        consume("end") {
            check(length <= 32 * 1024 * 1024 - it.size) { "HTTP body exceeds 32 MiB" }
            chunks.add(it); length += it.size
        }
        val result = ByteArray(length)
        var offset = 0
        chunks.forEach { it.copyInto(result, offset); offset += it.size }
        return result
    }
    override suspend fun receiveText() = receiveBody().decodeToString()
    override suspend fun handleMultipart(handler: suspend (HttpMultipartPart) -> Unit) {
        while (true) {
            val frame = receiveFrame()
            val packet = Json.parseToJsonElement(checkNotNull(frame.text)).jsonObject
            when (packet["kind"]?.jsonPrimitive?.content) {
                "end" -> return
                "error" -> error(packet["message"]?.jsonPrimitive?.content ?: "Invalid multipart body")
                "part" -> {
                    val part = RustHttpMultipartPart(this, packet)
                    handler(part)
                    part.discard()
                }
                else -> error("Unexpected multipart packet")
            }
        }
    }

    internal suspend fun sendPacket(kind: String, fields: JsonObject = JsonObject(emptyMap())) {
        socket.sendText(buildJsonObject {
            put("kind", kind)
            fields.forEach { (key, value) -> put(key, value) }
        }.toString())
    }
    private suspend fun startResponse(kind: String, contentType: String?, fields: JsonObject = JsonObject(emptyMap())) {
        check(!responded) { "HTTP response already started" }
        responded = true
        contentType?.let { responseHeader("Content-Type", it) }
        sendPacket(kind, buildJsonObject {
            put("status", status)
            put("headers", buildJsonObject { outputHeaders.forEach { (name, value) -> put(name, buildJsonArray { add(value) }) } })
            fields.forEach { (key, value) -> put(key, value) }
        })
    }
    override suspend fun respond(bytes: ByteArray, contentType: String?) {
        responseHeader("Content-Length", bytes.size.toString())
        respondStream(contentType, status) { it.write(bytes) }
    }
    override suspend fun respondText(body: String, contentType: String?, status: Int) {
        responseStatus(status)
        respond(body.encodeToByteArray(), contentType)
    }
    override suspend fun respondNoBody(status: Int) { responseStatus(status); respond(ByteArray(0)) }
    override suspend fun respondStream(contentType: String?, status: Int, headers: Map<String, String>, writer: suspend (StreamSink) -> Unit) {
        responseStatus(status)
        headers.forEach { (name, value) -> responseHeader(name, value) }
        startResponse("response", contentType)
        writer(RustStreamSink(socket))
        sendPacket("end")
    }
    override suspend fun respondFile(path: String, contentType: String?, contentDisposition: String?) {
        contentDisposition?.let { responseHeader("Content-Disposition", it) }
        securityHeadersFor(contentType).forEach { (name, value) -> responseHeader(name, value) }
        startResponse("file", contentType, buildJsonObject { put("path", path); put("contentType", contentType ?: "application/octet-stream") })
    }
    override suspend fun proxyUrl(url: String): Boolean {
        return try {
            createPlainHttpClient(PlainHttpClientSpec.Unsafe).use { client ->
                client.get(url).use { response ->
                    response.headers.forEach { (name, values) ->
                        if (!name.equals("Transfer-Encoding", true) && !name.equals("Connection", true)) responseHeader(name, values.joinToString(", "))
                    }
                    respondStream(status = response.status.value) { sink -> response.channel.copyTo { buffer, length -> sink.write(buffer, 0, length) } }
                }
            }
            true
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: Exception) { if (responded) throw error; false }
    }
    override suspend fun respondDlnaFile(path: String): Boolean {
        if (statFile(path) == null) return false
        responseHeader("realTimeInfo.dlna.org", "DLNA.ORG_TLAG=*")
        responseHeader("contentFeatures.dlna.org", "")
        responseHeader("transferMode.dlna.org", "Streaming")
        responseHeader("Server", "DLNADOC/1.50 UPnP/1.0 Plain/1.0")
        val mime = path.getContentType().toString()
        securityHeadersFor(mime).forEach { (name, value) -> responseHeader(name, value) }
        startResponse("file", mime, buildJsonObject { put("path", path); put("contentType", mime); put("dlna", true) })
        return true
    }
    internal suspend fun upgrade() { startResponse("upgrade", null) }
}

private fun isScriptableDocument(mime: String): Boolean {
    val type = mime.substringBefore(';').trim().lowercase()
    return type == "image/svg+xml" || type == "text/html" || type == "application/xhtml+xml" || type.endsWith("+xml") || type.endsWith("/xml")
}

/**
 * Stored-XSS mitigation for user-supplied files (the `/fs` and DLNA families).
 * Document types a browser executes scripts in when navigated to must go out
 * with `Content-Security-Policy: sandbox` — an opaque origin, so scripts and
 * forms are dead while the visual preview still renders. Everything else gets
 * `nosniff` alone, so normal media embedding is untouched.
 *
 * One rule for both response paths, so `/fs` and `/media/{id}` cannot drift.
 */
internal fun securityHeadersFor(contentType: String?): Map<String, String> = buildMap {
    put("X-Content-Type-Options", "nosniff")
    if (isScriptableDocument(contentType.orEmpty())) put("Content-Security-Policy", "sandbox")
}
