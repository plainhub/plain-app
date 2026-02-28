package com.ismartcoding.plain.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.ismartcoding.lib.helpers.StringHelper

@Entity(tableName = "chat_channels")
data class DChatChannel(
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
interface ChatChannelDao {
    @Query("SELECT * FROM chat_channels")
    fun getAll(): List<DChatChannel>

    @Query("SELECT * FROM chat_channels WHERE id = :id")
    fun getById(id: String): DChatChannel?

    @Insert
    fun insert(vararg item: DChatChannel)

    @Update
    fun update(vararg item: DChatChannel)

    @Query("DELETE FROM chat_channels WHERE id = :id")
    fun delete(id: String)

    @Query("DELETE FROM chat_channels WHERE id in (:ids)")
    fun deleteByIds(ids: List<String>)
} 