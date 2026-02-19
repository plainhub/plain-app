package com.ismartcoding.plain.db

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.ismartcoding.lib.helpers.StringHelper
import com.ismartcoding.plain.data.IData
import kotlin.time.Instant

/**
 * A single bookmarked URL.
 *
 * Table design:
 *  - id            : Short UUID primary key
 *  - url           : The bookmark URL (required)
 *  - title         : User-editable title, initially fetched from the page
 *  - favicon_path  : Local file path to the stored favicon image
 *  - group_id      : FK reference to bookmark_groups.id, empty = ungrouped
 *  - pinned        : Whether the bookmark is pinned to the top
 *  - click_count   : Used for "sort by recent click" (bumped on open)
 *  - last_clicked_at : Timestamp of the most recent click
 *  - sort_order    : Manual sort order within group
 *  - created_at / updated_at : from DEntityBase
 */
@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["group_id"])],
)
data class DBookmark(
    @PrimaryKey override var id: String = StringHelper.shortUUID(),
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
    fun search(query: SupportSQLiteQuery): List<DBookmark>

    @RawQuery
    fun count(query: SupportSQLiteQuery): Int

    @Insert
    fun insert(vararg item: DBookmark)

    @Update
    fun update(vararg item: DBookmark)

    @Query("DELETE FROM bookmarks WHERE id IN (:ids)")
    fun delete(ids: Set<String>)

    @Query("DELETE FROM bookmarks WHERE group_id = :groupId")
    fun deleteByGroupId(groupId: String)
}

/**
 * A bookmark group (folder).
 *
 * Table design:
 *  - id         : Short UUID primary key
 *  - name       : Display name
 *  - collapsed  : Whether the group is currently collapsed in UI
 *  - sort_order : Display order of the group
 *  - created_at / updated_at : from DEntityBase
 */
@Entity(tableName = "bookmark_groups")
data class DBookmarkGroup(
    @PrimaryKey override var id: String = StringHelper.shortUUID(),
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
