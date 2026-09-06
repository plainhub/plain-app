package com.ismartcoding.plain.db

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant

@Entity(tableName = "video_play_progress")
data class DVideoPlayProgress(
    @PrimaryKey
    @ColumnInfo(name = "media_id")
    val mediaId: String,
    @ColumnInfo(name = "duration")
    val duration: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = TimeHelper.now(),
)

@Dao
interface VideoPlayProgressDao {
    @Query("SELECT * FROM video_play_progress WHERE updated_at >= :since")
    suspend fun getRecentProgress(since: String): List<DVideoPlayProgress>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: DVideoPlayProgress)

    @Query("DELETE FROM video_play_progress WHERE media_id = :mediaId")
    suspend fun deleteByMediaId(mediaId: String)
}
