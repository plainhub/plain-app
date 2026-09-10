package com.ismartcoding.plain.httpserver.mainschemas

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed class MergeJobState {
    data object Merging : MergeJobState()
    data class Done(val value: String, val size: Long) : MergeJobState()
    data class Failed(val error: String) : MergeJobState()
}

sealed class MergeClaim {
    data object Claimed : MergeClaim()
    data object InProgress : MergeClaim()
    data class AlreadyDone(val reply: String) : MergeClaim()
}

/**
 * Tracks async merge jobs per fileId so repeated `mergeChunksAsync` calls are
 * idempotent and a lost WS result event can be recovered via `mergeStatus`.
 * Chunks stay on disk until a merge succeeds, so retries reuse them.
 */
object MergeJobs {
    private const val CAP = 1024
    private val mutex = Mutex()
    private val jobs = HashMap<String, MergeJobState>()

    suspend fun claim(fileId: String): MergeClaim = mutex.withLock {
        when (val state = jobs[fileId]) {
            is MergeJobState.Done -> MergeClaim.AlreadyDone("done:${state.value}:${state.size}")
            MergeJobState.Merging -> MergeClaim.InProgress
            // A failed merge keeps its chunks on disk, so claiming again
            // (a retry) is expected and restarts the job.
            null, is MergeJobState.Failed -> {
                if (jobs.size > CAP) {
                    jobs.entries.removeAll { it.value !is MergeJobState.Merging }
                }
                jobs[fileId] = MergeJobState.Merging
                MergeClaim.Claimed
            }
        }
    }

    /** Drop the claim when the merge never started (pre-flight failure). */
    suspend fun release(fileId: String) = mutex.withLock {
        if (jobs[fileId] == MergeJobState.Merging) jobs.remove(fileId)
    }

    suspend fun finish(fileId: String, value: String, size: Long) = mutex.withLock {
        jobs[fileId] = MergeJobState.Done(value, size)
    }

    suspend fun fail(fileId: String, error: String) = mutex.withLock {
        jobs[fileId] = MergeJobState.Failed(error)
    }

    suspend fun statusString(fileId: String): String = mutex.withLock {
        when (val state = jobs[fileId]) {
            null -> "none"
            MergeJobState.Merging -> "merging"
            is MergeJobState.Done -> "done:${state.value}:${state.size}"
            is MergeJobState.Failed -> "failed:${state.error}"
        }
    }

    fun clearForTests() {
        jobs.clear()
    }

    val capForTests: Int
        get() = CAP

    fun sizeForTests(): Int = jobs.size
}
