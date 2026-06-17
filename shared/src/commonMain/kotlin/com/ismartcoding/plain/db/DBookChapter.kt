package com.ismartcoding.plain.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Update
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.helpers.generateId

@Entity(tableName = "book_chapters")
data class DBookChapter(
    @PrimaryKey override var id: String = generateId(),
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
