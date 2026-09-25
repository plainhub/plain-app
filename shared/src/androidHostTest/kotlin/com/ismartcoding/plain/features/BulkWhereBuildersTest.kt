package com.ismartcoding.plain.features

import com.ismartcoding.plain.features.feed.FeedEntryHelper
import com.ismartcoding.plain.helpers.ContentWhere
import com.ismartcoding.plain.helpers.QueryHelper
import com.ismartcoding.plain.helpers.SearchHelper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Locks the SQL built for bulk queries — the layer under the GraphQL empty-query guard.
 * Regression of the 2026-09-21 fix: `getTrashedIdsAsync("")` used to skip the WHERE
 * clause entirely, hard-deleting every note (including never-trashed ones).
 */
class BulkWhereBuildersTest {

    @Test
    fun trashedNotesSelectionAlwaysFiltersDeletedAt() {
        // Empty filter on the trashed scope must still constrain to trashed rows.
        val where = ContentWhere()
        where.trash = true
        NoteHelper.applyNotesFilterFields(where, emptyList())
        assertTrue(where.toSelection().contains("deleted_at IS NOT NULL"), where.toSelection())

        // The whole-table sentinel keeps the trashed scope.
        val sentinel = ContentWhere()
        sentinel.trash = true
        NoteHelper.applyNotesFilterFields(sentinel, SearchHelper.parse("all:true"))
        assertTrue(sentinel.toSelection().contains("deleted_at IS NOT NULL"), sentinel.toSelection())
    }

    @Test
    fun nonTrashedNotesSelectionExcludesDeleted() {
        val where = ContentWhere()
        NoteHelper.applyNotesFilterFields(where, emptyList())
        assertTrue(where.toSelection().contains("deleted_at IS NULL"), where.toSelection())
    }

    @Test
    fun feedEntryAllSentinelAddsNoCondition() {
        val where = ContentWhere()
        FeedEntryHelper.applyFeedEntryFilterFields(where, SearchHelper.parse("all:true"))
        assertEquals("1=1", where.toSelection())
        assertTrue(where.args.isEmpty())
    }

    @Test
    fun feedEntryTextFilterStillConstrains() {
        val where = ContentWhere()
        FeedEntryHelper.applyFeedEntryFilterFields(where, SearchHelper.parse("text:foo"))
        assertTrue(where.toSelection().contains("LIKE"), where.toSelection())
    }

    @Test
    fun clipboardAllSentinelAddsNoCondition() {
        val where = ContentWhere()
        ClipboardHelper.applyClipboardFilterFields(where, SearchHelper.parse("all:true"))
        assertEquals("1=1", where.toSelection())
        assertTrue(where.args.isEmpty())
    }

    @Test
    fun clipboardIdsFilterConstrainsToIdSet() {
        val where = ContentWhere()
        ClipboardHelper.applyClipboardFilterFields(where, SearchHelper.parse("ids:a,b"))
        assertEquals("id IN (?,?)", where.toSelection())
        assertEquals(listOf("a", "b"), where.args)
    }

    @Test
    fun clipboardTextFilterLikesTextColumn() {
        val where = ContentWhere()
        ClipboardHelper.applyClipboardFilterFields(where, SearchHelper.parse("text:foo"))
        assertTrue(where.toSelection().contains("text LIKE"), where.toSelection())
        assertEquals(listOf("foo"), where.args)
    }

    @Test
    fun clipboardTextFilterEscapesLikeWildcards() {
        // User input must match literally — % / _ are escaped and the SQL carries ESCAPE '\'.
        val where = ContentWhere()
        ClipboardHelper.applyClipboardFilterFields(where, SearchHelper.parse("text:\"100%_a\""))
        assertTrue(where.toSelection().contains("ESCAPE '\\'"), where.toSelection())
        assertEquals(listOf("100\\%\\_a"), where.args)
    }

    @Test
    fun clipboardHistorySearchEscapesWildcardsOnAllColumns() {
        val where = ContentWhere()
        ClipboardHelper.applyClipboardSearch(where, "a_b%c")
        assertTrue(where.toSelection().contains("ESCAPE '\\'"), where.toSelection())
        assertEquals(listOf("a\\_b\\%c", "a\\_b\\%c", "a\\_b\\%c"), where.args)
    }

    @Test
    fun clipboardUnknownFieldAddsNoCondition() {
        // The clipboard seam serves only ids:/text:/all — other DSL fields must not silently widen the filter.
        val where = ContentWhere()
        ClipboardHelper.applyClipboardFilterFields(where, SearchHelper.parse("source:abc"))
        assertEquals("1=1", where.toSelection())
        assertTrue(where.args.isEmpty())
    }
}
