package com.ismartcoding.plain.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.room.RoomRawQuery
import com.ismartcoding.plain.data.IDData
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.helpers.generateId
import kotlin.time.Instant

@Entity(tableName = "notes")
data class DNote(
    @PrimaryKey override var id: String = generateId(),
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
    fun getIds(query: RoomRawQuery): List<IDData>

    @RawQuery
    fun search(query: RoomRawQuery): List<DNote>

    @RawQuery
    fun delete(query: RoomRawQuery): Int

    @RawQuery
    fun count(query: RoomRawQuery): Int

    @Query("SELECT * FROM notes WHERE id=:id")
    fun getById(id: String): DNote?

    @Query("UPDATE notes SET deleted_at=:deletedAt, updated_at=:updatedAt WHERE id in (:ids)")
    fun trash(ids: Set<String>, deletedAt: Instant?, updatedAt: Instant)

    @Insert
    fun insert(vararg item: DNote)

    @Update
    fun update(vararg item: DNote)

    @Query("DELETE FROM notes WHERE id in (:ids)")
    fun delete(ids: Set<String>)
}
