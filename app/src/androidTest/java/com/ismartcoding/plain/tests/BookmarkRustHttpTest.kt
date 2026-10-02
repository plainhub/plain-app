package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.features.BookmarkHelper
import com.ismartcoding.plain.platform.AppDatabase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class BookmarkRustHttpTest {
    @Test
    fun bookmarksUseRustAndKeepClicksWhenEditing() = runBlocking {
        val marker = "bookmark-rust-${UUID.randomUUID()}"
        val group = BookmarkHelper.createGroup(marker)
        val ids = mutableSetOf<String>()
        try {
            val bookmark = BookmarkHelper.addBookmarks(listOf("https://example.invalid/$marker", " "), group.id).single()
            ids += bookmark.id
            assertNull(AppDatabase.instance.bookmarkDao().getById(bookmark.id))
            assertNull(AppDatabase.instance.bookmarkGroupDao().getById(group.id))
            coroutineScope { (0 until 12).map { async { BookmarkHelper.recordClick(bookmark.id) } }.awaitAll() }
            val updated = checkNotNull(BookmarkHelper.updateBookmark(bookmark.id) { title = marker; pinned = true; sortOrder = 7 })
            assertEquals(12, updated.clickCount)
            assertNotNull(updated.lastClickedAt)
            assertEquals(marker, updated.title)
            assertTrue(updated.pinned)
            assertEquals(7, updated.sortOrder)
            assertEquals(1, BookmarkHelper.groupItemCounts()[group.id])
            assertTrue(checkNotNull(BookmarkHelper.updateGroup(group.id) { collapsed = true }).collapsed)
            BookmarkHelper.deleteGroup(group.id)
            assertNull(BookmarkHelper.getGroupById(group.id))
            assertEquals("", checkNotNull(BookmarkHelper.getById(bookmark.id)).groupId)
            assertEquals(0, BookmarkHelper.deleteBookmarks(emptySet()))
            assertEquals(1, BookmarkHelper.deleteBookmarks(ids))
            assertNull(BookmarkHelper.getById(bookmark.id))
        } finally {
            BookmarkHelper.deleteBookmarks(ids)
            BookmarkHelper.deleteGroup(group.id)
        }
    }
}
