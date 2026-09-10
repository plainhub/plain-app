package com.ismartcoding.plain.httpserver.mainschemas

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
        assertEquals("merging", MergeJobs.statusString("f1"))
    }

    @Test
    fun finished_job_replies_done_immediately() = runBlocking {
        MergeJobs.clearForTests()
        MergeJobs.claim("f2")
        MergeJobs.finish("f2", "a:b.jpg", 42)
        assertEquals(MergeClaim.AlreadyDone("done:a:b.jpg:42"), MergeJobs.claim("f2"))
        assertEquals("done:a:b.jpg:42", MergeJobs.statusString("f2"))
    }

    @Test
    fun failed_job_reports_error() = runBlocking {
        MergeJobs.clearForTests()
        MergeJobs.claim("f3")
        MergeJobs.fail("f3", "Merge integrity failed")
        assertEquals("failed:Merge integrity failed", MergeJobs.statusString("f3"))
    }

    @Test
    fun release_drops_only_unstarted_claims() = runBlocking {
        MergeJobs.clearForTests()
        MergeJobs.claim("f4")
        MergeJobs.release("f4")
        assertEquals("none", MergeJobs.statusString("f4"))
        MergeJobs.finish("f5", "v", 1)
        MergeJobs.release("f5")
        assertEquals("done:v:1", MergeJobs.statusString("f5"))
    }

    @Test
    fun prune_keeps_in_flight_jobs() = runBlocking {
        MergeJobs.clearForTests()
        for (i in 0..MergeJobs.capForTests) {
            MergeJobs.claim("prune-$i")
        }
        MergeJobs.finish("keep", "v", 1)
        MergeJobs.claim("next") // triggers the prune
        assertEquals("merging", MergeJobs.statusString("prune-0"))
        assertEquals("none", MergeJobs.statusString("keep"))
        assertTrue(MergeJobs.sizeForTests() <= MergeJobs.capForTests + 2)
        MergeJobs.clearForTests()
    }
}
