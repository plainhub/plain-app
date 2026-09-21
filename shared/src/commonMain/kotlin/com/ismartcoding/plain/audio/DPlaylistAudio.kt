package com.ismartcoding.plain.audio

import com.ismartcoding.plain.lib.extensions.formatDuration
import kotlinx.serialization.Serializable

@Serializable
data class DPlaylistAudio(
    val title: String,
    val path: String,
    val artist: String,
    val duration: Long,
    /** MediaStore album snapshot for the playlist cover mosaic. */
    val albumId: String = "",
) {
    fun getSubtitle(): String {
        return listOf(artist, duration.formatDuration()).filter { it.isNotEmpty() }.joinToString(" · ")
    }

    companion object {
        private const val serialVersionUID = -11L
    }
}
