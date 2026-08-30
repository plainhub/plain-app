package com.ismartcoding.plain.features.sms

import com.ismartcoding.plain.events.SmsSendResultData
import com.ismartcoding.plain.platform.PlainLock
import kotlinx.serialization.Serializable

@Serializable
data class SmsPendingSendState(
    val requestId: String,
    val clientId: String?,
    val clientRequestId: String?,
    val partCount: Int,
    val completedParts: Set<Int>,
    val createdAtMillis: Long,
    val terminalSuccess: Boolean? = null,
    val terminalResultCode: Int? = null,
    val terminalAtMillis: Long? = null,
)

/**
 * Storage boundary for SMS callback state. Android's sent `PendingIntent` can
 * outlive this app process, so the production implementation must be durable;
 * host tests use an in-memory implementation to exercise the state machine.
 */
interface SmsSendStateStore {
    fun read(requestId: String): SmsPendingSendState?
    fun readAll(): List<SmsPendingSendState>
    fun write(state: SmsPendingSendState)
    fun remove(requestId: String)
}

class SmsSendStateTracker(private val store: SmsSendStateStore) {
    private val lock = PlainLock()

    fun register(
        requestId: String,
        clientId: String?,
        clientRequestId: String?,
        partCount: Int,
        createdAtMillis: Long,
    ) = lock.withLock {
        store.write(
            SmsPendingSendState(
                requestId = requestId,
                clientId = clientId,
                clientRequestId = clientRequestId,
                partCount = partCount.coerceAtLeast(1),
                completedParts = emptySet(),
                createdAtMillis = createdAtMillis,
            ),
        )
    }

    fun cancel(requestId: String) = lock.withLock {
        store.remove(requestId)
    }

    fun acknowledge(requestId: String) = lock.withLock {
        if (store.read(requestId)?.terminalResultCode != null) {
            store.remove(requestId)
        }
    }

    fun pending(): List<SmsPendingSendState> = lock.withLock { store.readAll() }

    fun terminalResults(): List<SmsSendResultData> = lock.withLock {
        store.readAll().mapNotNull { state ->
            val success = state.terminalSuccess ?: return@mapNotNull null
            val resultCode = state.terminalResultCode ?: return@mapNotNull null
            SmsSendResultData(state.clientRequestId, success, resultCode)
        }
    }

    fun expire(requestId: String, terminalAtMillis: Long): SmsSendResultData? = lock.withLock {
        val send = store.read(requestId) ?: return@withLock null
        if (send.terminalResultCode != null) return@withLock null
        val result = SmsSendResultData(
            send.clientRequestId,
            false,
            SmsProviderContract.SEND_RESULT_TIMEOUT,
        )
        store.write(
            send.copy(
                terminalSuccess = result.success,
                terminalResultCode = result.resultCode,
                terminalAtMillis = terminalAtMillis,
            ),
        )
        return@withLock result
    }

    fun record(
        requestId: String,
        partIndex: Int,
        partCount: Int,
        resultCode: Int,
        successResultCode: Int,
        terminalAtMillis: Long,
    ): SmsSendResultData? = lock.withLock {
        val send = store.read(requestId) ?: return@withLock null
        if (send.terminalResultCode != null) return@withLock null
        if (partCount.coerceAtLeast(1) != send.partCount || partIndex !in 0 until send.partCount) return@withLock null

        if (resultCode != successResultCode) {
            val result = SmsSendResultData(send.clientRequestId, false, resultCode)
            store.write(
                send.copy(
                    terminalSuccess = false,
                    terminalResultCode = resultCode,
                    terminalAtMillis = terminalAtMillis,
                ),
            )
            return@withLock result
        }

        val completedParts = send.completedParts + partIndex
        if (completedParts.size != send.partCount) {
            store.write(send.copy(completedParts = completedParts))
            return@withLock null
        }

        val result = SmsSendResultData(send.clientRequestId, true, resultCode)
        store.write(
            send.copy(
                completedParts = completedParts,
                terminalSuccess = true,
                terminalResultCode = resultCode,
                terminalAtMillis = terminalAtMillis,
            ),
        )
        return@withLock result
    }
}
