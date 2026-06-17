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
    suspend fun getAll(): List<DNote>

    @RawQuery
    suspend fun getIds(query: RoomRawQuery): List<IDData>

    @RawQuery
    suspend fun search(query: RoomRawQuery): List<DNote>

    @RawQuery
    suspend fun delete(query: RoomRawQuery): Int

    @RawQuery
    suspend fun count(query: RoomRawQuery): Int

    @Query("SELECT * FROM notes WHERE id=:id")
    suspend fun getById(id: String): DNote?

    @Query("UPDATE notes SET deleted_at=:deletedAt, updated_at=:updatedAt WHERE id in (:ids)")
    suspend fun trash(ids: Set<String>, deletedAt: Instant?, updatedAt: Instant)

    @Insert
    suspend fun insert(vararg item: DNote)

    @Update
    suspend fun update(vararg item: DNote)

    @Query("DELETE FROM notes WHERE id in (:ids)")
    suspend fun delete(ids: Set<String>)
}
