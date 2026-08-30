package com.ismartcoding.plain.features.sms

import android.content.Context
import android.provider.BaseColumns
import android.provider.Telephony
import androidx.core.net.toUri
import com.ismartcoding.plain.helpers.ContentWhere
import com.ismartcoding.plain.helpers.FilterField
import com.ismartcoding.plain.lib.extensions.find
import com.ismartcoding.plain.lib.extensions.getIntValue
import com.ismartcoding.plain.lib.extensions.getStringValue
import com.ismartcoding.plain.lib.extensions.getTimeSecondsValue
import com.ismartcoding.plain.lib.extensions.getTimeValue
import com.ismartcoding.plain.lib.extensions.map
import com.ismartcoding.plain.lib.extensions.queryCursor
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.platform.getSims
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
    private suspend fun getActiveArchivedIds(context: Context): Set<String> = withIO {
        val archivedRecords = AppDatabase.instance.archivedConversationDao().getAll()
        if (archivedRecords.isEmpty()) return@withIO emptySet()
        val convDates = queryConversationsByThreadIds(context, archivedRecords.map { it.conversationId })
        return@withIO archivedRecords.filter { archived ->
            val conv = convDates[archived.conversationId]
            conv == null || conv.date.toEpochMilliseconds() <= archived.conversationDate
        }.map { it.conversationId }.toSet()
    }

    private data class DatedSnippet(val value: String, val dateMillis: Long)

    private fun getSnippetBeforeDate(context: Context, threadId: String, beforeDate: Long): String? {
        val smsSnippet = context.contentResolver.queryCursor(
            smsUri,
            arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE),
            "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.DATE} <= ?",
            arrayOf(threadId, beforeDate.toString()),
            "${Telephony.Sms.DATE} DESC LIMIT 1",
        )?.find { cursor, cache ->
            DatedSnippet(
                cursor.getStringValue(Telephony.Sms.BODY, cache),
                cursor.getTimeValue(Telephony.Sms.DATE, cache).toEpochMilliseconds(),
            )
        }
        val mmsSnippet = context.contentResolver.queryCursor(
            mmsUri,
            arrayOf(Telephony.Mms._ID, Telephony.Mms.DATE),
            "${Telephony.Mms.THREAD_ID} = ? AND ${Telephony.Mms.DATE} <= ? AND ${SmsProviderContract.MMS_CONTENT_FILTER}",
            arrayOf(threadId, (beforeDate / 1000).toString()),
            "${Telephony.Mms.DATE} DESC LIMIT 1",
        )?.find { cursor, cache ->
            val id = cursor.getStringValue(Telephony.Mms._ID, cache)
            DatedSnippet(
                SmsHelper.readMmsBodyAndAttachments(context, id).first,
                cursor.getTimeSecondsValue(Telephony.Mms.DATE, cache).toEpochMilliseconds(),
            )
        }
        return listOfNotNull(smsSnippet, mmsSnippet).maxByOrNull { it.dateMillis }?.value
    }

    /**
     * Returns archived conversations sorted by archive date descending,
     * with snippet/date adjusted to reflect state at archive time.
     */
    suspend fun getArchivedConversations(context: Context): List<DMessageConversation> = withIO {
        val archivedRecords = AppDatabase.instance.archivedConversationDao().getAll()
            .sortedByDescending { it.conversationDate }
        if (archivedRecords.isEmpty()) return@withIO emptyList()
        val conversations = getConversationsByIds(context, archivedRecords.map { it.conversationId })
        val archivedMap = archivedRecords.associateBy { it.conversationId }
        return@withIO conversations.map { conv ->
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
        val threadRecipientMap = mutableMapOf<String, List<String>>()
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
                    threadRecipientMap[threadId] = SmsProviderContract.parseRecipientIds(recipientIds)
                }
            }
        }

        val addressMap = batchGetCanonicalAddresses(context, threadRecipientMap.values.flatten().toSet())
        val ownNumbers = getSims().map { it.number }.filter(String::isNotEmpty).toSet()
        threadRecipientMap.forEach { (threadId, recipientIds) ->
            val addresses = SmsProviderContract.selectConversationAddresses(
                recipientIds.mapNotNull(addressMap::get),
                ownNumbers,
            )
            if (addresses.isNotEmpty()) {
                conversationMap[threadId] = conversationMap[threadId]!!.copy(
                    address = addresses.first(),
                    addresses = addresses,
                )
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
                        val ids = SmsProviderContract.partitionMessageIds(it.value).sms
                        if (ids.isEmpty()) where.add("${BaseColumns._ID} = ?", "-1") else where.addIn(BaseColumns._ID, ids)
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

    private fun buildMmsWhere(conditions: List<FilterField>, textMatchedMmsIds: Set<String>?): ContentWhere? {
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
        return where
    }

    private suspend fun getMatchedThreadIdsAsync(context: Context, query: String): List<String> = withIO {
        if (query.isEmpty()) {
            return@withIO emptyList()
        }

        val conditions = QueryHelper.parseAsync(query)
        val where = buildWhereAsync(query)
        val textMatchedMmsIds = SmsHelper.findMmsIdsMatchingText(
            context,
            conditions.filter { it.name == "text" }.map { it.value },
        )
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

        buildMmsWhere(conditions, textMatchedMmsIds)?.let { mmsWhere ->
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

        return@withIO ids.toList()
    }

    suspend fun getConversationsByIds(context: Context, ids: List<String>): List<DMessageConversation> = withIO {
        if (ids.isEmpty()) return@withIO emptyList()
        val conversationMap = queryConversationsWithAddresses(context, ids)
        return@withIO ids.mapNotNull { conversationMap[it] }
    }

    suspend fun searchConversationsAsync(
        context: Context,
        query: String,
        limit: Int,
        offset: Int,
    ): List<DMessageConversation> = withIO {
        if (query.isNotEmpty()) {
            val activeArchivedIds = getActiveArchivedIds(context)
            val matchedThreadIds = getMatchedThreadIdsAsync(context, query)
                .filter { !activeArchivedIds.contains(it) }
            if (matchedThreadIds.isEmpty()) return@withIO emptyList()
            val conversationMap = queryConversationsWithAddresses(context, matchedThreadIds)
            return@withIO conversationMap.values
                .sortedByDescending { it.date }
                .drop(offset)
                .take(limit)
        }

        // Single-pass: read conversations with full data, filter archived, paginate, resolve addresses
        val archivedRecords = AppDatabase.instance.archivedConversationDao().getAll()
        val archivedMap = archivedRecords.associateBy { it.conversationId }

        val conversations = mutableListOf<DMessageConversation>()
        val recipientMap = mutableMapOf<String, List<String>>()
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
                    recipientMap[id] = SmsProviderContract.parseRecipientIds(recipientIds)
                }
            }
        }

        if (conversations.isEmpty()) return@withIO emptyList()

        // Batch resolve addresses
        val addressMap = batchGetCanonicalAddresses(context, recipientMap.values.flatten().toSet())
        val ownNumbers = getSims().map { it.number }.filter(String::isNotEmpty).toSet()
        return@withIO conversations.map { conv ->
            val addresses = SmsProviderContract.selectConversationAddresses(
                recipientMap[conv.id].orEmpty().mapNotNull(addressMap::get),
                ownNumbers,
            )
            if (addresses.isNotEmpty()) {
                conv.copy(address = addresses.first(), addresses = addresses)
            } else {
                conv
            }
        }
    }

    suspend fun conversationCountAsync(context: Context, query: String): Int = withIO {
        val activeArchivedIds = getActiveArchivedIds(context)

        if (query.isNotEmpty()) {
            return@withIO getMatchedThreadIdsAsync(context, query).count { !activeArchivedIds.contains(it) }
        }

        // Count all conversations properly, minus truly archived ones.
        // Note: avoid using "COUNT(*) as count" projection because some vendors
        // (e.g. vivo) rewrite the projection in their MmsSmsProvider and inject
        // extra columns like "snippet_financial_info as snippet", which corrupts
        // aggregate expressions and produces a SQLite syntax error
        // (near "as": syntax error). Use a plain projection and read cursor.count.
        var count = 0
        context.contentResolver.queryCursor(
            conversationsUri,
            arrayOf(BaseColumns._ID),
            null,
            null,
            null
        )?.use { cursor ->
            count = cursor.count
        }

        return@withIO count - activeArchivedIds.size
    }
}
