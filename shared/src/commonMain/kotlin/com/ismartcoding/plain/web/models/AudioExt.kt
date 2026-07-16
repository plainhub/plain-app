package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.platform.getAudioAlbumArtFileId

fun DAudio.toModel(): Audio {
    return Audio(ID(id), title, artist, path, duration, size, bucketId, getAudioAlbumArtFileId(this), createdAt, updatedAt, isFavorite)
}

fun DPlaylistAudio.toModel(): PlaylistAudio {
    return PlaylistAudio(title, artist, path, duration)
}
