package com.ismartcoding.plain.db

import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant

data class DAudioPlaylist(
    val id: String,
    var name: String,
    var createdAt: Instant = TimeHelper.now(),
    var updatedAt: Instant = TimeHelper.now(),
)

data class AudioPlaylistItemCount(
    val playlistId: String,
    val cnt: Int,
)
