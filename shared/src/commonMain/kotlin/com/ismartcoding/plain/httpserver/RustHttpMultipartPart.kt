package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.httpserver.http.HttpMultipartPart
import com.ismartcoding.plain.httpserver.http.StreamSink
import kotlinx.serialization.json.*

internal class RustHttpMultipartPart(private val call: RustHttpCall, packet: JsonObject) : HttpMultipartPart {
    override val name = packet["name"]?.jsonPrimitive?.contentOrNull
    override val originalFileName = packet["filename"]?.jsonPrimitive?.contentOrNull
    override val contentType = packet["contentType"]?.jsonPrimitive?.contentOrNull
    private var consumed = false
    private suspend fun consume(write: suspend (ByteArray) -> Unit) {
        check(!consumed) { "Multipart part already consumed" }
        call.consume("partEnd", write)
        consumed = true
    }
    override suspend fun readBytes(): ByteArray {
        val chunks = ArrayList<ByteArray>()
        var length = 0
        consume {
            check(length <= 32 * 1024 * 1024 - it.size) { "Multipart field exceeds 32 MiB" }
            chunks.add(it); length += it.size
        }
        val result = ByteArray(length)
        var offset = 0
        chunks.forEach { it.copyInto(result, offset); offset += it.size }
        return result
    }
    override suspend fun copyTo(sink: StreamSink) {
        try { consume { sink.write(it) } } finally { sink.close() }
    }
    suspend fun discard() { if (!consumed) consume {} }
}
