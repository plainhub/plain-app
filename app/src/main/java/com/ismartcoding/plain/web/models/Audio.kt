package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DAudio
import com.ismartcoding.plain.data.DPlaylistAudio

data class Audio(
    val id: ID,
    val title: String,
    val artist: String,
    val path: String,
    val duration: Long,
    val size: Long,
    val bucketId: String,
)

data class PlaylistAudio(
    val title: String,
    val artist: String,
    val path: String,
    val duration: Long,
)

fun DAudio.toModel(): Audio {
    return Audio(ID(id), title, artist, path, duration, size, bucketId)
}

fun DPlaylistAudio.toModel(): PlaylistAudio {
    return PlaylistAudio(title, artist, path, duration)
}
