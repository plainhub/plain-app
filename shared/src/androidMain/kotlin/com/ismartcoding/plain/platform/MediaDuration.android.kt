package com.ismartcoding.plain.platform

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.features.MediaDurationHelper
import com.ismartcoding.plain.events.MediaDurationZeroItem
import com.ismartcoding.plain.helpers.Mp4Helper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.audio.AudioMediaStoreHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper

/**
 * Calculate actual duration for a single zero-duration media item and persist
 * it through [MediaDurationHelper] + in-memory [TempData.mediaDurationMap].
 *
 * MediaStore.DURATION is read-only on Android 10+ (contentResolver.update
 * silently returns 0 rows), so we cannot write back to MediaStore. Instead we
 * cache the computed duration locally and merge it during list queries.
 *
 * Runs on [MediaDurationFixQueue]'s worker coroutine — never blocks the list API.
 */
actual suspend fun processSingleDurationZero(
    mediaType: String,
    item: MediaDurationZeroItem,
) {
    try {
        val durationMs = Mp4Helper.getMp4DurationMs(item.path)
        if (durationMs <= 0) return
        val stillCurrent = when (mediaType) {
            "audio" -> AudioMediaStoreHelper.isCurrentItemAsync(appContext, item.id, item.path)
            "video" -> VideoMediaStoreHelper.isCurrentItemAsync(appContext, item.id, item.path)
            else -> false
        }
        if (!stillCurrent) return
        MediaDurationHelper.save(mediaType, item.id, durationMs)
        LogCat.d("Cached duration for $mediaType ${item.id}: ${durationMs}ms")
    } catch (e: Exception) {
        LogCat.e("Failed to cache duration for ${item.path}: ${e.message}")
    }
}
