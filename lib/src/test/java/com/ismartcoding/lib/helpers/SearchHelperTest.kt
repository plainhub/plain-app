package com.ismartcoding.lib.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchHelperTest {
    @Test
    fun parse_fileSize_bytes_opIsKept() {
        val fields = SearchHelper.parse("file_size:>10485760")
        assertEquals(1, fields.size)
        assertEquals("file_size", fields[0].name)
        assertEquals(">", fields[0].op)
        assertEquals("10485760", fields[0].value)
    }

    @Test
    fun parse_fileSize_opIsNeverBlank() {
        val fields = SearchHelper.parse("file_size:>1MB")
        assertEquals(1, fields.size)
        assertEquals("file_size", fields[0].name)
        assertTrue("op should not be blank", fields[0].op.isNotEmpty())
    }

    @Test
    fun parse_fileSize_humanUnit_opIsKept() {
        val fields = SearchHelper.parse("file_size:>10MB")
        assertEquals(1, fields.size)
        assertEquals("file_size", fields[0].name)
        assertEquals(">", fields[0].op)
        assertEquals("10MB", fields[0].value)
    }

    @Test
    fun parse_duration_ge_opIsKept() {
        val fields = SearchHelper.parse("duration:>=60")
        assertEquals(1, fields.size)
        assertEquals("duration", fields[0].name)
        assertEquals(">=", fields[0].op)
        assertEquals("60", fields[0].value)
    }

    @Test
    fun parse_not_doesNotBlankOutOp() {
        val fields = SearchHelper.parse("NOT file_size:>10485760")
        assertEquals(1, fields.size)
        assertEquals("file_size", fields[0].name)
        // Inverted: ">" becomes "<="
        assertEquals("<=", fields[0].op)
        assertEquals("10485760", fields[0].value)
    }

    @Test
    fun parse_plainField_defaultsToEquals() {
        val fields = SearchHelper.parse("type:user")
        assertEquals(1, fields.size)
        assertEquals("type", fields[0].name)
        assertEquals("=", fields[0].op)
        assertEquals("user", fields[0].value)
    }

    @Test
    fun parse_textGroup_hasEmptyOp() {
        val fields = SearchHelper.parse("hello")
        assertEquals(1, fields.size)
        assertEquals("text", fields[0].name)
        assertTrue(fields[0].op.isEmpty())
        assertEquals("hello", fields[0].value)
    }
}
