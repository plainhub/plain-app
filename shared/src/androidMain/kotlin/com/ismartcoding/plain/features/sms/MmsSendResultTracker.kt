package com.ismartcoding.plain.features.sms

import android.content.Context
import com.ismartcoding.plain.events.MmsSendResultData
import org.json.JSONObject

private class AndroidMmsSendResultStateStore(context: Context) : MmsSendResultStateStore {
    private val preferences = context.applicationContext.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)

    override fun readAll(): List<MmsTerminalResultState> {
        return preferences.all.mapNotNull { (key, value) ->
            if (!key.startsWith(KEY_PREFIX) || value !is String) null else runCatching { decode(value) }.getOrNull()
        }
    }

    override fun write(state: MmsTerminalResultState) {
        check(preferences.edit().putString(KEY_PREFIX + state.pendingId, encode(state)).commit())
    }

    override fun remove(pendingId: String) {
        check(preferences.edit().remove(KEY_PREFIX + pendingId).commit())
    }

    private fun encode(state: MmsTerminalResultState): String = JSONObject().apply {
        put("pendingId", state.pendingId)
        put("success", state.success)
        put("resultCode", state.resultCode)
        put("terminalAtMillis", state.terminalAtMillis)
    }.toString()

    private fun decode(value: String): MmsTerminalResultState {
        val json = JSONObject(value)
        return MmsTerminalResultState(
            pendingId = json.getString("pendingId"),
            success = json.getBoolean("success"),
            resultCode = json.getInt("resultCode"),
            terminalAtMillis = json.getLong("terminalAtMillis"),
        )
    }

    private companion object {
        const val STORE_NAME = "mms_send_results"
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
