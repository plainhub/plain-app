package com.ismartcoding.plain.db

import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.generateId
import kotlin.time.Instant

data class DBookmark(
    override var id: String = generateId(),
    var url: String = "",
    var title: String = "",

    var faviconPath: String = "",

    var groupId: String = "",

    var pinned: Boolean = false,

    var clickCount: Int = 0,

    var lastClickedAt: Instant? = null,

    var sortOrder: Int = 0,

    var createdAt: Instant = TimeHelper.now(),

    var updatedAt: Instant = TimeHelper.now(),
) : IData

data class DBookmarkGroup(
    override var id: String = generateId(),
    var name: String = "",
    var collapsed: Boolean = false,

    var sortOrder: Int = 0,

    var createdAt: Instant = TimeHelper.now(),

    var updatedAt: Instant = TimeHelper.now(),
) : IData

/** Aggregate row for BookmarkGroup.itemCount. */
data class BookmarkGroupItemCount(
    val groupId: String,
    val itemCount: Int,
)
