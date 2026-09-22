package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.db.DAudioPlaylistItem
import com.ismartcoding.plain.db.DAudioQueueItem

fun DAudioPlaylistItem.toPlaylistAudio(): DPlaylistAudio =
    DPlaylistAudio(title = title, path = audioPath, artist = artist, duration = duration, albumId = albumId)

fun DAudioQueueItem.toPlaylistAudio(): DPlaylistAudio =
    DPlaylistAudio(title = title, path = path, artist = artist, duration = duration)

fun DPlaylistAudio.toQueueItem(sortOrder: Int): DAudioQueueItem =
    DAudioQueueItem(path = path, sortOrder = sortOrder, title = title, artist = artist, duration = duration)
