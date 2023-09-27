package com.ismartcoding.plain.db

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.ismartcoding.lib.helpers.StringHelper
import com.ismartcoding.plain.data.IData
import kotlinx.datetime.*

@Entity(
    tableName = "book_chapters",
)
data class DBookChapter(
    @PrimaryKey override var id: String = StringHelper.shortUUID(),
) : IData, DEntityBase() {
    var name: String = ""

    @ColumnInfo(name = "book_id")
    var bookId: String = ""

    @ColumnInfo(name = "parent_id")
    var parentId: String = ""

    var content: String = ""

    @ColumnInfo(name = "display_order")
    var displayOrder: Int = 0
}

@Dao
interface BookChapterDao {
    @Query("SELECT id,name,parent_id FROM book_chapters WHERE book_id=:bookId")
    fun getAll(bookId: String): List<DBookChapter>

    @RawQuery
    fun search(query: SupportSQLiteQuery): List<DBookChapter>

    @RawQuery
    fun count(query: SupportSQLiteQuery): Int

    @Query("SELECT * FROM book_chapters WHERE id=:id")
    fun getById(id: String): DBookChapter?

    @Insert
    fun insert(vararg item: DBookChapter)

    @Update
    fun update(vararg item: DBookChapter)

    @Query("DELETE FROM book_chapters WHERE id in (:ids)")
    fun delete(ids: Set<String>)
}
