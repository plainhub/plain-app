package com.ismartcoding.plain.ui.components.downloads

import com.ismartcoding.plain.features.download.DownloadStatus
import com.ismartcoding.plain.features.share.ShareBatchType
import com.ismartcoding.plain.features.share.SharedFileDto
import com.ismartcoding.plain.features.share.SharedFolderBatchTask
import com.ismartcoding.plain.features.share.SharedLink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Locks the toast-free completion flow of the shared-folder download UI: a
 * finished batch is carried by the mini bar itself (full green progress, no
 * percent text), and the bar still prioritizes running work over finished
 * history.
 */
class DownloadMiniBarTest {

    private fun task(id: String, status: DownloadStatus, type: ShareBatchType = ShareBatchType.FILE): SharedFolderBatchTask =
        SharedFolderBatchTask(
            id = id, messageId = "m1", type = type, title = id, targetDir = "",
            link = SharedLink("", 0, "", ""), urlToken = "",
            entries = listOf(SharedFileDto(name = "f", size = 100)),
        ).apply {
            this.status = status
            downloadedSize = 100
            totalSize = 100
            totalFiles = 1
            doneFiles = 1
        }

    @Test
    fun completedBatchRendersAsFullBarWithoutPercent() {
        val t = task("done", DownloadStatus.COMPLETED)
        assertEquals(1f, t.fraction())
        assertEquals("", miniBarPct(t))
    }

    @Test
    fun runningBatchKeepsPercentText() {
        val t = task("run", DownloadStatus.DOWNLOADING).apply {
            downloadedSize = 25
        }
        assertEquals("25%", miniBarPct(t))
    }

    @Test
    fun runningOutranksCompletedForHeadline() {
        val running = task("run", DownloadStatus.DOWNLOADING)
        val completed = task("done", DownloadStatus.COMPLETED)
        assertEquals(running.id, mostRelevant(listOf(completed, running))?.id)
        // All-finished list still shows the latest completed batch (the green check state).
        assertEquals(completed.id, mostRelevant(listOf(completed))?.id)
    }

    @Test
    fun failuresStillOutrankPlainCompleted() {
        val partial = task("partial", DownloadStatus.PARTIAL)
        val completed = task("done", DownloadStatus.COMPLETED)
        assertEquals(partial.id, mostRelevant(listOf(completed, partial))?.id)
        assertTrue(miniBarPct(partial).isEmpty())
    }
}
