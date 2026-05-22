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
    fun getAll(): List<DBook>

    @RawQuery
    fun search(query: RoomRawQuery): List<DBook>

    @RawQuery
    fun count(query: RoomRawQuery): Int

    @Query("SELECT * FROM books WHERE id=:id")
    fun getById(id: String): DBook?

    @Insert
    fun insert(vararg item: DBook)

    @Update
    fun update(vararg item: DBook)

    @Query("DELETE FROM books WHERE id in (:ids)")
    fun delete(ids: Set<String>)
}
