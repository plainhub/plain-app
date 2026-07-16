package com.ismartcoding.plain.platform

import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.data.TagRelationStub
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.file.FileSortBy

/**
 * Returns all media buckets (folders) for the given [dataType].
 * Returns an empty list on platforms without a media store (iOS).
 */
expect suspend fun getMediaBuckets(dataType: DataType): List<DMediaBucket>

/**
 * Search media items of [dataType] matching [query], paginated by [limit]/[offset]
 * and sorted by [sortBy]. Returns an empty list on unsupported platforms.
 */
expect suspend fun searchMedia(
    dataType: DataType,
    query: String,
    limit: Int,
    offset: Int,
    sortBy: FileSortBy,
): List<IData>

/**
 * Count media items of [dataType] matching [query]. Returns 0 on unsupported platforms.
 */
expect suspend fun countMedia(dataType: DataType, query: String): Int

/**
 * Move media items of [dataType] identified by [ids] to the trash.
 */
expect suspend fun trashMedia(dataType: DataType, ids: Set<String>)

/**
 * Restore media items of [dataType] identified by [ids] from the trash.
 */
expect suspend fun restoreMedia(dataType: DataType, ids: Set<String>)

/**
 * Permanently delete media items of [dataType] identified by [ids] from disk and MediaStore.
 * If [fromTrash] is true, only items already in the trash are deleted.
 */
expect suspend fun deleteMedia(dataType: DataType, ids: Set<String>, fromTrash: Boolean)

/**
 * Returns file-extension groups for documents matching [query].
 * Each pair is (extension, count). Returns an empty list on unsupported platforms.
 */
expect suspend fun getDocExtGroups(query: String): List<Pair<String, Int>>

/**
 * Combined image search: filename + optional AI semantic search.
 * If the AI model is not ready or [queryText] is blank, falls back to filename-only search.
 */
expect suspend fun searchImagesCombined(
    queryText: String,
    extraQuery: String,
    limit: Int,
    offset: Int,
    sortBy: FileSortBy,
): List<DImage>

/**
 * Whether the on-device AI image search model is loaded and ready.
 */
expect fun isImageSearchModelReady(): Boolean

/**
 * Enqueue removal of the given image [ids] from the AI search index.
 */
expect fun enqueueRemoveImageIndex(ids: Set<String>)

/**
 * Returns a URI string for the media item identified by [id] of [dataType],
 * suitable for sharing via the system share sheet. Empty string on failure.
 */
expect fun getMediaItemUriString(dataType: DataType, id: String): String

/**
 * Returns the IDs of media items of [dataType] matching [query].
 * Used by tag operations and trash/delete mutations.
 */
expect suspend fun getMediaIds(dataType: DataType, query: String): Set<String>

/**
 * Returns the IDs of trashed media items of [dataType] matching [query].
 * On platforms without a trash concept, returns the same as [getMediaIds].
 */
expect suspend fun getTrashedMediaIds(dataType: DataType, query: String): Set<String>

/**
 * Returns the file paths for the media items of [dataType] identified by [ids].
 * Used to clean up playlist entries when items are trashed.
 */
expect suspend fun getMediaPathsByIds(dataType: DataType, ids: Set<String>): Set<String>

/**
 * Returns [TagRelationStub]s for media items of [dataType] matching [query].
 * Used by tag-batch-add operations.
 */
expect suspend fun getMediaTagRelationStubs(dataType: DataType, query: String): List<TagRelationStub>

/**
 * Count images matching a combined filename + AI semantic search [queryText]
 * with the additional [extraQuery] filter. Returns 0 on unsupported platforms
 * or when the AI model is not ready.
 */
expect suspend fun countImagesCombined(queryText: String, extraQuery: String): Int

/**
 * Start a full scan of all images to (re)build the AI search index.
 * If [force] is true, re-index even if already indexed.
 */
expect fun startImageIndexFullScan(force: Boolean)

/**
 * Cancel any in-progress image indexing operation.
 */
expect fun cancelImageIndex()

/**
 * Build a snapshot of the current image-search status (model state, indexing progress).
 * Returns a default "unavailable" status on platforms without AI image search.
 */
expect fun buildImageSearchStatus(): com.ismartcoding.plain.web.models.ImageSearchStatus

/**
 * Look up geographic information (province, city, ISP) for a phone [number].
 * Returns null if no match is found or the platform has no lookup capability.
 */
expect fun lookupPhoneGeo(number: String): com.ismartcoding.plain.web.models.PhoneGeo?

/**
 * Search SMS conversations matching [query], paginated by [limit]/[offset].
 * Returns an empty list on unsupported platforms.
 */
expect suspend fun searchSmsConversations(
    query: String,
    limit: Int,
    offset: Int,
): List<com.ismartcoding.plain.features.sms.DMessageConversation>

/**
 * Count SMS conversations matching [query]. Returns 0 on unsupported platforms.
 */
expect suspend fun countSmsConversations(query: String): Int

/**
 * Returns archived SMS conversations. Empty list on unsupported platforms.
 */
expect suspend fun getArchivedSmsConversations(): List<com.ismartcoding.plain.features.sms.DMessageConversation>

/**
 * Aggregate SMS counts (total, unread, sent, drafts) on supported platforms.
 * Returns zeroes on unsupported platforms.
 */
expect suspend fun getSmsAllCounts(): SmsCounts

/**
 * Send an SMS text message to [number] with body [body].
 * @param subscriptionId SIM subscription id, or null for default.
 */
expect fun sendSmsText(number: String, body: String, subscriptionId: Int?)

/**
 * Resolve a content URI (e.g. from app file store) to a real filesystem path.
 * Returns the input unchanged on platforms without content URIs.
 */
expect fun resolveAppFileUri(uri: String): String

/**
 * Returns the MIME type for a file extension (e.g. "image/png").
 * Returns "application/octet-stream" if unknown.
 */
expect fun mimeTypeFromExtension(extension: String): String

/**
 * Launch the platform's default SMS/MMS app with the supplied [number], [body]
 * and pre-resolved attachments (path, mimeType) pairs. Returns the launch time
 * in seconds since epoch (used to track delivery via polling).
 */
expect fun launchDefaultSmsApp(
    number: String,
    body: String,
    attachments: List<Pair<String, String>>,
): Long

/**
 * Returns the screen size in pixels as a [Pair] of (width, height) on supported
 * platforms, or (0, 0) on platforms without a screen mirror surface.
 */
expect fun getScreenSize(): Pair<Int, Int>

/**
 * Returns the absolute path of the platform's public Downloads directory.
 * Empty string on unsupported platforms.
 */
expect fun getDownloadsDirPath(): String

/**
 * Aggregate SMS counts returned by [getSmsAllCounts].
 */
data class SmsCounts(
    val total: Int,
    val inbox: Int,
    val sent: Int,
    val drafts: Int,
)

/**
 * Returns the contact identified by [id], or null if not found.
 */
expect suspend fun getContactById(id: String): com.ismartcoding.plain.data.DContact?

/**
 * Update the contact identified by [id] with the values in [input].
 * No-op on platforms without a contacts provider.
 */
expect fun updateContact(id: String, input: com.ismartcoding.plain.web.models.ContactInput)

/**
 * Create a new contact with the values in [input]. Returns the new contact's id,
 * or empty string on failure / unsupported platforms.
 */
expect fun createContact(input: com.ismartcoding.plain.web.models.ContactInput): String

/**
 * Delete the contacts identified by [ids]. Used by tag-cleanup flow.
 */
expect suspend fun deleteContacts(ids: Set<String>)

/**
 * Poll the platform MMS provider for up to 5 minutes waiting for a sent MMS
 * to appear (identified by [launchTimeSec]). When found, removes the pending
 * entry identified by [pendingId] from [TempData.pendingMmsMessages], deletes
 * the temporary [attachmentPaths], and emits an `MMS_SENT` WebSocket event.
 *
 * No-op on platforms without an MMS provider (iOS).
 */
expect fun startMmsPolling(pendingId: String, launchTimeSec: Long, attachmentPaths: List<String>)

/**
 * Enable the on-device AI image search model (downloads model if needed).
 * No-op on platforms without AI image search (iOS).
 */
expect suspend fun enableImageSearchAsync()

/**
 * Disable the on-device AI image search model and release resources.
 * No-op on platforms without AI image search (iOS).
 */
expect suspend fun disableImageSearchAsync()

/**
 * Cancel any in-progress download of the AI image search model.
 * No-op on platforms without AI image search (iOS).
 */
expect fun cancelImageModelDownload()
