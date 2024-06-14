package com.ismartcoding.plain.db

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.ismartcoding.lib.extensions.getSummary
import com.ismartcoding.lib.helpers.StringHelper
import com.ismartcoding.plain.data.IDData
import com.ismartcoding.plain.data.IData
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

// https://validator.w3.org/feed/docs/rss2.html
// https://validator.w3.org/feed/docs/atom.html
@Entity(tableName = "feed_entries_fts")
@Serializable
@Fts4
data class DFeedEntryFts(
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
    var publishedAt: Instant = Clock.System.now()

    @ColumnInfo(name = "read")
    var read: Boolean = false

    fun getSummary(): String {
        return description.getSummary()
    }
}

@Dao
interface FeedEntryFtsDao {
    @Query("SELECT * FROM feed_entries_fts")
    fun getAll(): List<DFeedEntryFts>

    @RawQuery
    fun getIds(query: SupportSQLiteQuery): List<IDData>

    @RawQuery
    fun search(query: SupportSQLiteQuery): List<DFeedEntryFts>

    @RawQuery
    fun count(query: SupportSQLiteQuery): Int

    @Query("SELECT * FROM feed_entries_fts WHERE id=:id")
    fun getById(id: String): DFeedEntryFts?

    @Insert
    fun insert(vararg item: DFeedEntryFts)

    @Update
    fun update(vararg item: DFeedEntryFts)

    @Query("DELETE FROM feed_entries_fts")
    fun deleteAll()

    @Query("DELETE FROM feed_entries_fts WHERE id in (:ids)")
    fun delete(ids: Set<String>)

    @Query("DELETE FROM feed_entries_fts WHERE feed_id in (:ids)")
    fun deleteByFeedIds(ids: Set<String>)

    @Query("SELECT * from feed_entries_fts WHERE url=:url AND feed_id=:feedId")
    fun getByUrl(
        url: String,
        feedId: String,
    ): DFeedEntryFts?

    @Query("SELECT id from feed_entries_fts WHERE feed_id in (:ids)")
    fun getIds(ids: Set<String>): List<String>

    @Insert
    fun insertList(entries: List<DFeedEntryFts>): List<Long>

    @Transaction
    fun insertListIfNotExist(entries: List<DFeedEntryFts>): List<DFeedEntryFts> {
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
