package com.ismartcoding.plain.platform.files

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class ResourceChunksTest {
    @Test
    fun synchronousCompletionPreservesExactBytesAndDoesNotCancelTheCompletedRequest() = runTest {
        val output = mutableListOf<Byte>()
        val cancellations = mutableListOf<Int>()
        val bytes = listOf(byteArrayOf(-1, 0, 42), byteArrayOf(1, 127))
        val length = readResourceChunks<ByteArray>(
            start = { data, complete ->
                bytes.forEach(data)
                complete(null)
                data(byteArrayOf(99))
                complete(IllegalStateException("late completion"))
                7
            },
            cancel = { cancellations.add(it) },
            size = { it.size.toLong() },
            write = { output.addAll(it.toList()) },
        )
        assertEquals(5L, length)
        assertEquals(listOf<Byte>(-1, 0, 42, 1, 127), output)
        assertEquals(emptyList(), cancellations)
    }

    @Test
    fun cancellationBeforeRequestIdPublicationCancelsOnceAndIgnoresLaterData() = runTest {
        val cancellations = mutableListOf<Int>()
        var writes = 0
        var data: ((ByteArray) -> Unit)? = null
        val read = async(start = CoroutineStart.UNDISPATCHED) {
            val context = currentCoroutineContext()
            readResourceChunks<ByteArray>(
                start = { receive, complete ->
                    data = receive
                    context.cancel()
                    receive(byteArrayOf(1))
                    complete(null)
                    7
                },
                cancel = { cancellations.add(it) },
                size = { it.size.toLong() },
                write = { writes++ },
            )
        }
        assertFailsWith<CancellationException> { read.await() }
        data!!(byteArrayOf(2))
        assertEquals(listOf(7), cancellations)
        assertEquals(0, writes)
    }

    @Test
    fun cancellationAfterPublicationClosesTheSinkBeforeLateCallbacks() = runTest {
        val cancellations = mutableListOf<Int>()
        var data: ((ByteArray) -> Unit)? = null
        var complete: ((Throwable?) -> Unit)? = null
        var writes = 0
        val read = async {
            readResourceChunks<ByteArray>(
                start = { receive, done -> data = receive; complete = done; 7 },
                cancel = {
                    cancellations.add(it)
                    data!!(byteArrayOf(99))
                    complete!!(null)
                },
                size = { it.size.toLong() },
                write = { writes++ },
            )
        }
        runCurrent()
        data!!(byteArrayOf(1))
        read.cancelAndJoin()
        data!!(byteArrayOf(2))
        complete!!(null)
        complete!!(IllegalStateException("canceled"))
        assertEquals(listOf(7), cancellations)
        assertEquals(1, writes)
    }

    @Test
    fun aSinkFailureStopsWritesAndCancelsAnIdPublishedAfterTheFailure() = runTest {
        val cancellations = mutableListOf<Int>()
        var writes = 0
        val failure = assertFailsWith<IllegalStateException> {
            readResourceChunks<ByteArray>(
                start = { data, complete ->
                    data(byteArrayOf(1))
                    data(byteArrayOf(2))
                    complete(null)
                    7
                },
                cancel = { cancellations.add(it) },
                size = { it.size.toLong() },
                write = { writes++; error("sink failed") },
            )
        }
        assertEquals("sink failed", failure.message)
        assertEquals(1, writes)
        assertEquals(listOf(7), cancellations)
    }

    @Test
    fun aSourceFailureNeverTurnsPartialDataIntoASuccessfulResource() = runTest {
        val failure = assertFailsWith<IllegalStateException> {
            readResourceChunks<ByteArray>(
                start = { data, complete -> data(byteArrayOf(1)); complete(IllegalStateException("unavailable locally")); 7 },
                cancel = { error("completed source must not be canceled") },
                size = { it.size.toLong() },
                write = {},
            )
        }
        assertEquals("unavailable locally", failure.message)
    }

    @Test
    fun invalidLengthsFailBeforeTheInvalidChunkTouchesTheSink() = runTest {
        for (sizes in listOf(listOf(-1L), listOf(Long.MAX_VALUE, 1L))) {
            var writes = 0
            val failure = assertFailsWith<IllegalStateException> {
                readResourceChunks<Long>(
                    start = { data, _ -> sizes.forEach(data); 7 },
                    cancel = {},
                    size = { it },
                    write = { writes++ },
                )
            }
            assertEquals("Resource length overflow", failure.message)
            assertEquals(sizes.size - 1, writes)
        }
    }

    @Test
    fun nullNativeDataCancelsTheSourceWithoutWritingAnEmptyResource() = runTest {
        var writes = 0
        val cancellations = mutableListOf<Int>()
        val failure = assertFailsWith<IllegalStateException> {
            readResourceChunks<ByteArray?>(
                start = { data, _ -> data(null); 7 },
                cancel = { cancellations.add(it) },
                size = { checkNotNull(it) { "native data missing" }.size.toLong() },
                write = { writes++ },
            )
        }
        assertEquals("native data missing", failure.message)
        assertEquals(0, writes)
        assertEquals(listOf(7), cancellations)
    }

    @Test
    fun missingOrFailedRequestCreationDoesNotLeaveASuspendedReader() = runTest {
        assertFailsWith<IllegalStateException> {
            readResourceChunks<Unit>(start = { _, _ -> 0 }, cancel = {}, size = { 0 }, write = {})
        }
        val failure = assertFailsWith<IllegalArgumentException> {
            readResourceChunks<Unit>(start = { _, _ -> throw IllegalArgumentException("start failed") }, cancel = {}, size = { 0 }, write = {})
        }
        assertEquals("start failed", failure.message)
    }
}
