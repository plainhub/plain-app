package com.ismartcoding.plain.features.sms

import com.ismartcoding.plain.events.SmsSendResultData
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.preferences.appPreferences
import com.ismartcoding.plain.preferences.stringPreferenceKey
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive

private class RustSmsSendStateStore : SmsSendStateStore {
    override fun read(requestId: String): SmsPendingSendState? {
        val encoded = appPreferences.snapshot[stringPreferenceKey(KEY_PREFIX + requestId)]
        return encoded?.let { runCatching { JsonHelper.jsonDecode<SmsPendingSendState>(it) }.getOrNull() }
    }

    override fun readAll(): List<SmsPendingSendState> {
        return appPreferences.snapshot.entries.mapNotNull { (key, value) ->
            if (!key.startsWith(KEY_PREFIX) || value !is JsonPrimitive || !value.isString) null else runCatching { JsonHelper.jsonDecode<SmsPendingSendState>(value.content) }.getOrNull()
        }
    }

    override fun write(state: SmsPendingSendState) {
        runBlocking { appPreferences.put(stringPreferenceKey(KEY_PREFIX + state.requestId), JsonHelper.jsonEncode(state)) }
    }

    override fun remove(requestId: String) {
        runBlocking { appPreferences.remove(KEY_PREFIX + requestId) }
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
