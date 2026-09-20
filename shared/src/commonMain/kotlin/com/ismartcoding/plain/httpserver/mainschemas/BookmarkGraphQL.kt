package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.events.FetchBookmarkMetadataEvent
import com.ismartcoding.plain.features.BookmarkHelper
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.httpserver.models.ActionResult
import com.ismartcoding.plain.httpserver.models.Bookmark
import com.ismartcoding.plain.httpserver.models.BookmarkGroup
import com.ismartcoding.plain.httpserver.models.BookmarkInput
import com.ismartcoding.plain.httpserver.models.ID
import com.ismartcoding.plain.httpserver.models.toModel

@GraphQLQuery
suspend fun bookmarks(): List<Bookmark> {
    return BookmarkHelper.getAll().map { it.toModel() }
}

@GraphQLQuery
suspend fun bookmarkGroups(): List<BookmarkGroup> {
    val counts = AppDatabase.instance.bookmarkDao().getGroupItemCount().associate { it.groupId to it.itemCount }
    return BookmarkHelper.getAllGroups().map { it.toModel(counts[it.id] ?: 0) }
}

@GraphQLMutation
suspend fun addBookmarks(urls: List<String>, groupId: ID): List<Bookmark> {
    val created = BookmarkHelper.addBookmarks(urls, groupId.value)
    created.forEach { b -> sendEvent(FetchBookmarkMetadataEvent(b.id, b.url)) }
    return created.map { it.toModel() }
}

@GraphQLMutation
suspend fun updateBookmark(id: ID, input: BookmarkInput): Bookmark {
    return BookmarkHelper.updateBookmark(id.value) {
        this.url = input.url
        this.title = input.title
        this.groupId = input.groupId.value
        this.pinned = input.pinned
        this.sortOrder = input.sortOrder
    }?.toModel() ?: throw GraphQLError("Bookmark ${id.value} not found")
}

@GraphQLMutation
suspend fun deleteBookmarks(ids: List<ID>): ActionResult {
    val deleted = BookmarkHelper.deleteBookmarks(ids.map { it.value }.toSet())
    return ActionResult(deleted)
}

@GraphQLMutation
suspend fun recordBookmarkClick(id: ID): Boolean {
    BookmarkHelper.recordClick(id.value)
    return true
}

@GraphQLMutation
suspend fun createBookmarkGroup(name: String): BookmarkGroup {
    return BookmarkHelper.createGroup(name).toModel(0)
}

@GraphQLMutation
suspend fun updateBookmarkGroup(id: ID, name: String, collapsed: Boolean, sortOrder: Int): BookmarkGroup {
    return BookmarkHelper.updateGroup(id.value) {
        this.name = name
        this.collapsed = collapsed
        this.sortOrder = sortOrder
    }?.let { group ->
        val counts = AppDatabase.instance.bookmarkDao().getGroupItemCount().associate { it.groupId to it.itemCount }
        group.toModel(counts[group.id] ?: 0)
    } ?: throw GraphQLError("Bookmark group ${id.value} not found")
}

@GraphQLMutation
suspend fun deleteBookmarkGroup(id: ID): Boolean {
    BookmarkHelper.deleteGroup(id.value)
    return true
}

fun SchemaBuilder.addBookmarkSchema() {
}
