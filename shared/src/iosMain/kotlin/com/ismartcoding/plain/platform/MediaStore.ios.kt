package com.ismartcoding.plain.platform

import com.ismartcoding.plain.ai.ImageSearchStatusType
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.db.IData
import com.ismartcoding.plain.data.TagRelationStub
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.file.FileSortBy
import kotlinx.cinterop.useContents

actual suspend fun getMediaBuckets(dataType: DataType): List<DMediaBucket> = emptyList()

actual suspend fun searchMedia(
    dataType: DataType,
    query: String,
    limit: Int,
    offset: Int,
    sortBy: FileSortBy,
): List<IData> = emptyList()

actual suspend fun countMedia(dataType: DataType, query: String): Int = 0

actual suspend fun trashMedia(dataType: DataType, ids: Set<String>) {}

actual suspend fun restoreMedia(dataType: DataType, ids: Set<String>) {}

actual suspend fun deleteMedia(dataType: DataType, ids: Set<String>, fromTrash: Boolean) {}

actual suspend fun getDocExtGroups(query: String): List<Pair<String, Int>> = emptyList()

actual suspend fun searchImagesCombined(
    queryText: String,
    extraQuery: String,
    limit: Int,
    offset: Int,
    sortBy: FileSortBy,
): List<DImage> = emptyList()

actual fun isImageSearchModelReady(): Boolean = false

actual fun enqueueRemoveImageIndex(ids: Set<String>) {}

actual fun getMediaItemUriString(dataType: DataType, id: String): String = ""

actual suspend fun getMediaIds(dataType: DataType, query: String): Set<String> = emptySet()

actual suspend fun getTrashedMediaIds(dataType: DataType, query: String): Set<String> = emptySet()

actual suspend fun getMediaPathsByIds(dataType: DataType, ids: Set<String>): Set<String> = emptySet()

actual suspend fun getMediaTagRelationStubs(dataType: DataType, query: String): List<TagRelationStub> = emptyList()

actual suspend fun countImagesCombined(queryText: String, extraQuery: String): Int = 0

actual fun startImageIndexFullScan(force: Boolean) {}

actual fun cancelImageIndex() {}

actual fun buildImageSearchStatus(): com.ismartcoding.plain.httpserver.models.ImageSearchStatus =
    com.ismartcoding.plain.httpserver.models.ImageSearchStatus(
        status = ImageSearchStatusType.UNAVAILABLE,
        downloadProgress = 0,
        errorMessage = "",
        modelSize = 0L,
        modelDir = "",
        isIndexing = false,
        totalImages = 0,
        indexedImages = 0,
    )

actual fun lookupPhoneGeo(number: String): com.ismartcoding.plain.httpserver.models.PhoneGeo? = null

actual suspend fun searchSmsConversations(
    query: String,
    limit: Int,
    offset: Int,
): List<com.ismartcoding.plain.features.sms.DMessageConversation> = emptyList()

actual suspend fun countSmsConversations(query: String): Int = 0

actual suspend fun getArchivedSmsConversations(): List<com.ismartcoding.plain.features.sms.DMessageConversation> = emptyList()

actual suspend fun getSmsAllCounts(): DSmsCounts = DSmsCounts(0, 0, 0, 0)

actual fun sendSmsText(number: String, body: String, subscriptionId: Int?) {}

actual fun call(number: String, showDialer: Boolean) {}

actual fun resolveAppFileUri(uri: String): String = uri

actual fun mimeTypeFromExtension(extension: String): String = "application/octet-stream"

actual fun launchDefaultSmsApp(
    number: String,
    body: String,
    attachments: List<Pair<String, String>>,
): Long = 0L

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
actual fun getScreenSize(): Pair<Int, Int> {
    val bounds = platform.UIKit.UIScreen.mainScreen.bounds.useContents {
        Pair(size.width.toInt(), size.height.toInt())
    }
    return bounds
}

actual fun getDownloadsDirPath(): String = ""

actual suspend fun getContactById(id: String): com.ismartcoding.plain.data.DContact? = null

actual fun updateContact(id: String, input: com.ismartcoding.plain.httpserver.models.ContactInput) {}

actual fun createContact(input: com.ismartcoding.plain.httpserver.models.ContactInput): String = ""

actual suspend fun deleteContacts(ids: Set<String>) {}

actual fun startMmsPolling(pendingId: String, launchTimeSec: Long, attachmentPaths: List<String>) {}

actual suspend fun enableImageSearchAsync() {}

actual suspend fun disableImageSearchAsync() {}

actual fun cancelImageModelDownload() {}
