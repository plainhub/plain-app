package com.ismartcoding.plain.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.ismartcoding.lib.helpers.StringHelper

@Entity(tableName = "chat_groups")
data class DChatGroup(
    @PrimaryKey var id: String = StringHelper.shortUUID(),
) : DEntityBase() {
    @ColumnInfo(name = "name")
    var name: String = ""

    @ColumnInfo(name = "key")
    var key: String = ""

    @ColumnInfo(name = "members")
    var members: ArrayList<String> = arrayListOf()
}

@Dao
interface ChatGroupDao {
    @Query("SELECT * FROM chat_groups")
    fun getAll(): List<DChatGroup>

    @Query("SELECT * FROM chat_groups WHERE id = :id")
    fun getById(id: String): DChatGroup?

    @Insert
    fun insert(vararg item: DChatGroup)

    @Update
    fun update(vararg item: DChatGroup)

    @Query("DELETE FROM chat_groups WHERE id = :id")
    fun delete(id: String)

    @Query("DELETE FROM chat_groups WHERE id in (:ids)")
    fun deleteByIds(ids: List<String>)
} 