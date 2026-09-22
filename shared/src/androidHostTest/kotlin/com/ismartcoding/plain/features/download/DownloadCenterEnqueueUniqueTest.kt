package com.ismartcoding.plain.features.download

import kotlinx.coroutines.Job
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Locks [DownloadCenter.enqueueUnique] re-download semantics: a fresh user
 * action on an already-finished task id must re-run it (keeping engine
 * bookkeeping), never silently no-op; a still-active twin must be rejected.
 */
class DownloadCenterEnqueueUniqueTest {

    private val gate = Channel<Unit>(Channel.RENDEZVOUS)
    private var released = false

    private val gateEngine = object : DownloadEngine {
        override suspend fun execute(task: DownloadTaskHandle) {
            task.status = DownloadStatus.DOWNLOADING
            gate.receive()
            task.status = DownloadStatus.COMPLETED
        }
    }

    private class DummyTask(override val id: String) : DownloadTaskHandle {
        override val kind = TEST_KIND
        override var status: DownloadStatus = DownloadStatus.PENDING
        override var error: String = ""
        override var aborted: Boolean = false
        override var job: Job? = null
        var refreshed = false
        override fun flowSnapshot(): DownloadTaskHandle = this
        override fun refreshFrom(fresh: DownloadTaskHandle) {
            refreshed = true
        }
    }

    init {
        DownloadCenter.registerEngine(TEST_KIND, gateEngine)
    }

    @AfterTest
    fun tearDown() {
        if (!released) gate.tryReceive()
        released = false
        DownloadCenter.remove(TASK_ID)
    }

    @Test
    fun duplicateWhileActiveIsRejected() = runBlocking {
        assertTrue(DownloadCenter.enqueueUnique(DummyTask(TASK_ID)))
        awaitStatus(DownloadStatus.DOWNLOADING)

        assertFalse(
            DownloadCenter.enqueueUnique(DummyTask(TASK_ID)),
            "an identical task that is queued or running must not be re-dispatched",
        )
        assertEquals(DownloadStatus.DOWNLOADING, DownloadCenter.progress.value[TASK_ID]?.status)
    }

    @Test
    fun finishedTaskIsReRunByFreshEnqueue() = runBlocking {
        assertTrue(DownloadCenter.enqueueUnique(DummyTask(TASK_ID)))
        awaitStatus(DownloadStatus.DOWNLOADING)
        releaseGate()
        awaitStatus(DownloadStatus.COMPLETED)

        assertTrue(
            DownloadCenter.enqueueUnique(DummyTask(TASK_ID)),
            "a finished twin must be re-run, not silently dropped",
        )
        withTimeout(10_000) {
            while (DownloadCenter.progress.value[TASK_ID]?.status?.let { it == DownloadStatus.PENDING || it == DownloadStatus.DOWNLOADING } != true) {
                delay(10)
            }
        }
        releaseGate()
        awaitStatus(DownloadStatus.COMPLETED)
    }

    @Test
    fun reRunRefreshesContextFromTheFreshTask() = runBlocking {
        val first = DummyTask(TASK_ID)
        assertTrue(DownloadCenter.enqueueUnique(first))
        awaitStatus(DownloadStatus.DOWNLOADING)
        releaseGate()
        awaitStatus(DownloadStatus.COMPLETED)

        val fresh = DummyTask(TASK_ID)
        assertTrue(DownloadCenter.enqueueUnique(fresh))
        awaitStatus(DownloadStatus.DOWNLOADING)
        assertTrue(first.refreshed, "a re-run must merge the fresh task's transport context (e.g. new endpoint)")
        assertFalse(fresh.refreshed, "the discarded fresh task is never the one refreshed")
        releaseGate()
        awaitStatus(DownloadStatus.COMPLETED)
        Unit
    }

    @Test
    fun taskWithoutEngineFailsVisibly() = runBlocking {
        val orphan = object : DownloadTaskHandle {
            override val id = "orphan|$TASK_ID"
            override val kind = "no-such-engine"
            override var status: DownloadStatus = DownloadStatus.PENDING
            override var error: String = ""
            override var aborted: Boolean = false
            override var job: Job? = null
            override fun flowSnapshot(): DownloadTaskHandle = this
        }
        assertTrue(DownloadCenter.add(orphan))
        assertEquals(
            DownloadStatus.FAILED,
            awaitTerminal(orphan.id),
            "a task whose engine is missing must fail fast, never hang or vanish",
        )
        assertTrue(
            DownloadCenter.progress.value[orphan.id]!!.error.isNotEmpty(),
            "engine-less failures must carry a visible error, not just a log line",
        )
        DownloadCenter.remove(orphan.id)
        Unit
    }

    // Poll the progress StateFlow (happens-before with worker status writes);
    // reading the registry handle's raw field can loop on a stale value on arm64.
    /**
     * Locks the requeue-vs-verdict race: a previous execution that is parked
     * between engine completion and its own terminal check must never flip a
     * freshly requeued (PENDING) task to FAILED — the re-dispatched entry
     * would then be dropped as stale and the task silently disappear.
     */
    @Test
    fun previousRunNeverFailsARequeuedTask() = runBlocking {
        val run = java.util.concurrent.atomic.AtomicInteger(0)
        val run1Done = kotlinx.coroutines.CompletableDeferred<Unit>()
        val proceedRun1 = kotlinx.coroutines.CompletableDeferred<Unit>()
        val proceedRun2 = kotlinx.coroutines.CompletableDeferred<Unit>()
        val raceEngine = object : DownloadEngine {
            override suspend fun execute(task: DownloadTaskHandle) {
                task.status = DownloadStatus.DOWNLOADING
                if (run.incrementAndGet() == 1) {
                    // run 1 reports COMPLETED, then parks the DownloadCenter
                    // worker between engine completion and its own verdict
                    task.status = DownloadStatus.COMPLETED
                    run1Done.complete(Unit)
                    proceedRun1.await()
                } else {
                    // run 2 parks mid-transfer until the test releases it
                    proceedRun2.await()
                    task.status = DownloadStatus.COMPLETED
                }
            }
        }
        DownloadCenter.registerEngine(TEST_KIND, raceEngine)
        assertTrue(DownloadCenter.enqueueUnique(DummyTask(TASK_ID)))
        run1Done.await()

        // While the previous execution is parked in join(), requeue the task.
        assertTrue(
            DownloadCenter.enqueueUnique(DummyTask(TASK_ID)),
            "a completed twin must be re-runnable",
        )
        proceedRun1.complete(Unit)

        // The requeued entry must actually execute — never get FAILED by the
        // parked previous run, and never be dropped as a stale entry.
        withTimeout(10_000) {
            while (DownloadCenter.progress.value[TASK_ID]?.status != DownloadStatus.DOWNLOADING) {
                delay(10)
            }
        }
        proceedRun2.complete(Unit)
        awaitStatus(DownloadStatus.COMPLETED)
        Unit
    }

    private suspend fun awaitStatus(expected: DownloadStatus) {
        withTimeout(10_000) {
            while (DownloadCenter.progress.value[TASK_ID]?.status != expected) delay(10)
        }
    }

    private suspend fun awaitTerminal(taskId: String): DownloadStatus {
        withTimeout(10_000) {
            while (DownloadCenter.progress.value[taskId]?.status?.isTerminalDownloadStatus() != true) delay(10)
        }
        return DownloadCenter.progress.value[taskId]!!.status
    }

    private suspend fun releaseGate() {
        released = true
        withTimeout(10_000) { gate.send(Unit) }
    }

    @Test
    fun rerunCannotExecuteWhilePreviousEngineIsStillReturning() = runBlocking {
        val firstTerminal = CompletableDeferred<Unit>()
        val finishFirst = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()
        val finishSecond = CompletableDeferred<Unit>()
        val executions = java.util.concurrent.atomic.AtomicInteger()
        DownloadCenter.registerEngine(TEST_KIND, object : DownloadEngine {
            override suspend fun execute(task: DownloadTaskHandle) {
                if (executions.incrementAndGet() == 1) {
                    task.status = DownloadStatus.COMPLETED
                    firstTerminal.complete(Unit)
                    finishFirst.await()
                } else {
                    secondStarted.complete(Unit)
                    finishSecond.await()
                    task.status = DownloadStatus.COMPLETED
                }
            }
        })
        try {
            assertTrue(DownloadCenter.enqueueUnique(DummyTask(TASK_ID)))
            withTimeout(5000) { firstTerminal.await() }
            assertTrue(DownloadCenter.enqueueUnique(DummyTask(TASK_ID)))
            assertEquals(null, withTimeoutOrNull(300) { secondStarted.await(); true },
                "a terminal status must not let two engines mutate the same task concurrently")
            finishFirst.complete(Unit)
            withTimeout(5000) { secondStarted.await() }
            assertEquals(DownloadStatus.DOWNLOADING, DownloadCenter.get(TASK_ID)?.status)
            finishSecond.complete(Unit)
            awaitStatus(DownloadStatus.COMPLETED)
        } finally {
            finishFirst.complete(Unit)
            finishSecond.complete(Unit)
        }
    }

    @Test
    fun resumeWaitsForCanceledExecutionCleanup() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val cleaning = CompletableDeferred<Unit>()
        val finishCleanup = CompletableDeferred<Unit>()
        val resumed = CompletableDeferred<Unit>()
        val finishResumed = CompletableDeferred<Unit>()
        val executions = java.util.concurrent.atomic.AtomicInteger()
        DownloadCenter.registerEngine(TEST_KIND, object : DownloadEngine {
            override suspend fun execute(task: DownloadTaskHandle) {
                if (executions.incrementAndGet() == 1) {
                    started.complete(Unit)
                    try {
                        CompletableDeferred<Unit>().await()
                    } finally {
                        withContext(NonCancellable) {
                            cleaning.complete(Unit)
                            finishCleanup.await()
                        }
                    }
                } else {
                    resumed.complete(Unit)
                    finishResumed.await()
                    task.status = DownloadStatus.COMPLETED
                }
            }
        })
        try {
            assertTrue(DownloadCenter.enqueueUnique(DummyTask(TASK_ID)))
            withTimeout(5000) { started.await() }
            assertTrue(DownloadCenter.pause(TASK_ID))
            withTimeout(5000) { cleaning.await() }
            assertTrue(DownloadCenter.resume(TASK_ID))
            assertEquals(null, withTimeoutOrNull(300) { resumed.await(); true })
            finishCleanup.complete(Unit)
            withTimeout(5000) { resumed.await() }
            assertEquals(DownloadStatus.DOWNLOADING, DownloadCenter.get(TASK_ID)?.status)
            finishResumed.complete(Unit)
            awaitStatus(DownloadStatus.COMPLETED)
        } finally {
            finishCleanup.complete(Unit)
            finishResumed.complete(Unit)
        }
    }

    companion object {
        private const val TEST_KIND = "test-gate"
        private const val TASK_ID = "enqueue-unique-test"
    }
}
