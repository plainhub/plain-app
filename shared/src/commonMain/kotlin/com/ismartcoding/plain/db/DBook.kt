package com.ismartcoding.plain.db

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

@Entity(tableName = "books")
data class DBook(
    @PrimaryKey override var id: String = generateId(),
) : IData, DEntityBase() {
    var name: String = ""
    var author: String = ""
    var image: String = ""
    var description: String = ""
}

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
