package com.ismartcoding.plain.features

import com.ismartcoding.plain.api.*
import com.ismartcoding.plain.db.DBookmark
import com.ismartcoding.plain.db.DBookmarkGroup
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*

object BookmarkHelper {
    private const val FIELDS = "id url title faviconPath groupId pinned clickCount lastClickedAt sortOrder createdAt updatedAt"
    private const val GROUP_FIELDS = "id name collapsed sortOrder itemCount createdAt updatedAt"

    suspend fun getAll(): List<DBookmark> = RustContentApi.query("bookmarks { $FIELDS }")
        .getValue("bookmarks").jsonArray.map { it.bookmark() }
    suspend fun getById(id: String): DBookmark? = getAll().find { it.id == id }
    suspend fun getAllGroups(): List<DBookmarkGroup> = groupRows().map { it.group() }
    suspend fun getGroupById(id: String): DBookmarkGroup? = getAllGroups().find { it.id == id }
    suspend fun groupItemCounts(): Map<String, Int> = groupRows().associate {
        it.jsonObject.string("id") to it.jsonObject.getValue("itemCount").jsonPrimitive.int
    }
    private suspend fun groupRows(): JsonArray = RustContentApi.query("bookmarkGroups { $GROUP_FIELDS }")
        .getValue("bookmarkGroups").jsonArray

    suspend fun addBookmarks(urls: List<String>, groupId: String = ""): List<DBookmark> =
        RustContentApi.mutate("addBookmarks(urls: ${gqlIds(urls)}, groupId: ${gql(groupId)}) { $FIELDS }")
            .getValue("addBookmarks").jsonArray.map { it.bookmark() }

    suspend fun updateBookmark(id: String, block: DBookmark.() -> Unit): DBookmark? {
        val row = getById(id)?.apply(block) ?: return null
        val input = "{url: ${gql(row.url)}, title: ${gql(row.title)}, groupId: ${gql(row.groupId)}, pinned: ${row.pinned}, sortOrder: ${row.sortOrder}}"
        return RustContentApi.mutate("updateBookmark(id: ${gql(id)}, input: $input) { $FIELDS }")
            .getValue("updateBookmark").bookmark()
    }

    suspend fun deleteBookmarks(ids: Set<String>): Int {
        if (ids.isEmpty()) return 0
        return RustContentApi.mutate("deleteBookmarks(ids: ${gqlIds(ids)}) { affectedCount }")
            .getValue("deleteBookmarks").jsonObject.getValue("affectedCount").jsonPrimitive.int
    }
    suspend fun recordClick(id: String) { RustContentApi.mutate("recordBookmarkClick(id: ${gql(id)})") }
    suspend fun createGroup(name: String): DBookmarkGroup =
        RustContentApi.mutate("createBookmarkGroup(name: ${gql(name)}) { $GROUP_FIELDS }").getValue("createBookmarkGroup").group()
    suspend fun updateGroup(id: String, block: DBookmarkGroup.() -> Unit): DBookmarkGroup? {
        val row = getGroupById(id)?.apply(block) ?: return null
        return RustContentApi.mutate("updateBookmarkGroup(id: ${gql(id)}, name: ${gql(row.name)}, collapsed: ${row.collapsed}, sortOrder: ${row.sortOrder}) { $GROUP_FIELDS }")
            .getValue("updateBookmarkGroup").group()
    }
    suspend fun deleteGroup(id: String) { RustContentApi.mutate("deleteBookmarkGroup(id: ${gql(id)})") }
    suspend fun fetchAndUpdateSingle(bookmarkId: String): DBookmark? = try {
        RustContentApi.mutate("fetchBookmarkMetadata(id: ${gql(bookmarkId)}) { $FIELDS }")
            .getValue("fetchBookmarkMetadata").takeUnless { it is JsonNull }?.bookmark()
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (e: Exception) { LogCat.e("Bookmark metadata: ${e.message}"); null }
    suspend fun fetchMetadataAsync(bookmarkIds: List<String>) = coroutineScope {
        bookmarkIds.map { id -> async { fetchAndUpdateSingle(id) } }.awaitAll()
    }

    private fun JsonElement.bookmark(): DBookmark = jsonObject.let {
        DBookmark(id = it.string("id"), url = it.string("url"), title = it.string("title"), faviconPath = it.string("faviconPath"),
            groupId = it.string("groupId"), pinned = it.getValue("pinned").jsonPrimitive.boolean,
            clickCount = it.getValue("clickCount").jsonPrimitive.int, lastClickedAt = it.nullableInstant("lastClickedAt"),
            sortOrder = it.getValue("sortOrder").jsonPrimitive.int, createdAt = it.instant("createdAt"), updatedAt = it.instant("updatedAt"))
    }
    private fun JsonElement.group(): DBookmarkGroup = jsonObject.let {
        DBookmarkGroup(id = it.string("id"), name = it.string("name"), collapsed = it.getValue("collapsed").jsonPrimitive.boolean,
            sortOrder = it.getValue("sortOrder").jsonPrimitive.int, createdAt = it.instant("createdAt"), updatedAt = it.instant("updatedAt"))
    }
}
