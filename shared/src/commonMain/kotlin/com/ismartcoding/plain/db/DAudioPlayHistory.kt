package com.ismartcoding.plain.db

import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant

/** Recently played tracks, the data source of the home "最近" section. */
data class DAudioPlayHistory(
    val path: String,
    var title: String,
    var artist: String,
    var durationMs: Long,
    /** Total times this track was played (incremented on every play). */
    var playCount: Int = 0,
    var playedAt: Instant = TimeHelper.now(),
)

data class ArtistPlayCount(
    val artist: String,
    val count: Long,
)
