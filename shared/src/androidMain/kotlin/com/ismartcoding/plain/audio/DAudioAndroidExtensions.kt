package com.ismartcoding.plain.audio

import android.net.Uri

fun DAudio.getAlbumUri(): Uri {
    val albumArtUri = Uri.parse("content://media/external/audio/albumart")
    return Uri.withAppendedPath(albumArtUri, albumId)
}
