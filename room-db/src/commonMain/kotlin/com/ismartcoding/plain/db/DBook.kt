package com.ismartcoding.plain.db

import androidx.room3.Dao
import androidx.room3.ColumnInfo
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

@Entity(tableName = "books")
data class DBook(
    @PrimaryKey override var id: String = generateId(),
    var name: String = "",
    var author: String = "",
    var image: String = "",
    var description: String = "",

    @ColumnInfo(name = "created_at")
    var createdAt: Instant = TimeHelper.now(),

    @ColumnInfo(name = "updated_at")
    var updatedAt: Instant = TimeHelper.now(),
) : IData

@Dao
interface BookDao {
    @Query("SELECT * FROM books")
    suspend fun getAll(): List<DBook>

    @RawQuery
    suspend fun search(query: RoomRawQuery): List<DBook>

    @RawQuery
    suspend fun count(query: RoomRawQuery): Int

    @Query("SELECT * FROM books WHERE id=:id")
    suspend fun getById(id: String): DBook?

    @Insert
    suspend fun insert(vararg item: DBook)

    @Update
    suspend fun update(vararg item: DBook)

    @Query("DELETE FROM books WHERE id in (:ids)")
    suspend fun delete(ids: Set<String>)
}
