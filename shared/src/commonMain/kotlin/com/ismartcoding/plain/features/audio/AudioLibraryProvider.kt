package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.features.file.FileSortBy

interface AudioLibraryProvider {
    suspend fun count(): Int
    suspend fun page(offset: Int, limit: Int, sort: FileSortBy): List<DPlaylistAudio>
    suspend fun contains(path: String): Boolean
}
