package com.ismartcoding.plain.ui.page.playlist

import com.ismartcoding.plain.db.DAudioPlaylistItem
import com.ismartcoding.plain.lib.withIO

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
 */
internal suspend fun loadPlaylistAlbumCovers(items: List<DAudioPlaylistItem>): List<PlaylistAlbumCover> = withIO {
    val byAlbum = LinkedHashMap<String, MutableList<DAudioPlaylistItem>>()
    items.forEach { row ->
        val album = row.albumId
        if (album.isNotBlank()) byAlbum.getOrPut(album) { mutableListOf() }.add(row)
    }
    byAlbum.entries
        .sortedByDescending { it.value.size }
        .take(MAX_ALBUMS)
        .map { (album, tracks) ->
            PlaylistAlbumCover(album, tracks.size, tracks.take(CANDIDATES_PER_ALBUM).map { it.audioPath })
        }
}

private const val MAX_ALBUMS = 8
private const val CANDIDATES_PER_ALBUM = 3
