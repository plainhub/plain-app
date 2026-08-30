package com.ismartcoding.plain.features.sms

import com.ismartcoding.plain.events.MmsSendResultData
import com.ismartcoding.plain.platform.PlainLock

data class MmsTerminalResultState(
    val pendingId: String,
    val success: Boolean,
    val resultCode: Int,
    val terminalAtMillis: Long,
)

interface MmsSendResultStateStore {
    fun readAll(): List<MmsTerminalResultState>
    fun write(state: MmsTerminalResultState)
    fun remove(pendingId: String)
}

class MmsSendResultOutbox(
    private val store: MmsSendResultStateStore,
    private val maxEntries: Int = 500,
) {
    private val lock = PlainLock()

    init {
        require(maxEntries > 0)
    }

    fun record(result: MmsSendResultData, terminalAtMillis: Long) = lock.withLock {
        store.write(
            MmsTerminalResultState(
                pendingId = result.pendingId,
                success = result.success,
                resultCode = result.resultCode,
                terminalAtMillis = terminalAtMillis,
            ),
        )
        store.readAll()
            .sortedByDescending { it.terminalAtMillis }
            .drop(maxEntries)
            .forEach { store.remove(it.pendingId) }
    }

    fun replayable(nowMillis: Long, ttlMillis: Long): List<MmsSendResultData> = lock.withLock {
        require(ttlMillis >= 0L)
        return@withLock store.readAll()
            .sortedBy { it.terminalAtMillis }
            .mapNotNull { state ->
                if ((nowMillis - state.terminalAtMillis).coerceAtLeast(0L) >= ttlMillis) {
                    store.remove(state.pendingId)
                    null
                } else {
                    MmsSendResultData(state.pendingId, state.success, state.resultCode)
                }
            }
    }
}
