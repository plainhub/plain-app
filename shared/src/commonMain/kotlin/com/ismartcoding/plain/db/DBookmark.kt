package com.ismartcoding.plain.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Update
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.helpers.generateId
import kotlin.time.Instant

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["group_id"])],
)
data class DBookmark(
    @PrimaryKey override var id: String = generateId(),
) : IData, DEntityBase() {
    var url: String = ""
    var title: String = ""

    @ColumnInfo(name = "favicon_path")
    var faviconPath: String = ""

    @ColumnInfo(name = "group_id")
    var groupId: String = ""

    var pinned: Boolean = false

    @ColumnInfo(name = "click_count")
    var clickCount: Int = 0

    @ColumnInfo(name = "last_clicked_at")
    var lastClickedAt: Instant? = null

    @ColumnInfo(name = "sort_order")
    var sortOrder: Int = 0
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY pinned DESC, sort_order ASC, created_at ASC")
    fun getAll(): List<DBookmark>

    @Query("SELECT * FROM bookmarks WHERE id = :id")
    fun getById(id: String): DBookmark?

    @Query("SELECT * FROM bookmarks WHERE group_id = :groupId ORDER BY pinned DESC, sort_order ASC, created_at ASC")
    fun getByGroupId(groupId: String): List<DBookmark>

    @RawQuery
    fun search(query: RoomRawQuery): List<DBookmark>

    @RawQuery
    fun count(query: RoomRawQuery): Int

    @Insert
    fun insert(vararg item: DBookmark)

    @Update
    fun update(vararg item: DBookmark)

    @Query("DELETE FROM bookmarks WHERE id IN (:ids)")
    fun delete(ids: Set<String>)

    @Query("DELETE FROM bookmarks WHERE group_id = :groupId")
    fun deleteByGroupId(groupId: String)
}

@Entity(tableName = "bookmark_groups")
data class DBookmarkGroup(
    @PrimaryKey override var id: String = generateId(),
) : IData, DEntityBase() {
    var name: String = ""
    var collapsed: Boolean = false

    @ColumnInfo(name = "sort_order")
    var sortOrder: Int = 0
}

@Dao
interface BookmarkGroupDao {
    @Query("SELECT * FROM bookmark_groups ORDER BY sort_order ASC, created_at ASC")
    fun getAll(): List<DBookmarkGroup>

    @Query("SELECT * FROM bookmark_groups WHERE id = :id")
    fun getById(id: String): DBookmarkGroup?

    @Insert
    fun insert(vararg item: DBookmarkGroup)

    @Update
    fun update(vararg item: DBookmarkGroup)

    @Query("DELETE FROM bookmark_groups WHERE id IN (:ids)")
    fun delete(ids: Set<String>)
}
