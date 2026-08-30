package com.ismartcoding.plain.features.sms

import android.content.Context
import com.ismartcoding.plain.events.SmsSendResultData
import org.json.JSONArray
import org.json.JSONObject

private class AndroidSmsSendStateStore(context: Context) : SmsSendStateStore {
    private val preferences = context.applicationContext.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)

    override fun read(requestId: String): SmsPendingSendState? {
        val encoded = preferences.getString(KEY_PREFIX + requestId, null) ?: return null
        return runCatching { decode(encoded) }.getOrNull()
    }

    override fun readAll(): List<SmsPendingSendState> {
        return preferences.all.mapNotNull { (key, value) ->
            if (!key.startsWith(KEY_PREFIX) || value !is String) null else runCatching { decode(value) }.getOrNull()
        }
    }

    override fun write(state: SmsPendingSendState) {
        check(preferences.edit().putString(KEY_PREFIX + state.requestId, encode(state)).commit())
    }

    override fun remove(requestId: String) {
        check(preferences.edit().remove(KEY_PREFIX + requestId).commit())
    }

    private fun encode(state: SmsPendingSendState): String = JSONObject().apply {
        put("requestId", state.requestId)
        put("clientId", state.clientId ?: JSONObject.NULL)
        put("clientRequestId", state.clientRequestId ?: JSONObject.NULL)
        put("partCount", state.partCount)
        put("completedParts", JSONArray(state.completedParts.sorted()))
        put("createdAtMillis", state.createdAtMillis)
        put("terminalSuccess", state.terminalSuccess ?: JSONObject.NULL)
        put("terminalResultCode", state.terminalResultCode ?: JSONObject.NULL)
        put("terminalAtMillis", state.terminalAtMillis ?: JSONObject.NULL)
    }.toString()

    private fun decode(value: String): SmsPendingSendState {
        val json = JSONObject(value)
        val completed = json.getJSONArray("completedParts")
        return SmsPendingSendState(
            requestId = json.getString("requestId"),
            clientId = if (json.isNull("clientId")) null else json.getString("clientId"),
            clientRequestId = if (!json.has("clientRequestId") || json.isNull("clientRequestId")) {
                null
            } else {
                json.getString("clientRequestId")
            },
            partCount = json.getInt("partCount"),
            completedParts = buildSet {
                repeat(completed.length()) { add(completed.getInt(it)) }
            },
            createdAtMillis = json.getLong("createdAtMillis"),
            terminalSuccess = if (json.isNull("terminalSuccess")) null else json.getBoolean("terminalSuccess"),
            terminalResultCode = if (json.isNull("terminalResultCode")) null else json.getInt("terminalResultCode"),
            terminalAtMillis = if (!json.has("terminalAtMillis") || json.isNull("terminalAtMillis")) {
                null
            } else {
                json.getLong("terminalAtMillis")
            },
        )
    }

    private companion object {
        const val STORE_NAME = "sms_send_results"
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
