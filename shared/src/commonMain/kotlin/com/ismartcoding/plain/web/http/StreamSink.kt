package com.ismartcoding.plain.web.http

/**
 * Destination for streaming binary data. Used by [HttpCall.respondStream],
 * [HttpMultipartPart.copyTo], and platform helpers like `createFileSink`.
 *
 * Implementations must be safe to call from a suspend context. Blocking IO
 * inside [write] should be wrapped in `withIO` on platforms where it is
 * required (JVM/Android) or be natively non-blocking (NIO on iOS).
 */
interface StreamSink {
    suspend fun write(bytes: ByteArray)
    suspend fun write(bytes: ByteArray, offset: Int, length: Int)
    suspend fun flush()
    suspend fun close()
}

/**
 * Sink that discards all data. Handy as a fallback when a real sink is
 * unavailable (e.g. iOS stubs during migration).
 */
object NullStreamSink : StreamSink {
    override suspend fun write(bytes: ByteArray) {}
    override suspend fun write(bytes: ByteArray, offset: Int, length: Int) {}
    override suspend fun flush() {}
    override suspend fun close() {}
}
