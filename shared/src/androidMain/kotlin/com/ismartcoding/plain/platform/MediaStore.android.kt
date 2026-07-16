package com.ismartcoding.plain.platform

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.audio.AudioMediaStoreHelper
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.data.TagRelationStub
import com.ismartcoding.plain.docs.DocMediaStoreHelper
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.call.PhoneGeoCache
import com.ismartcoding.plain.features.media.CallMediaStoreHelper
import com.ismartcoding.plain.features.media.ContactMediaStoreHelper
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
import com.ismartcoding.plain.features.sms.SmsHelper
import com.ismartcoding.plain.features.file.FileSortBy

actual suspend fun getMediaBuckets(dataType: DataType): List<DMediaBucket> {
    return when (dataType) {
        DataType.IMAGE -> ImageMediaStoreHelper.getBucketsAsync(appContext)
        DataType.VIDEO -> VideoMediaStoreHelper.getBucketsAsync(appContext)
        DataType.AUDIO -> if (isQPlus()) AudioMediaStoreHelper.getBucketsAsync(appContext) else emptyList()
        DataType.DOC -> DocMediaStoreHelper.getDocBucketsAsync(appContext)
        else -> emptyList()
    }
}

actual suspend fun searchMedia(
    dataType: DataType,
    query: String,
    limit: Int,
    offset: Int,
    sortBy: FileSortBy,
): List<IData> {
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
    when (dataType) {
        DataType.AUDIO -> AudioMediaStoreHelper.trashByIdsAsync(appContext, ids)
        DataType.DOC -> DocMediaStoreHelper.trashByIdsAsync(appContext, ids)
        DataType.IMAGE -> ImageMediaStoreHelper.trashByIdsAsync(appContext, ids)
        DataType.VIDEO -> VideoMediaStoreHelper.trashByIdsAsync(appContext, ids)
        else -> {}
    }
}

actual suspend fun restoreMedia(dataType: DataType, ids: Set<String>) {
    when (dataType) {
        DataType.AUDIO -> AudioMediaStoreHelper.restoreByIdsAsync(appContext, ids)
        DataType.DOC -> DocMediaStoreHelper.restoreByIdsAsync(appContext, ids)
        DataType.IMAGE -> ImageMediaStoreHelper.restoreByIdsAsync(appContext, ids)
        DataType.VIDEO -> VideoMediaStoreHelper.restoreByIdsAsync(appContext, ids)
        else -> {}
    }
}

actual suspend fun deleteMedia(dataType: DataType, ids: Set<String>, fromTrash: Boolean) {
    when (dataType) {
        DataType.AUDIO -> AudioMediaStoreHelper.deleteRecordsAndFilesByIdsAsync(appContext, ids, fromTrash)
        DataType.DOC -> DocMediaStoreHelper.deleteRecordsAndFilesByIdsAsync(appContext, ids, fromTrash)
        DataType.IMAGE -> ImageMediaStoreHelper.deleteRecordsAndFilesByIdsAsync(appContext, ids, fromTrash)
        DataType.VIDEO -> VideoMediaStoreHelper.deleteRecordsAndFilesByIdsAsync(appContext, ids, fromTrash)
        else -> {}
    }
}

actual suspend fun getDocExtGroups(query: String): List<Pair<String, Int>> =
    DocMediaStoreHelper.getDocExtGroupsAsync(appContext, query)

actual suspend fun searchImagesCombined(
    queryText: String,
    extraQuery: String,
    limit: Int,
    offset: Int,
    sortBy: FileSortBy,
): List<DImage> = com.ismartcoding.plain.features.media.ImageSearchHelper.searchCombinedAsync(
    context = appContext,
    queryText = queryText,
    extraQuery = extraQuery,
    limit = limit,
    offset = offset,
    sortBy = sortBy,
)

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

actual suspend fun getMediaIds(dataType: DataType, query: String): Set<String> = when (dataType) {
    DataType.AUDIO -> AudioMediaStoreHelper.getIdsAsync(appContext, query)
    DataType.VIDEO -> VideoMediaStoreHelper.getIdsAsync(appContext, query)
    DataType.IMAGE -> ImageMediaStoreHelper.getIdsAsync(appContext, query)
    DataType.DOC -> DocMediaStoreHelper.getIdsAsync(appContext, query)
    DataType.CALL -> CallMediaStoreHelper.getIdsAsync(appContext, query)
    DataType.CONTACT -> ContactMediaStoreHelper.getIdsAsync(appContext, query)
    DataType.SMS -> SmsHelper.getIdsAsync(appContext, query)
    else -> emptySet()
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
    com.ismartcoding.plain.features.media.ImageSearchHelper.countCombinedAsync(
        context = appContext,
        queryText = queryText,
        extraQuery = extraQuery,
    )

actual fun startImageIndexFullScan(force: Boolean) =
    com.ismartcoding.plain.ai.ImageIndexManager.fullScan(force)

actual fun cancelImageIndex() =
    com.ismartcoding.plain.ai.ImageSearchIndexer.cancel()

actual fun buildImageSearchStatus(): com.ismartcoding.plain.web.models.ImageSearchStatus {
    val mgr = com.ismartcoding.plain.ai.ImageSearchManager
    val indexer = com.ismartcoding.plain.ai.ImageSearchIndexer
    return com.ismartcoding.plain.web.models.ImageSearchStatus(
        status = mgr.status.value.name,
        downloadProgress = mgr.downloadProgress.value,
        errorMessage = mgr.errorMessage.value,
        modelSize = mgr.totalModelSize(),
        modelDir = mgr.getModelDir(),
        isIndexing = indexer.isRunning,
        totalImages = indexer.totalImages,
        indexedImages = indexer.indexedImages,
    )
}

actual fun lookupPhoneGeo(number: String): com.ismartcoding.plain.web.models.PhoneGeo? =
    PhoneGeoCache.lookup(number)

actual suspend fun searchSmsConversations(
    query: String,
    limit: Int,
    offset: Int,
): List<com.ismartcoding.plain.features.sms.DMessageConversation> =
    com.ismartcoding.plain.features.sms.SmsConversationHelper.searchConversationsAsync(appContext, query, limit, offset)

actual suspend fun countSmsConversations(query: String): Int =
    com.ismartcoding.plain.features.sms.SmsConversationHelper.conversationCountAsync(appContext, query)

actual suspend fun getArchivedSmsConversations(): List<com.ismartcoding.plain.features.sms.DMessageConversation> =
    com.ismartcoding.plain.features.sms.SmsConversationHelper.getArchivedConversations(appContext)

actual suspend fun getSmsAllCounts(): SmsCounts =
    com.ismartcoding.plain.features.sms.SmsHelper.countAllAsync(appContext).let {
        SmsCounts(it.total, it.inbox, it.sent, it.drafts)
    }

actual fun sendSmsText(number: String, body: String, subscriptionId: Int?) =
    com.ismartcoding.plain.features.sms.SmsHelper.sendText(number, body, subscriptionId)

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

actual fun getScreenSize(): Pair<Int, Int> {
    val displayMetrics = appContext.resources.displayMetrics
    return Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
}

actual fun getDownloadsDirPath(): String =
    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS).absolutePath

actual suspend fun getContactById(id: String): com.ismartcoding.plain.data.DContact? =
    com.ismartcoding.plain.features.media.ContactMediaStoreHelper.getByIdAsync(appContext, id)

actual fun updateContact(id: String, input: com.ismartcoding.plain.web.models.ContactInput) =
    com.ismartcoding.plain.features.media.ContactMediaStoreHelper.updateAsync(id, input)

actual fun createContact(input: com.ismartcoding.plain.web.models.ContactInput): String =
    com.ismartcoding.plain.features.media.ContactMediaStoreHelper.createAsync(input)

actual suspend fun deleteContacts(ids: Set<String>) {
    com.ismartcoding.plain.features.media.ContactMediaStoreHelper.deleteByIdsAsync(appContext, ids)
}

actual fun startMmsPolling(pendingId: String, launchTimeSec: Long, attachmentPaths: List<String>) {
    com.ismartcoding.plain.helpers.coIO {
        val context = appContext
        repeat(150) { // 2 s × 150 = 5 minutes max
            kotlinx.coroutines.delay(2000)
            val found = context.contentResolver.query(
                android.net.Uri.parse("content://mms"),
                arrayOf("_id"),
                "msg_box = 2 AND m_type = 128 AND date >= ?",
                arrayOf(launchTimeSec.toString()),
                null,
            )?.use { cursor -> cursor.count > 0 } ?: false
            if (found) {
                com.ismartcoding.plain.TempData.pendingMmsMessages.removeIf { it.id == pendingId }
                attachmentPaths.forEach { path ->
                    try {
                        java.io.File(path).delete()
                    } catch (_: Exception) {
                    }
                }
                com.ismartcoding.plain.lib.channel.sendEvent(
                    com.ismartcoding.plain.events.WebSocketEvent(
                        com.ismartcoding.plain.events.EventType.MMS_SENT,
                        com.ismartcoding.plain.helpers.JsonHelper.jsonEncode(pendingId),
                    ),
                )
                return@coIO
            }
        }
    }
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
