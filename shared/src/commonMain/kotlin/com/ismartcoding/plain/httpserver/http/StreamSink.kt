package com.ismartcoding.plain.httpserver.http

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
