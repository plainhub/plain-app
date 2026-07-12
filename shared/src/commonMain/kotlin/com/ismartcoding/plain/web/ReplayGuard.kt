package com.ismartcoding.plain.web

import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Anti-replay protection for GraphQL API requests.
 *
 * After decryption, new-format requests look like: "TIMESTAMP|NONCE|{...json...}"
 */
object ReplayGuard {
    private const val MAX_TIMESTAMP_DIFF_MS = 30_000L // 30-second window
    private const val CLEANUP_INTERVAL_MS = 60_000L

    private val mutex = Mutex()
    private val nonceCache = mutableMapOf<String, MutableSet<String>>()
    private var lastCleanupMs = TimeHelper.nowMillis()

    data class ParsedRequest(
        val timestamp: Long,
        val nonce: String,
        val body: String,
    )

    /**
     * Parse a decrypted request string.
     */
    fun parse(decrypted: String): ParsedRequest? {
        val parts = decrypted.split("|", limit = 3)
        if (parts.size != 3) return null
        val ts = parts[0].toLongOrNull() ?: return null
        return ParsedRequest(ts, parts[1], parts[2])
    }

    /**
     * Validate a parsed request. Returns an error message or null if valid.
     */
    suspend fun validate(clientId: String, req: ParsedRequest): String? {
        val now = TimeHelper.nowMillis()
        val diff = kotlin.math.abs(now - req.timestamp)
        if (diff > MAX_TIMESTAMP_DIFF_MS) {
            LogCat.e("Replay guard: timestamp too far off (${diff}ms) from client $clientId")
            return "timestamp_expired"
        }
        val nonceKey = "${req.timestamp}:${req.nonce}"
        mutex.withLock {
            val nonces = nonceCache.getOrPut(clientId) { mutableSetOf() }
            if (!nonces.add(nonceKey)) {
                LogCat.e("Replay guard: duplicate nonce from client $clientId")
                return "duplicate_nonce"
            }
            cleanupIfNeeded(now)
        }
        return null
    }

    private fun cleanupIfNeeded(now: Long) {
        if (now - lastCleanupMs < CLEANUP_INTERVAL_MS) return
        lastCleanupMs = now
        nonceCache.forEach { (_, nonces) ->
            val iter = nonces.iterator()
            while (iter.hasNext()) {
                val key = iter.next()
                val ts = key.substringBefore(':').toLongOrNull() ?: 0L
                if (now - ts > MAX_TIMESTAMP_DIFF_MS * 2) iter.remove()
            }
        }
        val mapIter = nonceCache.entries.iterator()
        while (mapIter.hasNext()) {
            if (mapIter.next().value.isEmpty()) mapIter.remove()
        }
    }
}
