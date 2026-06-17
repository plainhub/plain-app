package com.ismartcoding.plain.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.helpers.generateId

@Entity(tableName = "tags")
data class DTag(
    @PrimaryKey override var id: String = generateId(),
    var name: String = "",
    var type: Int = 0,
    var count: Int = 0,
) : IData, DEntityBase()

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
