package com.ismartcoding.plain.db

import androidx.room3.Dao
import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant
import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Ignore
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RawQuery
import androidx.room3.RoomRawQuery
import androidx.room3.Update
import com.ismartcoding.plain.lib.generateId

@Entity(
    tableName = "feeds",
    indices = [(Index(value = ["url"], unique = true))],
)
data class DFeed(
    @PrimaryKey override var id: String = generateId(),
    var name: String = "",
    var url: String = "",

    @androidx.room3.ColumnInfo(name = "fetch_content")
    var fetchContent: Boolean = false,

    @Ignore
    var count: Int = 0,

    @ColumnInfo(name = "created_at")
    var createdAt: Instant = TimeHelper.now(),

    @ColumnInfo(name = "updated_at")
    var updatedAt: Instant = TimeHelper.now(),
) : IData

@Dao
interface FeedDao {
    @Query("SELECT * FROM feeds")
    suspend fun getAll(): List<DFeed>

    @RawQuery
    suspend fun search(query: RoomRawQuery): List<DFeed>

    @RawQuery
    suspend fun count(query: RoomRawQuery): Int

    @Query("SELECT * FROM feeds WHERE id=:id")
    suspend fun getById(id: String): DFeed?

    @Query("SELECT * FROM feeds WHERE url=:url")
    suspend fun getByUrl(url: String): DFeed?

    @Insert
    suspend fun insert(vararg item: DFeed)

    @Update
    suspend fun update(vararg item: DFeed)

    @Query("DELETE FROM feeds WHERE id in (:ids)")
    suspend fun delete(ids: Set<String>)

    @Query(
        "SELECT feed_entries.feed_id AS id, count(feed_entries.feed_id) AS count FROM feed_entries GROUP BY feed_entries.feed_id",
    )
    suspend fun getFeedCounts(): List<DFeedCount>
}

data class DFeedCount(var id: String, var count: Int)
