package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.events.FetchBookmarkMetadataEvent
import com.ismartcoding.plain.features.BookmarkHelper
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.web.models.Bookmark
import com.ismartcoding.plain.web.models.BookmarkGroup
import com.ismartcoding.plain.web.models.BookmarkInput
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.toModel

@GraphQLQuery
suspend fun bookmarks(): List<Bookmark> {
    return BookmarkHelper.getAll().map { it.toModel() }
}

@GraphQLQuery
suspend fun bookmarkGroups(): List<BookmarkGroup> {
    return BookmarkHelper.getAllGroups().map { it.toModel() }
}

@GraphQLMutation
suspend fun addBookmarks(urls: List<String>, groupId: String): List<Bookmark> {
    val created = BookmarkHelper.addBookmarks(urls, groupId)
    created.forEach { b -> sendEvent(FetchBookmarkMetadataEvent(b.id, b.url)) }
    return created.map { it.toModel() }
}

@GraphQLMutation
suspend fun updateBookmark(id: ID, input: BookmarkInput): Bookmark? {
    return BookmarkHelper.updateBookmark(id.value) {
        this.url = input.url
        this.title = input.title
        this.groupId = input.groupId
        this.pinned = input.pinned
        this.sortOrder = input.sortOrder
    }?.toModel()
}

@GraphQLMutation
suspend fun deleteBookmarks(ids: List<ID>): Boolean {
    BookmarkHelper.deleteBookmarks(ids.map { it.value }.toSet())
    return true
}

@GraphQLMutation
suspend fun recordBookmarkClick(id: ID): Boolean {
    BookmarkHelper.recordClick(id.value)
    return true
}

@GraphQLMutation
suspend fun createBookmarkGroup(name: String): BookmarkGroup {
    return BookmarkHelper.createGroup(name).toModel()
}

@GraphQLMutation
suspend fun updateBookmarkGroup(id: ID, name: String, collapsed: Boolean, sortOrder: Int): BookmarkGroup? {
    return BookmarkHelper.updateGroup(id.value) {
        this.name = name
        this.collapsed = collapsed
        this.sortOrder = sortOrder
    }?.toModel()
}

@GraphQLMutation
suspend fun deleteBookmarkGroup(id: ID): Boolean {
    BookmarkHelper.deleteGroup(id.value)
    return true
}

fun SchemaBuilder.addBookmarkSchema() {
}
