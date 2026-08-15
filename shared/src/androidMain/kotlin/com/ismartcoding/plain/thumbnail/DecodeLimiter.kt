package com.ismartcoding.plain.thumbnail

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Global gate for bitmap/video-frame decode work behind /fs.
 *
 * Permit count scales with the runtime heap limit (see [DecodePolicy]):
 * large-heap phones decode up to 6 in parallel — the per-origin browser
 * connection limit anyway — so scroll smoothness is unchanged, while
 * memory-constrained containers serialize bursts instead of OOM-ing.
 */
object DecodeLimiter {

    private val semaphore = Semaphore(
        DecodePolicy.permits((Runtime.getRuntime().maxMemory() / (1024L * 1024L)).toInt())
    )

    suspend fun <T> withPermit(block: suspend () -> T): T = semaphore.withPermit { block() }
}
