package com.ismartcoding.plain.platform

import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.events.MediaDurationZeroItem
import com.ismartcoding.plain.helpers.Mp4Helper
import com.ismartcoding.plain.lib.logcat.LogCat

/**
 * Calculate actual duration for a single zero-duration media item and update
 * MediaStore. Runs on [MediaDurationFixQueue]'s worker coroutine — never blocks
 * the list API.
 */
actual suspend fun processSingleDurationZero(
    mediaType: String,
    item: MediaDurationZeroItem,
) {
    val context = appContext
    try {
        val durationMs = Mp4Helper.getMp4DurationMs(item.path)
        if (durationMs <= 0) return

        val baseUri = when (mediaType) {
            "video" -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            "audio" -> MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else -> return
        }

        val contentUri = Uri.withAppendedPath(baseUri, item.id)
        val values = ContentValues().apply {
            when (mediaType) {
                "video" -> put(MediaStore.Video.Media.DURATION, durationMs)
                "audio" -> put(MediaStore.Audio.Media.DURATION, durationMs)
            }
        }

        val rows = context.contentResolver.update(contentUri, values, null, null)
        if (rows > 0) {
            LogCat.d("Updated duration for $mediaType ${item.id}: ${durationMs}ms")
        }
    } catch (e: Exception) {
        LogCat.e("Failed to update duration for ${item.path}: ${e.message}")
    }
}
