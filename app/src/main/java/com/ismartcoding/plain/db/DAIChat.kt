package com.ismartcoding.plain.db

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.ismartcoding.lib.helpers.StringHelper
import com.ismartcoding.plain.data.IDData
import com.ismartcoding.plain.data.IData
import kotlinx.datetime.*

@Entity(
    tableName = "aichats",
)
data class DAIChat(
    @PrimaryKey override var id: String = StringHelper.shortUUID(),
) : IData, DEntityBase() {
    @ColumnInfo(name = "parent_id")
    var parentId: String = ""

    @ColumnInfo(name = "is_me")
    var isMe: Boolean = false
    var content: String = ""
    var type: Int = 1 // 1 means ChatGPT, reserved field
}

@Dao
interface AIChatDao {
    @Query("SELECT * FROM aichats")
    fun getAll(): List<DAIChat>

    @RawQuery
    fun getIds(query: SupportSQLiteQuery): List<IDData>

    @RawQuery
    fun search(query: SupportSQLiteQuery): List<DAIChat>

    @RawQuery
    fun delete(query: SupportSQLiteQuery): Int

    @RawQuery
    fun count(query: SupportSQLiteQuery): Int

    @Query("SELECT * FROM aichats WHERE parent_id=:parentId")
    fun getByParentId(parentId: String): List<DAIChat>

    @Query("SELECT * FROM aichats WHERE id=:parentId OR parent_id=:parentId")
    fun getChats(parentId: String): List<DAIChat>

    @Query("SELECT * FROM aichats WHERE id=:id")
    fun getById(id: String): DAIChat?

    @Insert
    fun insert(vararg item: DAIChat)

    @Update
    fun update(vararg item: DAIChat)

    @Query("DELETE FROM aichats WHERE id in (:ids)")
    fun delete(ids: Set<String>)

    @Query("DELETE FROM aichats WHERE parent_id in (:ids)")
    fun deleteByParentIds(ids: Set<String>)
}
