package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.api.gql
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.platform.*

object PlatformAudioLibraryProvider : AudioLibraryProvider {
    private fun checkPermission() {
        check(Permission.WRITE_EXTERNAL_STORAGE.isGranted() || Permission.READ_MEDIA_AUDIO.isGranted()) { "Audio library permission denied" }
    }
    override suspend fun count(): Int { checkPermission(); return countMedia(DataType.AUDIO, "") }
    override suspend fun page(offset: Int, limit: Int, sort: FileSortBy): List<DPlaylistAudio> {
        checkPermission()
        require(offset >= 0 && limit >= 0)
        return searchMedia(DataType.AUDIO, "", limit, offset, sort).filterIsInstance<DAudio>().map { it.toPlaylistAudio() }
    }
    override suspend fun contains(path: String): Boolean {
        checkPermission()
        return countMedia(DataType.AUDIO, "path:${gql(path)}") > 0
    }
}
