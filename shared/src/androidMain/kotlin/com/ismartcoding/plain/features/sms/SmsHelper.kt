package com.ismartcoding.plain.features.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.net.toUri
import com.ismartcoding.plain.helpers.ContentWhere
import com.ismartcoding.plain.lib.extensions.find
import com.ismartcoding.plain.lib.extensions.getIntValue
import com.ismartcoding.plain.lib.extensions.getStringValue
import com.ismartcoding.plain.lib.extensions.getTimeSecondsValue
import com.ismartcoding.plain.lib.extensions.getTimeValue
import com.ismartcoding.plain.lib.extensions.map
import com.ismartcoding.plain.lib.extensions.queryCursor
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.platform.getSims
import com.ismartcoding.plain.db.DArchivedConversation
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.helpers.FilterField
import com.ismartcoding.plain.helpers.QueryHelper
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.SmsSendResultData
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.httpserver.websocket.WebSocketHelper
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.*
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.smsManager
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.receivers.SmsSentReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object SmsHelper {
    private const val SMS_SEND_TIMEOUT_MILLIS = 5 * 60 * 1000L
    // Lazy so merely touching this object (e.g. from the server stop hooks or
    // JVM host tests) never initializes the Telephony provider classes.
    private val smsUri by lazy { Telephony.Sms.CONTENT_URI }
    private val mmsUri by lazy { Telephony.Mms.CONTENT_URI }
    private val mmsPartUri by lazy { "content://mms/part".toUri() }

    private const val MMS_ADDR_TYPE_FROM = 137
    private const val MMS_ADDR_TYPE_TO = 151
    private const val MMS_INSERT_ADDRESS_TOKEN = "insert-address-token"

    private val smsTimeoutJobs = ConcurrentHashMap<String, Job>()

    fun sendText(
        to: String,
        message: String,
        subscriptionId: Int? = null,
        clientId: String? = null,
        clientRequestId: String? = null,
    ) {
        val manager: SmsManager = if (subscriptionId != null && subscriptionId >= 0) {
            @Suppress("DEPRECATION")
            SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
        } else {
            smsManager
        }
        val parts = manager.divideMessage(message)
        val requestId = UUID.randomUUID().toString()
        SmsSendResultTracker.register(
            requestId,
            clientId,
            clientRequestId,
            parts.size,
            TimeHelper.nowMillis(),
        )
        val sentIntents = ArrayList(parts.indices.map { partIndex ->
            val identity = SmsProviderContract.smsSentIntentIdentity(appContext.packageName, requestId, partIndex)
            val intent = Intent(appContext, SmsSentReceiver::class.java).apply {
                action = identity.action
                data = Uri.parse(identity.data)
                putExtra(SmsSentReceiver.EXTRA_REQUEST_ID, requestId)
                putExtra(SmsSentReceiver.EXTRA_PART_INDEX, partIndex)
                putExtra(SmsSentReceiver.EXTRA_PART_COUNT, parts.size)
            }
            PendingIntent.getBroadcast(
                appContext,
                (requestId.hashCode() * 31) + partIndex,
                intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
            )
        })
        try {
            if (parts.size > 1) {
                manager.sendMultipartTextMessage(to, null, ArrayList(parts), sentIntents, null)
            } else {
                manager.sendTextMessage(to, null, message, sentIntents.single(), null)
            }
        } catch (e: Exception) {
            SmsSendResultTracker.cancel(requestId)
            throw e
        }
        scheduleSmsTimeout(requestId, SMS_SEND_TIMEOUT_MILLIS)
    }

    private fun scheduleSmsTimeout(requestId: String, delayMillis: Long) {
        smsTimeoutJobs.remove(requestId)?.cancel()
        val job = coIO {
            delay(delayMillis.coerceAtLeast(0L))
            val result = SmsSendResultTracker.expire(requestId, TimeHelper.nowMillis()) ?: return@coIO
            dispatchSmsSendResult(requestId, result)
        }
        trackSmsJob(requestId, job)
    }

    private fun scheduleSmsTerminalCleanup(requestId: String, delayMillis: Long) {
        smsTimeoutJobs.remove(requestId)?.cancel()
        val job = coIO {
            delay(delayMillis.coerceAtLeast(0L))
            SmsSendResultTracker.acknowledge(requestId)
        }
        trackSmsJob(requestId, job)
    }

    private fun trackSmsJob(requestId: String, job: Job) {
        smsTimeoutJobs[requestId] = job
        job.invokeOnCompletion { smsTimeoutJobs.remove(requestId, job) }
        if (job.isCompleted) smsTimeoutJobs.remove(requestId, job)
    }

    fun restoreSmsSendTracking() {
        val now = TimeHelper.nowMillis()
        SmsSendResultTracker.pending().forEach { state ->
            if (state.terminalResultCode != null) {
                val terminalAtMillis = state.terminalAtMillis ?: state.createdAtMillis
                val elapsed = (now - terminalAtMillis).coerceAtLeast(0L)
                scheduleSmsTerminalCleanup(state.requestId, SMS_SEND_TIMEOUT_MILLIS - elapsed)
            } else {
                val elapsed = (now - state.createdAtMillis).coerceAtLeast(0L)
                scheduleSmsTimeout(state.requestId, SMS_SEND_TIMEOUT_MILLIS - elapsed)
            }
        }
    }

    fun stopSmsSendTracking() {
        smsTimeoutJobs.values.forEach(Job::cancel)
        smsTimeoutJobs.clear()
    }

    internal fun cancelSmsTimeout(requestId: String) {
        smsTimeoutJobs.remove(requestId)?.cancel()
    }

    internal suspend fun dispatchSmsSendResult(requestId: String, result: SmsSendResultData) {
        sendSmsResultEvent(result)
        // WebSocket events are broadcast, so a successful send to some session cannot prove
        // that the originating browser received it. Retain the terminal result as
        // a bounded outbox entry for reconnect replay; cleanup is time-limited.
        scheduleSmsTerminalCleanup(requestId, SMS_SEND_TIMEOUT_MILLIS)
    }

    suspend fun replayTerminalSmsSendResults() {
        SmsSendResultTracker.terminalResults().forEach { result ->
            sendSmsResultEvent(result)
        }
    }

    private suspend fun sendSmsResultEvent(result: SmsSendResultData) {
        sendEvent(WebSocketEvent(EventType.SMS_SEND_RESULT, JsonHelper.jsonEncode(result)))
    }

    private fun getProjection(): Array<String> {
        return arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.TYPE,
            Telephony.Sms.BODY,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.READ,
            Telephony.Sms.DATE,
            Telephony.Sms.SERVICE_CENTER,
            Telephony.Sms.SUBSCRIPTION_ID,
        )
    }

    private fun cursorToSmsMessage(cursor: Cursor, cache: MutableMap<String, Int>): DMessage {
        return DMessage(
            cursor.getStringValue(Telephony.Sms._ID, cache),
            cursor.getStringValue(Telephony.Sms.BODY, cache),
            cursor.getStringValue(Telephony.Sms.ADDRESS, cache),
            cursor.getTimeValue(Telephony.Sms.DATE, cache),
            cursor.getStringValue(Telephony.Sms.SERVICE_CENTER, cache),
            cursor.getIntValue(Telephony.Sms.READ, cache) == 1,
            cursor.getStringValue(Telephony.Sms.THREAD_ID, cache),
            cursor.getIntValue(Telephony.Sms.TYPE, cache),
            cursor.getIntValue(Telephony.Sms.SUBSCRIPTION_ID, cache, -1),
        )
    }

    private fun queryCount(context: Context, uri: Uri, selection: String? = null, selectionArgs: Array<String>? = null): Int {
        var count = 0
        context.contentResolver.queryCursor(
            uri, arrayOf("COUNT(*) as count"),
            selection, selectionArgs, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) count = cursor.getInt(0)
        }
        return count
    }

    suspend fun searchAsync(context: Context, query: String, limit: Int, offset: Int, includeTrashed: Boolean = false): List<DMessage> =
        RustSmsQuery.search(query, limit, offset, includeTrashed)

    private fun cursorToMmsMessage(context: Context, cursor: Cursor, cache: MutableMap<String, Int>): DMessage {
        val rawMmsId = cursor.getStringValue(Telephony.Mms._ID, cache)
        val bodyAndAttachments = readMmsBodyAndAttachments(context, rawMmsId)
        return DMessage(
            id = "mms_$rawMmsId",
            body = bodyAndAttachments.first,
            address = readMmsAddress(context, rawMmsId),
            date = cursor.getTimeSecondsValue(Telephony.Mms.DATE, cache),
            serviceCenter = "",
            read = cursor.getIntValue(Telephony.Mms.READ, cache) == 1,
            threadId = cursor.getStringValue(Telephony.Mms.THREAD_ID, cache),
            type = cursor.getIntValue(Telephony.Mms.MESSAGE_BOX, cache),
            subscriptionId = cursor.getIntValue(Telephony.Mms.SUBSCRIPTION_ID, cache, -1),
            isMms = true,
            attachments = bodyAndAttachments.second,
        )
    }

    private fun getCanonicalAddressForThread(context: Context, threadId: String): String {
        val conversationsUri = "content://mms-sms/conversations?simple=true".toUri()
        val recipientIds = context.contentResolver.queryCursor(
            conversationsUri,
            arrayOf(BaseColumns._ID, "recipient_ids"),
            "${BaseColumns._ID} = ?",
            arrayOf(threadId),
            null
        )?.find { cursor, cache ->
            cursor.getStringValue("recipient_ids", cache)
        } ?: ""

        if (recipientIds.isEmpty()) return ""

        val addresses = SmsProviderContract.parseRecipientIds(recipientIds).mapNotNull { recipientId ->
            val canonicalUri = "content://mms-sms/canonical-address/$recipientId".toUri()
            context.contentResolver.queryCursor(
                canonicalUri,
                arrayOf("address"),
            )?.find { cursor, cache ->
                cursor.getStringValue("address", cache)
            }?.takeIf(String::isNotEmpty)
        }
        val ownNumbers = getSims().map { it.number }.filter(String::isNotEmpty).toSet()
        return SmsProviderContract.selectConversationAddresses(addresses, ownNumbers).firstOrNull().orEmpty()
    }

    internal fun readMmsAddress(context: Context, mmsId: String): String {
        val addrUri = "content://mms/$mmsId/addr".toUri()
        val colType = Telephony.Mms.Addr.TYPE
        val colAddress = Telephony.Mms.Addr.ADDRESS
        val candidates = context.contentResolver.queryCursor(
            addrUri,
            arrayOf(colAddress, colType),
            "$colType = ? OR $colType = ?",
            arrayOf(MMS_ADDR_TYPE_FROM.toString(), MMS_ADDR_TYPE_TO.toString()),
            null
        )?.map { cursor, cache ->
            val address = cursor.getStringValue(colAddress, cache)
            val type = cursor.getIntValue(colType, cache)
            Pair(address, type)
        } ?: emptyList()

        val preferred = candidates.firstOrNull {
            it.second == MMS_ADDR_TYPE_FROM &&
                it.first.isNotEmpty() &&
                !it.first.equals(MMS_INSERT_ADDRESS_TOKEN, true)
        }?.first
        if (!preferred.isNullOrEmpty()) {
            return preferred
        }

        return candidates.firstOrNull {
            it.first.isNotEmpty() &&
                !it.first.equals(MMS_INSERT_ADDRESS_TOKEN, true)
        }?.first ?: ""
    }

    internal fun readMmsBodyAndAttachments(context: Context, mmsId: String): Pair<String, List<DMessageAttachment>> {
        val bodyParts = mutableListOf<String>()
        val attachments = mutableListOf<DMessageAttachment>()

        context.contentResolver.queryCursor(
            mmsPartUri,
            arrayOf(
                Telephony.Mms.Part._ID,
                Telephony.Mms.Part.CONTENT_TYPE,
                Telephony.Mms.Part.NAME,
                Telephony.Mms.Part.FILENAME,
                Telephony.Mms.Part._DATA,
                Telephony.Mms.Part.TEXT,
            ),
            "mid = ?",
            arrayOf(mmsId),
            null
        )?.use { cursor ->
            val cache = mutableMapOf<String, Int>()
            while (cursor.moveToNext()) {
                val partId = cursor.getStringValue(Telephony.Mms.Part._ID, cache)
                val contentType = cursor.getStringValue(Telephony.Mms.Part.CONTENT_TYPE, cache)
                val contentTypeLower = contentType.lowercase(Locale.ROOT)
                val dataColumn = cursor.getStringValue(Telephony.Mms.Part._DATA, cache)

                if (contentTypeLower == "text/plain") {
                    val text = readMmsTextPart(
                        context,
                        partId,
                        cursor.getStringValue(Telephony.Mms.Part.TEXT, cache),
                        dataColumn,
                    )
                    if (text.isNotEmpty()) {
                        bodyParts.add(text)
                    }
                    continue
                }

                if (contentTypeLower.startsWith("image/") ||
                    contentTypeLower.startsWith("video/") ||
                    contentTypeLower.startsWith("audio/") ||
                    dataColumn.isNotEmpty()
                ) {
                    val rawName = cursor.getStringValue(Telephony.Mms.Part.NAME, cache)
                    val fileName = if (rawName.isNotEmpty()) rawName else cursor.getStringValue(Telephony.Mms.Part.FILENAME, cache)
                    attachments.add(
                        DMessageAttachment(
                            path = "content://mms/part/$partId",
                            contentType = contentType,
                            name = fileName,
                        )
                    )
                }
            }
        }

        val body = bodyParts.joinToString("\n").trim().ifEmpty {
            if (attachments.isNotEmpty()) "[MMS]" else ""
        }
        return Pair(body, attachments)
    }

    private fun readMmsTextPart(context: Context, partId: String, inlineText: String, dataColumn: String): String {
        if (inlineText.isNotEmpty() || dataColumn.isEmpty()) return inlineText
        return runCatching {
            context.contentResolver.openInputStream(Uri.parse("content://mms/part/$partId"))
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
        }.getOrDefault("")
    }

    internal suspend fun findMmsIdsMatchingText(context: Context, filters: List<String>): Set<String>? =
        if (filters.isEmpty()) null else RustSmsQuery.textIds(filters)

    private fun mmsTextFacts(context: Context): Map<String, List<String>> {
        val textByMmsId = linkedMapOf<String, MutableList<String>>()
        context.contentResolver.queryCursor(
            mmsPartUri,
            arrayOf(
                Telephony.Mms.Part._ID,
                "mid",
                Telephony.Mms.Part.TEXT,
                Telephony.Mms.Part._DATA,
            ),
            "${Telephony.Mms.Part.CONTENT_TYPE} = ?",
            arrayOf("text/plain"),
            null,
        )?.use { cursor ->
            val cache = mutableMapOf<String, Int>()
            while (cursor.moveToNext()) {
                val partId = cursor.getStringValue(Telephony.Mms.Part._ID, cache)
                val mmsId = cursor.getStringValue("mid", cache)
                val text = readMmsTextPart(
                    context,
                    partId,
                    cursor.getStringValue(Telephony.Mms.Part.TEXT, cache),
                    cursor.getStringValue(Telephony.Mms.Part._DATA, cache),
                )
                textByMmsId.getOrPut(mmsId) { mutableListOf() }.add(text)
            }
        }
        return textByMmsId
    }

    data class SmsCounts(val total: Int, val inbox: Int, val sent: Int, val drafts: Int)

    suspend fun countAllAsync(context: Context): SmsCounts {
        val result = RustSmsQuery.counts()
        return SmsCounts(result.getValue("total").jsonPrimitive.int, result.getValue("inbox").jsonPrimitive.int,
            result.getValue("sent").jsonPrimitive.int, result.getValue("drafts").jsonPrimitive.int)
    }
    suspend fun countAsync(context: Context, query: String): Int = RustSmsQuery.count(query)
    suspend fun getIdsAsync(context: Context, query: String, includeTrashed: Boolean = false): Set<String> = RustSmsQuery.ids(query, includeTrashed)

    private fun JsonElement.where(): ContentWhere = ContentWhere().apply {
        jsonObject.getValue("clauses").jsonArray.forEach { add(it.jsonPrimitive.content) }
        args.addAll(jsonObject.getValue("args").jsonArray.map { it.jsonPrimitive.content })
    }
    suspend fun facts(context: Context, method: String, params: JsonObject): JsonElement = withIO {
        if (method == "systemMmsTextFacts") return@withIO Json.parseToJsonElement(JsonHelper.jsonEncode(mmsTextFacts(context)))
        if (method == "systemSmsConversationFacts") return@withIO SmsConversationHelper.facts(context, params)
        val plans = if (method == "systemSmsRowsFacts") params.getValue("plans").jsonObject else params
        val smsWhere = plans.getValue("sms").where()
        val mmsWhere = plans["mms"]?.takeUnless { it is JsonNull }?.where()
        when (method) {
            "systemSmsThreadFacts" -> {
                val hits=mutableListOf<Pair<String,String>>()
                context.contentResolver.queryCursor(smsUri,arrayOf(Telephony.Sms.THREAD_ID,Telephony.Sms.DATE),smsWhere.toSelection(),smsWhere.args.toTypedArray(),null)?.use { cursor ->
                    val cache=mutableMapOf<String,Int>(); while(cursor.moveToNext()) {
                        hits.add(cursor.getStringValue(Telephony.Sms.THREAD_ID,cache) to cursor.getTimeValue(Telephony.Sms.DATE,cache).toString())
                    }
                }
                if(mmsWhere!=null) context.contentResolver.queryCursor(mmsUri,arrayOf(Telephony.Mms.THREAD_ID,Telephony.Mms.DATE),mmsWhere.toSelection(),mmsWhere.args.toTypedArray(),null)?.use { cursor ->
                    val cache=mutableMapOf<String,Int>(); while(cursor.moveToNext()) {
                        hits.add(cursor.getStringValue(Telephony.Mms.THREAD_ID,cache) to cursor.getTimeSecondsValue(Telephony.Mms.DATE,cache).toString())
                    }
                }
                Json.parseToJsonElement(JsonHelper.jsonEncode(hits))
            }
            "systemSmsCountFacts" -> buildJsonObject {
                put("sms", queryCount(context, smsUri, smsWhere.toSelection(), smsWhere.args.toTypedArray()))
                put("mms", mmsWhere?.let { queryCount(context, mmsUri, it.toSelection(), it.args.toTypedArray()) } ?: 0)
            }
            "systemSmsIdsFacts" -> {
                val smsIds = context.contentResolver.queryCursor(smsUri, arrayOf(BaseColumns._ID), smsWhere.toSelection(), smsWhere.args.toTypedArray(), null)
                    ?.map { cursor, cache -> cursor.getStringValue(BaseColumns._ID, cache) }.orEmpty()
                val mmsIds = mmsWhere?.let { where ->
                    context.contentResolver.queryCursor(mmsUri, arrayOf(BaseColumns._ID), where.toSelection(), where.args.toTypedArray(), null)
                        ?.map { cursor, cache -> "mms_${cursor.getStringValue(BaseColumns._ID, cache)}" }
                }.orEmpty()
                JsonArray((smsIds + mmsIds).map(::JsonPrimitive))
            }
            "systemSmsRowsFacts" -> {
                val limit = params.getValue("limit").jsonPrimitive.long
                require(limit in 1..4294967294L)
                val sms = context.contentResolver.queryCursor(smsUri, getProjection(), smsWhere.toSelection(), smsWhere.args.toTypedArray(), "${Telephony.Sms.DATE} DESC LIMIT $limit")
                    ?.map { cursor, cache -> cursorToSmsMessage(cursor, cache) }.orEmpty()
                val mms = mmsWhere?.let { where ->
                    context.contentResolver.queryCursor(mmsUri, arrayOf(Telephony.Mms._ID, Telephony.Mms.DATE, Telephony.Mms.THREAD_ID,
                        Telephony.Mms.MESSAGE_BOX, Telephony.Mms.READ, Telephony.Mms.SUBSCRIPTION_ID), where.toSelection(), where.args.toTypedArray(), "${Telephony.Mms.DATE} DESC LIMIT $limit")
                        ?.map { cursor, cache -> cursorToMmsMessage(context, cursor, cache) }
                }.orEmpty()
                val threadId = plans.getValue("threadId").jsonPrimitive.content
                buildJsonObject {
                    put("items", Json.parseToJsonElement(JsonHelper.jsonEncode(sms + mms)))
                    put("canonicalAddress", if (threadId.isNotEmpty()) getCanonicalAddressForThread(context, threadId) else "")
                }
            }
            else -> error("Unsupported SMS facts")
        }
    }
}
