package com.ismartcoding.plain.ui.videos

import coil3.disk.DiskCache
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.models.MediaSortBy
import com.ismartcoding.plain.models.Video

internal class VideoGridCache(private val disk: DiskCache) {
    fun read(scope: String, sort: MediaSortBy): List<Video>? {
        val key = key(scope, sort)
        val cached = disk.openSnapshot(key) ?: return null
        return try {
            cached.use { snapshot ->
                JsonHelper.jsonDecode(disk.fileSystem.read(snapshot.data) { readUtf8() })
            }
        } catch (error: Exception) {
            LogCat.w("Video grid cache read failed: ${error::class.simpleName}")
            disk.remove(key)
            null
        }
    }

    fun write(scope: String, sort: MediaSortBy, videos: List<Video>) {
        val entry = disk.openEditor(key(scope, sort)) ?: return
        try {
            disk.fileSystem.write(entry.metadata) { writeUtf8("videos") }
            disk.fileSystem.write(entry.data) { writeUtf8(JsonHelper.jsonEncode(videos)) }
            entry.commit()
        } catch (error: Exception) {
            entry.abort()
            LogCat.w("Video grid cache write failed: ${error::class.simpleName}")
        }
    }

    private fun key(scope: String, sort: MediaSortBy): String = "video-grid-v1:$scope:${sort.name}"
}
