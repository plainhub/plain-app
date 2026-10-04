package com.ismartcoding.plain.features.download

import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals

class DownloadIoTest {
    @Test
    fun sharedIoCapsTransfersAndReleasesCanceledPermits() = runBlocking {
        val started = AtomicInteger()
        val active = AtomicInteger()
        val max = AtomicInteger()
        val gates = List(6) { CompletableDeferred<Unit>() }
        val jobs = gates.map { gate -> launch(Dispatchers.Default) {
            DownloadIo.run {
                val current = active.incrementAndGet()
                max.updateAndGet { maxOf(it, current) }
                started.incrementAndGet()
                try { gate.await() } finally { active.decrementAndGet() }
            }
        } }
        withTimeout(5000) { while (started.get() != 3) yield() }
        assertEquals(3, active.get())
        jobs.forEach { it.cancel() }
        jobs.joinAll()
        DownloadIo.run { assertEquals(0, active.get()) }
        assertEquals(3, max.get())
    }
}
