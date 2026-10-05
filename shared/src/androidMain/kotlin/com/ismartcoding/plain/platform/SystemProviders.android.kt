package com.ismartcoding.plain.platform

import android.content.ContentUris
import android.provider.CallLog
import android.provider.ContactsContract
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.lib.withIO

actual suspend fun deleteSystemProviderFacts(provider: DataType, ids: Set<String>): Set<String> = withIO {
    val uri = when (provider) {
        DataType.CALL -> CallLog.Calls.CONTENT_URI
        DataType.CONTACT -> ContactsContract.RawContacts.CONTENT_URI
        else -> error("Unsupported system provider")
    }
    ids.filter { id ->
        val number = id.toLongOrNull() ?: return@filter false
        val row = ContentUris.withAppendedId(uri, number)
        runCatching {
            appContext.contentResolver.delete(row, null, null) > 0 &&
                appContext.contentResolver.query(row, arrayOf("_id"), null, null, null)?.use { !it.moveToFirst() } == true
        }.getOrDefault(false)
    }.toSet()
}

actual suspend fun systemSmsFacts(method: String, params: kotlinx.serialization.json.JsonObject): kotlinx.serialization.json.JsonElement =
    com.ismartcoding.plain.features.sms.SmsHelper.facts(appContext, method, params)
