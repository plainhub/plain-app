package com.ismartcoding.plain.ui.page.playlist

import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.db.DAudioPlaylistItem
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.audio.LegacyAudioPlaylistManager
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.platform.searchMedia

internal suspend fun resolveLegacyBlankAlbums(items: List<DAudioPlaylistItem>): Map<String, String> {
    val blanks = items.filter { it.albumId.isBlank() }
    if (blanks.isEmpty()) return emptyMap()
    val byPath = searchMedia(DataType.AUDIO, "", LIBRARY_SCAN_LIMIT, 0, FileSortBy.DATE_DESC)
        .filterIsInstance<DAudio>()
        .associateBy { it.path }
    val updates = blanks.mapNotNull { row ->
        val albumId = byPath[row.audioPath]?.albumId ?: return@mapNotNull null
        if (albumId.isNotBlank()) row.id to albumId else null
    }
    if (updates.isNotEmpty()) LegacyAudioPlaylistManager.updatePlaylistItemAlbums(updates)
    return updates.toMap()
}

private const val LIBRARY_SCAN_LIMIT = 5000
