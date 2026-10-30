package com.ismartcoding.plain.ui.audio.playlists.components

import com.ismartcoding.plain.models.Audio as AudioTrack
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.models.AudioItem
import com.ismartcoding.plain.lib.withIO

private const val TRASHED_LOOKUP_LIMIT = 10_000

/** Paths of audio files sitting in MediaStore trash; empty on platforms without trash. */
private suspend fun trashedAudioPaths(source: com.ismartcoding.plain.rust.graphql.ContentSource): Set<String> = withIO {
    source.catalog.rows(DataType.AUDIO, "trash:true", TRASHED_LOOKUP_LIMIT, 0, FileSortBy.DATE_DESC)
        .filterIsInstance<AudioTrack>()
        .map { it.path }
        .toSet()
}

/**
 * Drops rows whose audio is trashed and prunes them from the playlist DB, so
 * count, covers and playback agree with the visible list. Trashing cascades at
 * trash time; this heals rows orphaned before that.
 */
internal suspend fun List<AudioItem>.pruneTrashedRows(playlistId: String, source: com.ismartcoding.plain.rust.graphql.ContentSource): List<AudioItem> {
    if (isEmpty()) return this
    val trashed = trashedAudioPaths(source)
    if (trashed.isEmpty()) return this
    val orphanPaths = filter { it.path in trashed }.mapNotNull { it.mediaId }
    if (orphanPaths.isEmpty()) return this
    withIO { source.playlists.removePlaylistItems(playlistId, orphanPaths) }
    return filterNot { it.path in trashed }
}
