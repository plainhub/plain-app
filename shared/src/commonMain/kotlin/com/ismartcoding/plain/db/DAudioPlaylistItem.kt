package com.ismartcoding.plain.db

import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant

data class DAudioPlaylistItem(
    val id: String,
    var playlistId: String,
    var audioPath: String,
    // Snapshot columns let lists render without touching MediaStore; playback
    // still resolves the file live from audioPath.
    var title: String,
    var artist: String,
    // MediaStore album id snapshot for the playlist cover mosaic; blank for
    // rows written before the column existed (backfilled on first load).
    var albumId: String = "",
    var durationMs: Long,
    var sortOrder: Int,
    var addedAt: Instant = TimeHelper.now(),
)
