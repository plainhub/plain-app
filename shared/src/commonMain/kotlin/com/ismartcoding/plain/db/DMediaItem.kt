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

/**
 * Cached duration for a MediaStore item whose MediaStore.DURATION is 0 (e.g.
 * fragmented-MP4 files where MediaMetadataRetriever fails). The MediaStore
 * DURATION column is read-only on Android 10+, so we cannot write back; instead
 * we persist the computed duration here and merge it during list queries.
 *
 * Duration is stored in **seconds** to match DVideo/DAudio.duration's unit,
 * so list queries can use the cached value directly without conversion.
 *
 * `media_id` is the single primary key — MediaStore _ID is globally unique
 * across video/audio content URIs in practice (same pattern as
 * DVideoPlayProgress). This also ensures the developer DB view's idKey
 * resolves to `media_id` (not `media_type`) for correct row deletion.
 */
@Entity(tableName = "media_item")
data class DMediaItem(
    @ColumnInfo(name = "media_type")
    val mediaType: String, // "video" | "audio"
    @PrimaryKey
    @ColumnInfo(name = "media_id")
    val mediaId: String,
    @ColumnInfo(name = "duration")
    val duration: Long, // seconds
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = TimeHelper.now(),
)

@Dao
interface MediaItemDao {
    @Query("SELECT * FROM media_item")
    suspend fun getAll(): List<DMediaItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: DMediaItem)

    @Query("DELETE FROM media_item WHERE media_id = :mediaId")
    suspend fun delete(mediaId: String)
}
