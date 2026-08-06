package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.db.DBookmark
import com.ismartcoding.plain.db.DBookmarkGroup
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLInput
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@GraphQLType
@Serializable
data class Bookmark(
    val id: ID,
    val url: String,
    val title: String,
    val faviconPath: String,
    val groupId: String,
    val pinned: Boolean,
    val clickCount: Int,
    val lastClickedAt: Instant?,
    val sortOrder: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@GraphQLType
data class BookmarkGroup(
    val id: ID,
    val name: String,
    val collapsed: Boolean,
    val sortOrder: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@GraphQLInput
@Serializable
data class BookmarkInput(
    val url: String,
    val title: String,
    val groupId: String,
    val pinned: Boolean,
    val sortOrder: Int,
)

fun DBookmark.toModel(): Bookmark {
    return Bookmark(
        id = ID(id),
        url = url,
        title = title,
        faviconPath = faviconPath,
        groupId = groupId,
        pinned = pinned,
        clickCount = clickCount,
        lastClickedAt = lastClickedAt,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun DBookmarkGroup.toModel(): BookmarkGroup {
    return BookmarkGroup(
        id = ID(id),
        name = name,
        collapsed = collapsed,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
