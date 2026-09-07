package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.httpserver.http.HttpCall
import com.ismartcoding.plain.httpserver.http.HttpMethod
import com.ismartcoding.plain.httpserver.http.HttpMultipartPart
import com.ismartcoding.plain.httpserver.http.HttpStatus
import com.ismartcoding.plain.httpserver.http.StreamSink
import java.io.File
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the `/fs` codec-negotiation additions: `probe=1` answers the video
 * track's sample entry as JSON, and `tr=1` either serves the transcoded
 * cache or (when the platform pipeline cannot run, as on the host JVM)
 * answers 415 so the client can fall back to the download hint instead of
 * silently playing audio-only.
 */
class FileServerCodecRouteTest {

    private class RecordingCall(private val params: Map<String, String>) : HttpCall {
        override val method = HttpMethod.GET
        override val path = "/fs"
        override val remoteHost = "127.0.0.1"
        val headers = mutableMapOf<String, String>()
        var status: Int = 0
        var body: String? = null
        var bodyContentType: String? = null
        var filePath: String? = null

        override fun queryParam(name: String): String? = params[name]
        override fun queryParamStrings(): Map<String, List<String>> = params.mapValues { listOf(it.value) }
        override fun pathParam(name: String): String? = null
        override fun header(name: String): String? = null
        override suspend fun receiveBody(): ByteArray = ByteArray(0)
        override suspend fun receiveText(): String = ""
        override suspend fun handleMultipart(handler: suspend (HttpMultipartPart) -> Unit) {}
        override fun responseHeader(name: String, value: String) { headers[name] = value }
        override fun responseStatus(status: Int) { this.status = status }
        override suspend fun respond(bytes: ByteArray, contentType: String?) { body = "${bytes.size} bytes"; bodyContentType = contentType }
        override suspend fun respondText(body: String, contentType: String?, status: Int) {
            this.body = body; this.bodyContentType = contentType; this.status = status
        }
        override suspend fun respondNoBody(status: Int) { this.status = status }
        override suspend fun respondStream(
            contentType: String?,
            status: Int,
            headers: Map<String, String>,
            writer: suspend (StreamSink) -> Unit,
        ) { this.status = status; bodyContentType = contentType }
        override suspend fun respondFile(path: String, contentType: String?, contentDisposition: String?) {
            filePath = path; bodyContentType = contentType
        }
        override suspend fun proxyUrl(url: String): Boolean = false
        override suspend fun respondDlnaFile(path: String): Boolean = false
    }

    private fun box(type: String, vararg payloads: ByteArray): ByteArray {
        val size = 8 + payloads.sumOf { it.size }
        val out = ByteBuffer.allocate(size)
        out.putInt(size)
        out.put(type.toByteArray(Charsets.US_ASCII))
        payloads.forEach { out.put(it) }
        return out.array()
    }

    private fun trak(sampleEntry: String, handler: String): ByteArray {
        val hdlrPayload = ByteBuffer.allocate(16).apply {
            putInt(0); putInt(0); put(handler.toByteArray(Charsets.US_ASCII)); putInt(0)
        }
        val entry = box(sampleEntry, ByteArray(20))
        val stsd = box("stsd", byteArrayOf(0, 0, 0, 0, 0, 0, 0, 1), entry)
        return box(
            "trak",
            box("tkhd", ByteArray(16)),
            box("mdia", box("hdlr", hdlrPayload.array()), box("minf", box("stbl", stsd))),
        )
    }

    private fun mp4File(videoEntry: String, audioEntry: String = "mp4a"): File {
        val file = File.createTempFile("fs_codec_", ".mp4")
        file.deleteOnExit()
        file.outputStream().use {
            it.write(box("ftyp", ByteArray(8)))
            it.write(box("mdat", ByteArray(64)))
            it.write(box("moov", trak(videoEntry, "vide"), trak(audioEntry, "soun")))
        }
        return file
    }

    private suspend fun serve(file: File, params: Map<String, String>): RecordingCall {
        val call = RecordingCall(params)
        FileServer.serve(call, file.absolutePath)
        return call
    }

    @Test
    fun probeHevcFile_answersHvc1Json() = kotlinx.coroutines.runBlocking {
        val call = serve(mp4File("hvc1"), mapOf("probe" to "1"))
        assertEquals(HttpStatus.OK, call.status)
        assertEquals("application/json", call.bodyContentType)
        assertEquals("""{"codec":"hvc1"}""", call.body)
    }

    @Test
    fun probeAvcFile_answersAvc1Json() = kotlinx.coroutines.runBlocking {
        val call = serve(mp4File("avc1"), mapOf("probe" to "1"))
        assertEquals("""{"codec":"avc1"}""", call.body)
    }

    @Test
    fun transcodeRequestOnAvcFile_servesOriginalBytes() = kotlinx.coroutines.runBlocking {
        val file = mp4File("avc1")
        val call = serve(file, mapOf("tr" to "1"))
        // Not HEVC — every browser decodes it; tr is ignored.
        assertEquals(file.absolutePath, call.filePath)
        assertEquals("video/mp4", call.bodyContentType)
    }

    @Test
    fun transcodeRequestOnHevcFile_answers415WhenPipelineUnavailable() = kotlinx.coroutines.runBlocking {
        val call = serve(mp4File("hvc1"), mapOf("tr" to "1"))
        // The host JVM has no MediaCodec, so the transcode deterministically
        // fails and the route must surface an explicit error instead of the
        // original audio-only bytes.
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, call.status)
        assertTrue(call.body.isNullOrEmpty().not())
    }

    @Test
    fun plainRequest_servesOriginalFile() = kotlinx.coroutines.runBlocking {
        val file = mp4File("hvc1")
        val call = serve(file, emptyMap())
        assertEquals(file.absolutePath, call.filePath)
    }
}
