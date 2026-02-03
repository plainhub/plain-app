package com.ismartcoding.plain.db

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.ismartcoding.lib.extensions.getSummary
import com.ismartcoding.lib.helpers.StringHelper
import com.ismartcoding.plain.data.IDData
import com.ismartcoding.plain.data.IData
import kotlinx.datetime.Instant
import com.ismartcoding.plain.helpers.TimeHelper
import kotlinx.serialization.Serializable

// https://validator.w3.org/feed/docs/rss2.html
// https://validator.w3.org/feed/docs/atom.html
@Entity(tableName = "feed_entries")
@Serializable
data class DFeedEntry(
    @PrimaryKey override var id: String = StringHelper.shortUUID(),
) : IData, DEntityBase() {
    var title: String = ""
    var url: String = ""

    var image: String = ""
    var description: String = ""
    var author: String = ""
    var content: String = ""

    @ColumnInfo(name = "feed_id", index = true)
    var feedId: String = ""

    @ColumnInfo(name = "raw_id", index = true)
    var rawId: String = ""

    @ColumnInfo(name = "published_at")
    var publishedAt: Instant = TimeHelper.now()

    @ColumnInfo(name = "read")
    var read: Boolean = false

    fun getSummary(): String {
        return description.getSummary()
    }
}

@Dao
interface FeedEntryDao {
    @Query("SELECT * FROM feed_entries")
    fun getAll(): List<DFeedEntry>

    @RawQuery
    fun getIds(query: SupportSQLiteQuery): List<IDData>

    @RawQuery
    fun search(query: SupportSQLiteQuery): List<DFeedEntry>

    @RawQuery
    fun count(query: SupportSQLiteQuery): Int

    @Query("SELECT * FROM feed_entries WHERE id=:id")
    fun getById(id: String): DFeedEntry?

    @Insert
    fun insert(vararg item: DFeedEntry)

    @Update
    fun update(vararg item: DFeedEntry)

    @Query("DELETE FROM feed_entries")
    fun deleteAll()

    @Query("DELETE FROM feed_entries WHERE id in (:ids)")
    fun delete(ids: Set<String>)

    @Query("DELETE FROM feed_entries WHERE feed_id in (:ids)")
    fun deleteByFeedIds(ids: Set<String>)

    @Query("SELECT * from feed_entries WHERE url=:url AND feed_id=:feedId")
    fun getByUrl(
        url: String,
        feedId: String,
    ): DFeedEntry?

    @Query("SELECT id from feed_entries WHERE feed_id in (:ids)")
    fun getIds(ids: Set<String>): List<String>

    @Insert
    fun insertList(entries: List<DFeedEntry>): List<Long>

    @Transaction
    fun insertListIfNotExist(entries: List<DFeedEntry>): List<DFeedEntry> {
        return entries.mapNotNull {
            if (getByUrl(
                    url = it.url,
                    feedId = it.feedId,
                ) == null
            ) {
                it
            } else {
                null
            }
        }.also {
            insertList(it)
        }
    }
}
