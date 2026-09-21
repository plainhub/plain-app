package com.ismartcoding.plain.ui.page.playlist

import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.db.DAudioPlaylistItem
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.audio.AudioPlaylistManager
import com.ismartcoding.plain.features.audio.AudioQueueManager
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.searchMedia

/** One album inside a playlist: how many tracks it contributes and where a cover may come from. */
data class PlaylistAlbumCover(
    val albumId: String,
    val trackCount: Int,
    /** Track paths in playlist order; the first with embedded art becomes the tile. */
    val candidatePaths: List<String>,
)

/**
 * Groups playlist tracks by album (MediaStore album id snapshot) and returns
 * the albums carrying the most tracks, up to
 * [MAX_ALBUMS]—[PlaylistMosaicCover] then keeps the first four that actually
 * decode a cover.
 *
 * Rows written before the album snapshot column existed resolve against the
 * audio library once and are backfilled, so the cost is paid a single time
 * per legacy playlist.
 */
internal suspend fun loadPlaylistAlbumCovers(items: List<DAudioPlaylistItem>): List<PlaylistAlbumCover> = withIO {
    val resolved = resolveBlankAlbums(items)
    val byAlbum = LinkedHashMap<String, MutableList<DAudioPlaylistItem>>()
    items.forEach { row ->
        val album = row.albumId.ifBlank { resolved[row.id].orEmpty() }
        if (album.isNotBlank()) byAlbum.getOrPut(album) { mutableListOf() }.add(row)
    }
    byAlbum.entries
        .sortedByDescending { it.value.size }
        .take(MAX_ALBUMS)
        .map { (album, tracks) ->
            PlaylistAlbumCover(album, tracks.size, tracks.take(CANDIDATES_PER_ALBUM).map { it.audioPath })
        }
}

/** Row id -> album id for previously-stored rows lacking the snapshot, after backfilling them. */
private suspend fun resolveBlankAlbums(items: List<DAudioPlaylistItem>): Map<String, String> {
    val blanks = items.filter { it.albumId.isBlank() }
    if (blanks.isEmpty()) return emptyMap()
    val byPath = searchMedia(DataType.AUDIO, "", LIBRARY_SCAN_LIMIT, 0, FileSortBy.DATE_DESC)
        .filterIsInstance<DAudio>()
        .associateBy { it.path }
    val updates = blanks.mapNotNull { row ->
        val albumId = byPath[row.audioPath]?.albumId ?: return@mapNotNull null
        if (albumId.isNotBlank()) row.id to albumId else null
    }
    if (updates.isNotEmpty()) AudioPlaylistManager.updatePlaylistItemAlbums(updates)
    return updates.toMap()
}

private const val MAX_ALBUMS = 8
private const val CANDIDATES_PER_ALBUM = 3
private const val LIBRARY_SCAN_LIMIT = 5000
