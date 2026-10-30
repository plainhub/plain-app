package com.ismartcoding.plain.ui.audio.playlists.components

import com.ismartcoding.plain.models.Audio as AudioTrack
import com.ismartcoding.plain.models.AudioItem
import com.ismartcoding.plain.lib.TimeHelper

internal fun AudioItem.toAudioTrack(): AudioTrack = AudioTrack(
    id = requireNotNull(mediaId),
    title = title,
    artist = artist,
    path = path,
    durationMs = durationMs,
    size = 0,
    bucketId = "",
    albumId = "",
    createdAt = TimeHelper.now(),
    updatedAt = TimeHelper.now(),
)
