package com.ismartcoding.plain.platform

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.db.DMediaItem
import com.ismartcoding.plain.events.MediaDurationZeroItem
import com.ismartcoding.plain.helpers.Mp4Helper
import com.ismartcoding.plain.lib.logcat.LogCat

/**
 * Calculate actual duration for a single zero-duration media item and persist
 * it to the app's [mediaItemDao] + in-memory [TempData.mediaDurationMap].
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

        val durationSec = durationMs / 1000
        AppDatabase.instance.mediaItemDao().upsert(
            DMediaItem(
                mediaType = mediaType,
                mediaId = item.id,
                duration = durationSec,
            )
        )
        TempData.mediaDurationMap["$mediaType:${item.id}"] = durationSec
        LogCat.d("Cached duration for $mediaType ${item.id}: ${durationSec}s")
    } catch (e: Exception) {
        LogCat.e("Failed to cache duration for ${item.path}: ${e.message}")
    }
}
