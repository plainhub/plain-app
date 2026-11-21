package com.ismartcoding.plain.platform.system

import com.ismartcoding.plain.platform.isQPlus

import com.ismartcoding.plain.models.File as FileEntry

import android.provider.CallLog
import android.provider.ContactsContract
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.features.audio.AudioMediaStoreHelper
import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.features.telephony.sms.SmsHelper
import com.ismartcoding.plain.platform.media.provider.DocMediaStoreHelper
import com.ismartcoding.plain.platform.media.provider.FileMediaStoreHelper
import com.ismartcoding.plain.platform.media.provider.ImageMediaStoreHelper
import com.ismartcoding.plain.platform.media.provider.VideoMediaStoreHelper
import com.ismartcoding.plain.rust.host.provider.MediaBucketItemFacts
import com.ismartcoding.plain.rust.host.provider.ProviderPlan
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

suspend fun deleteSystemProviderFacts(provider: DataType, ids: Set<String>): Set<String> = withIO {
    val uri = when (provider) {
        DataType.CALL -> CallLog.Calls.CONTENT_URI
        DataType.CONTACT -> ContactsContract.RawContacts.CONTENT_URI
        else -> error("Unsupported system provider")
    }
    ids.filter { id ->
        val number = id.toLongOrNull() ?: return@filter false
        val arguments = arrayOf(number.toString())
        val selection = if (provider == DataType.CONTACT) "_id = ? AND deleted = 0" else "_id = ?"
        appContext.contentResolver.delete(uri, "_id = ?", arguments) > 0 &&
            appContext.contentResolver.query(uri, arrayOf("_id"), selection, arguments, null)?.use { !it.moveToFirst() } == true
    }.toSet()
}

suspend fun systemSmsFacts(method: String, params: JsonObject): JsonElement =
    SmsHelper.facts(appContext, method, params)

suspend fun mediaBucketItemFacts(dataType: DataType, plan: ProviderPlan): List<MediaBucketItemFacts> = when (dataType) {
    DataType.IMAGE -> ImageMediaStoreHelper.getBucketItemFactsAsync(appContext, plan.where())
    DataType.VIDEO -> VideoMediaStoreHelper.getBucketItemFactsAsync(appContext, plan.where())
    DataType.AUDIO -> if (isQPlus()) AudioMediaStoreHelper.getBucketItemFactsAsync(appContext, plan.where()) else emptyList()
    DataType.DOC -> if (isQPlus()) DocMediaStoreHelper.getBucketItemFactsAsync(appContext, plan.where()) else emptyList()
    else -> emptyList()
}

suspend fun fileIndexId(path: String): String? = withIO { FileMediaStoreHelper.getIdByPathAsync(appContext, path) }
suspend fun fileIndexRows(
    plan: ProviderPlan,
    limit: Int,
    offset: Int,
    sortBy: FileSortBy,
    includeTrashed: Boolean = false,
): List<FileEntry> = FileMediaStoreHelper.searchAsync(appContext, plan.where(), limit, offset, sortBy, includeTrashed)
suspend fun fileIndexCount(plan: ProviderPlan): Int = withIO {
    plan.wheres().sumOf { FileMediaStoreHelper.countAsync(appContext, it) }
}
