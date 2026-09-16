package com.ismartcoding.plain.db

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Upsert
import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant

/** Recently played tracks, the data source of the home "最近" section. */
@Entity(tableName = "audio_play_history", indices = [Index(value = ["played_at"])])
data class DAudioPlayHistory(
    @PrimaryKey
    @ColumnInfo(name = "path")
    val path: String,
    @ColumnInfo(name = "title")
    var title: String,
    @ColumnInfo(name = "artist")
    var artist: String,
    @ColumnInfo(name = "duration")
    var duration: Long,
    @ColumnInfo(name = "played_at")
    var playedAt: Instant = TimeHelper.now(),
)

@Dao
interface AudioPlayHistoryDao {
    @Query("SELECT * FROM audio_play_history ORDER BY played_at DESC LIMIT :limit OFFSET :offset")
    suspend fun page(limit: Int, offset: Int): List<DAudioPlayHistory>

    @Query("SELECT COUNT(*) FROM audio_play_history")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(item: DAudioPlayHistory)

    @Query("DELETE FROM audio_play_history WHERE path IN (:paths)")
    suspend fun deleteByPaths(paths: List<String>)

    /** Keep only the newest :keep rows. */
    @Query(
        "DELETE FROM audio_play_history WHERE path NOT IN (" +
            "SELECT path FROM audio_play_history ORDER BY played_at DESC LIMIT :keep)"
    )
    suspend fun trim(keep: Int)
}
