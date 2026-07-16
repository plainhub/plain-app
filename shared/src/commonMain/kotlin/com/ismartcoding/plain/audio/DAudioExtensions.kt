package com.ismartcoding.plain.audio

fun DAudio.toPlaylistAudio(): DPlaylistAudio {
    return DPlaylistAudio(title, path, artist, duration)
}
