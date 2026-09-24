package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.db.DAudioPlayHistory
import com.ismartcoding.plain.platform.getAudioAlbumArtFileId

fun DAudio.toModel(): Audio {
    return Audio(ID(id), title, artist, path, durationMs = durationMs, size = size, ID(bucketId), getAudioAlbumArtFileId(this), createdAt, updatedAt, isFavorite)
}

fun DPlaylistAudio.toModel(): AudioItem {
    return AudioItem(title, artist, path, durationMs = durationMs)
}

fun DAudioPlayHistory.toModel(): AudioPlayHistory {
    return AudioPlayHistory(path, title, artist, durationMs = durationMs, playCount = playCount, playedAt = playedAt)
}
