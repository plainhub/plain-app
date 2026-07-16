package com.ismartcoding.plain.webserver

import com.ismartcoding.plain.api.OkHttpClientFactory
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.web.http.HttpCall
import com.ismartcoding.plain.web.http.HttpMethod
import com.ismartcoding.plain.web.http.HttpMultipartPart
import com.ismartcoding.plain.web.http.StreamSink
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.plugins.origin
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.utils.io.jvm.javaio.copyTo
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.toByteArray
import io.ktor.util.toMap
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
        val file = java.io.File(path)
        if (contentType != null) {
            applicationCall.respond(io.ktor.server.http.content.LocalFileContent(file, ContentType.parse(contentType)))
        } else {
            applicationCall.respond(io.ktor.server.http.content.LocalFileContent(file))
        }
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
            if (body == null) {
                applicationCall.respond(HttpStatusCode.NotFound)
                return@proxyUrl false
            }
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
        val file = java.io.File(path)
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
        applicationCall.respond(io.ktor.server.http.content.LocalFileContent(file))
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
