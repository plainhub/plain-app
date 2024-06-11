package com.ismartcoding.plain.features.media

import android.content.Context
import android.net.Uri
import android.provider.BaseColumns
import android.provider.Telephony
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.data.SortBy
import com.ismartcoding.lib.data.enums.SortDirection
import com.ismartcoding.lib.extensions.getIntValue
import com.ismartcoding.lib.extensions.getPagingCursor
import com.ismartcoding.lib.extensions.getPagingCursorWithSql
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.extensions.getTimeValue
import com.ismartcoding.lib.extensions.map
import com.ismartcoding.plain.features.sms.DMessage
import com.ismartcoding.plain.helpers.QueryHelper

object SmsMediaStoreHelper : BaseContentHelper() {
    override val uriExternal: Uri = Telephony.Sms.CONTENT_URI

    override fun getProjection(): Array<String> {
        return arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.TYPE,
            Telephony.Sms.BODY,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.READ,
            Telephony.Sms.DATE,
            Telephony.Sms.SERVICE_CENTER,
        )
    }

    override suspend fun buildWhereAsync(query: String): ContentWhere {
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
    ): List<DMessage> {
        return context.contentResolver.getPagingCursorWithSql(
            uriExternal, getProjection(), buildWhereAsync(query),
            limit, offset, SortBy(Telephony.Sms.DATE, SortDirection.DESC)
        )?.map { cursor, cache ->
            DMessage(
                cursor.getStringValue(Telephony.Sms._ID, cache),
                cursor.getStringValue(Telephony.Sms.BODY, cache),
                cursor.getStringValue(Telephony.Sms.ADDRESS, cache),
                cursor.getTimeValue(Telephony.Sms.DATE, cache),
                cursor.getStringValue(Telephony.Sms.SERVICE_CENTER, cache),
                cursor.getIntValue(Telephony.Sms.READ, cache) == 1,
                cursor.getStringValue(Telephony.Sms.THREAD_ID, cache),
                cursor.getIntValue(Telephony.Sms.TYPE, cache),
            )
        } ?: emptyList()
    }
}
