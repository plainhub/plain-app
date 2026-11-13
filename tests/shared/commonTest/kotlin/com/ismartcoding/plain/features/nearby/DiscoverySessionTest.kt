package com.ismartcoding.plain.features.nearby

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverySessionTest {
    @Test fun status_is_checked_every_three_seconds_without_restarting_an_active_scan() = runTest {
        var checks = 0
        val calls = mutableListOf<String>()
        val job = launch { discoverySession({ calls.add("start") }, { calls.add("stop") }, { checks++; true }) }
        runCurrent()
        assertEquals(1, checks)
        advanceTimeBy(2999)
        runCurrent()
        assertEquals(1, checks)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, checks)
        assertTrue(calls.isEmpty())
        job.cancelAndJoin()
        advanceTimeBy(9000)
        assertEquals(2, checks)
        assertEquals(listOf("stop"), calls)
    }

    @Test fun a_stopped_scan_restarts_on_the_next_poll() = runTest {
        var scanning = false
        val calls = mutableListOf<String>()
        val job = launch {
            discoverySession({ calls.add("start"); scanning = true }, { calls.add("stop") }, { scanning })
        }
        runCurrent()
        assertEquals(listOf("start"), calls)
        scanning = false
        advanceTimeBy(3000)
        runCurrent()
        assertEquals(listOf("start", "start"), calls)
        advanceTimeBy(3000)
        runCurrent()
        assertEquals(listOf("start", "start"), calls)
        job.cancelAndJoin()
        assertEquals(listOf("start", "start", "stop"), calls)
    }

    @Test fun cancellation_waits_for_a_blocking_start_before_stop() = runTest {
        val entered = CompletableDeferred<Unit>()
        val complete = CompletableDeferred<Unit>()
        val released = mutableListOf<String>()
        val job = launch {
            discoverySession({
                entered.complete(Unit)
                withContext(NonCancellable) { complete.await() }
            }, { released.add("stop") }, { false })
        }
        entered.await()
        job.cancel()
        runCurrent()
        assertTrue(released.isEmpty())
        complete.complete(Unit)
        job.join()
        assertEquals(listOf("stop"), released)
    }

    @Test fun a_failed_status_query_keeps_the_page_polling() = runTest {
        var checks = 0
        val calls = mutableListOf<String>()
        val job = launch {
            discoverySession({ calls.add("start") }, { calls.add("stop") }, {
                checks++
                if (checks == 1) error("Fixture status failure")
                false
            })
        }
        runCurrent()
        assertTrue(calls.isEmpty())
        advanceTimeBy(3000)
        runCurrent()
        assertEquals(listOf("start"), calls)
        job.cancelAndJoin()
        assertEquals(listOf("start", "stop"), calls)
    }
}
