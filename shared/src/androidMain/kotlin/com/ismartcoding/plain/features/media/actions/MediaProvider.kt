package com.ismartcoding.plain.features.media.actions

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.app.*
import com.ismartcoding.plain.ui.app.*
import com.ismartcoding.plain.features.chat.*
import com.ismartcoding.plain.ui.chat.*
import com.ismartcoding.plain.features.feeds.*
import com.ismartcoding.plain.ui.feeds.*
import com.ismartcoding.plain.features.images.search.*
import com.ismartcoding.plain.features.sessions.*
import com.ismartcoding.plain.ui.sessions.*
import com.ismartcoding.plain.features.shares.*
import com.ismartcoding.plain.ui.shares.*
import com.ismartcoding.plain.platform.*
import com.ismartcoding.plain.platform.contacts.*
import com.ismartcoding.plain.platform.files.*
import com.ismartcoding.plain.ui.platform.files.*
import com.ismartcoding.plain.platform.media.*
import com.ismartcoding.plain.ui.platform.media.*
import com.ismartcoding.plain.platform.network.*
import com.ismartcoding.plain.platform.permissions.*
import com.ismartcoding.plain.ui.navigation.*
import com.ismartcoding.plain.features.settings.*
import com.ismartcoding.plain.ui.settings.*
import com.ismartcoding.plain.platform.notifications.*
import com.ismartcoding.plain.ui.platform.notifications.*
import com.ismartcoding.plain.platform.system.*
import com.ismartcoding.plain.ui.platform.system.*
import com.ismartcoding.plain.ui.state.*
import com.ismartcoding.plain.platform.telephony.*
import com.ismartcoding.plain.network.*
import kotlinx.serialization.json.*
import kotlinx.serialization.Serializable
import com.ismartcoding.plain.lib.JsonHelper
import java.io.File

internal object MediaProvider {
    fun handle(method: String, params: JsonObject): JsonElement {
        if (method == "mediaProviderTrashRows") return trashedRows()
        val request: MediaProviderRequest = when (method) {
            "mediaProviderRows" -> JsonHelper.jsonDecodeFromElement<MediaProviderRowsRequest>(params)
            "mediaProviderWrite" -> JsonHelper.jsonDecodeFromElement<MediaProviderWritesRequest>(params)
            else -> error("Unsupported media provider primitive")
        }
        val type = DataType.entries.single { it.value == request.type }
        require(type in setOf(DataType.AUDIO, DataType.VIDEO, DataType.IMAGE, DataType.DOC))
        val permission = when (type) {
            DataType.AUDIO -> Permission.READ_MEDIA_AUDIO
            DataType.VIDEO -> Permission.READ_MEDIA_VIDEOS
            DataType.IMAGE -> Permission.READ_MEDIA_IMAGES
            else -> Permission.WRITE_EXTERNAL_STORAGE
        }
        check(Permission.WRITE_EXTERNAL_STORAGE.isGranted() || permission.isGranted()) { "Media provider permission denied" }
        return when (request) {
            is MediaProviderRowsRequest -> {
                JsonHelper.jsonEncodeToElement(request.ids.mapNotNull { id ->
                    try { row(type, id, request.includeVolumes) }
                    catch (_: Exception) { null }
                })
            }
            is MediaProviderWritesRequest -> {
                JsonHelper.jsonEncodeToElement(request.writes.map { write ->
                    val id = write.id
                    var count = 0
                    var after: MediaProviderRow? = null
                    try {
                        val uri = writeUri(type, write)
                        val selection = "${MediaStore.MediaColumns.DATA} = ?"
                        val args = arrayOf(write.expectedPath)
                        val extras = matching().apply {
                            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, if (write.expectedTrash) MediaStore.MATCH_ONLY else MediaStore.MATCH_EXCLUDE)
                            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, args)
                        }
                        if (write.delete) {
                            count = if (isRPlus()) appContext.contentResolver.delete(uri, extras) else appContext.contentResolver.delete(uri, selection, args)
                        } else {
                            val fields = ContentValues()
                            write.setTrash?.let {
                                check(isRPlus()) { "MediaProvider trash column unavailable" }
                                fields.put(MediaStore.MediaColumns.IS_TRASHED, if (it) 1 else 0)
                            }
                            write.relativePath?.let { fields.put(MediaStore.MediaColumns.RELATIVE_PATH, it) }
                            write.displayName?.let { fields.put(MediaStore.MediaColumns.DISPLAY_NAME, it) }
                            count = if (isRPlus()) appContext.contentResolver.update(uri, fields, extras) else appContext.contentResolver.update(uri, fields, selection, args)
                        }
                        after = row(type, id, false)
                    } catch (_: Exception) { count = 0 }
                    MediaProviderWriteResult(id, count, after)
                })
            }
        }
    }

    private fun trashedRows(): JsonElement {
        check(isRPlus() && Permission.WRITE_EXTERNAL_STORAGE.isGranted()) { "File trash permission denied" }
        val columns = arrayOf(
            MediaStore.Files.FileColumns._ID, MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MEDIA_TYPE, MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
        )
        val args = Bundle().apply { putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY) }
        val storage = appContext.getSystemService(StorageManager::class.java)
        val rows = mutableListOf<TrashRow>()
        val externalVolumes = MediaStore.getExternalVolumeNames(appContext)
        for (volume in storage.storageVolumes) {
            val name = volume.mediaStoreVolumeName ?: continue
            val directory = volume.directory ?: continue
            if (name !in externalVolumes) continue
            val uri = MediaStore.Files.getContentUri(name)
            checkNotNull(appContext.contentResolver.query(uri, columns, args, null)).use { cursor ->
                while (cursor.moveToNext()) {
                    rows.add(TrashRow(cursor.getLong(0).toString(), cursor.getString(1), cursor.getString(2),
                        directory.path, cursor.getInt(3), cursor.getString(4), cursor.getLong(5)))
                }
            }
        }
        return JsonHelper.jsonEncodeToElement(rows)
    }

    private fun writeUri(type: DataType, write: MediaProviderWrite): Uri {
        if (!isRPlus()) return Uri.parse(getSystemMediaItemUriString(type, write.id))
        val storage = appContext.getSystemService(StorageManager::class.java)
        val volume = requireNotNull(storage.getStorageVolume(File(write.expectedPath))?.mediaStoreVolumeName)
        val collection = when (type) {
            DataType.IMAGE -> MediaStore.Images.Media.getContentUri(volume)
            DataType.VIDEO -> MediaStore.Video.Media.getContentUri(volume)
            DataType.AUDIO -> MediaStore.Audio.Media.getContentUri(volume)
            DataType.DOC -> MediaStore.Files.getContentUri(volume)
            else -> error("Unsupported media type")
        }
        return android.content.ContentUris.withAppendedId(collection, write.id.toLong())
    }

    private fun matching() = Bundle().apply { putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE) }

    private fun row(type: DataType, id: String, volume: Boolean): MediaProviderRow? {
        require(id.toLongOrNull() != null)
        val uri = Uri.parse(getSystemMediaItemUriString(type, id))
        val columns = mutableListOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.MIME_TYPE)
        if (isRPlus()) columns.add(MediaStore.MediaColumns.IS_TRASHED)
        val cursor = if (isRPlus()) appContext.contentResolver.query(uri, columns.toTypedArray(), matching(), null)
            else appContext.contentResolver.query(uri, columns.toTypedArray(), null, null, null)
        return checkNotNull(cursor) { "Media provider query failed" }.use {
            if (!it.moveToFirst()) return@use null
            val path = checkNotNull(it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)))
            val root = if (!volume) null else if (isRPlus()) appContext.getSystemService(StorageManager::class.java).getStorageVolume(File(path))?.directory
                else Environment.getExternalStorageDirectory()
            MediaProviderRow(
                id = id,
                path = path,
                trashed = isRPlus() && it.getInt(it.getColumnIndexOrThrow(MediaStore.MediaColumns.IS_TRASHED)) != 0,
                mimeType = it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)) ?: "",
                volumeRoot = root?.canonicalPath,
            )
        }
    }
}

private sealed interface MediaProviderRequest { val type: Int }

@Serializable
private data class MediaProviderRowsRequest(override val type: Int, val ids: List<String>, val includeVolumes: Boolean) : MediaProviderRequest

@Serializable
private data class MediaProviderWritesRequest(override val type: Int, val writes: List<MediaProviderWrite>) : MediaProviderRequest

@Serializable
private data class MediaProviderWrite(
    val id: String,
    val expectedPath: String,
    val expectedTrash: Boolean,
    val delete: Boolean,
    val setTrash: Boolean? = null,
    val relativePath: String? = null,
    val displayName: String? = null,
)

@Serializable
private data class MediaProviderRow(
    val id: String,
    val path: String,
    val trashed: Boolean,
    val mimeType: String,
    val volumeRoot: String?,
)

@Serializable
private data class MediaProviderWriteResult(val id: String, val count: Int, val row: MediaProviderRow?)


@Serializable
private data class TrashRow(
    val id: String, val path: String, val displayName: String, val volumeRoot: String,
    val mediaType: Int, val mimeType: String?, val size: Long,
)
