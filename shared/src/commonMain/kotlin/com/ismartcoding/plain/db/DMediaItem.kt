package com.ismartcoding.plain.db

import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant

/**
 * Cached duration for a MediaStore item whose MediaStore.DURATION is 0 (e.g.
 * fragmented-MP4 files where MediaMetadataRetriever fails). The MediaStore
 * DURATION column is read-only on Android 10+, so we cannot write back; instead
 * we persist the computed duration here and merge it during list queries.
 *
 * Duration is stored in **milliseconds** to match DVideo/DAudio.durationMs's
 * unit (and the MediaStore DURATION column), so list queries can use the
 * cached value directly without conversion.
 *
 * `media_id` is the single primary key — MediaStore _ID is globally unique
 * across video/audio content URIs in practice (same pattern as
 * DVideoPlayProgress). This also ensures the developer DB view's idKey
 * resolves to `media_id` (not `media_type`) for correct row deletion.
 */
data class DMediaItem(
    val mediaType: String, // "video" | "audio"
    val mediaId: String,
    val durationMs: Long, // milliseconds
    val updatedAt: Instant = TimeHelper.now(),
)
