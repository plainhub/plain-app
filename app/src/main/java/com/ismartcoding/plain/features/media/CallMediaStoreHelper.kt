package com.ismartcoding.plain.features.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.BaseColumns
import android.provider.CallLog
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.data.SortBy
import com.ismartcoding.lib.data.enums.SortDirection
import com.ismartcoding.lib.extensions.getIntValue
import com.ismartcoding.lib.extensions.getPagingCursor
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.extensions.getTimeValue
import com.ismartcoding.lib.extensions.map
import com.ismartcoding.plain.data.DCall
import com.ismartcoding.plain.extensions.normalizeComparison
import com.ismartcoding.plain.extensions.parseEpochMillis
import com.ismartcoding.plain.helpers.QueryHelper

object CallMediaStoreHelper : BaseContentHelper() {
    override val uriExternal: Uri = CallLog.Calls.CONTENT_URI

    override fun getProjection(): Array<String> {
        return arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.CACHED_PHOTO_URI,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE,
            CallLog.Calls.PHONE_ACCOUNT_ID,
        )
    }

    override suspend fun buildWhereAsync(query: String): ContentWhere {
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            QueryHelper.parseAsync(query).forEach {
                when (it.name) {
                    "text" -> {
                        where.add("${CallLog.Calls.NUMBER} LIKE ?", "%${it.value}%")
                    }

                    "ids" -> {
                        where.addIn(BaseColumns._ID, it.value.split(","))
                    }

                    "type" -> {
                        where.add("${CallLog.Calls.TYPE} = ?", it.value)
                    }

                    "duration" -> {
                        val (op, rawValue) = it.normalizeComparison(defaultOp = "=")
                        val seconds = rawValue.trim().toLongOrNull() ?: return@forEach
                        where.add("${CallLog.Calls.DURATION} $op ?", seconds.toString())
                    }

                    "start_time" -> {
                        val (op, rawValue) = it.normalizeComparison(defaultOp = "=")
                        val ts = rawValue.parseEpochMillis() ?: return@forEach
                        where.add("${CallLog.Calls.DATE} $op ?", ts.toString())
                    }
                }
            }
        }

        return where
    }

    suspend fun searchAsync(
        context: Context,
        query: String,
        limit: Int,
        offset: Int,
    ): List<DCall> {
        return context.contentResolver.getPagingCursor(
            uriExternal, getProjection(), buildWhereAsync(query),
            limit, offset, SortBy(CallLog.Calls._ID, SortDirection.DESC)
        )?.map { cursor, cache ->
            val id = cursor.getStringValue(CallLog.Calls._ID, cache)
            val number = cursor.getStringValue(CallLog.Calls.NUMBER, cache)
            val name = cursor.getStringValue(CallLog.Calls.CACHED_NAME, cache)
            val photoUri = cursor.getStringValue(CallLog.Calls.CACHED_PHOTO_URI, cache)
            val startTS = cursor.getTimeValue(CallLog.Calls.DATE, cache)
            val duration = cursor.getIntValue(CallLog.Calls.DURATION, cache)
            val type = cursor.getIntValue(CallLog.Calls.TYPE, cache)
            val accountId = cursor.getStringValue(CallLog.Calls.PHONE_ACCOUNT_ID, cache)
            DCall(id, number, name, photoUri, startTS, duration, type, accountId)
        } ?: emptyList()
    }

    fun call(
        context: Context,
        number: String,
    ) {
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:$number")
        context.startActivity(intent)
    }
}
