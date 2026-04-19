package com.ismartcoding.plain.features.sms

import android.content.Context
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
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.helpers.QueryHelper
import kotlin.time.Instant

object SmsConversationHelper {
    private val conversationsUri = "content://mms-sms/conversations?simple=true".toUri()
    private val smsUri = Telephony.Sms.CONTENT_URI
    private val mmsUri = Telephony.Mms.CONTENT_URI

    /**
     * Returns the set of archived conversation IDs that have NO new messages
     * after the archive date (i.e., still effectively archived).
     */
    private suspend fun getActiveArchivedIds(context: Context): Set<String> {
        val archivedRecords = AppDatabase.instance.archivedConversationDao().getAll()
        if (archivedRecords.isEmpty()) return emptySet()
        val convDates = queryConversationsByThreadIds(context, archivedRecords.map { it.conversationId })
        return archivedRecords.filter { archived ->
            val conv = convDates[archived.conversationId]
            conv == null || conv.date.toEpochMilliseconds() <= archived.conversationDate
        }.map { it.conversationId }.toSet()
    }

    /**
     * Returns the latest SMS snippet before the given date for a conversation.
     */
    private fun getSnippetBeforeDate(context: Context, threadId: String, beforeDate: Long): String? {
        return context.contentResolver.queryCursor(
            smsUri,
            arrayOf(Telephony.Sms.BODY),
            "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.DATE} <= ?",
            arrayOf(threadId, beforeDate.toString()),
            "${Telephony.Sms.DATE} DESC"
        )?.find { cursor, cache ->
            cursor.getStringValue(Telephony.Sms.BODY, cache)
        }
    }

    /**
     * Returns archived conversations sorted by archive date descending,
     * with snippet/date adjusted to reflect state at archive time.
     */
    suspend fun getArchivedConversations(context: Context): List<DMessageConversation> {
        val archivedRecords = AppDatabase.instance.archivedConversationDao().getAll()
            .sortedByDescending { it.conversationDate }
        if (archivedRecords.isEmpty()) return emptyList()
        val conversations = getConversationsByIds(context, archivedRecords.map { it.conversationId })
        val archivedMap = archivedRecords.associateBy { it.conversationId }
        return conversations.map { conv ->
            val archiveDate = archivedMap[conv.id]?.conversationDate ?: return@map conv
            val oldSnippet = getSnippetBeforeDate(context, conv.id, archiveDate)
            conv.copy(
                snippet = oldSnippet ?: conv.snippet,
                date = Instant.fromEpochMilliseconds(archiveDate),
            )
        }
    }

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

    private val canonicalAddressesUri = "content://mms-sms/canonical-addresses".toUri()

    private fun batchGetCanonicalAddresses(context: Context, recipientIds: Set<String>): Map<String, String> {
        if (recipientIds.isEmpty()) return emptyMap()

        val where = ContentWhere().apply { addIn(BaseColumns._ID, recipientIds.toList()) }
        val result = mutableMapOf<String, String>()

        context.contentResolver.queryCursor(
            canonicalAddressesUri,
            arrayOf(BaseColumns._ID, "address"),
            where.toSelection(),
            where.args.toTypedArray(),
            null
        )?.use { cursor ->
            val cache = mutableMapOf<String, Int>()
            while (cursor.moveToNext()) {
                val id = cursor.getStringValue(BaseColumns._ID, cache)
                val address = cursor.getStringValue("address", cache)
                if (address.isNotEmpty()) result[id] = address
            }
        }

        return result
    }

    private fun queryConversationsWithAddresses(context: Context, threadIds: List<String>): Map<String, DMessageConversation> {
        if (threadIds.isEmpty()) return emptyMap()

        val where = ContentWhere().apply { addIn(BaseColumns._ID, threadIds) }
        val threadRecipientMap = mutableMapOf<String, String>()
        val conversationMap = mutableMapOf<String, DMessageConversation>()

        context.contentResolver.queryCursor(
            conversationsUri,
            arrayOf(
                BaseColumns._ID, Telephony.Threads.SNIPPET, Telephony.Threads.DATE,
                Telephony.Threads.MESSAGE_COUNT, Telephony.Threads.READ, "recipient_ids",
            ),
            where.toSelection(),
            where.args.toTypedArray(),
            "${Telephony.Threads.DATE} DESC"
        )?.use { cursor ->
            val cache = mutableMapOf<String, Int>()
            while (cursor.moveToNext()) {
                val threadId = cursor.getStringValue(BaseColumns._ID, cache)
                conversationMap[threadId] = DMessageConversation(
                    threadId, "",
                    cursor.getStringValue(Telephony.Threads.SNIPPET, cache),
                    cursor.getTimeValue(Telephony.Threads.DATE, cache),
                    cursor.getIntValue(Telephony.Threads.MESSAGE_COUNT, cache),
                    cursor.getIntValue(Telephony.Threads.READ, cache) == 1,
                )
                val recipientIds = cursor.getStringValue("recipient_ids", cache)
                if (recipientIds.isNotEmpty()) {
                    val firstId = recipientIds.trim().split("\\s+".toRegex()).firstOrNull()
                    if (firstId != null) threadRecipientMap[threadId] = firstId
                }
            }
        }

        val addressMap = batchGetCanonicalAddresses(context, threadRecipientMap.values.toSet())
        threadRecipientMap.forEach { (threadId, recipientId) ->
            val address = addressMap[recipientId]
            if (!address.isNullOrEmpty()) {
                conversationMap[threadId] = conversationMap[threadId]!!.copy(address = address)
            }
        }

        return conversationMap
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

        val conditions = QueryHelper.parseAsync(query)
        val where = buildWhereAsync(query)
        val ids = linkedSetOf<String>()

        // Query SMS table for matching thread IDs
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

        // Also query MMS table for matching thread IDs (SMS type maps to MMS msg_box)
        val hasTextOrIdsFilter = conditions.any { it.name == "text" || it.name == "ids" }
        if (!hasTextOrIdsFilter) {
            val mmsWhere = ContentWhere()
            val typeCondition = conditions.firstOrNull { it.name == "type" }
            if (typeCondition != null) {
                mmsWhere.add("${Telephony.Mms.MESSAGE_BOX} = ?", typeCondition.value)
            }
            val threadIdCondition = conditions.firstOrNull { it.name == "thread_id" }
            if (threadIdCondition != null) {
                mmsWhere.add("${Telephony.Mms.THREAD_ID} = ?", threadIdCondition.value)
            }
            mmsWhere.add("m_type IN (128, 130)")

            context.contentResolver.queryCursor(
                mmsUri,
                arrayOf(Telephony.Mms.THREAD_ID),
                mmsWhere.toSelection(),
                mmsWhere.args.toTypedArray(),
                "${Telephony.Mms.DATE} DESC"
            )?.use { cursor ->
                val cache = mutableMapOf<String, Int>()
                while (cursor.moveToNext()) {
                    ids.add(cursor.getStringValue(Telephony.Mms.THREAD_ID, cache))
                }
            }
        }

        return ids.toList()
    }

    suspend fun getConversationsByIds(context: Context, ids: List<String>): List<DMessageConversation> {
        if (ids.isEmpty()) return emptyList()
        val conversationMap = queryConversationsWithAddresses(context, ids)
        return ids.mapNotNull { conversationMap[it] }
    }

    suspend fun searchConversationsAsync(
        context: Context,
        query: String,
        limit: Int,
        offset: Int,
    ): List<DMessageConversation> {
        if (query.isNotEmpty()) {
            val activeArchivedIds = getActiveArchivedIds(context)
            val threadIds = getMatchedThreadIdsAsync(context, query)
                .filter { !activeArchivedIds.contains(it) }
                .drop(offset).take(limit)
            if (threadIds.isEmpty()) return emptyList()
            val conversationMap = queryConversationsWithAddresses(context, threadIds)
            return threadIds.mapNotNull { conversationMap[it] }
        }

        // Single-pass: read conversations with full data, filter archived, paginate, resolve addresses
        val archivedRecords = AppDatabase.instance.archivedConversationDao().getAll()
        val archivedMap = archivedRecords.associateBy { it.conversationId }

        val conversations = mutableListOf<DMessageConversation>()
        val recipientMap = mutableMapOf<String, String>() // threadId -> recipientId
        var skip = 0

        // Use SQL LIMIT when no archived conversations to skip (common case)
        val sortOrder = if (archivedMap.isEmpty() && offset == 0) {
            "${Telephony.Threads.DATE} DESC LIMIT $limit"
        } else if (archivedMap.isEmpty()) {
            "${Telephony.Threads.DATE} DESC LIMIT ${offset + limit}"
        } else {
            // Need full scan to filter archived conversations
            "${Telephony.Threads.DATE} DESC LIMIT ${offset + limit + archivedMap.size}"
        }

        context.contentResolver.queryCursor(
            conversationsUri,
            arrayOf(
                BaseColumns._ID, Telephony.Threads.SNIPPET, Telephony.Threads.DATE,
                Telephony.Threads.MESSAGE_COUNT, Telephony.Threads.READ, "recipient_ids",
            ),
            null, null,
            sortOrder
        )?.use { cursor ->
            val cache = mutableMapOf<String, Int>()
            while (cursor.moveToNext() && conversations.size < limit) {
                val id = cursor.getStringValue(BaseColumns._ID, cache)
                val date = cursor.getTimeValue(Telephony.Threads.DATE, cache)
                // Check if this conversation is actively archived
                val archived = archivedMap[id]
                if (archived != null && date.toEpochMilliseconds() <= archived.conversationDate) continue
                if (skip < offset) { skip++; continue }
                conversations.add(DMessageConversation(
                    id, "",
                    cursor.getStringValue(Telephony.Threads.SNIPPET, cache),
                    date,
                    cursor.getIntValue(Telephony.Threads.MESSAGE_COUNT, cache),
                    cursor.getIntValue(Telephony.Threads.READ, cache) == 1,
                ))
                val recipientIds = cursor.getStringValue("recipient_ids", cache)
                if (recipientIds.isNotEmpty()) {
                    val firstId = recipientIds.trim().split("\\s+".toRegex()).firstOrNull()
                    if (firstId != null) recipientMap[id] = firstId
                }
            }
        }

        if (conversations.isEmpty()) return emptyList()

        // Batch resolve addresses
        val addressMap = batchGetCanonicalAddresses(context, recipientMap.values.toSet())
        return conversations.map { conv ->
            val recipientId = recipientMap[conv.id]
            if (recipientId != null) {
                val address = addressMap[recipientId]
                if (!address.isNullOrEmpty()) conv.copy(address = address) else conv
            } else conv
        }
    }

    suspend fun conversationCountAsync(context: Context, query: String): Int {
        val activeArchivedIds = getActiveArchivedIds(context)

        if (query.isNotEmpty()) {
            return getMatchedThreadIdsAsync(context, query).count { !activeArchivedIds.contains(it) }
        }

        // Count all conversations properly, minus truly archived ones
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

        return count - activeArchivedIds.size
    }
}
