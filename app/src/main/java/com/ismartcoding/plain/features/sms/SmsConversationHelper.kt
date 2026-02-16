package com.ismartcoding.plain.features.sms

import android.content.Context
import android.net.Uri
import android.provider.BaseColumns
import android.provider.Telephony
import androidx.core.net.toUri
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.extensions.find
import com.ismartcoding.lib.extensions.getIntValue
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.extensions.getTimeValue
import com.ismartcoding.lib.extensions.map
import com.ismartcoding.lib.extensions.queryCursor
import com.ismartcoding.plain.helpers.QueryHelper

object SmsConversationHelper {
    private val conversationsUri = "content://mms-sms/conversations?simple=true".toUri()
    private val smsUri = Telephony.Sms.CONTENT_URI
    private val mmsUri = Telephony.Mms.CONTENT_URI

    // MMS Addr constants (no standard Telephony constants for these)
    private const val COL_MMS_ADDR_ADDRESS = Telephony.Mms.Addr.ADDRESS
    private const val COL_MMS_ADDR_TYPE = Telephony.Mms.Addr.TYPE

    private const val MMS_ADDR_TYPE_FROM = 137
    private const val MMS_ADDR_TYPE_TO = 151
    private const val MMS_INSERT_ADDRESS_TOKEN = "insert-address-token"

    private fun getConversationsProjection(): Array<String> {
        return arrayOf(
            BaseColumns._ID,
            Telephony.Threads.SNIPPET,
            Telephony.Threads.DATE,
            Telephony.Threads.MESSAGE_COUNT,
            Telephony.Threads.READ,
        )
    }

    private fun queryConversationsByThreadIds(context: Context, threadIds: List<String>): Map<String, DMessageConversation> {
        if (threadIds.isEmpty()) {
            return emptyMap()
        }

        val where = ContentWhere().apply {
            addIn(BaseColumns._ID, threadIds)
        }

        return context.contentResolver.queryCursor(
            conversationsUri,
            getConversationsProjection(),
            where.toSelection(),
            where.args.toTypedArray(),
            "${Telephony.Threads.DATE} DESC"
        )?.map { cursor, cache ->
            DMessageConversation(
                cursor.getStringValue(BaseColumns._ID, cache),
                "",
                cursor.getStringValue(Telephony.Threads.SNIPPET, cache),
                cursor.getTimeValue(Telephony.Threads.DATE, cache),
                cursor.getIntValue(Telephony.Threads.MESSAGE_COUNT, cache),
                cursor.getIntValue(Telephony.Threads.READ, cache) == 1,
            )
        }?.associateBy { it.id } ?: emptyMap()
    }

    private fun getLatestAddressMap(context: Context, threadIds: List<String>): Map<String, String> {
        if (threadIds.isEmpty()) {
            return emptyMap()
        }

        val where = ContentWhere().apply {
            addIn(Telephony.Sms.THREAD_ID, threadIds)
        }

        val result = linkedMapOf<String, String>()

        context.contentResolver.queryCursor(
            smsUri,
            arrayOf(Telephony.Sms.THREAD_ID, Telephony.Sms.ADDRESS),
            where.toSelection(),
            where.args.toTypedArray(),
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val cache = mutableMapOf<String, Int>()
            while (cursor.moveToNext()) {
                val threadId = cursor.getStringValue(Telephony.Sms.THREAD_ID, cache)
                val address = cursor.getStringValue(Telephony.Sms.ADDRESS, cache)
                if (!result.containsKey(threadId) && address.isNotEmpty()) {
                    result[threadId] = address
                }
            }
        }

        return result
    }

    private fun readMmsAddress(context: Context, mmsId: String): String {
        val addrUri = Uri.parse("content://mms/$mmsId/addr")
        val candidates = context.contentResolver.queryCursor(
            addrUri,
            arrayOf(COL_MMS_ADDR_ADDRESS, COL_MMS_ADDR_TYPE),
            "$COL_MMS_ADDR_TYPE = ? OR $COL_MMS_ADDR_TYPE = ?",
            arrayOf(MMS_ADDR_TYPE_FROM.toString(), MMS_ADDR_TYPE_TO.toString()),
            null
        )?.map { cursor, cache ->
            val address = cursor.getStringValue(COL_MMS_ADDR_ADDRESS, cache)
            val type = cursor.getIntValue(COL_MMS_ADDR_TYPE, cache)
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

    private fun getCanonicalAddress(context: Context, recipientId: String): String {
        val uri = Uri.parse("content://mms-sms/canonical-address/$recipientId")
        return context.contentResolver.queryCursor(
            uri, arrayOf("address")
        )?.find { cursor, cache ->
            cursor.getStringValue("address", cache)
        } ?: ""
    }

    private fun getRecipientAddressMap(context: Context, threadIds: List<String>): Map<String, String> {
        if (threadIds.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, String>()
        val where = ContentWhere().apply {
            addIn(BaseColumns._ID, threadIds)
        }

        context.contentResolver.queryCursor(
            conversationsUri,
            arrayOf(BaseColumns._ID, "recipient_ids"),
            where.toSelection(),
            where.args.toTypedArray(),
            null
        )?.use { cursor ->
            val cache = mutableMapOf<String, Int>()
            while (cursor.moveToNext()) {
                val threadId = cursor.getStringValue(BaseColumns._ID, cache)
                val recipientIds = cursor.getStringValue("recipient_ids", cache)
                if (recipientIds.isNotEmpty()) {
                    val firstId = recipientIds.trim().split("\\s+".toRegex()).firstOrNull()
                    if (firstId != null) {
                        val address = getCanonicalAddress(context, firstId)
                        if (address.isNotEmpty()) {
                            result[threadId] = address
                        }
                    }
                }
            }
        }

        return result
    }

    private fun getLatestMmsAddressMap(context: Context, threadIds: List<String>): Map<String, String> {
        if (threadIds.isEmpty()) {
            return emptyMap()
        }

        val where = ContentWhere().apply {
            addIn(Telephony.Mms.THREAD_ID, threadIds)
        }

        val result = linkedMapOf<String, String>()

        context.contentResolver.queryCursor(
            mmsUri,
            arrayOf(Telephony.Mms._ID, Telephony.Mms.THREAD_ID),
            where.toSelection(),
            where.args.toTypedArray(),
            "${Telephony.Mms.DATE} DESC"
        )?.use { cursor ->
            val cache = mutableMapOf<String, Int>()
            while (cursor.moveToNext()) {
                val threadId = cursor.getStringValue(Telephony.Mms.THREAD_ID, cache)
                if (!result.containsKey(threadId)) {
                    val mmsId = cursor.getStringValue(Telephony.Mms._ID, cache)
                    val address = readMmsAddress(context, mmsId)
                    if (address.isNotEmpty()) {
                        result[threadId] = address
                    }
                }
            }
        }

        return result
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

    private suspend fun getMatchedThreadIdsAsync(context: Context, query: String): List<String> {
        if (query.isEmpty()) {
            return emptyList()
        }

        val where = buildWhereAsync(query)
        val ids = linkedSetOf<String>()

        context.contentResolver.queryCursor(
            smsUri,
            arrayOf(Telephony.Sms.THREAD_ID),
            where.toSelection(),
            where.args.toTypedArray(),
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val cache = mutableMapOf<String, Int>()
            while (cursor.moveToNext()) {
                ids.add(cursor.getStringValue(Telephony.Sms.THREAD_ID, cache))
            }
        }

        return ids.toList()
    }

    suspend fun searchConversationsAsync(
        context: Context,
        query: String,
        limit: Int,
        offset: Int,
    ): List<DMessageConversation> {
        // First, get paginated conversation IDs
        val threadIds = if (query.isNotEmpty()) {
            getMatchedThreadIdsAsync(context, query).drop(offset).take(limit)
        } else {
            val ids = mutableListOf<String>()
            context.contentResolver.queryCursor(
                conversationsUri,
                arrayOf(BaseColumns._ID),
                null,
                null,
                "${Telephony.Threads.DATE} DESC"
            )?.use { cursor ->
                val cache = mutableMapOf<String, Int>()
                var skip = 0
                while (cursor.moveToNext() && ids.size < limit) {
                    if (skip < offset) {
                        skip++
                        continue
                    }
                    ids.add(cursor.getStringValue(BaseColumns._ID, cache))
                }
            }
            ids
        }

        if (threadIds.isEmpty()) {
            return emptyList()
        }

        val conversationMap = queryConversationsByThreadIds(context, threadIds)
        val smsAddressMap = getLatestAddressMap(context, threadIds)
        val mmsAddressMap = getLatestMmsAddressMap(context, threadIds)
        // Fallback: resolve address from canonical_addresses table (via recipient_ids in threads)
        // This handles cases where SMS/MMS address fields are empty (e.g., failed SMS type=5)
        val missingThreadIds = threadIds.filter { smsAddressMap[it] == null && mmsAddressMap[it] == null }
        val canonicalAddressMap = getRecipientAddressMap(context, missingThreadIds)

        return threadIds.mapNotNull { threadId ->
            val address = smsAddressMap[threadId] ?: mmsAddressMap[threadId] ?: canonicalAddressMap[threadId] ?: ""
            conversationMap[threadId]?.copy(address = address)
        }
    }

    suspend fun conversationCountAsync(context: Context, query: String): Int {
        if (query.isNotEmpty()) {
            return getMatchedThreadIdsAsync(context, query).size
        }

        // Count all conversations properly
        var count = 0
        context.contentResolver.queryCursor(
            conversationsUri,
            arrayOf("COUNT(*) as count"),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
        }

        return count
    }
}
