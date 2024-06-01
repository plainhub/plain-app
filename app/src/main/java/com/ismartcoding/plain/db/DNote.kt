package com.ismartcoding.plain.db

import android.os.Parcelable
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.ismartcoding.lib.helpers.StringHelper
import com.ismartcoding.plain.data.IDData
import com.ismartcoding.plain.data.IData
import kotlinx.datetime.*
import kotlinx.parcelize.Parcelize

@Entity(tableName = "notes")
data class DNote(
    @PrimaryKey override var id: String = StringHelper.shortUUID(),
) : IData, DEntityBase() {
    var title: String = ""

    @ColumnInfo(name = "deleted_at")
    var deletedAt: Instant? = null

    var content: String = ""

    fun getSummary(): String {
        return content.replace("\n", "").replaceFirst("^\\s*".toRegex(), "")
    }
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes")
    fun getAll(): List<DNote>

    @RawQuery
    fun getIds(query: SupportSQLiteQuery): List<IDData>

    @RawQuery
    fun search(query: SupportSQLiteQuery): List<DNote>

    @RawQuery
    fun delete(query: SupportSQLiteQuery): Int

    @RawQuery
    fun count(query: SupportSQLiteQuery): Int

    @Query("SELECT * FROM notes WHERE id=:id")
    fun getById(id: String): DNote?

    @Query("UPDATE notes SET deleted_at=:time WHERE id in (:ids)")
    fun trash(
        ids: Set<String>,
        time: Instant?,
    )

    @Insert
    fun insert(vararg item: DNote)

    @Update
    fun update(vararg item: DNote)

    @Query("DELETE FROM notes WHERE id in (:ids)")
    fun delete(ids: Set<String>)
}
