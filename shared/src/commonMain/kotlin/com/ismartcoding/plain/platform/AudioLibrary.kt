package com.ismartcoding.plain.platform

import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.features.file.FileSortBy

expect suspend fun audioLibraryCount(): Int
expect suspend fun audioLibraryPage(offset: Int, limit: Int, sort: FileSortBy): List<DPlaylistAudio>
expect suspend fun audioLibraryContains(path: String): Boolean

expect suspend fun audioLibraryMetadata(path: String): DPlaylistAudio
