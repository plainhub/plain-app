package com.ismartcoding.plain.features.sms

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ismartcoding.plain.events.MmsSendResultData
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.mmsSendResultsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mms_send_results",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

private class AndroidMmsSendResultStateStore(private val context: Context) : MmsSendResultStateStore {
    override fun readAll(): List<MmsTerminalResultState> {
        return runBlocking { context.mmsSendResultsDataStore.data.first() }.asMap().mapNotNull { (key, value) ->
            if (!key.name.startsWith(KEY_PREFIX) || value !is String) null else runCatching { JsonHelper.jsonDecode<MmsTerminalResultState>(value) }.getOrNull()
        }
    }

    override fun write(state: MmsTerminalResultState) {
        runBlocking { context.mmsSendResultsDataStore.edit { it[stringPreferencesKey(KEY_PREFIX + state.pendingId)] = JsonHelper.jsonEncode(state) } }
    }

    override fun remove(pendingId: String) {
        runBlocking { context.mmsSendResultsDataStore.edit { it.remove(stringPreferencesKey(KEY_PREFIX + pendingId)) } }
    }

    private companion object {
        const val KEY_PREFIX = "terminal_"
    }
}

object MmsSendResultTracker {
    @Volatile
    private var outbox: MmsSendResultOutbox? = null

    private fun get(context: Context): MmsSendResultOutbox {
        return outbox ?: synchronized(this) {
            outbox ?: MmsSendResultOutbox(AndroidMmsSendResultStateStore(context)).also { outbox = it }
        }
    }

    fun record(context: Context, result: MmsSendResultData, terminalAtMillis: Long) {
        get(context).record(result, terminalAtMillis)
    }

    fun replayable(context: Context, nowMillis: Long, ttlMillis: Long): List<MmsSendResultData> {
        return get(context).replayable(nowMillis, ttlMillis)
    }
}
