package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.api.OkHttpClientFactory
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.httpserver.http.HttpCall
import com.ismartcoding.plain.httpserver.http.HttpMethod
import com.ismartcoding.plain.httpserver.http.HttpMultipartPart
import com.ismartcoding.plain.httpserver.http.StreamSink
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import com.ismartcoding.plain.lib.ktorserver.core.application.ApplicationCall
import com.ismartcoding.plain.lib.ktorserver.core.plugins.origin
import com.ismartcoding.plain.lib.ktorserver.core.request.path
import com.ismartcoding.plain.lib.ktorserver.core.request.receive
import com.ismartcoding.plain.lib.ktorserver.core.request.receiveMultipart
import com.ismartcoding.plain.lib.ktorserver.core.response.header
import com.ismartcoding.plain.lib.ktorserver.core.response.respond
import com.ismartcoding.plain.lib.ktorserver.core.response.respondBytes
import com.ismartcoding.plain.lib.ktorserver.core.response.respondOutputStream
import com.ismartcoding.plain.lib.ktorserver.core.response.respondText
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.toByteArray
import io.ktor.util.toMap
import java.io.File
import java.io.OutputStream

/**
 * Adapts a Ktor [ApplicationCall] to the commonMain [HttpCall] interface so
 * that route handlers can live entirely in shared code.
 */
class KtorHttpCall(
    private val applicationCall: ApplicationCall,
    private val pathParameters: Map<String, String> = emptyMap(),
) : HttpCall {

    override val method: HttpMethod
        get() = HttpMethod(applicationCall.request.local.method.value)

    override val path: String
        get() = applicationCall.request.path()

    override val remoteHost: String
        get() = applicationCall.request.origin.remoteHost

    override fun queryParam(name: String): String? =
        applicationCall.request.queryParameters[name]

    override fun queryParamStrings(): Map<String, List<String>> =
        applicationCall.request.queryParameters.toMap()

    override fun pathParam(name: String): String? = pathParameters[name]

    override fun header(name: String): String? = applicationCall.request.headers[name]

    override suspend fun receiveBody(): ByteArray = applicationCall.receive()

    override suspend fun receiveText(): String = applicationCall.receive()

    override suspend fun handleMultipart(handler: suspend (HttpMultipartPart) -> Unit) {
        applicationCall.receiveMultipart(formFieldLimit = Long.MAX_VALUE).forEachPart { part ->
            if (part is PartData.FileItem) {
                handler(KtorMultipartPart(part))
            }
            part.dispose()
        }
    }

    override fun responseHeader(name: String, value: String) {
        applicationCall.response.header(name, value)
    }

    override fun responseStatus(status: Int) {
        applicationCall.response.status(HttpStatusCode.fromValue(status))
    }

    override suspend fun respond(bytes: ByteArray, contentType: String?) {
        if (contentType != null) {
            applicationCall.respondBytes(bytes, ContentType.parse(contentType))
        } else {
            applicationCall.respondBytes(bytes)
        }
    }

    override suspend fun respondText(body: String, contentType: String?, status: Int) {
        applicationCall.response.status(HttpStatusCode.fromValue(status))
        if (contentType != null) {
            applicationCall.respondText(body, ContentType.parse(contentType))
        } else {
            applicationCall.respondText(body)
        }
    }

    override suspend fun respondNoBody(status: Int) {
        applicationCall.response.status(HttpStatusCode.fromValue(status))
        applicationCall.respond(HttpStatusCode.fromValue(status))
    }

    override suspend fun respondStream(
        contentType: String?,
        status: Int,
        headers: Map<String, String>,
        writer: suspend (StreamSink) -> Unit,
    ) {
        applicationCall.response.status(HttpStatusCode.fromValue(status))
        headers.forEach { (k, v) -> applicationCall.response.header(k, v) }
        val parsedContentType = contentType?.let { ContentType.parse(it) }
        applicationCall.respondOutputStream(parsedContentType) {
            val sink = OutputStreamSink(this)
            try {
                writer(sink)
            } finally {
                sink.flush()
            }
        }
    }

    override suspend fun respondFile(
        path: String,
        contentType: String?,
        contentDisposition: String?,
    ) {
        contentDisposition?.let { applicationCall.response.header("Content-Disposition", it) }
        val file = File(path)
        if (!file.exists() || !file.isFile) {
            applicationCall.response.status(HttpStatusCode.NotFound)
            applicationCall.respondBytes(ByteArray(0))
            return
        }

        val fileLength = file.length()
        val range = resolveSingleByteRange(applicationCall.request.headers["Range"], fileLength)
        if (range == null) {
            applicationCall.response.run {
                status(HttpStatusCode.RequestedRangeNotSatisfiable)
                header("Accept-Ranges", "bytes")
                header("Content-Range", "bytes */$fileLength")
            }
            applicationCall.respondBytes(ByteArray(0))
            return
        }

        val status = if (range.isPartial) HttpStatusCode.PartialContent else HttpStatusCode.OK
        val parsedContentType = contentType?.let { ContentType.parse(it) }
        applicationCall.respond(
            LowMemoryFileContent(
                file = file,
                contentType = parsedContentType,
                status = status,
                contentLength = range.length,
                range = range,
                totalLength = fileLength,
            )
        )
    }

    override suspend fun proxyUrl(url: String): Boolean {
        return try {
            val client = OkHttpClientFactory.createUnsafeOkHttpClient()
            val request = okhttp3.Request.Builder().url(url).build()
            val response = withIO { client.newCall(request).execute() }
            applicationCall.response.status(HttpStatusCode.fromValue(response.code))
            for ((name, value) in response.headers) {
                if (!name.equals("Transfer-Encoding", true) &&
                    !name.equals("Connection", true)
                ) {
                    applicationCall.response.headers.append(name, value)
                }
            }
            val body = response.body
            applicationCall.respondOutputStream {
                body.byteStream().use { input ->
                    input.copyTo(this)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun respondDlnaFile(path: String): Boolean {
        val file = File(path)
        if (!file.exists()) return false
        applicationCall.response.run {
            header("realTimeInfo.dlna.org", "DLNA.ORG_TLAG=*")
            header("contentFeatures.dlna.org", "")
            header("transferMode.dlna.org", "Streaming")
            header("Connection", "keep-alive")
            header(
                "Server",
                "DLNADOC/1.50 UPnP/1.0 Plain/1.0 Android/${android.os.Build.VERSION.RELEASE}",
            )
            io.ktor.http.content.EntityTagVersion(file.lastModified().hashCode().toString())
            io.ktor.http.content.LastModifiedVersion(java.util.Date(file.lastModified()))
            status(HttpStatusCode.PartialContent) // some TV OS only accept 206
        }
        applicationCall.respond(com.ismartcoding.plain.lib.ktorserver.core.http.content.LocalFileContent(file))
        return true
    }
}

/**
 * Bridges a Ktor [PartData.FileItem] to the platform-agnostic
 * [HttpMultipartPart] interface.
 */
private class KtorMultipartPart(
    private val part: PartData.FileItem,
) : HttpMultipartPart {
    override val name: String? get() = part.name
    override val originalFileName: String? get() = part.originalFileName
    override val contentType: String? get() = part.contentType?.toString()

    override suspend fun readBytes(): ByteArray = part.provider().toByteArray()

    override suspend fun copyTo(sink: StreamSink) {
        val channel = part.provider()
        val buffer = ByteArray(64 * 1024)
        try {
            while (!channel.isClosedForRead) {
                val read = channel.readAvailable(buffer)
                if (read <= 0) break
                sink.write(buffer, 0, read)
            }
            sink.flush()
        } finally {
            sink.close()
        }
    }
}

/** Adapts a Java [OutputStream] to the commonMain [StreamSink] interface. */
private class OutputStreamSink(private val os: OutputStream) : StreamSink {
    override suspend fun write(bytes: ByteArray) {
        os.write(bytes)
    }

    override suspend fun write(bytes: ByteArray, offset: Int, length: Int) {
        os.write(bytes, offset, length)
    }

    override suspend fun flush() {
        os.flush()
    }

    override suspend fun close() {
        // OutputStream is closed by Ktor after respondOutputStream returns
    }
}
