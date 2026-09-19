package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.httpserver.models.MergeTask
import com.ismartcoding.plain.httpserver.models.MergeTaskStatus
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MergeJobsTest {

    @Test
    fun claim_is_exclusive_until_terminal() = runBlocking {
        MergeJobs.clearForTests()
        assertEquals(MergeClaim.Claimed, MergeJobs.claim("f1"))
        assertEquals(MergeClaim.InProgress, MergeJobs.claim("f1"))
        assertEquals(MergeTask(MergeTaskStatus.MERGING), MergeJobs.status("f1"))
    }

    @Test
    fun finished_job_replies_done_immediately() = runBlocking {
        MergeJobs.clearForTests()
        MergeJobs.claim("f2")
        MergeJobs.finish("f2", "a:b.jpg", 42)
        assertEquals(MergeClaim.AlreadyDone(MergeTask(MergeTaskStatus.DONE, "a:b.jpg", 42)), MergeJobs.claim("f2"))
        assertEquals(MergeTask(MergeTaskStatus.DONE, "a:b.jpg", 42), MergeJobs.status("f2"))
    }

    @Test
    fun failed_job_reports_error() = runBlocking {
        MergeJobs.clearForTests()
        MergeJobs.claim("f3")
        MergeJobs.fail("f3", "Merge integrity failed")
        assertEquals(MergeTask(MergeTaskStatus.FAILED, error = "Merge integrity failed"), MergeJobs.status("f3"))
    }

    @Test
    fun release_drops_only_unstarted_claims() = runBlocking {
        MergeJobs.clearForTests()
        MergeJobs.claim("f4")
        MergeJobs.release("f4")
        assertEquals(MergeTask(MergeTaskStatus.NONE), MergeJobs.status("f4"))
        MergeJobs.finish("f5", "v", 1)
        MergeJobs.release("f5")
        assertEquals(MergeTask(MergeTaskStatus.DONE, "v", 1), MergeJobs.status("f5"))
    }

    @Test
    fun prune_keeps_in_flight_jobs() = runBlocking {
        MergeJobs.clearForTests()
        for (i in 0..MergeJobs.capForTests) {
            MergeJobs.claim("prune-$i")
        }
        MergeJobs.finish("keep", "v", 1)
        MergeJobs.claim("next") // triggers the prune
        assertEquals(MergeTask(MergeTaskStatus.MERGING), MergeJobs.status("prune-0"))
        assertEquals(MergeTask(MergeTaskStatus.NONE), MergeJobs.status("keep"))
        assertTrue(MergeJobs.sizeForTests() <= MergeJobs.capForTests + 2)
        MergeJobs.clearForTests()
    }
}
