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
    tableName = "audio_playlist_songs",
    indices = [
        Index(value = ["playlist_id", "position"]),
        Index(value = ["playlist_id", "audio_path"], unique = true),
    ],
)
data class DAudioPlaylistSong(
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
    @ColumnInfo(name = "duration")
    var duration: Long,
    @ColumnInfo(name = "position")
    var position: Int,
    @ColumnInfo(name = "added_at")
    var addedAt: Instant = TimeHelper.now(),
)

@Dao
interface AudioPlaylistSongDao {
    @Query(
        "SELECT * FROM audio_playlist_songs WHERE playlist_id = :playlistId " +
            "ORDER BY position LIMIT :limit OFFSET :offset"
    )
    suspend fun pageByPlaylist(playlistId: String, limit: Int, offset: Int): List<DAudioPlaylistSong>

    @Query("SELECT * FROM audio_playlist_songs WHERE playlist_id = :playlistId ORDER BY position")
    suspend fun getByPlaylist(playlistId: String): List<DAudioPlaylistSong>

    @Query("SELECT * FROM audio_playlist_songs WHERE playlist_id = :playlistId AND audio_path = :path")
    suspend fun getByPath(playlistId: String, path: String): DAudioPlaylistSong?

    @Query("SELECT COALESCE(MAX(position), -1) FROM audio_playlist_songs WHERE playlist_id = :playlistId")
    suspend fun maxPosition(playlistId: String): Int

    @Query("SELECT COUNT(*) FROM audio_playlist_songs WHERE playlist_id = :playlistId")
    suspend fun countByPlaylist(playlistId: String): Int


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(songs: List<DAudioPlaylistSong>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(song: DAudioPlaylistSong): Long

    @Query("UPDATE audio_playlist_songs SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: String, position: Int)

    @Query("DELETE FROM audio_playlist_songs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM audio_playlist_songs WHERE playlist_id = :playlistId AND audio_path = :path")
    suspend fun deleteByPath(playlistId: String, path: String)

    @Query("DELETE FROM audio_playlist_songs WHERE playlist_id = :playlistId")
    suspend fun deleteByPlaylist(playlistId: String)

    @Query("DELETE FROM audio_playlist_songs WHERE audio_path IN (:paths)")
    suspend fun deleteByPaths(paths: List<String>)
}
