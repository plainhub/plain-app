package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.channel.sendEvent
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
        resolver("urls", "groupId") { urls: List<String>, groupId: String ->
            val created = BookmarkHelper.addBookmarks(urls, groupId)
            created.forEach { b -> sendEvent(FetchBookmarkMetadataEvent(b.id, b.url)) }
            created.map { it.toModel() }
        }
    }
    mutation("updateBookmark") {
        resolver("id", "input") { id: ID, input: BookmarkInput ->
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
        resolver("ids") { ids: List<ID> ->
            BookmarkHelper.deleteBookmarks(ids.map { it.value }.toSet())
            true
        }
    }
    mutation("recordBookmarkClick") {
        resolver("id") { id: ID ->
            BookmarkHelper.recordClick(id.value)
            true
        }
    }
    mutation("createBookmarkGroup") {
        resolver("name") { name: String ->
            BookmarkHelper.createGroup(name).toModel()
        }
    }
    mutation("updateBookmarkGroup") {
        resolver("id", "name", "collapsed", "sortOrder") { id: ID, name: String, collapsed: Boolean, sortOrder: Int ->
            BookmarkHelper.updateGroup(id.value) {
                this.name = name
                this.collapsed = collapsed
                this.sortOrder = sortOrder
            }?.toModel()
        }
    }
    mutation("deleteBookmarkGroup") {
        resolver("id") { id: ID ->
            BookmarkHelper.deleteGroup(id.value)
            true
        }
    }
}
