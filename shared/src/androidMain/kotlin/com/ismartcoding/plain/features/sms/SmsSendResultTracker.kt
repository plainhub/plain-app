package com.ismartcoding.plain.features.sms

import com.ismartcoding.plain.events.SmsSendResultData
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.preferences.Prefs
import kotlinx.serialization.json.JsonPrimitive

private class RustSmsSendStateStore : SmsSendStateStore {
    override fun read(requestId: String): SmsPendingSendState? {
        val encoded = Prefs.string(KEY_PREFIX + requestId)
        return encoded?.let { runCatching { JsonHelper.jsonDecode<SmsPendingSendState>(it) }.getOrNull() }
    }

    override fun readAll(): List<SmsPendingSendState> {
        return Prefs.snapshot.mapNotNull { (key, value) ->
            if (!key.startsWith(KEY_PREFIX) || value !is JsonPrimitive || !value.isString) null else runCatching { JsonHelper.jsonDecode<SmsPendingSendState>(value.content) }.getOrNull()
        }
    }

    override fun write(state: SmsPendingSendState) {
        Prefs.setString(KEY_PREFIX + state.requestId, JsonHelper.jsonEncode(state))
    }

    override fun remove(requestId: String) {
        Prefs.remove(KEY_PREFIX + requestId)
    }

    private companion object {
        const val KEY_PREFIX = "sms_pending_"
    }
}

object SmsSendResultTracker {
    @Volatile
    private var tracker: SmsSendStateTracker? = null

    private fun get(): SmsSendStateTracker {
        return tracker ?: synchronized(this) {
            tracker ?: SmsSendStateTracker(RustSmsSendStateStore()).also { tracker = it }
        }
    }

    fun register(
        requestId: String,
        clientId: String?,
        clientRequestId: String?,
        partCount: Int,
        createdAtMillis: Long,
    ) {
        get().register(requestId, clientId, clientRequestId, partCount, createdAtMillis)
    }

    fun cancel(requestId: String) = get().cancel(requestId)

    fun acknowledge(requestId: String) = get().acknowledge(requestId)

    fun pending(): List<SmsPendingSendState> = get().pending()

    fun terminalResults(): List<SmsSendResultData> = get().terminalResults()

    fun expire(requestId: String, terminalAtMillis: Long): SmsSendResultData? =
        get().expire(requestId, terminalAtMillis)

    fun record(
        requestId: String,
        partIndex: Int,
        partCount: Int,
        resultCode: Int,
        successResultCode: Int,
        terminalAtMillis: Long,
    ): SmsSendResultData? = get().record(
        requestId,
        partIndex,
        partCount,
        resultCode,
        successResultCode,
        terminalAtMillis,
    )
}
