package com.ismartcoding.plain.platform.media

import com.ismartcoding.plain.platform.permissions.Permission
import com.ismartcoding.plain.platform.permissions.isGranted
import com.ismartcoding.plain.platform.system.ShizukuHelper

import android.provider.Telephony
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.features.audio.AudioMediaStoreHelper
import com.ismartcoding.plain.IData
import com.ismartcoding.plain.platform.media.provider.DocMediaStoreHelper
import com.ismartcoding.plain.rust.host.provider.ProviderPlan
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.platform.media.provider.CallMediaStoreHelper
import com.ismartcoding.plain.platform.media.provider.ContactMediaStoreHelper
import com.ismartcoding.plain.platform.media.provider.ImageMediaStoreHelper
import com.ismartcoding.plain.platform.media.provider.VideoMediaStoreHelper
import com.ismartcoding.plain.features.telephony.sms.SmsHelper
import com.ismartcoding.plain.features.telephony.sms.SmsProviderContract
import com.ismartcoding.plain.features.files.FileSortBy
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.CancellationException
import android.content.ContentUris
import android.os.Environment
import com.ismartcoding.plain.features.telephony.sms.MmsCandidateFacts
import com.ismartcoding.plain.features.telephony.sms.MmsHelper
import com.ismartcoding.plain.features.telephony.sms.SmsDeletionReceipt
import com.ismartcoding.plain.features.telephony.sms.SmsDispatchRequest
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.withIO
import kotlinx.coroutines.currentCoroutineContext

suspend fun deleteSmsRows(ids: List<String>): SmsDeletionReceipt = withIO {
    check(ShizukuHelper.isGranted()) { "Shizuku is not available or not granted" }
    val deleted = mutableListOf<String>()
    val failed = mutableListOf<String>()
    for (id in ids) {
        currentCoroutineContext().ensureActive()
        val raw = id.removePrefix("mms_")
        val number = raw.toLongOrNull()
        if (number == null || number < 0) {
            failed.add(id)
            continue
        }
        val provider = if (id.startsWith("mms_")) Telephony.Mms.CONTENT_URI else Telephony.Sms.CONTENT_URI
        val uri = ContentUris.withAppendedId(provider, number)
        val removed = try {
            ShizukuHelper.exec("content delete --uri $uri")
            appContext.contentResolver.query(uri, arrayOf("_id"), null, null, null)?.use { !it.moveToFirst() } == true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            LogCat.e("SMS provider deletion failed", error)
            false
        }
        if (removed) deleted.add(id) else failed.add(id)
    }
    SmsDeletionReceipt(deleted, failed)
}

actual fun getMediaItemUriString(dataType: DataType, id: String, path: String): String = getSystemMediaItemUriString(dataType, id)

internal fun getSystemMediaItemUriString(dataType: DataType, id: String): String = when (dataType) {
    DataType.IMAGE -> ImageMediaStoreHelper.getItemUri(id).toString()
    DataType.VIDEO -> VideoMediaStoreHelper.getItemUri(id).toString()
    DataType.AUDIO -> AudioMediaStoreHelper.getItemUri(id).toString()
    DataType.DOC -> DocMediaStoreHelper.getItemUri(id).toString()
    else -> ""
}


fun divideSmsText(body: String, subscriptionId: Int?): List<String> = SmsHelper.divideText(body, subscriptionId)
fun sendSmsParts(request: SmsDispatchRequest) = SmsHelper.sendParts(request)

fun call(number: String, showDialer: Boolean) =
    CallMediaStoreHelper.call(appContext, number, showDialer)


fun launchDefaultSmsApp(
    number: String,
    body: String,
    attachments: List<Pair<String, String>>,
): Long {
    MmsHelper.launchDefaultSmsApp(number, body, attachments)
    return System.currentTimeMillis() / 1000 - 1
}

fun getLatestSentMmsId(): Long {
    return runCatching {
        appContext.contentResolver.query(
            Telephony.Mms.CONTENT_URI,
            arrayOf(Telephony.Mms._ID),
            "${Telephony.Mms.MESSAGE_BOX} = 2 AND m_type = ${SmsProviderContract.MMS_PDU_SEND_REQ}",
            null,
            "${Telephony.Mms._ID} DESC LIMIT 1",
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        } ?: 0L
    }.getOrDefault(0L)
}

internal actual fun platformDownloadsDirPath(): String =
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath

fun readSentMmsCandidates(minimumId: Long, launchTimeSec: Long): List<MmsCandidateFacts> {
    val context = appContext
    return context.contentResolver.query(
        Telephony.Mms.CONTENT_URI,
        arrayOf(Telephony.Mms._ID),
        "${Telephony.Mms.MESSAGE_BOX} = 2 AND m_type = ${SmsProviderContract.MMS_PDU_SEND_REQ} AND ${Telephony.Mms._ID} > ? AND ${Telephony.Mms.DATE} >= ?",
        arrayOf(minimumId.toString(), launchTimeSec.toString()),
        "${Telephony.Mms._ID} ASC",
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(Telephony.Mms._ID)
        buildList {
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                add(MmsCandidateFacts(id,
                    SmsHelper.readMmsAddresses(context, id.toString()), SmsHelper.readMmsParts(context, id.toString())))
            }
        }
    } ?: emptyList()
}

suspend fun searchMedia(dataType: DataType, plan: ProviderPlan, limit: Int, offset: Int, sortBy: FileSortBy): List<IData> {
    if (dataType == DataType.CALL && !Permission.READ_CALL_LOG.isGranted()) return emptyList()
    val where = plan.where()
    return when (dataType) {
        DataType.AUDIO -> AudioMediaStoreHelper.searchAsync(appContext, where, limit, offset, sortBy)
        DataType.DOC -> DocMediaStoreHelper.searchAsync(appContext, where, limit, offset, sortBy)
        DataType.IMAGE -> ImageMediaStoreHelper.searchAsync(appContext, where, limit, offset, sortBy)
        DataType.VIDEO -> VideoMediaStoreHelper.searchAsync(appContext, where, limit, offset, sortBy)
        DataType.CALL -> CallMediaStoreHelper.searchAsync(appContext, where, limit, offset)
        else -> error("Unsupported provider: $dataType")
    }
}

suspend fun countMedia(dataType: DataType, plan: ProviderPlan): Int {
    if (dataType == DataType.CALL && !Permission.READ_CALL_LOG.isGranted()) return 0
    if (dataType == DataType.CONTACT && !Permission.READ_CONTACTS.isGranted()) return 0
    return plan.wheres().sumOf { where ->
        when (dataType) {
            DataType.AUDIO -> AudioMediaStoreHelper.countAsync(appContext, where)
            DataType.DOC -> DocMediaStoreHelper.countAsync(appContext, where)
            DataType.IMAGE -> ImageMediaStoreHelper.countAsync(appContext, where)
            DataType.VIDEO -> VideoMediaStoreHelper.countAsync(appContext, where)
            DataType.CALL -> CallMediaStoreHelper.countAsync(appContext, where)
            DataType.CONTACT -> ContactMediaStoreHelper.countAsync(appContext, where)
            else -> error("Unsupported provider: $dataType")
        }
    }
}

suspend fun getMediaIds(dataType: DataType, plan: ProviderPlan): Set<String> {
    if (dataType == DataType.CALL && !Permission.READ_CALL_LOG.isGranted()) return emptySet()
    if (dataType == DataType.CONTACT && !Permission.READ_CONTACTS.isGranted()) return emptySet()
    return when (dataType) {
        DataType.AUDIO -> AudioMediaStoreHelper.getIdsAsync(appContext, plan.where())
        DataType.DOC -> DocMediaStoreHelper.getIdsAsync(appContext, plan.where())
        DataType.IMAGE -> ImageMediaStoreHelper.getIdsAsync(appContext, plan.where())
        DataType.VIDEO -> VideoMediaStoreHelper.getIdsAsync(appContext, plan.where())
        DataType.CALL -> CallMediaStoreHelper.getIdsAsync(appContext, plan.where())
        DataType.CONTACT -> ContactMediaStoreHelper.getIdsAsync(appContext, plan.where())
        else -> error("Unsupported provider: $dataType")
    }
}
