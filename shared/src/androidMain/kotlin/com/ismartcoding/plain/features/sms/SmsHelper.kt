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
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.smsManager
import com.ismartcoding.plain.appContext
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
    private val smsUri = Telephony.Sms.CONTENT_URI
    private val mmsUri = Telephony.Mms.CONTENT_URI
    private val mmsPartUri = "content://mms/part".toUri()

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
            appContext,
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
            SmsSendResultTracker.cancel(appContext, requestId)
            throw e
        }
        scheduleSmsTimeout(requestId, SMS_SEND_TIMEOUT_MILLIS)
    }

    private fun scheduleSmsTimeout(requestId: String, delayMillis: Long) {
        smsTimeoutJobs.remove(requestId)?.cancel()
        val job = coIO {
            delay(delayMillis.coerceAtLeast(0L))
            val result = SmsSendResultTracker.expire(appContext, requestId, TimeHelper.nowMillis()) ?: return@coIO
            dispatchSmsSendResult(requestId, result)
        }
        trackSmsJob(requestId, job)
    }

    private fun scheduleSmsTerminalCleanup(requestId: String, delayMillis: Long) {
        smsTimeoutJobs.remove(requestId)?.cancel()
        val job = coIO {
            delay(delayMillis.coerceAtLeast(0L))
            SmsSendResultTracker.acknowledge(appContext, requestId)
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
        SmsSendResultTracker.pending(appContext).forEach { state ->
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
        SmsSendResultTracker.terminalResults(appContext).forEach { result ->
            sendSmsResultEvent(result)
        }
    }

    private suspend fun sendSmsResultEvent(result: SmsSendResultData) {
        WebSocketHelper.sendEventAsync(
            WebSocketEvent(
                EventType.SMS_SEND_RESULT,
                JsonHelper.jsonEncode(result),
            ),
        )
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

    private fun buildWhere(
        conditions: List<FilterField>,
        archivedRecords: List<DArchivedConversation>,
    ): ContentWhere {
        val where = ContentWhere()
        conditions.forEach {
            when (it.name) {
                "text" -> where.add("${Telephony.Sms.BODY} LIKE ?", "%${it.value}%")
                "ids" -> {
                    val ids = SmsProviderContract.partitionMessageIds(it.value).sms
                    if (ids.isEmpty()) where.add("${BaseColumns._ID} = ?", "-1") else where.addIn(BaseColumns._ID, ids)
                }
                "type" -> where.add("${Telephony.Sms.TYPE} = ?", it.value)
                "thread_id" -> where.add("${Telephony.Sms.THREAD_ID} = ?", it.value)
            }
        }

        val threadIdCondition = conditions.firstOrNull { it.name == "thread_id" }
        val isArchived = conditions.any { it.name == "archived" && it.value == "1" }
        if (threadIdCondition != null) {
            val archivedConversation = archivedRecords.firstOrNull { it.conversationId == threadIdCondition.value }
            if (archivedConversation != null) {
                if (isArchived) {
                    where.add("${Telephony.Sms.DATE} <= ?", archivedConversation.conversationDate.toString())
                } else {
                    where.add("${Telephony.Sms.DATE} > ?", archivedConversation.conversationDate.toString())
                }
            }
        }

        return where
    }

    private fun buildMmsWhere(
        conditions: List<FilterField>,
        archivedRecords: List<DArchivedConversation>,
        textMatchedMmsIds: Set<String>? = null,
    ): ContentWhere? {
        val where = ContentWhere()
        if (conditions.any { it.name == "text" }) {
            if (textMatchedMmsIds.isNullOrEmpty()) return null
            val predicate = SmsProviderContract.numericIdPredicate(BaseColumns._ID, textMatchedMmsIds) ?: return null
            where.add(predicate)
        }
        conditions.forEach {
            when (it.name) {
                "ids" -> {
                    val ids = SmsProviderContract.partitionMessageIds(it.value).mms
                    if (ids.isEmpty()) return null
                    val predicate = SmsProviderContract.numericIdPredicate(BaseColumns._ID, ids) ?: return null
                    where.add(predicate)
                }

                "type" -> where.add("${Telephony.Mms.MESSAGE_BOX} = ?", it.value)
                "thread_id" -> where.add("${Telephony.Mms.THREAD_ID} = ?", it.value)
            }
        }
        where.add(SmsProviderContract.MMS_CONTENT_FILTER)

        val threadId = conditions.firstOrNull { it.name == "thread_id" }?.value
        val archivedConversation = archivedRecords.firstOrNull { it.conversationId == threadId }
        if (archivedConversation != null) {
            val isArchived = conditions.any { it.name == "archived" && it.value == "1" }
            where.add(
                "${Telephony.Mms.DATE} ${if (isArchived) "<=" else ">"} ?",
                (archivedConversation.conversationDate / 1000).toString(),
            )
        }
        return where
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

    suspend fun searchAsync(
        context: Context,
        query: String,
        limit: Int,
        offset: Int,
    ): List<DMessage> = withIO {
        val conditions = QueryHelper.parseAsync(query)
        val archivedRecords = AppDatabase.instance.archivedConversationDao().getAll()
        val threadId = conditions.firstOrNull { it.name == "thread_id" }?.value ?: ""
        if (threadId.isNotEmpty()) {
            return@withIO searchByThreadAsync(context, threadId, conditions, archivedRecords, limit, offset)
        }

        val where = buildWhere(conditions, archivedRecords)
        val fetchCap = offset + limit
        val textMatchedMmsIds = findMmsIdsMatchingText(
            context,
            conditions.filter { it.name == "text" }.map { it.value },
        )
        val smsItems = context.contentResolver.queryCursor(
            smsUri, getProjection(), where.toSelection(), where.args.toTypedArray(),
            "${Telephony.Sms.DATE} DESC LIMIT $fetchCap"
        )?.map { cursor, cache ->
            cursorToSmsMessage(cursor, cache)
        } ?: emptyList()

        val mmsItems = buildMmsWhere(conditions, archivedRecords, textMatchedMmsIds)?.let { mmsWhere ->
            context.contentResolver.queryCursor(
                mmsUri,
                arrayOf(
                    Telephony.Mms._ID, Telephony.Mms.DATE, Telephony.Mms.THREAD_ID,
                    Telephony.Mms.MESSAGE_BOX, Telephony.Mms.READ, Telephony.Mms.SUBSCRIPTION_ID,
                ),
                mmsWhere.toSelection(),
                mmsWhere.args.toTypedArray(),
                "${Telephony.Mms.DATE} DESC LIMIT $fetchCap",
            )?.map { cursor, cache ->
                cursorToMmsMessage(context, cursor, cache)
            }
        }.orEmpty()

        return@withIO smsItems.plus(mmsItems)
            .sortedByDescending { it.date }
            .drop(offset)
            .take(limit)
    }

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

    private fun searchByThreadAsync(
        context: Context,
        threadId: String,
        conditions: List<FilterField>,
        archivedRecords: List<DArchivedConversation>,
        limit: Int,
        offset: Int,
    ): List<DMessage> {
        val fetchCap = offset + limit

        val smsWhere = buildWhere(conditions, archivedRecords)
        val textMatchedMmsIds = findMmsIdsMatchingText(
            context,
            conditions.filter { it.name == "text" }.map { it.value },
        )
        val mmsWhere = buildMmsWhere(conditions, archivedRecords, textMatchedMmsIds)

        // Query SMS and MMS separately — this is reliable across all devices.
        val smsItems = context.contentResolver.queryCursor(
            smsUri,
            getProjection(),
            smsWhere.toSelection(),
            smsWhere.args.toTypedArray(),
            "${Telephony.Sms.DATE} DESC LIMIT $fetchCap",
        )?.map { cursor, cache ->
            cursorToSmsMessage(cursor, cache)
        } ?: emptyList()

        val mmsItems = mmsWhere?.let { where ->
            context.contentResolver.queryCursor(
                mmsUri,
                arrayOf(
                    Telephony.Mms._ID, Telephony.Mms.DATE, Telephony.Mms.THREAD_ID,
                    Telephony.Mms.MESSAGE_BOX, Telephony.Mms.READ, Telephony.Mms.SUBSCRIPTION_ID,
                ),
                where.toSelection(),
                where.args.toTypedArray(),
                "${Telephony.Mms.DATE} DESC LIMIT $fetchCap",
            )?.map { cursor, cache ->
                cursorToMmsMessage(context, cursor, cache)
            }
        }.orEmpty()

        val allItems = smsItems.plus(mmsItems).sortedByDescending { it.date }

        // Fill empty addresses from canonical_addresses (e.g., for failed SMS type=5)
        val canonicalAddress = if (allItems.any { it.address.isEmpty() }) {
            getCanonicalAddressForThread(context, threadId)
        } else ""

        val result = if (canonicalAddress.isNotEmpty()) {
            allItems.map { if (it.address.isEmpty()) it.copy(address = canonicalAddress) else it }
        } else {
            allItems
        }

        return result.drop(offset).take(limit)
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

    internal fun findMmsIdsMatchingText(context: Context, filters: List<String>): Set<String>? {
        if (filters.isEmpty()) return null
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
        return textByMmsId.filterValues { SmsProviderContract.mmsTextMatches(it, filters) }.keys
    }

    data class SmsCounts(val total: Int, val inbox: Int, val sent: Int, val drafts: Int)

    suspend fun countAllAsync(context: Context): SmsCounts = coroutineScope {
        val totalSms = async(Dispatchers.IO) { queryCount(context, smsUri, null, null) }
        val totalMms = async(Dispatchers.IO) { queryCount(context, mmsUri, SmsProviderContract.MMS_CONTENT_FILTER, null) }
        val inboxSms = async(Dispatchers.IO) { queryCount(context, smsUri, "${Telephony.Sms.TYPE} = ?", arrayOf("1")) }
        val inboxMms = async(Dispatchers.IO) { queryCount(context, mmsUri, "${Telephony.Mms.MESSAGE_BOX} = ? AND ${SmsProviderContract.MMS_CONTENT_FILTER}", arrayOf("1")) }
        val sentSms = async(Dispatchers.IO) { queryCount(context, smsUri, "${Telephony.Sms.TYPE} = ?", arrayOf("2")) }
        val sentMms = async(Dispatchers.IO) { queryCount(context, mmsUri, "${Telephony.Mms.MESSAGE_BOX} = ? AND ${SmsProviderContract.MMS_CONTENT_FILTER}", arrayOf("2")) }
        val draftsSms = async(Dispatchers.IO) { queryCount(context, smsUri, "${Telephony.Sms.TYPE} = ?", arrayOf("3")) }
        val draftsMms = async(Dispatchers.IO) { queryCount(context, mmsUri, "${Telephony.Mms.MESSAGE_BOX} = ? AND ${SmsProviderContract.MMS_CONTENT_FILTER}", arrayOf("3")) }
        SmsCounts(
            total = totalSms.await() + totalMms.await(),
            inbox = inboxSms.await() + inboxMms.await(),
            sent = sentSms.await() + sentMms.await(),
            drafts = draftsSms.await() + draftsMms.await(),
        )
    }

    suspend fun countAsync(context: Context, query: String): Int = withIO {
        val conditions = QueryHelper.parseAsync(query)
        val archivedRecords = AppDatabase.instance.archivedConversationDao().getAll()
        val threadId = conditions.firstOrNull { it.name == "thread_id" }?.value ?: ""
        val textMatchedMmsIds = findMmsIdsMatchingText(
            context,
            conditions.filter { it.name == "text" }.map { it.value },
        )

        if (threadId.isNotEmpty()) {
            return@withIO countByThread(context, conditions, archivedRecords, textMatchedMmsIds)
        }

        val where = buildWhere(conditions, archivedRecords)

        // Count SMS (date filter for archived conversations applied in buildWhereAsync)
        val smsCount = queryCount(context, smsUri, where.toSelection(), where.args.toTypedArray())

        val mmsCount = buildMmsWhere(conditions, archivedRecords, textMatchedMmsIds)?.let { mmsWhere ->
            queryCount(context, mmsUri, mmsWhere.toSelection(), mmsWhere.args.toTypedArray())
        } ?: 0

        return@withIO smsCount + mmsCount
    }

    private fun countByThread(
        context: Context,
        conditions: List<FilterField>,
        archivedRecords: List<DArchivedConversation>,
        textMatchedMmsIds: Set<String>?,
    ): Int {
        val smsWhere = buildWhere(conditions, archivedRecords)
        val smsCount = queryCount(context, smsUri, smsWhere.toSelection(), smsWhere.args.toTypedArray())
        val mmsCount = buildMmsWhere(conditions, archivedRecords, textMatchedMmsIds)?.let { mmsWhere ->
            queryCount(context, mmsUri, mmsWhere.toSelection(), mmsWhere.args.toTypedArray())
        } ?: 0
        return smsCount + mmsCount
    }

    suspend fun getIdsAsync(context: Context, query: String): Set<String> = withIO {
        val conditions = QueryHelper.parseAsync(query)
        val archivedRecords = AppDatabase.instance.archivedConversationDao().getAll()
        val where = buildWhere(conditions, archivedRecords)
        val textMatchedMmsIds = findMmsIdsMatchingText(
            context,
            conditions.filter { it.name == "text" }.map { it.value },
        )
        val smsIds = context.contentResolver.queryCursor(
            smsUri,
            arrayOf(BaseColumns._ID),
            where.toSelection(),
            where.args.toTypedArray(),
            null
        )?.map { cursor, cache ->
            cursor.getStringValue(BaseColumns._ID, cache)
        }.orEmpty()
        val mmsIds = buildMmsWhere(conditions, archivedRecords, textMatchedMmsIds)?.let { mmsWhere ->
            context.contentResolver.queryCursor(
                mmsUri,
                arrayOf(BaseColumns._ID),
                mmsWhere.toSelection(),
                mmsWhere.args.toTypedArray(),
                null,
            )?.map { cursor, cache ->
                "mms_${cursor.getStringValue(BaseColumns._ID, cache)}"
            }
        }.orEmpty()
        return@withIO (smsIds + mmsIds).toSet()
    }
}
