package com.ismartcoding.plain.db

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.ismartcoding.lib.helpers.StringHelper
import com.ismartcoding.plain.data.IData
import kotlinx.datetime.*

@Entity(
    tableName = "feeds",
    indices = [(Index(value = ["url"], unique = true))],
)
data class DFeed(
    @PrimaryKey override var id: String = StringHelper.shortUUID(),
) : IData, DEntityBase() {
    var name: String = ""
    var url: String = ""

    @ColumnInfo(name = "fetch_content")
    var fetchContent: Boolean = false
}

@Dao
interface FeedDao {
    @Query("SELECT * FROM feeds")
    fun getAll(): List<DFeed>

    @RawQuery
    fun search(query: SupportSQLiteQuery): List<DFeed>

    @RawQuery
    fun count(query: SupportSQLiteQuery): Int

    @Query("SELECT * FROM feeds WHERE id=:id")
    fun getById(id: String): DFeed?

    @Query("SELECT * FROM feeds WHERE url=:url")
    fun getByUrl(url: String): DFeed?

    @Insert
    fun insert(vararg item: DFeed)

    @Update
    fun update(vararg item: DFeed)

    @Query("DELETE FROM feeds WHERE id in (:ids)")
    fun delete(ids: Set<String>)
}
