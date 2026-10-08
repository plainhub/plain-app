package com.ismartcoding.plain.platform

interface StreamSink {
    suspend fun write(bytes: ByteArray)
    suspend fun write(bytes: ByteArray, offset: Int, length: Int)
    suspend fun flush()
    suspend fun close()
}
