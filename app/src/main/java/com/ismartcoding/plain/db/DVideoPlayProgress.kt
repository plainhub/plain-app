package com.ismartcoding.plain.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.ismartcoding.plain.helpers.TimeHelper
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