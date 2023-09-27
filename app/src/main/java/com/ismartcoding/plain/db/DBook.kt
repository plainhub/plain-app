package com.ismartcoding.plain.db

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.ismartcoding.lib.helpers.StringHelper
import com.ismartcoding.plain.data.IData
import kotlinx.datetime.*

@Entity(
    tableName = "books",
)
data class DBook(
    @PrimaryKey override var id: String = StringHelper.shortUUID(),
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
    fun search(query: SupportSQLiteQuery): List<DBook>

    @RawQuery
    fun count(query: SupportSQLiteQuery): Int

    @Query("SELECT * FROM books WHERE id=:id")
    fun getById(id: String): DBook?

    @Insert
    fun insert(vararg item: DBook)

    @Update
    fun update(vararg item: DBook)

    @Query("DELETE FROM books WHERE id in (:ids)")
    fun delete(ids: Set<String>)
}
