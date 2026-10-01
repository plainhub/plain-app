package com.ismartcoding.plain.features.sms

import com.ismartcoding.plain.events.MmsSendResultData
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.preferences.Prefs
import kotlinx.serialization.json.JsonPrimitive

private class RustMmsSendResultStateStore : MmsSendResultStateStore {
    override fun readAll(): List<MmsTerminalResultState> {
        return Prefs.snapshot.mapNotNull { (key, value) ->
            if (!key.startsWith(KEY_PREFIX) || value !is JsonPrimitive || !value.isString) null else runCatching { JsonHelper.jsonDecode<MmsTerminalResultState>(value.content) }.getOrNull()
        }
    }

    override fun write(state: MmsTerminalResultState) {
        Prefs.setString(KEY_PREFIX + state.pendingId, JsonHelper.jsonEncode(state))
    }

    override fun remove(pendingId: String) {
        Prefs.remove(KEY_PREFIX + pendingId)
    }

    private companion object {
        const val KEY_PREFIX = "mms_terminal_"
    }
}

object MmsSendResultTracker {
    @Volatile
    private var outbox: MmsSendResultOutbox? = null

    private fun get(): MmsSendResultOutbox {
        return outbox ?: synchronized(this) {
            outbox ?: MmsSendResultOutbox(RustMmsSendResultStateStore()).also { outbox = it }
        }
    }

    fun record(result: MmsSendResultData, terminalAtMillis: Long) {
        get().record(result, terminalAtMillis)
    }

    fun replayable(nowMillis: Long, ttlMillis: Long): List<MmsSendResultData> {
        return get().replayable(nowMillis, ttlMillis)
    }
}
