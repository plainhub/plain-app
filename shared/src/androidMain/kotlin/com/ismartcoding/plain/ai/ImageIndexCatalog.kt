package com.ismartcoding.plain.ai

import android.database.ContentObserver
import android.os.Bundle
import android.provider.MediaStore
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.isGranted
import com.ismartcoding.plain.platform.isQPlus
import com.ismartcoding.plain.platform.isRPlus
import com.ismartcoding.plain.lib.extensions.paging
import com.ismartcoding.plain.lib.extensions.where
import kotlinx.serialization.json.*
import java.util.concurrent.atomic.AtomicLong

object ImageIndexCatalog {
    private val epoch = AtomicLong()
    private var observer: ContentObserver? = null
    private val uri get() = if (isQPlus()) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    private fun requirePermission() {
        check(Permission.WRITE_EXTERNAL_STORAGE.isGranted() || Permission.READ_MEDIA_IMAGES.isGranted()) { "Image catalog permission denied" }
    }
    @Synchronized
    private fun observe() {
        if (observer != null) return
        val value = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) { epoch.incrementAndGet() }
        }
        appContext.contentResolver.registerContentObserver(uri, true, value)
        observer = value
    }
    private fun revision(): String {
        requirePermission()
        return if (isRPlus()) {
            MediaStore.getExternalVolumeNames(appContext).sorted().joinToString("|") { volume ->
                "$volume:${MediaStore.getVersion(appContext,volume)}:${MediaStore.getGeneration(appContext,volume)}"
            } + "|${epoch.get()}"
        } else "${MediaStore.getVersion(appContext)}:${epoch.get()}"
    }
    fun snapshot(): JsonObject {
        requirePermission()
        observe()
        val revision = revision()
        val total = checkNotNull(appContext.contentResolver.query(uri,arrayOf(MediaStore.Images.Media._ID),null,null,null)) { "Image catalog query failed" }.use { it.count }
        verify(revision)
        return JsonHelper.jsonEncodeToElement(ImageCatalogSnapshot(revision,total)).jsonObject
    }
    fun verify(expected: String) { check(revision() == expected) { "Image catalog changed during indexing" } }
    fun page(expected: String, cursor: String, limit: Int): JsonObject {
        require(limit in 1..128)
        verify(expected)
        val after = if (cursor.isEmpty()) -1L else cursor.toLong()
        val columns = arrayOf(MediaStore.Images.Media._ID,MediaStore.Images.Media.DATA)
        val selection = "${MediaStore.Images.Media._ID} > ?"
        val args = arrayOf(after.toString())
        val queried = if (isRPlus()) {
            appContext.contentResolver.query(uri,columns,Bundle().apply {
                paging(0,limit)
                where(selection,args.toList())
                putStringArray(android.content.ContentResolver.QUERY_ARG_SORT_COLUMNS,arrayOf(MediaStore.Images.Media._ID))
                putInt(android.content.ContentResolver.QUERY_ARG_SORT_DIRECTION,android.content.ContentResolver.QUERY_SORT_DIRECTION_ASCENDING)
            },null)
        } else appContext.contentResolver.query(uri,columns,selection,args,"${MediaStore.Images.Media._ID} ASC LIMIT $limit")
        var last = cursor
        val items = checkNotNull(queried) { "Image catalog page failed" }.use { rows ->
            val idColumn = rows.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val pathColumn = rows.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            buildList<ImageCatalogItem> {
                while (rows.moveToNext()) {
                    val id = rows.getLong(idColumn).toString()
                    val path = checkNotNull(rows.getString(pathColumn)) { "Image path unavailable" }
                    check(path.isNotEmpty()) { "Image path unavailable" }
                    add(ImageCatalogItem(id,path))
                    last = id
                }
            }
        }
        verify(expected)
        return JsonHelper.jsonEncodeToElement(ImageCatalogPage(expected,items,last,items.size < limit)).jsonObject
    }
    fun resolve(expected: String, ids: List<String>): JsonArray {
        require(ids.size <= 100)
        verify(expected)
        if (ids.isEmpty()) return JsonArray(emptyList())
        val selection = "${MediaStore.Images.Media._ID} IN (${ids.joinToString(",") { "?" }})"
        val rows = checkNotNull(appContext.contentResolver.query(uri,arrayOf(MediaStore.Images.Media._ID,MediaStore.Images.Media.DATA),selection,ids.toTypedArray(),null)) { "Image selection query failed" }
        val items = rows.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            buildList<ImageCatalogItem> {
                while (cursor.moveToNext()) {
                    val path = checkNotNull(cursor.getString(pathColumn)) { "Image path unavailable" }
                    check(path.isNotEmpty()) { "Image path unavailable" }
                    add(ImageCatalogItem(cursor.getLong(idColumn).toString(),path))
                }
            }
        }
        verify(expected)
        return JsonHelper.jsonEncodeToElement(items).jsonArray
    }
    @Synchronized
    fun close() {
        observer?.let { appContext.contentResolver.unregisterContentObserver(it) }
        observer = null
        epoch.incrementAndGet()
    }
}
