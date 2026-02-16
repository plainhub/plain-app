package com.ismartcoding.plain.features.sms

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import android.provider.Telephony
import androidx.core.net.toUri
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.data.SortBy
import com.ismartcoding.lib.data.enums.SortDirection
import com.ismartcoding.lib.extensions.find
import com.ismartcoding.lib.extensions.getIntValue
import com.ismartcoding.lib.extensions.getPagingCursorWithSql
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.extensions.getTimeSecondsValue
import com.ismartcoding.lib.extensions.getTimeValue
import com.ismartcoding.lib.extensions.map
import com.ismartcoding.lib.extensions.queryCursor
import com.ismartcoding.plain.helpers.QueryHelper
import com.ismartcoding.plain.smsManager
import java.util.Locale

object SmsHelper {
    private val smsUri = Telephony.Sms.CONTENT_URI
    private val mmsUri = Telephony.Mms.CONTENT_URI
    private val mmsPartUri = "content://mms/part".toUri()

    private const val MMS_ADDR_TYPE_FROM = 137
    private const val MMS_ADDR_TYPE_TO = 151
    private const val MMS_INSERT_ADDRESS_TOKEN = "insert-address-token"

    fun sendText(to: String, message: String) {
        val parts = smsManager.divideMessage(message)
        if (parts.size > 1) {
            smsManager.sendMultipartTextMessage(to, null, ArrayList(parts), null, null)
        } else {
            smsManager.sendTextMessage(to, null, message, null, null)
        }
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

    private suspend fun buildWhereAsync(query: String): ContentWhere {
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            QueryHelper.parseAsync(query).forEach {
                when (it.name) {
                    "text" -> {
                        where.add("${Telephony.Sms.BODY} LIKE ?", "%${it.value}%")
                    }

                    "ids" -> {
                        where.addIn(BaseColumns._ID, it.value.split(","))
                    }

                    "type" -> {
                        where.add("${Telephony.Sms.TYPE} = ?", it.value)
                    }

                    "thread_id" -> {
                        where.add("${Telephony.Sms.THREAD_ID} = ?", it.value)
                    }
                }
            }
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
            cursor.getIntValue(Telephony.Sms.SUBSCRIPTION_ID, cache),
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
    ): List<DMessage> {
        val conditions = QueryHelper.parseAsync(query)
        val threadId = conditions.firstOrNull { it.name == "thread_id" }?.value ?: ""
        if (threadId.isNotEmpty()) {
            return searchByThreadAsync(context, threadId, limit, offset)
        }

        return context.contentResolver.getPagingCursorWithSql(
            smsUri, getProjection(), buildWhereAsync(query),
            limit, offset, SortBy(Telephony.Sms.DATE, SortDirection.DESC)
        )?.map { cursor, cache ->
            cursorToSmsMessage(cursor, cache)
        } ?: emptyList()
    }

    private suspend fun searchByThreadAsync(
        context: Context,
        threadId: String,
        limit: Int,
        offset: Int,
    ): List<DMessage> {
        // Query SMS and MMS separately — this is reliable across all devices.
        val smsItems = context.contentResolver.queryCursor(
            smsUri,
            getProjection(),
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId),
            "${Telephony.Sms.DATE} DESC"
        )?.map { cursor, cache ->
            cursorToSmsMessage(cursor, cache)
        } ?: emptyList()

        val mmsItems = context.contentResolver.queryCursor(
            mmsUri,
            arrayOf(
                Telephony.Mms._ID, Telephony.Mms.DATE, Telephony.Mms.THREAD_ID,
                Telephony.Mms.MESSAGE_BOX, Telephony.Mms.READ, Telephony.Mms.SUBSCRIPTION_ID,
            ),
            "${Telephony.Mms.THREAD_ID} = ?",
            arrayOf(threadId),
            "${Telephony.Mms.DATE} DESC"
        )?.map { cursor, cache ->
            val rawMmsId = cursor.getStringValue(Telephony.Mms._ID, cache)
            val bodyAndAttachments = readMmsBodyAndAttachments(context, rawMmsId)
            DMessage(
                id = "mms_$rawMmsId",
                body = bodyAndAttachments.first,
                address = readMmsAddress(context, rawMmsId),
                date = cursor.getTimeSecondsValue(Telephony.Mms.DATE, cache),
                serviceCenter = "",
                read = cursor.getIntValue(Telephony.Mms.READ, cache) == 1,
                threadId = cursor.getStringValue(Telephony.Mms.THREAD_ID, cache),
                type = if (cursor.getIntValue(Telephony.Mms.MESSAGE_BOX, cache) == 2) 2 else 1,
                subscriptionId = cursor.getIntValue(Telephony.Mms.SUBSCRIPTION_ID, cache),
                isMms = true,
                attachments = bodyAndAttachments.second,
            )
        } ?: emptyList()

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

        val firstId = recipientIds.trim().split("\\s+".toRegex()).firstOrNull() ?: return ""
        val canonicalUri = "content://mms-sms/canonical-address/$firstId".toUri()
        return context.contentResolver.queryCursor(
            canonicalUri, arrayOf("address")
        )?.find { cursor, cache ->
            cursor.getStringValue("address", cache)
        } ?: ""
    }

    private fun readMmsAddress(context: Context, mmsId: String): String {
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

    private fun readMmsBodyAndAttachments(context: Context, mmsId: String): Pair<String, List<DMessageAttachment>> {
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
                    var text = cursor.getStringValue(Telephony.Mms.Part.TEXT, cache)
                    if (text.isEmpty() && dataColumn.isNotEmpty()) {
                        text = context.contentResolver.openInputStream(Uri.parse("content://mms/part/$partId"))
                            ?.bufferedReader()
                            ?.use { it.readText() }
                            .orEmpty()
                    }
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

    suspend fun countAsync(context: Context, query: String): Int {
        val conditions = QueryHelper.parseAsync(query)
        val threadId = conditions.firstOrNull { it.name == "thread_id" }?.value ?: ""

        // For thread_id queries, count via mms-sms union to be consistent with searchByThreadAsync
        if (threadId.isNotEmpty()) {
            return countByThread(context, threadId)
        }

        val where = buildWhereAsync(query)

        // Count SMS
        val smsCount = queryCount(context, smsUri, where.toSelection(), where.args.toTypedArray())

        // Count MMS (only when no text/ids filter which are SMS-specific)
        val mmsCount = if (query.isEmpty() || (!query.contains("text") && !query.contains("ids"))) {
            queryCount(context, mmsUri)
        } else 0

        return smsCount + mmsCount
    }

    private fun countByThread(context: Context, threadId: String): Int {
        val smsCount = queryCount(context, smsUri, "${Telephony.Sms.THREAD_ID} = ?", arrayOf(threadId))
        val mmsCount = queryCount(context, mmsUri, "${Telephony.Mms.THREAD_ID} = ?", arrayOf(threadId))
        return smsCount + mmsCount
    }

    suspend fun getIdsAsync(context: Context, query: String): Set<String> {
        val where = buildWhereAsync(query)
        return context.contentResolver.queryCursor(
            smsUri,
            arrayOf(BaseColumns._ID),
            where.toSelection(),
            where.args.toTypedArray(),
            null
        )?.map { cursor, cache ->
            cursor.getStringValue(BaseColumns._ID, cache)
        }?.toSet() ?: emptySet()
    }
}
