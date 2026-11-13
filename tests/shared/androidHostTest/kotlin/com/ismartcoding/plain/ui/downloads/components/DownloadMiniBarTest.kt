package com.ismartcoding.plain.ui.downloads.components

import com.ismartcoding.plain.features.downloads.DownloadStatus
import com.ismartcoding.plain.models.Download
import com.ismartcoding.plain.models.ResourceDownloadSource
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

    private fun task(id: String, status: DownloadStatus): Download = Download(
        id, id, ResourceDownloadSource(listOf("https://fixture.invalid/fs?id=s.share.AA&action=download")), "/downloads", null, status,
        100, 100, 0, 1, 1, "", "", emptyList(),
    )

    @Test
    fun completedBatchRendersAsFullBarWithoutPercent() {
        val t = task("done", DownloadStatus.COMPLETED)
        assertEquals(1f, t.fraction())
        assertEquals("", miniBarPct(t))
    }

    @Test
    fun runningBatchKeepsPercentText() {
        val t = task("run", DownloadStatus.DOWNLOADING).copy(downloadedBytes = 25)
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
    @Test
    fun fractionIsZeroForUnknownTotalsAndClampedForDisplay() {
        val sample = task("sample", DownloadStatus.DOWNLOADING)
        assertEquals(0f, sample.copy(totalBytes = 0).fraction())
        assertEquals(1f, sample.copy(downloadedBytes = 150).fraction())
        assertEquals(0.4f, sample.copy(downloadedBytes = 40).fraction())
    }

}
