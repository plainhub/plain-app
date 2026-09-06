package com.ismartcoding.plain.db

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RawQuery
import androidx.room3.RoomRawQuery
import androidx.room3.Update
import com.ismartcoding.plain.lib.generateId

@Entity(tableName = "book_chapters")
data class DBookChapter(
    @PrimaryKey override var id: String = generateId(),
    var name: String = "",

    @ColumnInfo(name = "book_id")
    var bookId: String = "",

    @ColumnInfo(name = "parent_id")
    var parentId: String = "",

    var content: String = "",

    @ColumnInfo(name = "display_order")
    var displayOrder: Int = 0,

    @ColumnInfo(name = "created_at")
    var createdAt: Instant = TimeHelper.now(),

    @ColumnInfo(name = "updated_at")
    var updatedAt: Instant = TimeHelper.now(),
) : IData

@Dao
interface BookChapterDao {
    @Query("SELECT * FROM book_chapters WHERE book_id=:bookId")
    suspend fun getAll(bookId: String): List<DBookChapter>

    @RawQuery
    suspend fun search(query: RoomRawQuery): List<DBookChapter>

    @RawQuery
    suspend fun count(query: RoomRawQuery): Int

    @Query("SELECT * FROM book_chapters WHERE id=:id")
    suspend fun getById(id: String): DBookChapter?

    @Insert
    suspend fun insert(vararg item: DBookChapter)

    @Update
    suspend fun update(vararg item: DBookChapter)

    @Query("DELETE FROM book_chapters WHERE id in (:ids)")
    suspend fun delete(ids: Set<String>)
}
