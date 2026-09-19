package com.ismartcoding.plain.features.download

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Deterministic queue-semantics tests for [DownloadCenter] using gated fake
 * engines — assertions are on states and call counts, never on wall-clock
 * timing (timeouts only guard against a hung test).
 */
class DownloadCenterTest {

    private class GatedEngine : DownloadEngine {
        val started = mutableListOf<String>()
        val finished = mutableListOf<String>()
        val gates = mutableMapOf<String, CompletableDeferred<Unit>>()
        val failThese = mutableSetOf<String>()

        fun gate(id: String) = gates.getOrPut(id) { CompletableDeferred() }

        override suspend fun execute(task: DownloadTaskHandle) {
            task.status = DownloadStatus.DOWNLOADING
            started.add(task.id)
            DownloadCenter.notifyProgressUpdate()
            if (task.id in failThese) throw RuntimeException("boom")
            gate(task.id).await()
            task.status = DownloadStatus.COMPLETED
        }

        override fun onFinished(task: DownloadTaskHandle) {
            finished.add(task.id)
            if (task.status == DownloadStatus.COMPLETED) {
                DownloadCenter.remove(task.id)
            }
        }
    }

    private class Task(
        override val id: String,
        override val kind: String,
    ) : DownloadTaskHandle {
        override var status = DownloadStatus.PENDING
        override var error = ""
        override var aborted = false
        override var job: kotlinx.coroutines.Job? = null
        override fun flowSnapshot(): DownloadTaskHandle = this
    }

    /** Drops every task (aborting gated ones) so tests can't poison each other. */
    private fun resetCenter() {
        DownloadCenter.all().forEach { DownloadCenter.remove(it.id) }
        until { DownloadCenter.all().isEmpty() }
    }

    private fun until(timeoutMs: Long = 5000, cond: () -> Boolean) {
        runBlocking {
            withTimeoutOrNull(timeoutMs) {
                while (!cond()) yield()
            } ?: error("condition not met in time")
        }
    }

    @Test
    fun addDeduplicatesById() {
        resetCenter()
        val engine = GatedEngine()
        DownloadCenter.registerEngine("test-dedupe", engine)
        val a = Task("dedupe-1", "test-dedupe")
        assertTrue(DownloadCenter.add(a))
        assertFalse(DownloadCenter.add(Task("dedupe-1", "test-dedupe")), "same id must be rejected")
        engine.gate("dedupe-1").complete(Unit)
        until { engine.finished.contains("dedupe-1") }
        until { DownloadCenter.get("dedupe-1") == null }
    }

    @Test
    fun runsAtMostThreeConcurrentlyAndKeepsFifoWaiting() {
        resetCenter()
        val engine = GatedEngine()
        DownloadCenter.registerEngine("test-cap", engine)
        val ids = (1..5).map { "cap-$it" }
        ids.forEach { DownloadCenter.add(Task(it, "test-cap")) }

        until { engine.started.size == 3 }
        // The two extra tasks stay queued in the registry.
        val snapshot = DownloadCenter.all().filter { it.id.startsWith("cap-") }
        assertEquals(5, snapshot.size)
        assertEquals(
            2,
            snapshot.count { it.status == DownloadStatus.PENDING },
            "tasks beyond the concurrency cap must wait as PENDING",
        )

        engine.gate("cap-1").complete(Unit)
        until { engine.started.contains("cap-4") }
        (ids - "cap-1").forEach { engine.gate(it).complete(Unit) }
        until { engine.finished.containsAll(ids) }
    }

    @Test
    fun cancelMarksCanceledAndKeepsTaskForTheFinishedList() {
        resetCenter()
        val engine = GatedEngine()
        DownloadCenter.registerEngine("test-cancel", engine)
        DownloadCenter.add(Task("cancel-1", "test-cancel"))
        until { engine.started.contains("cancel-1") }

        assertTrue(DownloadCenter.cancel("cancel-1"))
        until { DownloadCenter.get("cancel-1")?.status == DownloadStatus.CANCELED }
        assertNotNull(DownloadCenter.get("cancel-1"), "canceled task stays visible until removed")
        assertFalse(DownloadCenter.cancel("cancel-1"), "cancel of a terminal task is a no-op")

        DownloadCenter.remove("cancel-1")
        until { DownloadCenter.get("cancel-1") == null }
    }

    @Test
    fun pauseThenResumeRunsTheTaskAgain() {
        resetCenter()
        val engine = GatedEngine()
        DownloadCenter.registerEngine("test-pause", engine)
        DownloadCenter.add(Task("pause-1", "test-pause"))
        until { engine.started.contains("pause-1") }

        assertTrue(DownloadCenter.pause("pause-1"))
        until { DownloadCenter.get("pause-1")?.status == DownloadStatus.PAUSED }

        assertTrue(DownloadCenter.resume("pause-1"))
        until { engine.started.count { it == "pause-1" } == 2 }
        engine.gate("pause-1").complete(Unit)
        until { engine.finished.contains("pause-1") }
    }

    @Test
    fun engineFailureMarksTaskFailedAndStillNotifiesFinished() {
        resetCenter()
        val engine = GatedEngine()
        DownloadCenter.registerEngine("test-fail", engine)
        engine.failThese.add("fail-1")
        DownloadCenter.add(Task("fail-1", "test-fail"))
        until { DownloadCenter.get("fail-1")?.status == DownloadStatus.FAILED }
        until { engine.finished.contains("fail-1") }
        assertTrue(DownloadCenter.retry("fail-1"), "retry accepts FAILED tasks")
        engine.gate("fail-1").complete(Unit)
        until { engine.finished.count { it == "fail-1" } == 2 }
    }

    @Test
    fun updatesFlowEmitsSnapshots() = runBlocking {
        resetCenter()
        val engine = GatedEngine()
        DownloadCenter.registerEngine("test-updates", engine)
        val seen = mutableListOf<Int>()
        val job = launch(kotlinx.coroutines.Dispatchers.Unconfined) {
            DownloadCenter.updates.collect { seen.add(it.size) }
        }
        DownloadCenter.add(Task("updates-1", "test-updates"))
        until { seen.isNotEmpty() }
        engine.gate("updates-1").complete(Unit)
        until { DownloadCenter.get("updates-1") == null }
        job.cancel()
        assertTrue(seen.isNotEmpty())
    }

    @Test
    fun missingEngineFailsTheTask() {
        resetCenter()
        DownloadCenter.add(Task("noengine-1", "test-missing"))
        until { DownloadCenter.get("noengine-1")?.status == DownloadStatus.FAILED }
        DownloadCenter.remove("noengine-1")
        until { DownloadCenter.get("noengine-1") == null }
    }
}
