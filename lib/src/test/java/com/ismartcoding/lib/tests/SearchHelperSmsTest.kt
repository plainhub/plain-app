package com.ismartcoding.lib.tests

import com.ismartcoding.lib.helpers.SearchHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchHelperSmsTest {
    @Test
    fun parse_threadId_field() {
        val fields = SearchHelper.parse("thread_id:17")
        assertEquals(1, fields.size)
        assertEquals("thread_id", fields[0].name)
        assertEquals("=", fields[0].op)
        assertEquals("17", fields[0].value)
    }

    @Test
    fun parse_threadId_with_hidden() {
        val fields = SearchHelper.parse("thread_id:17 hidden:1")
        assertEquals(2, fields.size)
        val threadField = fields.first { it.name == "thread_id" }
        assertEquals("17", threadField.value)
        val hiddenField = fields.first { it.name == "hidden" }
        assertEquals("1", hiddenField.value)
    }

    @Test
    fun parse_type_filter() {
        val fields = SearchHelper.parse("type:1")
        assertEquals(1, fields.size)
        assertEquals("type", fields[0].name)
        assertEquals("=", fields[0].op)
        assertEquals("1", fields[0].value)
    }

    @Test
    fun parse_text_search_bare_word() {
        val fields = SearchHelper.parse("hello")
        assertEquals(1, fields.size)
        assertEquals("text", fields[0].name)
        assertTrue(fields[0].op.isEmpty())
        assertEquals("hello", fields[0].value)
    }

    @Test
    fun parse_text_search_quoted() {
        val fields = SearchHelper.parse("\"hello world\"")
        assertEquals(1, fields.size)
        assertEquals("text", fields[0].name)
        assertTrue(fields[0].op.isEmpty())
        assertEquals("hello world", fields[0].value)
    }

    @Test
    fun parse_ids_filter() {
        val fields = SearchHelper.parse("ids:1,2,3")
        assertEquals(1, fields.size)
        assertEquals("ids", fields[0].name)
        assertEquals("1,2,3", fields[0].value)
    }

    @Test
    fun parse_empty_returns_empty() {
        val fields = SearchHelper.parse("")
        assertTrue(fields.isEmpty())
    }

    @Test
    fun parse_tag_id_filter() {
        val fields = SearchHelper.parse("tag_id:abc")
        assertEquals(1, fields.size)
        assertEquals("tag_id", fields[0].name)
        assertEquals("abc", fields[0].value)
    }
}
