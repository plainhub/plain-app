package com.ismartcoding.plain.web.schemas

import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.events.FetchBookmarkMetadataEvent
import com.ismartcoding.plain.features.BookmarkHelper
import com.ismartcoding.plain.web.models.BookmarkInput
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addBookmarkSchema() {
    query("bookmarks") {
        resolver { ->
            BookmarkHelper.getAll().map { it.toModel() }
        }
    }
    query("bookmarkGroups") {
        resolver { ->
            BookmarkHelper.getAllGroups().map { it.toModel() }
        }
    }
    mutation("addBookmarks") {
        resolver { urls: List<String>, groupId: String ->
            val created = BookmarkHelper.addBookmarks(urls, groupId)
            created.forEach { b -> sendEvent(FetchBookmarkMetadataEvent(b.id, b.url)) }
            created.map { it.toModel() }
        }
    }
    mutation("updateBookmark") {
        resolver { id: ID, input: BookmarkInput ->
            BookmarkHelper.updateBookmark(id.value) {
                this.url = input.url
                this.title = input.title
                this.groupId = input.groupId
                this.pinned = input.pinned
                this.sortOrder = input.sortOrder
            }?.toModel()
        }
    }
    mutation("deleteBookmarks") {
        resolver { ids: List<ID> ->
            BookmarkHelper.deleteBookmarks(ids.map { it.value }.toSet(), MainApp.instance)
            true
        }
    }
    mutation("recordBookmarkClick") {
        resolver { id: ID ->
            BookmarkHelper.recordClick(id.value)
            true
        }
    }
    mutation("createBookmarkGroup") {
        resolver { name: String ->
            BookmarkHelper.createGroup(name).toModel()
        }
    }
    mutation("updateBookmarkGroup") {
        resolver { id: ID, name: String, collapsed: Boolean, sortOrder: Int ->
            BookmarkHelper.updateGroup(id.value) {
                this.name = name
                this.collapsed = collapsed
                this.sortOrder = sortOrder
            }?.toModel()
        }
    }
    mutation("deleteBookmarkGroup") {
        resolver { id: ID ->
            BookmarkHelper.deleteGroup(id.value)
            true
        }
    }
}
