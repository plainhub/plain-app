package com.ismartcoding.plain.platform

import android.provider.Telephony
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.audio.AudioMediaStoreHelper
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.db.DTrashedMessage
import com.ismartcoding.plain.db.IData
import com.ismartcoding.plain.data.TagRelationStub
import com.ismartcoding.plain.docs.DocMediaStoreHelper
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.features.media.CallMediaStoreHelper
import com.ismartcoding.plain.features.media.ContactMediaStoreHelper
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
import com.ismartcoding.plain.features.sms.SmsHelper
import com.ismartcoding.plain.features.sms.DMessage
import com.ismartcoding.plain.features.sms.SmsProviderContract
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.api.WebSocketHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import java.io.File
import java.util.concurrent.ConcurrentHashMap

actual suspend fun getMediaBuckets(dataType: DataType): List<DMediaBucket> {
    return com.ismartcoding.plain.features.system.RustSystemProviders.mediaBuckets(dataType)
}

actual suspend fun searchMedia(
    dataType: DataType,
    query: String,
    limit: Int,
    offset: Int,
    sortBy: FileSortBy,
): List<IData> {
    if (dataType == DataType.CALL && !Permission.READ_CALL_LOG.isGranted()) return emptyList()
    if (dataType == DataType.CONTACT && !Permission.READ_CONTACTS.isGranted()) return emptyList()
    return when (dataType) {
        DataType.AUDIO -> AudioMediaStoreHelper.searchAsync(appContext, query, limit, offset, sortBy)
        DataType.DOC -> DocMediaStoreHelper.searchAsync(appContext, query, limit, offset, sortBy)
        DataType.IMAGE -> ImageMediaStoreHelper.searchAsync(appContext, query, limit, offset, sortBy)
        DataType.VIDEO -> VideoMediaStoreHelper.searchAsync(appContext, query, limit, offset, sortBy)
        DataType.CALL -> CallMediaStoreHelper.searchAsync(appContext, query, limit, offset)
        DataType.CONTACT -> ContactMediaStoreHelper.searchAsync(appContext, query, limit, offset)
        DataType.SMS -> SmsHelper.searchAsync(appContext, query, limit, offset)
        else -> emptyList()
    }
}

actual suspend fun countMedia(dataType: DataType, query: String): Int {
    if (dataType == DataType.CALL && !Permission.READ_CALL_LOG.isGranted()) return 0
    if (dataType == DataType.CONTACT && !Permission.READ_CONTACTS.isGranted()) return 0
    return when (dataType) {
        DataType.AUDIO -> AudioMediaStoreHelper.countAsync(appContext, query)
        DataType.DOC -> DocMediaStoreHelper.countAsync(appContext, query)
        DataType.IMAGE -> ImageMediaStoreHelper.countAsync(appContext, query)
        DataType.VIDEO -> VideoMediaStoreHelper.countAsync(appContext, query)
        DataType.CALL -> CallMediaStoreHelper.countAsync(appContext, query)
        DataType.CONTACT -> ContactMediaStoreHelper.countAsync(appContext, query)
        DataType.SMS -> SmsHelper.countAsync(appContext, query)
        else -> 0
    }
}

actual suspend fun trashMedia(dataType: DataType, ids: Set<String>) {
    com.ismartcoding.plain.features.mediaactions.MediaActionHelper.run(dataType,com.ismartcoding.plain.features.mediaactions.MediaAction.TRASH,ids)
}

actual suspend fun restoreMedia(dataType: DataType, ids: Set<String>) {
    com.ismartcoding.plain.features.mediaactions.MediaActionHelper.run(dataType,com.ismartcoding.plain.features.mediaactions.MediaAction.RESTORE,ids)
}

actual suspend fun deleteMedia(dataType: DataType, ids: Set<String>, fromTrash: Boolean) {
    com.ismartcoding.plain.features.mediaactions.MediaActionHelper.run(dataType,com.ismartcoding.plain.features.mediaactions.MediaAction.DELETE,ids,fromTrash)
}

actual suspend fun moveMedia(dataType: DataType, ids: Set<String>, destDir: String): Boolean {
    return com.ismartcoding.plain.features.mediaactions.MediaActionHelper.run(dataType,com.ismartcoding.plain.features.mediaactions.MediaAction.MOVE,ids,destDir=destDir) == ids.size
}

private suspend fun getTrashedMessageIds(): Set<String> = com.ismartcoding.plain.features.sms.RustSmsState.trashed()

actual suspend fun trashSms(query: String): Int =
    com.ismartcoding.plain.features.sms.RustSmsState.trash(SmsHelper.getIdsAsync(appContext, query))

actual suspend fun restoreSms(query: String): Int =
    com.ismartcoding.plain.features.sms.RustSmsState.restore(SmsHelper.getIdsAsync(appContext, query))

private const val SMS_TRASH_DIR = "sms-trash"
private const val SMS_TRASH_RETENTION_DAYS = 30L

/**
 * Archive full message content to app-private storage before a real delete.
 * Files are the last-resort recovery net (JSON for tools, CSV for spreadsheets)
 * and are cleaned up after 30 days.
 */
private fun archiveDeletedMessages(messages: List<DMessage>) {
    if (messages.isEmpty()) return
    val dir = File(appContext.filesDir, SMS_TRASH_DIR).apply { mkdirs() }
    val stamp = System.currentTimeMillis()
    runCatching {
        File(dir, "deleted_$stamp.json").writeText(JsonHelper.jsonEncode(messages, pretty = true))
    }
    runCatching {
        val header = "id,body,address,date,thread_id,is_mms"
        val rows = messages.joinToString("\n") { m ->
            listOf(
                m.id,
                m.body.replace("\"", "\"\"").replace("\n", "\\n"),
                m.address,
                m.date.toEpochMilliseconds().toString(),
                m.threadId,
                m.isMms.toString(),
            ).joinToString(",") { "\"$it\"" }
        }
        File(dir, "deleted_$stamp.csv").writeText(header + "\n" + rows)
    }
    // Retention: drop archives older than 30 days.
    val cutoff = stamp - SMS_TRASH_RETENTION_DAYS * 24 * 60 * 60 * 1000
    dir.listFiles()?.forEach { if (it.lastModified() < cutoff) it.delete() }
}

private fun shizukuDeleteUri(uri: String) {
    // `content delete` runs as shell uid, which carries the WRITE_SMS appop.
    ShizukuHelper.exec("content delete --uri $uri")
}

actual suspend fun deleteSms(query: String): Int {
    if (!ShizukuHelper.isGranted()) {
        throw IllegalStateException("Shizuku is not available or not granted")
    }
    // includeTrashed: permanently deleting from the trash bin must reach
    // app-side trashed messages, which normal queries exclude.
    val ids = SmsHelper.getIdsAsync(appContext, query, includeTrashed = true)
    if (ids.isEmpty()) return 0
    val messages = SmsHelper.searchAsync(
        appContext,
        "ids:${ids.joinToString(",")}",
        limit = ids.size,
        offset = 0,
        includeTrashed = true,
    )
    // Recovery net before touching the provider: full content archive +
    // shadow-table record (feeds the 30-day restore window).
    archiveDeletedMessages(messages)
    val known = getTrashedMessageIds()
    com.ismartcoding.plain.features.sms.RustSmsState.trash(ids)

    var deleted = 0
    ids.forEach { id ->
        val uri = if (id.startsWith("mms_")) "content://mms/${id.removePrefix("mms_")}" else "content://sms/$id"
        runCatching { shizukuDeleteUri(uri) }
            .onSuccess { deleted++ }
            .onFailure {
                // Provider refused (e.g. id vanished meanwhile); drop the
                // shadow record so it doesn't linger as a phantom entry.
                if (id !in known) com.ismartcoding.plain.features.sms.RustSmsState.restore(listOf(id))
            }
    }
    return deleted
}

actual suspend fun getDocExtGroups(query: String): List<Pair<String, Int>> =
    DocMediaStoreHelper.getDocExtGroupsAsync(appContext, query)

actual suspend fun searchImagesCombined(
    queryText: String,
    extraQuery: String,
    limit: Int,
    offset: Int,
    sortBy: FileSortBy,
): List<DImage> = com.ismartcoding.plain.ai.RustImageSearch.rows(queryText,extraQuery,limit,offset,sortBy)

actual fun isImageSearchModelReady(): Boolean =
    com.ismartcoding.plain.ai.ImageSearchManager.isModelReady()

actual fun enqueueRemoveImageIndex(ids: Set<String>) =
    com.ismartcoding.plain.ai.ImageIndexManager.enqueueRemove(ids)

actual fun getMediaItemUriString(dataType: DataType, id: String): String = when (dataType) {
    DataType.IMAGE -> ImageMediaStoreHelper.getItemUri(id).toString()
    DataType.VIDEO -> VideoMediaStoreHelper.getItemUri(id).toString()
    DataType.AUDIO -> AudioMediaStoreHelper.getItemUri(id).toString()
    DataType.DOC -> DocMediaStoreHelper.getItemUri(id).toString()
    else -> ""
}

actual suspend fun getMediaIds(dataType: DataType, query: String): Set<String> {
    if (dataType == DataType.CALL && !Permission.READ_CALL_LOG.isGranted()) return emptySet()
    if (dataType == DataType.CONTACT && !Permission.READ_CONTACTS.isGranted()) return emptySet()
    return when (dataType) {
        DataType.AUDIO -> AudioMediaStoreHelper.getIdsAsync(appContext, query)
        DataType.VIDEO -> VideoMediaStoreHelper.getIdsAsync(appContext, query)
        DataType.IMAGE -> ImageMediaStoreHelper.getIdsAsync(appContext, query)
        DataType.DOC -> DocMediaStoreHelper.getIdsAsync(appContext, query)
        DataType.CALL -> CallMediaStoreHelper.getIdsAsync(appContext, query)
        DataType.CONTACT -> ContactMediaStoreHelper.getIdsAsync(appContext, query)
        DataType.SMS -> SmsHelper.getIdsAsync(appContext, query)
        else -> emptySet()
    }
}

actual suspend fun getTrashedMediaIds(dataType: DataType, query: String): Set<String> = when (dataType) {
    DataType.AUDIO -> AudioMediaStoreHelper.getTrashedIdsAsync(appContext, query)
    DataType.VIDEO -> VideoMediaStoreHelper.getTrashedIdsAsync(appContext, query)
    DataType.IMAGE -> ImageMediaStoreHelper.getTrashedIdsAsync(appContext, query)
    DataType.DOC -> DocMediaStoreHelper.getTrashedIdsAsync(appContext, query)
    else -> emptySet()
}

actual suspend fun getMediaPathsByIds(dataType: DataType, ids: Set<String>): Set<String> = when (dataType) {
    DataType.AUDIO -> AudioMediaStoreHelper.getPathsByIdsAsync(appContext, ids)
    DataType.VIDEO -> VideoMediaStoreHelper.getPathsByIdsAsync(appContext, ids)
    DataType.IMAGE -> ImageMediaStoreHelper.getPathsByIdsAsync(appContext, ids)
    DataType.DOC -> DocMediaStoreHelper.getPathsByIdsAsync(appContext, ids)
    else -> emptySet()
}

actual suspend fun getMediaTagRelationStubs(dataType: DataType, query: String): List<TagRelationStub> = when (dataType) {
    DataType.AUDIO -> AudioMediaStoreHelper.getTagRelationStubsAsync(appContext, query)
    DataType.VIDEO -> VideoMediaStoreHelper.getTagRelationStubsAsync(appContext, query)
    DataType.IMAGE -> ImageMediaStoreHelper.getTagRelationStubsAsync(appContext, query)
    DataType.DOC -> DocMediaStoreHelper.getTagRelationStubsAsync(appContext, query)
    DataType.CALL -> CallMediaStoreHelper.getIdsAsync(appContext, query).map { TagRelationStub(it) }
    DataType.CONTACT -> ContactMediaStoreHelper.getIdsAsync(appContext, query).map { TagRelationStub(it) }
    DataType.SMS -> SmsHelper.getIdsAsync(appContext, query).map { TagRelationStub(it) }
    else -> emptyList()
}

actual suspend fun countImagesCombined(queryText: String, extraQuery: String): Int =
    com.ismartcoding.plain.ai.RustImageSearch.count(queryText,extraQuery)

actual fun startImageIndexFullScan(force: Boolean) =
    com.ismartcoding.plain.ai.ImageIndexManager.fullScan(force)

actual fun cancelImageIndex() =
    com.ismartcoding.plain.ai.ImageSearchIndexer.cancel()

actual suspend fun searchSmsConversations(query: String, limit: Int, offset: Int): List<com.ismartcoding.plain.features.sms.DMessageConversation> =
    com.ismartcoding.plain.features.sms.RustSmsQuery.conversations(query,limit,offset)

actual suspend fun countSmsConversations(query: String): Int =
    com.ismartcoding.plain.features.sms.RustSmsQuery.conversationCount(query)

actual suspend fun getArchivedSmsConversations(): List<com.ismartcoding.plain.features.sms.DMessageConversation> =
    com.ismartcoding.plain.features.sms.RustSmsQuery.archivedConversations()

actual suspend fun getSmsConversationDate(threadId: String): Long? =
    com.ismartcoding.plain.features.sms.RustSmsQuery.conversationDate(threadId)?.toEpochMilliseconds()

actual suspend fun getSmsAllCounts(): DSmsCounts =
    com.ismartcoding.plain.features.sms.SmsHelper.countAllAsync(appContext).let {
        DSmsCounts(it.total, it.inbox, it.sent, it.drafts)
    }

actual fun sendSmsText(
    number: String,
    body: String,
    subscriptionId: Int?,
    clientId: String?,
    clientRequestId: String?,
) = com.ismartcoding.plain.features.sms.SmsHelper.sendText(
    number,
    body,
    subscriptionId,
    clientId,
    clientRequestId,
)

actual fun call(number: String, showDialer: Boolean) =
    com.ismartcoding.plain.features.media.CallMediaStoreHelper.call(appContext, number, showDialer)

actual fun resolveAppFileUri(uri: String): String =
    com.ismartcoding.plain.helpers.AppFileStore.resolveUri(uri)

actual fun mimeTypeFromExtension(extension: String): String =
    android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"

actual fun launchDefaultSmsApp(
    number: String,
    body: String,
    attachments: List<Pair<String, String>>,
): Long {
    com.ismartcoding.plain.features.sms.MmsHelper.launchDefaultSmsApp(number, body, attachments)
    return System.currentTimeMillis() / 1000 - 1
}

actual fun getLatestSentMmsId(): Long {
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

actual fun getScreenSize(): Pair<Int, Int> {
    val displayMetrics = appContext.resources.displayMetrics
    return Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
}

actual fun getDownloadsDirPath(): String =
    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS).absolutePath

actual suspend fun getContactById(id: String): com.ismartcoding.plain.data.DContact? =
    com.ismartcoding.plain.features.media.ContactMediaStoreHelper.getByIdAsync(appContext, id)

actual fun updateContact(id: String, input: com.ismartcoding.plain.features.contact.ContactInput) =
    com.ismartcoding.plain.features.media.ContactMediaStoreHelper.updateAsync(id, input)

actual fun createContact(input: com.ismartcoding.plain.features.contact.ContactInput): String =
    com.ismartcoding.plain.features.media.ContactMediaStoreHelper.createAsync(input)

actual suspend fun deleteContacts(ids: Set<String>) {
    com.ismartcoding.plain.features.media.ContactMediaStoreHelper.deleteByIdsAsync(appContext, ids)
}

actual fun readSentMmsCandidates(minimumId: Long, launchTimeSec: Long): List<com.ismartcoding.plain.features.sms.MmsCandidateFacts> {
    val context = appContext
    return context.contentResolver.query(
        Telephony.Mms.CONTENT_URI,
        arrayOf(Telephony.Mms._ID, Telephony.Mms.THREAD_ID),
        "${Telephony.Mms.MESSAGE_BOX} = 2 AND m_type = ${SmsProviderContract.MMS_PDU_SEND_REQ} AND ${Telephony.Mms._ID} > ? AND ${Telephony.Mms.DATE} >= ?",
        arrayOf(minimumId.toString(), launchTimeSec.toString()),
        "${Telephony.Mms._ID} ASC",
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(Telephony.Mms._ID)
        val threadIndex = cursor.getColumnIndexOrThrow(Telephony.Mms.THREAD_ID)
        buildList {
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val (body, attachments) = SmsHelper.readMmsBodyAndAttachments(context, id.toString())
                add(com.ismartcoding.plain.features.sms.MmsCandidateFacts(id,
                    SmsHelper.readMmsAddress(context, id.toString()), body,
                    cursor.getString(threadIndex).orEmpty(), attachments.map { it.contentType }))
            }
        }
    } ?: emptyList()
}

actual suspend fun enableImageSearchAsync() {
    com.ismartcoding.plain.ai.ImageSearchManager.enableAsync()
}

actual suspend fun disableImageSearchAsync() {
    com.ismartcoding.plain.ai.ImageSearchManager.disableAsync()
}

actual fun cancelImageModelDownload() {
    com.ismartcoding.plain.ai.ImageSearchManager.cancelDownload()
}
