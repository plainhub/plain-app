package com.ismartcoding.plain.features.download

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
        override fun flowSnapshot(): DownloadTaskHandle = this
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
        assertEquals(DownloadStatus.DOWNLOADING, DownloadCenter.get(TASK_ID)?.status)
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
        val statusAfterRequeue = DownloadCenter.get(TASK_ID)?.status
        assertTrue(
            statusAfterRequeue == DownloadStatus.PENDING || statusAfterRequeue == DownloadStatus.DOWNLOADING,
            "re-run dispatches the existing task again, got $statusAfterRequeue",
        )
        releaseGate()
        awaitStatus(DownloadStatus.COMPLETED)
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
        DownloadCenter.remove(orphan.id)
        Unit
    }

    private suspend fun awaitStatus(expected: DownloadStatus) {
        withTimeout(10_000) {
            while (DownloadCenter.get(TASK_ID)?.status != expected) delay(10)
        }
    }

    private suspend fun awaitTerminal(taskId: String): DownloadStatus {
        withTimeout(10_000) {
            while (DownloadCenter.get(taskId)?.status?.isTerminalDownloadStatus() != true) delay(10)
        }
        return DownloadCenter.get(taskId)!!.status
    }

    private suspend fun releaseGate() {
        released = true
        gate.send(Unit)
    }

    companion object {
        private const val TEST_KIND = "test-gate"
        private const val TASK_ID = "enqueue-unique-test"
    }
}
