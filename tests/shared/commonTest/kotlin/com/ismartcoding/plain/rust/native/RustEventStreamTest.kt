package com.ismartcoding.plain.rust.native

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class RustEventStreamTest {
    @Test
    fun bridge_failure_cancels_pending_host_requests_instead_of_leaving_the_dispatcher_alive() = runTest {
        val requests = Channel<String>(2)
        val values = ArrayDeque(listOf("", "request"))
        var reads = 0
        var closures = 0
        assertFailsWith<IllegalStateException> {
            readRustStream(
                next = { reads++; if (values.isEmpty()) error("stream closed") else values.removeFirst() },
                consume = { requests.send(it) },
                closed = { closures++; requests.cancel() },
            )
        }
        assertEquals(3, reads)
        assertEquals(1, closures)
        assertTrue(requests.receiveCatching().isClosed)
    }

    @Test
    fun canceling_a_pending_native_read_runs_cleanup_once() = runTest {
        val entered = CompletableDeferred<Unit>()
        var closures = 0
        val reader = launch {
            readRustStream(
                next = { entered.complete(Unit); awaitCancellation() },
                consume = { fail("unexpected packet") },
                closed = { closures++ },
            )
        }
        entered.await()
        reader.cancelAndJoin()
        assertEquals(1, closures)
    }
}
