package com.ismartcoding.plain.features.sms

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ismartcoding.plain.events.SmsSendResultData
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.smsSendResultsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sms_send_results",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

private class AndroidSmsSendStateStore(private val context: Context) : SmsSendStateStore {
    override fun read(requestId: String): SmsPendingSendState? {
        val encoded = runBlocking { context.smsSendResultsDataStore.data.first() }[stringPreferencesKey(KEY_PREFIX + requestId)]
        return encoded?.let { runCatching { JsonHelper.jsonDecode<SmsPendingSendState>(it) }.getOrNull() }
    }

    override fun readAll(): List<SmsPendingSendState> {
        return runBlocking { context.smsSendResultsDataStore.data.first() }.asMap().mapNotNull { (key, value) ->
            if (!key.name.startsWith(KEY_PREFIX) || value !is String) null else runCatching { JsonHelper.jsonDecode<SmsPendingSendState>(value) }.getOrNull()
        }
    }

    override fun write(state: SmsPendingSendState) {
        runBlocking { context.smsSendResultsDataStore.edit { it[stringPreferencesKey(KEY_PREFIX + state.requestId)] = JsonHelper.jsonEncode(state) } }
    }

    override fun remove(requestId: String) {
        runBlocking { context.smsSendResultsDataStore.edit { it.remove(stringPreferencesKey(KEY_PREFIX + requestId)) } }
    }

    private companion object {
        const val KEY_PREFIX = "pending_"
    }
}

object SmsSendResultTracker {
    @Volatile
    private var tracker: SmsSendStateTracker? = null

    private fun get(context: Context): SmsSendStateTracker {
        return tracker ?: synchronized(this) {
            tracker ?: SmsSendStateTracker(AndroidSmsSendStateStore(context)).also { tracker = it }
        }
    }

    fun register(
        context: Context,
        requestId: String,
        clientId: String?,
        clientRequestId: String?,
        partCount: Int,
        createdAtMillis: Long,
    ) {
        get(context).register(requestId, clientId, clientRequestId, partCount, createdAtMillis)
    }

    fun cancel(context: Context, requestId: String) = get(context).cancel(requestId)

    fun acknowledge(context: Context, requestId: String) = get(context).acknowledge(requestId)

    fun pending(context: Context): List<SmsPendingSendState> = get(context).pending()

    fun terminalResults(context: Context): List<SmsSendResultData> = get(context).terminalResults()

    fun expire(context: Context, requestId: String, terminalAtMillis: Long): SmsSendResultData? =
        get(context).expire(requestId, terminalAtMillis)

    fun record(
        context: Context,
        requestId: String,
        partIndex: Int,
        partCount: Int,
        resultCode: Int,
        successResultCode: Int,
        terminalAtMillis: Long,
    ): SmsSendResultData? = get(context).record(
        requestId,
        partIndex,
        partCount,
        resultCode,
        successResultCode,
        terminalAtMillis,
    )
}
