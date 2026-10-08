package com.ismartcoding.plain.platform

import com.ismartcoding.plain.platform.PlainWebSocketSession

internal class ResourceStreamSink(private val socket: PlainWebSocketSession) : StreamSink {
    private var closed = false
    override suspend fun write(bytes: ByteArray) = write(bytes, 0, bytes.size)
    override suspend fun write(bytes: ByteArray, offset: Int, length: Int) {
        check(!closed) { "Native resource sink is closed" }
        require(offset >= 0 && length >= 0 && offset <= bytes.size - length)
        var current = offset
        while (current < offset + length) {
            val end = minOf(current + 64 * 1024, offset + length)
            socket.sendBinary(bytes.copyOfRange(current, end))
            current = end
        }
    }
    override suspend fun flush() {}
    override suspend fun close() { closed = true }
}
