package com.ismartcoding.plain.thumbnail

/**
 * Pure decode-budget policy shared by platforms.
 *
 * Rapid grid scrolling fires bursts of /fs requests; letting every request
 * decode in parallel stacks allocations until small-heap Android
 * compatibility containers (e.g. Zhuoyitong) die with OOM. The policy maps
 * the process heap budget to a concurrent-decode permit count so large-heap
 * devices keep full scroll smoothness while constrained devices queue.
 */
object DecodePolicy {

    /**
     * Upper bound (px) for a full-image viewing decode such as HEIF→PNG.
     * 4096 keeps 12 MP photos pixel-exact; larger photos are halved per
     * power-of-2 sample, trading only extreme zoom detail for bounded memory.
     */
    const val MAX_FULL_VIEW_EDGE = 4096

    /** Concurrent decode permits for a process whose Java heap limit is [maxMemoryMb] MB. */
    fun permits(maxMemoryMb: Int): Int = when {
        maxMemoryMb >= 384 -> 6
        maxMemoryMb >= 192 -> 4
        else -> 2
    }

    /** Smallest power-of-2 sample size that keeps both decoded edges ≤ [maxEdge]. */
    fun capSampleSize(srcW: Int, srcH: Int, maxEdge: Int): Int {
        var sample = 1
        while (srcW / sample > maxEdge || srcH / sample > maxEdge) sample *= 2
        return sample
    }
}
