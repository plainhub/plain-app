package com.ismartcoding.plain.chat.peer.transport

import java.util.concurrent.ConcurrentHashMap

object PeerCircuitBreaker {
    private const val WINDOW_MS = 30_000L
    private const val MAX_FAILURES = 2

    private data class State(val failures: Int, val openedAt: Long)
    private val states = ConcurrentHashMap<String, State>()

    private fun key(peerId: String, transportId: String) = "$peerId|$transportId"

    @Synchronized
    fun isOpen(peerId: String, transportId: String): Boolean {
        val state = states[key(peerId, transportId)] ?: return false
        if (state.failures < MAX_FAILURES) return false
        if (System.currentTimeMillis() - state.openedAt > WINDOW_MS) {
            states.remove(key(peerId, transportId), state)
            return false
        }
        return true
    }

    @Synchronized
    fun recordSuccess(peerId: String, transportId: String) {
        states.remove(key(peerId, transportId))
    }

    @Synchronized
    fun recordFailure(peerId: String, transportId: String) {
        val k = key(peerId, transportId)
        val current = states[k]
        val nextFailures = (current?.failures ?: 0) + 1
        states[k] = State(
            failures = nextFailures,
            openedAt = if (nextFailures >= MAX_FAILURES) System.currentTimeMillis() else (current?.openedAt ?: 0L),
        )
    }
}
