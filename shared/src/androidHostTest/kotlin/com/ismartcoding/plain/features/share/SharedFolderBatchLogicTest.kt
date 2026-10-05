package com.ismartcoding.plain.features.share

import kotlin.test.Test
import kotlin.test.assertEquals

/** Verifies the UI fraction for Rust-projected batch counters. */
class SharedFolderBatchLogicTest {

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
