package com.ismartcoding.plain.platform.files

import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class InputStreamChunksTest {
    private class Sink : StreamSink {
        val chunks = mutableListOf<ByteArray>()
        override suspend fun write(bytes: ByteArray) = write(bytes, 0, bytes.size)
        override suspend fun write(bytes: ByteArray, offset: Int, length: Int) { chunks += bytes.copyOfRange(offset, offset + length) }
        override suspend fun flush() {}
        override suspend fun close() {}
    }

    @Test
    fun nonSeekablePipeStreamsExactBytesInBoundedChunks() = runBlocking {
        val bytes = ByteArray(2 * 1024 * 1024 + 3) { (it % 251).toByte() }
        val input = PipedInputStream(8192)
        val output = PipedOutputStream(input)
        val worker = Executors.newSingleThreadExecutor()
        try {
            val produced = worker.submit { output.use { it.write(bytes) } }
            val sink = Sink()
            input.use { it.copyChunksTo(sink) }
            produced.get(5, TimeUnit.SECONDS)
            assertTrue(sink.chunks.all { it.isNotEmpty() && it.size <= 64 * 1024 })
            assertContentEquals(bytes, sink.chunks.fold(ByteArray(0)) { value, chunk -> value + chunk })
        } finally {
            input.close()
            output.close()
            worker.shutdownNow()
        }
    }

    @Test
    fun uploadCancellationPropagatesOutOfTheStream() = runBlocking<Unit> {
        val sink = object : StreamSink {
            override suspend fun write(bytes: ByteArray) = write(bytes, 0, bytes.size)
            override suspend fun write(bytes: ByteArray, offset: Int, length: Int) { throw CancellationException("Fixture cancellation") }
            override suspend fun flush() {}
            override suspend fun close() {}
        }
        assertFailsWith<CancellationException> { byteArrayOf(1, 2, 3).inputStream().use { it.copyChunksTo(sink) } }
    }
}
