package com.ismartcoding.plain.db

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import com.ismartcoding.plain.lib.TimeHelper
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RawQuery
import androidx.room3.Update
import androidx.room3.RoomRawQuery
import com.ismartcoding.plain.lib.generateId
import kotlin.time.Instant

@Entity(tableName = "notes")
data class DNote(
    @PrimaryKey override var id: String = generateId(),
    var title: String = "",

    @ColumnInfo(name = "deleted_at")
    var deletedAt: Instant? = null,

    var content: String = "",

    @ColumnInfo(name = "created_at")
    var createdAt: Instant = TimeHelper.now(),

    @ColumnInfo(name = "updated_at")
    var updatedAt: Instant = TimeHelper.now(),
) : IData {
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
