package com.ismartcoding.plain.platform

import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.features.file.FileSortBy

actual suspend fun audioLibraryCount(): Int = error("System audio catalog is unavailable on iOS")
actual suspend fun audioLibraryPage(offset: Int, limit: Int, sort: FileSortBy): List<DPlaylistAudio> = error("System audio catalog is unavailable on iOS")
actual suspend fun audioLibraryContains(path: String): Boolean = error("System audio catalog is unavailable on iOS")

actual suspend fun audioLibraryMetadata(path: String): DPlaylistAudio {
    check(platform.Foundation.NSFileManager.defaultManager.fileExistsAtPath(path)) { "Audio file not found" }
    return playlistAudioFromPath(path)
}
