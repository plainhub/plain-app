package com.ismartcoding.plain.db

import androidx.room3.Dao
import androidx.room3.ColumnInfo
import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Update
import com.ismartcoding.plain.lib.generateId

@Entity(tableName = "tags")
data class DTag(
    @PrimaryKey override var id: String = generateId(),
    var name: String = "",
    var type: Int = 0,
    var count: Int = 0,

    @ColumnInfo(name = "created_at")
    var createdAt: Instant = TimeHelper.now(),

    @ColumnInfo(name = "updated_at")
    var updatedAt: Instant = TimeHelper.now(),
) : IData

@Dao
interface TagDao {
    @Query("SELECT * FROM tags WHERE `type`=:type")
    suspend fun getAll(type: Int): List<DTag>

    @Query("SELECT * FROM tags WHERE id=:id")
    suspend fun getById(id: String): DTag?

    @Insert
    suspend fun insert(vararg item: DTag)

    @Update
    suspend fun update(vararg item: DTag)

    @Query("DELETE FROM tags WHERE id=:id")
    suspend fun delete(id: String)
}
