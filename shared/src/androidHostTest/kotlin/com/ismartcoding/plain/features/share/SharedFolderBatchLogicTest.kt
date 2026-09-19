package com.ismartcoding.plain.features.share

import com.ismartcoding.plain.features.download.DownloadStatus
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Locks the terminal batch-status derivation (active outranks error) and the
 * batch progress fraction — the aggregation math behind the download cards.
 */
class SharedFolderBatchLogicTest {

    @Test
    fun runningOutranksFailures() {
        assertEquals(
            DownloadStatus.DOWNLOADING,
            deriveShareBatchStatus(running = true, canceled = false, doneFiles = 3, failedFiles = 5),
            "a running batch with failures keeps its progress bar",
        )
    }

    @Test
    fun allDoneNoFailuresIsCompleted() {
        assertEquals(DownloadStatus.COMPLETED, deriveShareBatchStatus(false, false, doneFiles = 10, failedFiles = 0))
    }

    @Test
    fun someFailedSomeDoneIsPartial() {
        assertEquals(DownloadStatus.PARTIAL, deriveShareBatchStatus(false, false, doneFiles = 11, failedFiles = 3))
    }

    @Test
    fun allFailedIsFailed() {
        assertEquals(DownloadStatus.FAILED, deriveShareBatchStatus(false, false, doneFiles = 0, failedFiles = 3))
    }

    @Test
    fun canceledWinsOverFailuresWhenNotRunning() {
        assertEquals(DownloadStatus.CANCELED, deriveShareBatchStatus(false, true, doneFiles = 2, failedFiles = 4))
    }

    @Test
    fun fractionIsZeroWhileEnumeratingAndClampedDuringTransfer() {
        val task = SharedFolderBatchTask(
            id = "t", messageId = "m", type = ShareBatchType.SYNC, title = "t",
            targetDir = "", link = SharedLink("localhost", 0, "sid", ""), urlToken = "", entries = emptyList(),
        )
        assertEquals(0f, task.fraction(), "unknown total means no fraction yet")
        task.totalSize = 100
        task.downloadedSize = 150
        assertEquals(1f, task.fraction(), "fraction clamps to 1 when the remote under-reports size")
        task.downloadedSize = 40
        assertEquals(0.4f, task.fraction())
    }
}
