package com.ismartcoding.plain.db

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant

@Entity(
    tableName = "audio_playlist_items",
    indices = [
        Index(value = ["playlist_id", "sort_order"]),
        Index(value = ["playlist_id", "audio_path"], unique = true),
    ],
)
data class DAudioPlaylistItem(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "playlist_id")
    var playlistId: String,
    @ColumnInfo(name = "audio_path")
    var audioPath: String,
    // Snapshot columns let lists render without touching MediaStore; playback
    // still resolves the file live from audioPath.
    @ColumnInfo(name = "title")
    var title: String,
    @ColumnInfo(name = "artist")
    var artist: String,
    // MediaStore album id snapshot for the playlist cover mosaic; blank for
    // rows written before the column existed (backfilled on first load).
    @ColumnInfo(name = "album_id", defaultValue = "")
    var albumId: String = "",
    @ColumnInfo(name = "duration_ms")
    var durationMs: Long,
    @ColumnInfo(name = "sort_order")
    var sortOrder: Int,
    @ColumnInfo(name = "added_at")
    var addedAt: Instant = TimeHelper.now(),
)

@Dao
interface AudioPlaylistItemDao {
    @Query(
        "SELECT * FROM audio_playlist_items WHERE playlist_id = :playlistId " +
            "ORDER BY sort_order LIMIT :limit OFFSET :offset"
    )
    suspend fun pageByPlaylist(playlistId: String, limit: Int, offset: Int): List<DAudioPlaylistItem>

    @Query(
        "SELECT * FROM audio_playlist_items WHERE playlist_id = :playlistId AND (title LIKE :text OR artist LIKE :text OR audio_path LIKE :text) " +
            "ORDER BY sort_order LIMIT :limit OFFSET :offset",
    )
    suspend fun pageByPlaylistText(playlistId: String, text: String, limit: Int, offset: Int): List<DAudioPlaylistItem>

    @Query("SELECT * FROM audio_playlist_items WHERE playlist_id = :playlistId ORDER BY sort_order")
    suspend fun getByPlaylist(playlistId: String): List<DAudioPlaylistItem>

    @Query("SELECT * FROM audio_playlist_items WHERE playlist_id = :playlistId AND audio_path = :path")
    suspend fun getByPath(playlistId: String, path: String): DAudioPlaylistItem?

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM audio_playlist_items WHERE playlist_id = :playlistId")
    suspend fun maxSortOrder(playlistId: String): Int

    @Query("SELECT COUNT(*) FROM audio_playlist_items WHERE playlist_id = :playlistId")
    suspend fun countByPlaylist(playlistId: String): Int


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<DAudioPlaylistItem>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: DAudioPlaylistItem): Long

    @Query("UPDATE audio_playlist_items SET sort_order = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int)

    @Query("UPDATE audio_playlist_items SET album_id = :albumId WHERE id = :id")
    suspend fun updateAlbumId(id: String, albumId: String)

    @Query("DELETE FROM audio_playlist_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM audio_playlist_items WHERE playlist_id = :playlistId AND audio_path = :path")
    suspend fun deleteByPath(playlistId: String, path: String)

    @Query("DELETE FROM audio_playlist_items WHERE playlist_id = :playlistId AND audio_path IN (:paths)")
    suspend fun deleteByPlaylistPaths(playlistId: String, paths: List<String>)

    @Query("DELETE FROM audio_playlist_items WHERE playlist_id = :playlistId")
    suspend fun deleteByPlaylist(playlistId: String)

    @Query("DELETE FROM audio_playlist_items WHERE audio_path IN (:paths)")
    suspend fun deleteByPaths(paths: List<String>)
}
