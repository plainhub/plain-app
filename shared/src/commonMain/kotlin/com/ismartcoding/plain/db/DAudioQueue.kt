package com.ismartcoding.plain.db

/** Where the queue draws its tracks from. Stored as readable TEXT (enum name). */
enum class AudioPlaySource {
    /** No source — only manually queued items can play. */
    NONE,

    /** A user playlist ([DAudioQueueSource.playlistId]). */
    PLAYLIST,

    /** The whole audio library, ordered by [DAudioQueueSource.sortBy]. */
    LIBRARY,
}

/**
 * Single-row (id = 1) playback state: what is playing and where it comes from.
 * The queue itself is never materialized — next/previous are resolved from the
 * source on demand, so a 10k-track library costs the same as one track.
 */
data class DAudioQueueSource(
    val id: Int = 1,
    var source: AudioPlaySource = AudioPlaySource.NONE,
    /** Playlist id, meaningful only when source = PLAYLIST. */
    var playlistId: String = "",
    /** Path of the track currently playing. */
    var currentPath: String = "",
    /** Position of the current track inside the source, cached for fast skips.
     *  -1 = unknown (manual jump), re-located lazily on the next skip. */
    var currentIndex: Int = -1,
    /** FileSortBy name captured when a LIBRARY source was set. */
    var sortBy: String = "",
)

/** Manually queued tracks ("play next" / "add to queue"), ordered by [DAudioQueueItem.sortOrder]. */
data class DAudioQueueItem(
    val path: String,
    var sortOrder: Int,
    var title: String,
    var artist: String,
    var durationMs: Long,
)
