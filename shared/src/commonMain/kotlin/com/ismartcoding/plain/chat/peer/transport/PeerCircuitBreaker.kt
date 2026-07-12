package com.ismartcoding.plain.chat.peer.transport

import com.ismartcoding.plain.helpers.TimeHelper
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object PeerCircuitBreaker {
    private const val WINDOW_MS = 30_000L
    private const val MAX_FAILURES = 2

    private data class State(val failures: Int, val openedAt: Long)
    private val states = mutableMapOf<String, State>()
    private val mutex = Mutex()

    private fun key(peerId: String, transportId: String) = "$peerId|$transportId"

    suspend fun isOpen(peerId: String, transportId: String): Boolean = mutex.withLock {
        val state = states[key(peerId, transportId)] ?: return@withLock false
        if (state.failures < MAX_FAILURES) return@withLock false
        if (TimeHelper.nowMillis() - state.openedAt > WINDOW_MS) {
            states.remove(key(peerId, transportId))
            return@withLock false
        }
        return@withLock true
    }

    suspend fun recordSuccess(peerId: String, transportId: String) {
        mutex.withLock {
            states.remove(key(peerId, transportId))
        }
    }

    suspend fun recordFailure(peerId: String, transportId: String) {
        mutex.withLock {
            val k = key(peerId, transportId)
            val current = states[k]
            val nextFailures = (current?.failures ?: 0) + 1
            states[k] = State(
                failures = nextFailures,
                openedAt = if (nextFailures >= MAX_FAILURES) TimeHelper.nowMillis() else (current?.openedAt ?: 0L),
            )
        }
    }
}
