package com.ismartcoding.plain.features.mediaactions

import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import com.ismartcoding.plain.api.string
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.docs.DocMediaStoreHelper
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.helpers.FilePathValidator
import com.ismartcoding.plain.platform.*
import kotlinx.serialization.json.*
import java.io.File

object NativeMediaActions {
    private data class Row(val path: String, val trash: Boolean)
    fun run(params: JsonObject): JsonObject {
        val type = DataType.entries.single { it.value == params.getValue("type").jsonPrimitive.int }
        require(type in setOf(DataType.AUDIO,DataType.VIDEO,DataType.IMAGE,DataType.DOC))
        val permission = when (type) {
            DataType.AUDIO -> Permission.READ_MEDIA_AUDIO
            DataType.VIDEO -> Permission.READ_MEDIA_VIDEOS
            DataType.IMAGE -> Permission.READ_MEDIA_IMAGES
            else -> Permission.WRITE_EXTERNAL_STORAGE
        }
        check(Permission.WRITE_EXTERNAL_STORAGE.isGranted() || permission.isGranted()) { "Media action permission denied" }
        val action = MediaAction.valueOf(params.string("action"))
        if (action == MediaAction.TRASH || action == MediaAction.RESTORE) check(isRPlus()) { "Media trash is unavailable" }
        val ids = params.getValue("ids").jsonArray.map { it.jsonPrimitive.content }
        require(ids.size <= 500 && ids.distinct().size == ids.size && ids.all { it.toLongOrNull() != null })
        val fromTrash = params.getValue("fromTrash").jsonPrimitive.boolean
        val successful = mutableListOf<JsonObject>()
        val failed = mutableListOf<String>()
        val dest = if (action == MediaAction.MOVE) File(params.string("destDir")).canonicalFile.also {
            FilePathValidator.requireAllSafe(listOf(it.path))
            check(it.isDirectory || it.mkdirs()) { "Media destination unavailable" }
        } else null
        ids.forEach { id ->
            try {
            val uri = Uri.parse(getMediaItemUriString(type,id))
            val before = row(type,uri)
            if (before == null || (action == MediaAction.DELETE && before.trash != fromTrash)) {
                failed.add(id)
                return@forEach
            }
            var destination = ""
            val changed = try {
                when (action) {
                    MediaAction.DELETE -> delete(uri) > 0 && row(type,uri) == null
                    MediaAction.TRASH, MediaAction.RESTORE -> {
                        val trash = action == MediaAction.TRASH
                        if (before.trash == trash) false else {
                            val count = update(uri,ContentValues().apply { put(MediaStore.MediaColumns.IS_TRASHED,if (trash) 1 else 0) })
                            count > 0 && row(type,uri)?.trash == trash
                        }
                    }
                    MediaAction.MOVE -> {
                        val source = File(before.path)
                        val target = File(checkNotNull(dest),source.name)
                        destination = if (target.exists()) getNewPath(target.path) else target.path
                        if (source.parentFile?.canonicalFile == dest || !source.isFile || before.trash) false else {
                            val root = if (isRPlus()) appContext.getSystemService(android.os.storage.StorageManager::class.java).getStorageVolume(source)?.directory?.canonicalFile else android.os.Environment.getExternalStorageDirectory().canonicalFile
                            check(root != null && checkNotNull(dest).toPath().startsWith(root.toPath())) { "Media move must stay in its storage volume" }
                            val relative = checkNotNull(dest).relativeTo(checkNotNull(root)).invariantSeparatorsPath.trimEnd('/') + "/"
                            val count = update(uri,ContentValues().apply {
                                put(MediaStore.MediaColumns.RELATIVE_PATH,relative)
                                put(MediaStore.MediaColumns.DISPLAY_NAME,File(destination).name)
                            })
                            count > 0 && row(type,uri)?.path == destination && File(destination).isFile && !source.exists()
                        }
                    }
                }
            } catch (_: SecurityException) { false }
            catch (_: IllegalArgumentException) { false }
            if (changed) successful.add(buildJsonObject { put("id",id);put("path",before.path);put("destinationPath",destination) }) else failed.add(id)
            } catch (_: Exception) { if (id !in failed) failed.add(id) }
        }
        return buildJsonObject { put("successful",JsonArray(successful));put("failedIds",JsonArray(failed.map(::JsonPrimitive))) }
    }
    private fun matching() = Bundle().apply { putInt(MediaStore.QUERY_ARG_MATCH_TRASHED,MediaStore.MATCH_INCLUDE) }
    private fun update(uri: Uri, values: ContentValues): Int = if (isRPlus()) appContext.contentResolver.update(uri,values,matching()) else appContext.contentResolver.update(uri,values,null,null)
    private fun delete(uri: Uri): Int = if (isRPlus()) appContext.contentResolver.delete(uri,matching()) else appContext.contentResolver.delete(uri,null,null)
    private fun row(type: DataType, uri: Uri): Row? {
        val columns = mutableListOf(MediaStore.MediaColumns.DATA)
        if (isRPlus()) columns.add(MediaStore.MediaColumns.IS_TRASHED)
        if (type == DataType.DOC) columns.add(MediaStore.MediaColumns.MIME_TYPE)
        val queried = if (isRPlus()) appContext.contentResolver.query(uri,columns.toTypedArray(),Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED,MediaStore.MATCH_INCLUDE)
        },null) else appContext.contentResolver.query(uri,columns.toTypedArray(),null,null,null)
        return checkNotNull(queried) { "Media action query failed" }.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            if (type == DataType.DOC && !DocMediaStoreHelper.isDocumentMimeType(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)))) return@use null
            val path = checkNotNull(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))) { "Media path unavailable" }
            check(path.isNotEmpty()) { "Media path unavailable" }
            Row(path,isRPlus() && cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.IS_TRASHED)) != 0)
        }
    }
}
