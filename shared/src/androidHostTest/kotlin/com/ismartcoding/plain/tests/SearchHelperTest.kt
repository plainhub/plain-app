package com.ismartcoding.plain.tests

import com.ismartcoding.plain.helpers.SearchHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.collections.get

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

    /**
     * The exact tokens the escaper emits. `plain-rs` parses these same strings
     * in `provider_plan_survives_every_token_this_emits`, so a change to either
     * side of the wire shows up as one red test rather than as a search that
     * silently stops matching.
     */
    @Test
    fun buildTextFilter_emitsTokensTheQueryLanguageCanCarry() {
        val cases = listOf(
            "" to "",
            "   " to "",
            "hello" to "hello",
            "hello world" to """hello\ world""",
            "Meeting: notes" to """text:Meeting:\ notes""",
            "http://x.com" to "text:http://x.com",
            "12:30" to "text:12:30",
            "don't" to """don\'t""",
            "a\\b" to """a\\b""",
            "=foo" to "=foo",
            "50%_" to "50%_",
        )
        cases.forEach { (input, expected) ->
            assertEquals("for input <$input>", expected, SearchHelper.buildTextFilter(input))
        }
    }

    @Test
    fun buildTextFilter_survivesItsOwnParser() {
        listOf(
            "hello",
            "hello world",
            "Meeting: notes",
            "http://x.com",
            "12:30",
            "don't",
            "a\\b",
            "=foo",
            "50%_",
        ).forEach { text ->
            val fields = SearchHelper.parse(SearchHelper.buildTextFilter(text))
            assertEquals("for <$text>", 1, fields.size)
            assertEquals("for <$text>", "text", fields[0].name)
            assertEquals("for <$text>", text, fields[0].value)
        }
    }

    @Test
    fun buildTextFilter_keepsTheOtherFiltersAlongside() {
        val query = "${SearchHelper.buildTextFilter("Meeting: notes")} trash:false ids:1,2"
        val fields = SearchHelper.parse(query).associate { it.name to it.value }
        assertEquals("Meeting: notes", fields["text"])
        assertEquals("false", fields["trash"])
        assertEquals("1,2", fields["ids"])
    }
}
