package com.ismartcoding.plain.db

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Upsert

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
@Entity(tableName = "audio_queue_source")
data class DAudioQueueSource(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1,
    @ColumnInfo(name = "source")
    var source: AudioPlaySource = AudioPlaySource.NONE,
    /** Playlist id, meaningful only when source = PLAYLIST. */
    @ColumnInfo(name = "playlist_id")
    var playlistId: String = "",
    /** Path of the track currently playing. */
    @ColumnInfo(name = "current_path")
    var currentPath: String = "",
    /** Position of the current track inside the source, cached for fast skips.
     *  -1 = unknown (manual jump), re-located lazily on the next skip. */
    @ColumnInfo(name = "current_index")
    var currentIndex: Int = -1,
    /** FileSortBy name captured when a LIBRARY source was set. */
    @ColumnInfo(name = "sort_by")
    var sortBy: String = "",
)

/** Manually queued tracks ("play next" / "add to queue"), ordered by [DAudioQueueItem.sortOrder]. */
@Entity(tableName = "audio_queue_items", indices = [Index(value = ["sort_order"])])
data class DAudioQueueItem(
    @PrimaryKey
    @ColumnInfo(name = "path")
    val path: String,
    @ColumnInfo(name = "sort_order")
    var sortOrder: Int,
    @ColumnInfo(name = "title")
    var title: String,
    @ColumnInfo(name = "artist")
    var artist: String,
    @ColumnInfo(name = "duration_ms")
    var durationMs: Long,
)

@Dao
interface AudioQueueDao {
    // ---------- source ----------

    @Query("SELECT * FROM audio_queue_source WHERE id = 1")
    suspend fun getSource(): DAudioQueueSource?

    @Upsert
    suspend fun putSource(source: DAudioQueueSource)

    // ---------- manual queue items ----------

    @Query("SELECT * FROM audio_queue_items ORDER BY sort_order")
    suspend fun allItems(): List<DAudioQueueItem>

    @Query("SELECT * FROM audio_queue_items ORDER BY sort_order LIMIT :limit OFFSET :offset")
    suspend fun itemsPage(limit: Int, offset: Int): List<DAudioQueueItem>

    @Query("SELECT * FROM audio_queue_items WHERE path = :path")
    suspend fun itemByPath(path: String): DAudioQueueItem?

    /** The item at [rank] in sort order (sort orders have gaps after deletes). */
    @Query("SELECT * FROM audio_queue_items ORDER BY sort_order LIMIT 1 OFFSET :rank")
    suspend fun itemAt(rank: Int): DAudioQueueItem?

    @Query("SELECT COUNT(*) FROM audio_queue_items WHERE sort_order < :sortOrder")
    suspend fun countBefore(sortOrder: Int): Int

    @Query("SELECT COUNT(*) FROM audio_queue_items")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM audio_queue_items")
    suspend fun maxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<DAudioQueueItem>)

    @Upsert
    suspend fun upsert(item: DAudioQueueItem)

    @Query("UPDATE audio_queue_items SET sort_order = :sortOrder WHERE path = :path")
    suspend fun updateSortOrder(path: String, sortOrder: Int)

    @Query("UPDATE audio_queue_items SET sort_order = sort_order + :delta WHERE sort_order >= :from")
    suspend fun shiftSortOrdersFrom(from: Int, delta: Int)

    @Query("DELETE FROM audio_queue_items WHERE path = :path")
    suspend fun deleteByPath(path: String)

    @Query("DELETE FROM audio_queue_items WHERE path IN (:paths)")
    suspend fun deleteByPaths(paths: List<String>)

    @Query("DELETE FROM audio_queue_items")
    suspend fun deleteAll()
}
