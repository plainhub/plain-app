package com.ismartcoding.plain.db

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Upsert
import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant

@Entity(tableName = "audio_playlists")
data class DAudioPlaylist(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name")
    var name: String,
    @ColumnInfo(name = "created_at")
    var createdAt: Instant = TimeHelper.now(),
    @ColumnInfo(name = "updated_at")
    var updatedAt: Instant = TimeHelper.now(),
)

data class AudioPlaylistSongCount(
    val playlistId: String,
    val cnt: Int,
)

@Dao
interface AudioPlaylistDao {
    @Query("SELECT * FROM audio_playlists ORDER BY updated_at DESC")
    suspend fun getAll(): List<DAudioPlaylist>

    @Query("SELECT * FROM audio_playlists WHERE id = :id")
    suspend fun getById(id: String): DAudioPlaylist?

    @Upsert
    suspend fun upsert(item: DAudioPlaylist)

    @Query("UPDATE audio_playlists SET updated_at = :at WHERE id = :id")
    suspend fun touch(id: String, at: Instant)

    @Query("DELETE FROM audio_playlists WHERE id = :id")
    suspend fun delete(id: String)

    @Query(
        "SELECT s.playlist_id AS playlistId, COUNT(*) AS cnt FROM audio_playlist_songs s " +
            "GROUP BY s.playlist_id"
    )
    suspend fun songCounts(): List<AudioPlaylistSongCount>
}
