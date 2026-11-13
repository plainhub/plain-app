package com.ismartcoding.plain.platform.media.provider

import android.provider.BaseColumns

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CallLog
import com.ismartcoding.plain.helpers.ContentWhere
import com.ismartcoding.plain.lib.extensions.getIntValue
import com.ismartcoding.plain.lib.extensions.getStringValue
import com.ismartcoding.plain.lib.extensions.getTimeValue
import com.ismartcoding.plain.lib.extensions.map
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.models.Call as PhoneCall
import com.ismartcoding.plain.features.telephony.CallType

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

    suspend fun searchAsync(
        context: Context,
        where: ContentWhere,
        limit: Int,
        offset: Int,
    ): List<PhoneCall> = withIO {
        if (limit <= 0) {
            return@withIO emptyList()
        }

        context.contentResolver.query(
            uriExternal.withCallLogPaging(limit, offset),
            getProjection(),
            where.toSelection(),
            where.args.toTypedArray(),
            "${CallLog.Calls._ID} DESC",
        )?.map { cursor, cache ->
            val id = cursor.getStringValue(CallLog.Calls._ID, cache)
            val number = cursor.getStringValue(CallLog.Calls.NUMBER, cache)
            val name = cursor.getStringValue(CallLog.Calls.CACHED_NAME, cache)
            val photoUri = cursor.getStringValue(CallLog.Calls.CACHED_PHOTO_URI, cache)
            val startTS = cursor.getTimeValue(CallLog.Calls.DATE, cache)
            val durationSec = cursor.getIntValue(CallLog.Calls.DURATION, cache)
            val type = cursor.getIntValue(CallLog.Calls.TYPE, cache)
            val accountId = cursor.getStringValue(CallLog.Calls.PHONE_ACCOUNT_ID, cache)
            PhoneCall(id = id, number = number, name = name, photo = photoUri, startedAt = startTS, durationSec = durationSec, type = CallType.fromInt(type), typeCode = type, accountId = accountId)
        } ?: emptyList()
    }

    private fun Uri.withCallLogPaging(
        limit: Int,
        offset: Int,
    ): Uri {
        return buildUpon()
            .appendQueryParameter(CallLog.Calls.LIMIT_PARAM_KEY, limit.toString())
            .apply {
                if (offset > 0) {
                    appendQueryParameter(CallLog.Calls.OFFSET_PARAM_KEY, offset.toString())
                }
            }
            .build()
    }

    fun call(
        context: Context,
        number: String,
        showDialer: Boolean,
    ) {
        val action = if (showDialer) Intent.ACTION_DIAL else Intent.ACTION_CALL
        val intent = Intent(action).apply {
            data = Uri.parse("tel:$number")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
