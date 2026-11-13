package com.ismartcoding.plain.features.nearby

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverySessionTest {
    @Test fun leaving_view_stops_discovery() = runTest {
        val calls = mutableListOf<String>()
        val acquired = CompletableDeferred<Unit>()
        val job = launch { discoverySession({ calls.add("start"); acquired.complete(Unit) }, { calls.add("stop") }, MutableSharedFlow()) }
        acquired.await()
        assertEquals(listOf("start"), calls)
        job.cancelAndJoin()
        assertEquals(listOf("start", "stop"), calls)
    }

    @Test fun a_start_error_still_stops_the_possibly_started_scan() = runTest {
        val released = mutableListOf<String>()
        assertFailsWith<IllegalStateException> {
            discoverySession({ error("Fixture lost reply") }, { released.add("stop") }, emptyFlow())
        }
        assertEquals(listOf("stop"), released)
    }

    @Test fun cancellation_waits_for_a_blocking_start_before_stop() = runTest {
        val entered = CompletableDeferred<Unit>()
        val complete = CompletableDeferred<Unit>()
        val released = mutableListOf<String>()
        val job = launch {
            discoverySession({
                entered.complete(Unit)
                withContext(NonCancellable) { complete.await() }
            }, { released.add("stop") }, emptyFlow())
        }
        entered.await()
        job.cancel()
        runCurrent()
        assertTrue(released.isEmpty())
        complete.complete(Unit)
        job.join()
        assertEquals(listOf("stop"), released)
    }
    @Test fun a_stopped_scan_restarts_until_the_page_leaves() = runTest {
        val scanning = MutableSharedFlow<Boolean>()
        val calls = mutableListOf<String>()
        val job = launch { discoverySession({ calls.add("start") }, { calls.add("stop") }, scanning) }
        runCurrent()
        scanning.emit(true)
        scanning.emit(false)
        runCurrent()
        assertEquals(listOf("start", "start"), calls)
        job.cancelAndJoin()
        scanning.emit(false)
        assertEquals(listOf("start", "start", "stop"), calls)
    }
}
